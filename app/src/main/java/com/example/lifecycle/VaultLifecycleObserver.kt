package com.example.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.data.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Monitors app lifecycle to enforce automatic locking when backgrounded or inactive.
 */
class VaultLifecycleObserver(
    private val repository: VaultRepository,
    private val coroutineScope: CoroutineScope
) : DefaultLifecycleObserver {

    private var backgroundTimestamp: Long = 0L
    private var idleJob: Job? = null

    override fun onStop(owner: LifecycleOwner) {
        backgroundTimestamp = System.currentTimeMillis()
        coroutineScope.launch {
            val config = repository.preferences.configFlow.first()
            if (config.autoLockTimeoutSec == 0) {
                // Immediate lock
                repository.lockVault()
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (backgroundTimestamp > 0) {
            val elapsedSec = (System.currentTimeMillis() - backgroundTimestamp) / 1000
            coroutineScope.launch {
                val config = repository.preferences.configFlow.first()
                if (elapsedSec >= config.autoLockTimeoutSec) {
                    repository.lockVault()
                }
            }
        }
    }

    fun reportUserInteraction() {
        idleJob?.cancel()
        idleJob = coroutineScope.launch(Dispatchers.Default) {
            val config = repository.preferences.configFlow.first()
            if (config.autoLockTimeoutSec > 0) {
                delay(config.autoLockTimeoutSec * 1000L)
                repository.lockVault()
            }
        }
    }
}
