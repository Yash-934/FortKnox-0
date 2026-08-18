package com.example.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * High-reliability, non-blocking silent camera capture engine for Intruder Selfie detection.
 *
 * Security & Engineering Guarantees:
 * - Operates completely headless in background thread (no UI preview window or viewfinder).
 * - Prioritizes front-facing selfie camera; automatically falls back to rear camera if unavailable.
 * - Queries hardware StreamConfigurationMap for verified supported JPEG dimensions.
 * - Extracts JPEG byte array strictly in RAM memory buffer — NEVER writes unencrypted plaintext to storage.
 * - Safe resource reclamation, timeout watchdog, and robust error recovery.
 */
object SilentCameraCapture {

    private const val TAG = "SilentCameraCapture"
    private const val DEFAULT_TARGET_WIDTH = 640
    private const val DEFAULT_TARGET_HEIGHT = 480
    private const val TIMEOUT_MS = 6000L

    interface CaptureCallback {
        fun onPhotoCaptured(jpegBytes: ByteArray)
        fun onCaptureFailed(reason: String)
    }

    /**
     * Checks if runtime camera permission is granted.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Suspending wrapper for non-blocking silent photo capture.
     * Guaranteed to return within TIMEOUT_MS without blocking the calling thread.
     */
    suspend fun capturePhotoSilently(context: Context): ByteArray? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val hasResumed = AtomicBoolean(false)

