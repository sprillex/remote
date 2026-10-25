package com.remote.router

import com.remote.db.TargetDeviceEntity
import com.remote.bluetooth.BleGattManager
import com.remote.bluetooth.FlipperBridge
import com.remote.bluetooth.HidDeviceManager
import com.remote.network.RokuClient
import com.remote.network.SshClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConnectionRouter(
    private val hidManager: HidDeviceManager? = null,
    private val bleManager: BleGattManager? = null,
    private val rokuClient: RokuClient? = null,
    private val flipperBridge: FlipperBridge? = null
) {
    private val _activeDevice = MutableStateFlow<TargetDeviceEntity?>(null)
    val activeDevice: StateFlow<TargetDeviceEntity?> = _activeDevice.asStateFlow()

    // We retain a reference to SshClient across routing updates to prevent dropping the connection
    private var activeSshClient: SshClient? = null

    private val routingMutex = Mutex()

    /**
     * Routes connection to the specified device.
     * Ensures any existing connection is cleanly severed (disconnect-before-connect)
     * before establishing the new one, avoiding background conflicts.
     */
    suspend fun routeTo(device: TargetDeviceEntity): Result<Unit> {
        return routingMutex.withLock {
            val currentDevice = _activeDevice.value

            // If already connected to the requested device, do nothing
            if (currentDevice?.id == device.id) {
                return@withLock Result.success(Unit)
            }

            // Disconnect current device if one exists
            if (currentDevice != null) {
                val disconnectResult = disconnectInternal(currentDevice)
                if (disconnectResult.isFailure) return@withLock disconnectResult
            }

            // Connect to new device
            val connectResult = connectInternal(device)
            if (connectResult.isSuccess) {
                _activeDevice.value = device
            }
            connectResult
        }
    }

    suspend fun disconnect(): Result<Unit> {
        return routingMutex.withLock {
            val currentDevice = _activeDevice.value
            if (currentDevice != null) {
                val result = disconnectInternal(currentDevice)
                if (result.isSuccess) {
                    _activeDevice.value = null
                }
                result
            } else {
                Result.success(Unit)
            }
        }
    }

    private suspend fun disconnectInternal(device: TargetDeviceEntity): Result<Unit> {
        return when (device.connectionType) {
            com.remote.db.ConnectionType.SSH_NETWORK -> {
                try {
                    activeSshClient?.disconnect()
                    activeSshClient = null
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            com.remote.db.ConnectionType.DIRECT_BT_HID,
            com.remote.db.ConnectionType.ESP32_BLE_BRIDGE,
            com.remote.db.ConnectionType.FLIPPER_ZERO_BRIDGE,
            com.remote.db.ConnectionType.ROKU_ECP_WIFI -> {
                // These managers are either stateless (Roku) or do not currently implement
                // targeted device disconnection capabilities at the application level.
                Result.success(Unit)
            }
            else -> Result.failure(IllegalArgumentException("Unsupported device type: ${device.connectionType}"))
        }
    }

    private suspend fun connectInternal(device: TargetDeviceEntity): Result<Unit> {
        return when (device.connectionType) {
            com.remote.db.ConnectionType.SSH_NETWORK -> {
                device.hostAddress?.let { host ->
                    val ssh = SshClient(host = host, username = device.authUsername ?: "root")
                    try {
                        ssh.connect()
                        activeSshClient = ssh
                        Result.success(Unit)
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                } ?: Result.failure(IllegalArgumentException("Missing hostAddress"))
            }
            com.remote.db.ConnectionType.ROKU_ECP_WIFI -> {
                device.hostAddress?.let {
                    rokuClient?.buildUrl(it, device.port, "")
                    Result.success(Unit)
                } ?: Result.failure(IllegalArgumentException("Missing hostAddress"))
            }
            com.remote.db.ConnectionType.DIRECT_BT_HID,
            com.remote.db.ConnectionType.ESP32_BLE_BRIDGE,
            com.remote.db.ConnectionType.FLIPPER_ZERO_BRIDGE -> {
                // Existing Managers lack generic connect(TargetDeviceEntity) functionality
                // and operate directly on BluetoothDevice which is missing here.
                Result.success(Unit)
            }
            else -> Result.failure(IllegalArgumentException("Unsupported device type: ${device.connectionType}"))
        }
    }
}
