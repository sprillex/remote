package com.remote.router

import com.remote.db.TargetDeviceEntity
import com.remote.bluetooth.BleGattManager
import com.remote.bluetooth.FlipperBridge
import com.remote.bluetooth.HidDeviceManager
import com.remote.ir.AudioIrDriver
import com.remote.network.RokuClient
import com.remote.network.SshClientWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConnectionRouter(
    private val hidManager: HidDeviceManager? = null,
    private val bleManager: BleGattManager? = null,
    private val rokuClient: RokuClient? = null,
    private val flipperBridge: FlipperBridge? = null,
    private val audioIrDriver: AudioIrDriver? = null
) {
    private val _activeDevice = MutableStateFlow<TargetDeviceEntity?>(null)
    val activeDevice: StateFlow<TargetDeviceEntity?> = _activeDevice.asStateFlow()

    private var activeSshClient: SshClientWrapper? = null

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

    // visible for testing - allows injection of an SSH client since we lack a global factory
    fun injectSshClient(sshClient: SshClientWrapper) {
        this.activeSshClient = sshClient
    }

    private suspend fun disconnectInternal(device: TargetDeviceEntity): Result<Unit> {
        return when (device.connectionType) {
            com.remote.db.ConnectionType.SSH_NETWORK -> {
                try {
                    activeSshClient = null
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            com.remote.db.ConnectionType.DIRECT_BT_HID -> {
                // Not supported
                Result.success(Unit)
            }
            com.remote.db.ConnectionType.ESP32_BLE_BRIDGE -> {
                // Not supported
                Result.success(Unit)
            }
            com.remote.db.ConnectionType.FLIPPER_ZERO_BRIDGE -> {
                flipperBridge?.close()
                Result.success(Unit)
            }
            com.remote.db.ConnectionType.ROKU_ECP_WIFI,
            com.remote.db.ConnectionType.AUDIO_JACK_IR,
            com.remote.db.ConnectionType.INTERNAL_IR,
            com.remote.db.ConnectionType.USB_IR_DONGLE,
            com.remote.db.ConnectionType.ESP32_IR_EMITTER,
            com.remote.db.ConnectionType.ESP32_HYBRID -> {
                Result.success(Unit)
            }
        }
    }

    private suspend fun connectInternal(device: TargetDeviceEntity): Result<Unit> {
        return when (device.connectionType) {
            com.remote.db.ConnectionType.SSH_NETWORK -> {
                if (activeSshClient != null) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("No SSH client configured"))
                }
            }
            com.remote.db.ConnectionType.ROKU_ECP_WIFI -> {
                if (rokuClient != null) {
                    device.hostAddress?.let {
                        rokuClient.buildUrl(it, device.port, "")
                    }
                }
                Result.success(Unit)
            }
            com.remote.db.ConnectionType.DIRECT_BT_HID,
            com.remote.db.ConnectionType.AUDIO_JACK_IR,
            com.remote.db.ConnectionType.INTERNAL_IR,
            com.remote.db.ConnectionType.USB_IR_DONGLE,
            com.remote.db.ConnectionType.ESP32_IR_EMITTER,
            com.remote.db.ConnectionType.ESP32_HYBRID -> {
                Result.success(Unit)
            }
            com.remote.db.ConnectionType.ESP32_BLE_BRIDGE -> {
                Result.success(Unit)
            }
            com.remote.db.ConnectionType.FLIPPER_ZERO_BRIDGE -> {
                try {
                    val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                    val btDevice = device.macAddress?.let { adapter?.getRemoteDevice(it) }
                    if (btDevice != null) {
                        flipperBridge?.connect(btDevice)
                    }
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }
}