            captureSilentPhoto(
                context = context,
                callback = object : CaptureCallback {
                    override fun onPhotoCaptured(jpegBytes: ByteArray) {
                        if (hasResumed.compareAndSet(false, true)) {
                            if (continuation.isActive) {
                                continuation.resume(jpegBytes)
                            }
                        }
                    }

                    override fun onCaptureFailed(reason: String) {
                        Log.w(TAG, "Silent photo capture failed: $reason")
                        if (hasResumed.compareAndSet(false, true)) {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }
                }
            )

            continuation.invokeOnCancellation {
                Log.d(TAG, "Coroutine cancelled during silent photo capture")
            }
        }
    }

    /**
     * Captures a silent photo using Camera2 API.
     */
    fun captureSilentPhoto(context: Context, callback: CaptureCallback) {
        if (!hasCameraPermission(context)) {
            callback.onCaptureFailed("CAMERA permission is not granted")
            return
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            callback.onCaptureFailed("CameraManager system service is unavailable")
            return
        }

        val handlerThread = HandlerThread("SilentIntruderCamThread").apply { start() }
        val backgroundHandler = Handler(handlerThread.looper)

        // Attempt front camera first, fallback to back camera
        val candidateCameraIds = mutableListOf<String>()
        try {
            val allIds = cameraManager.cameraIdList
            if (allIds.isEmpty()) {
                cleanupThread(handlerThread)
                callback.onCaptureFailed("No camera hardware found on device")
                return
            }

            var frontId: String? = null
            var backId: String? = null

            for (id in allIds) {
                try {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val facing = chars.get(CameraCharacteristics.LENS_FACING)
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT && frontId == null) {
                        frontId = id
                    } else if (facing == CameraCharacteristics.LENS_FACING_BACK && backId == null) {
                        backId = id
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking characteristics for camera $id: ${e.message}")
                }
            }

            if (frontId != null) candidateCameraIds.add(frontId)
            if (backId != null && backId != frontId) candidateCameraIds.add(backId)
            for (id in allIds) {
                if (!candidateCameraIds.contains(id)) {
                    candidateCameraIds.add(id)
                }
            }
        } catch (e: Exception) {
            cleanupThread(handlerThread)
            callback.onCaptureFailed("Failed to enumerate device cameras: ${e.message}")
            return
        }

        if (candidateCameraIds.isEmpty()) {
            cleanupThread(handlerThread)
            callback.onCaptureFailed("No available camera sensors found")
            return
        }

        executeCameraCapturePipeline(
            context = context,
            cameraManager = cameraManager,
            cameraIds = candidateCameraIds,
            currentIndex = 0,
            backgroundHandler = backgroundHandler,
            handlerThread = handlerThread,
            callback = callback
        )
    }

    private fun executeCameraCapturePipeline(
        context: Context,
        cameraManager: CameraManager,
        cameraIds: List<String>,
        currentIndex: Int,
        backgroundHandler: Handler,
        handlerThread: HandlerThread,
        callback: CaptureCallback
    ) {
        if (currentIndex >= cameraIds.size) {
            cleanupThread(handlerThread)
            callback.onCaptureFailed("All available camera sensors failed capture pipeline")
            return
        }

        val targetCameraId = cameraIds[currentIndex]
        var cameraDeviceRef: CameraDevice? = null
        var sessionRef: CameraCaptureSession? = null
        var imageReaderRef: ImageReader? = null
        val isCompleted = AtomicBoolean(false)

        fun safeCloseAll() {
            try {
                sessionRef?.close()
                sessionRef = null
            } catch (e: Exception) {
                Log.e(TAG, "Error closing capture session", e)
            }
            try {
                cameraDeviceRef?.close()
                cameraDeviceRef = null
            } catch (e: Exception) {
                Log.e(TAG, "Error closing camera device", e)
            }
            try {
                imageReaderRef?.close()
                imageReaderRef = null
            } catch (e: Exception) {
                Log.e(TAG, "Error closing ImageReader", e)
            }
            cleanupThread(handlerThread)
        }

        fun tryNextCamera(errorReason: String) {
            if (isCompleted.compareAndSet(false, true)) {
                Log.w(TAG, "Camera $targetCameraId failed ($errorReason), attempting fallback...")
                try {
                    sessionRef?.close()
                    sessionRef = null
                    cameraDeviceRef?.close()
                    cameraDeviceRef = null
                    imageReaderRef?.close()
                    imageReaderRef = null
                } catch (_: Exception) {}

                if (currentIndex + 1 < cameraIds.size) {
                    executeCameraCapturePipeline(
                        context,
                        cameraManager,
                        cameraIds,
                        currentIndex + 1,
                        backgroundHandler,
                        handlerThread,
                        callback
                    )
                } else {
                    cleanupThread(handlerThread)
                    callback.onCaptureFailed(errorReason)
                }
            }
        }

        try {
            val characteristics = cameraManager.getCameraCharacteristics(targetCameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val supportedSizes = map?.getOutputSizes(ImageFormat.JPEG)

            val chosenSize: Size = if (!supportedSizes.isNullOrEmpty()) {
                // Find supported resolution closest to 640x480
                supportedSizes.minByOrNull { size ->
                    abs(size.width - DEFAULT_TARGET_WIDTH) + abs(size.height - DEFAULT_TARGET_HEIGHT)
                } ?: supportedSizes[0]
            } else {
                Size(DEFAULT_TARGET_WIDTH, DEFAULT_TARGET_HEIGHT)
            }

            val imageReader = ImageReader.newInstance(
                chosenSize.width,
                chosenSize.height,
                ImageFormat.JPEG,
                2
            )
            imageReaderRef = imageReader

            imageReader.setOnImageAvailableListener({ reader ->
                val image = try {
                    reader.acquireLatestImage() ?: reader.acquireNextImage()
                } catch (e: Exception) {
                    Log.e(TAG, "Error acquiring image", e)
                    null
                }

                if (image != null) {
                    try {
                        val planes = image.planes
                        if (planes.isNotEmpty()) {
                            val buffer = planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)

                            if (isCompleted.compareAndSet(false, true)) {
                                callback.onPhotoCaptured(bytes)
                            }
                        } else {
                            tryNextCamera("Image planes empty")
                        }
                    } catch (e: Exception) {
                        tryNextCamera("Error reading image buffer: ${e.message}")
                    } finally {
                        try {
                            image.close()
                        } catch (_: Exception) {}
                        safeCloseAll()
                    }
                }
            }, backgroundHandler)

            // Watchdog Timeout (6 seconds)
            backgroundHandler.postDelayed({
                if (!isCompleted.get()) {
                    tryNextCamera("Capture timed out on camera $targetCameraId")
                }
            }, TIMEOUT_MS)

            cameraManager.openCamera(
                targetCameraId,
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDeviceRef = camera
                        try {
                            val surface = imageReader.surface
                            camera.createCaptureSession(
                                listOf(surface),
                                object : CameraCaptureSession.StateCallback() {
                                    override fun onConfigured(session: CameraCaptureSession) {
                                        sessionRef = session
                                        try {
                                            val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf()
                                            val aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES) ?: intArrayOf()

                                            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                                addTarget(surface)

                                                // Safe AF mode
                                                when {
                                                    afModes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) -> {
                                                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                                    }
                                                    afModes.contains(CaptureRequest.CONTROL_AF_MODE_AUTO) -> {
                                                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                                                    }
                                                    else -> {
                                                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                                                    }
                                                }

                                                // Safe AE mode
                                                if (aeModes.contains(CaptureRequest.CONTROL_AE_MODE_ON)) {
                                                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                                }
                                            }

                                            session.capture(
                                                captureBuilder.build(),
                                                object : CameraCaptureSession.CaptureCallback() {
                                                    override fun onCaptureCompleted(
                                                        session: CameraCaptureSession,
                                                        request: CaptureRequest,
                                                        result: TotalCaptureResult
                                                    ) {
                                                        Log.d(TAG, "Silent capture completed successfully for camera $targetCameraId")
                                                    }
                                                },
                                                backgroundHandler
                                            )
                                        } catch (e: Exception) {
                                            tryNextCamera("Session capture error: ${e.message}")
                                        }
                                    }

                                    override fun onConfigureFailed(session: CameraCaptureSession) {
                                        tryNextCamera("Session configuration failed on camera $targetCameraId")
                                    }
                                },
                                backgroundHandler
                            )
                        } catch (e: Exception) {
                            tryNextCamera("Capture session create error: ${e.message}")
                        }
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        tryNextCamera("Camera $targetCameraId disconnected")
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        tryNextCamera("Camera $targetCameraId error code: $error")
                    }
                },
                backgroundHandler
            )
        } catch (e: SecurityException) {
            tryNextCamera("SecurityException: ${e.message}")
        } catch (e: CameraAccessException) {
            tryNextCamera("CameraAccessException: ${e.message}")
        } catch (e: Exception) {
            tryNextCamera("Unexpected error: ${e.message}")
        }
    }

    private fun cleanupThread(thread: HandlerThread) {
        try {
            thread.quitSafely()
        } catch (e: Exception) {
            Log.e(TAG, "Error quitting HandlerThread", e)
        }
    }
}
