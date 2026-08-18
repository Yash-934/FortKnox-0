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
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object SilentCameraCapture {

    private const val TAG = "SilentCameraCapture"

    interface CaptureCallback {
        fun onPhotoCaptured(jpegBytes: ByteArray)
        fun onCaptureFailed(reason: String)
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun capturePhotoSilently(context: Context): ByteArray? = suspendCancellableCoroutine { continuation ->
        captureSilentPhoto(
            context,
            object : CaptureCallback {
                override fun onPhotoCaptured(jpegBytes: ByteArray) {
                    if (continuation.isActive) {
                        continuation.resume(jpegBytes)
                    }
                }

                override fun onCaptureFailed(reason: String) {
                    Log.w(TAG, "Silent photo capture failed: $reason")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        )
    }

    fun captureSilentPhoto(context: Context, callback: CaptureCallback) {
        if (!hasCameraPermission(context)) {
            callback.onCaptureFailed("Camera permission not granted")
            return
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            callback.onCaptureFailed("Camera service unavailable")
            return
        }

        val handlerThread = HandlerThread("SilentCameraBackground").apply { start() }
        val backgroundHandler = Handler(handlerThread.looper)

        try {
            var targetCameraId: String? = null
            // Prioritize front camera
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    targetCameraId = id
                    break
                }
            }

            // Fallback to first available camera if no front camera
            if (targetCameraId == null && cameraManager.cameraIdList.isNotEmpty()) {
                targetCameraId = cameraManager.cameraIdList[0]
            }

            if (targetCameraId == null) {
                cleanupThread(handlerThread)
                callback.onCaptureFailed("No camera hardware found on device")
                return
            }

            val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2)
            var cameraDeviceRef: CameraDevice? = null
            var sessionRef: CameraCaptureSession? = null

            fun safeClose() {
                try {
                    sessionRef?.close()
                    sessionRef = null
                    cameraDeviceRef?.close()
                    cameraDeviceRef = null
                    imageReader.close()
                    cleanupThread(handlerThread)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during camera cleanup", e)
                }
            }

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    try {
                        val planes = image.planes
                        if (planes.isNotEmpty()) {
                            val buffer = planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            callback.onPhotoCaptured(bytes)
                        } else {
                            callback.onCaptureFailed("Empty image plane buffer")
                        }
                    } catch (e: Exception) {
                        callback.onCaptureFailed("Buffer read error: ${e.message}")
                    } finally {
                        image.close()
                        safeClose()
                    }
                }
            }, backgroundHandler)

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
                                            val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                                addTarget(surface)
                                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                            }
                                            session.capture(captureBuilder.build(), null, backgroundHandler)
                                        } catch (e: Exception) {
                                            callback.onCaptureFailed("Capture request failed: ${e.message}")
                                            safeClose()
                                        }
                                    }

                                    override fun onConfigureFailed(session: CameraCaptureSession) {
                                        callback.onCaptureFailed("Session configuration failed")
                                        safeClose()
                                    }
                                },
                                backgroundHandler
                            )
                        } catch (e: Exception) {
                            callback.onCaptureFailed("Capture session creation error: ${e.message}")
                            safeClose()
                        }
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        safeClose()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        callback.onCaptureFailed("Camera device error: $error")
                        safeClose()
                    }
                },
                backgroundHandler
            )

            // Safety timeout: 5 seconds
            backgroundHandler.postDelayed({
                if (cameraDeviceRef != null) {
                    callback.onCaptureFailed("Camera capture timed out")
                    safeClose()
                }
            }, 5000L)

        } catch (e: SecurityException) {
            cleanupThread(handlerThread)
            callback.onCaptureFailed("Security exception: ${e.message}")
        } catch (e: CameraAccessException) {
            cleanupThread(handlerThread)
            callback.onCaptureFailed("Camera access exception: ${e.message}")
        } catch (e: Exception) {
            cleanupThread(handlerThread)
            callback.onCaptureFailed("Unexpected camera exception: ${e.message}")
        }
    }

    private fun cleanupThread(thread: HandlerThread) {
        try {
            thread.quitSafely()
        } catch (e: Exception) {
            Log.e(TAG, "Error quitting thread", e)
        }
    }
}
