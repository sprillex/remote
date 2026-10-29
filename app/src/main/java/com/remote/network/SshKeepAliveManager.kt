package com.remote.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class SshKeepAliveManager(
    private val serverAliveIntervalMs: Long = 15000L,
    private val maxReconnectAttempts: Int = 3
) {
    private var keepAliveJob: Job? = null
    private val isRunning = AtomicBoolean(false)
    var isConnectedAndAlive: Boolean = false
        private set

    fun startKeepAlive(
        scope: CoroutineScope,
        session: SshSession,
        reconnectAction: (suspend () -> Boolean)? = null,
        onStatusChanged: ((Boolean) -> Unit)? = null
    ) {
        if (isRunning.getAndSet(true)) return

        keepAliveJob = scope.launch(Dispatchers.IO) {
            var consecutiveFailures = 0

            while (isActive && isRunning.get()) {
                delay(serverAliveIntervalMs)

                val result = session.executeCommand(":")
                if (result.isSuccess) {
                    consecutiveFailures = 0
                    if (!isConnectedAndAlive) {
                        isConnectedAndAlive = true
                        onStatusChanged?.invoke(true)
                    }
                } else {
                    consecutiveFailures++
                    isConnectedAndAlive = false
                    onStatusChanged?.invoke(false)

                    if (reconnectAction != null && consecutiveFailures <= maxReconnectAttempts) {
                        var reconnected = false
                        for (attempt in 1..maxReconnectAttempts) {
                            delay(1000L * attempt)
                            if (reconnectAction.invoke()) {
                                reconnected = true
                                consecutiveFailures = 0
                                isConnectedAndAlive = true
                                onStatusChanged?.invoke(true)
                                break
                            }
                        }
                        if (!reconnected && consecutiveFailures >= maxReconnectAttempts) {
                            break
                        }
                    }
                }
            }
            isRunning.set(false)
        }
    }

    fun stopKeepAlive() {
        isRunning.set(false)
        keepAliveJob?.cancel()
        keepAliveJob = null
        isConnectedAndAlive = false
    }
}
