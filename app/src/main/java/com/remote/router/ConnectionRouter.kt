package com.remote.router

import com.remote.db.TargetDeviceEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConnectionRouter {
    private val _activeDevice = MutableStateFlow<TargetDeviceEntity?>(null)
    val activeDevice: StateFlow<TargetDeviceEntity?> = _activeDevice.asStateFlow()

    private val routingMutex = Mutex()

    /**
     * Routes connection to the specified device.
     * Ensures any existing connection is cleanly severed (disconnect-before-connect)
     * before establishing the new one, avoiding background conflicts.
     */
    suspend fun routeTo(device: TargetDeviceEntity) {
        routingMutex.withLock {
            val currentDevice = _activeDevice.value

            // If already connected to the requested device, do nothing
            if (currentDevice?.id == device.id) {
                return
            }

            // Disconnect current device if one exists
            if (currentDevice != null) {
                disconnectInternal(currentDevice)
            }

            // Connect to new device
            connectInternal(device)
            _activeDevice.value = device
        }
    }

    suspend fun disconnect() {
        routingMutex.withLock {
            val currentDevice = _activeDevice.value
            if (currentDevice != null) {
                disconnectInternal(currentDevice)
                _activeDevice.value = null
            }
        }
    }

    private suspend fun disconnectInternal(device: TargetDeviceEntity) {
        // Here we would delegate to the specific protocol manager (e.g. BleGattManager, etc)
        // based on the device's connectionType.
        // For the sake of the router's state machine, we just model the lifecycle transition.

        // Simulating the disconnect operation
    }

    private suspend fun connectInternal(device: TargetDeviceEntity) {
        // Delegate to specific protocol manager based on connectionType.

        // Simulating the connect operation
    }
}
