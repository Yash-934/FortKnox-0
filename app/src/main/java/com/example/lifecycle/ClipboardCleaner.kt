package com.example.lifecycle

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages sensitive clipboard copy actions with a 30-second automatic wipe.
 */
object ClipboardCleaner {

    private val _countdownSec = MutableStateFlow<Int?>(null)
    val countdownSec: StateFlow<Int?> = _countdownSec.asStateFlow()

    private var wipeJob: Job? = null

    fun copyToClipboardWithAutoClear(
        context: Context,
        label: String,
        text: String,
        scope: CoroutineScope,
        durationSeconds: Int = 30
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val description = clip.description
            val extras = android.os.PersistableBundle().apply {
                putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            description.extras = extras
        }

        clipboard.setPrimaryClip(clip)

        wipeJob?.cancel()
        wipeJob = scope.launch(Dispatchers.Main) {
            for (sec in durationSeconds downTo 1) {
                _countdownSec.value = sec
                delay(1000L)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                _countdownSec.value = null
            }
        }
    }

    fun cancelWipe() {
        wipeJob?.cancel()
        _countdownSec.value = null
    }
}
