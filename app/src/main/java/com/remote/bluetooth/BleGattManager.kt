package com.remote.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

class BleGattManager(private val context: Context? = null) {
    companion object {
        private const val TAG = "BleGattManager"
        // Nordic UART Service UUIDs
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val NUS_RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val NUS_TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private fun logE(msg: String, throwable: Throwable? = null) {
        if (context != null) {
            try {
                if (throwable != null) {
                    Log.e(TAG, msg, throwable)
                } else {
                    Log.e(TAG, msg)
                }
            } catch (_: RuntimeException) {}
        }
    }

    private fun logD(msg: String) {
        if (context != null) {
            try {
                Log.d(TAG, msg)
            } catch (_: RuntimeException) {}
        }
    }

    private fun logW(msg: String) {
        if (context != null) {
            try {
                Log.w(TAG, msg)
            } catch (_: RuntimeException) {}
        }
    }

    fun hasPermission(): Boolean {
        val ctx = context ?: return false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    private val _incomingData = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val incomingData = _incomingData.asSharedFlow()
    
    private val _connectionState = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val connectionState = _connectionState.asSharedFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            logD("Connection state changed: $newState")
            _connectionState.tryEmit(newState)
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                logD("Attempting to start service discovery")
                if (hasPermission()) {
                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        logE("SecurityException on discoverServices", e)
                    } catch (e: Exception) {
                        logE("Exception on discoverServices", e)
                    }
                } else {
                    logE("Missing BLUETOOTH_CONNECT permission in onConnectionStateChange")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                logD("Disconnected from GATT server")
                close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                logE("Service discovery failed with status: $status")
                return
            }

            if (!hasPermission()) {
                logE("Missing BLUETOOTH_CONNECT permission in onServicesDiscovered")
                return
            }

            val service = gatt.getService(NUS_SERVICE_UUID)
            if (service == null) {
                logW("NUS service not found")
                return
            }

            rxCharacteristic = service.getCharacteristic(NUS_RX_CHARACTERISTIC_UUID)
            txCharacteristic = service.getCharacteristic(NUS_TX_CHARACTERISTIC_UUID)

            val tx = txCharacteristic
            if (tx != null) {
                try {
                    gatt.setCharacteristicNotification(tx, true)
                    val descriptor = tx.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                    if (descriptor != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        } else {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                } catch (e: SecurityException) {
                    logE("SecurityException configuring characteristics", e)
                } catch (e: Exception) {
                    logE("Exception configuring characteristics", e)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (characteristic.uuid == NUS_TX_CHARACTERISTIC_UUID) {
                _incomingData.tryEmit(value)
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            try {
                val value = characteristic.value
                if (value != null) {
                    this.onCharacteristicChanged(gatt, characteristic, value)
                }
            } catch (e: Exception) {
                logE("Exception on onCharacteristicChanged fallback", e)
            }
        }
    }

    fun pairDevice(device: BluetoothDevice): Boolean {
        if (!hasPermission()) {
            logE("Missing BLUETOOTH_CONNECT permission on pairDevice")
            return false
        }
        return try {
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                device.createBond()
            } else {
                true
            }
        } catch (e: SecurityException) {
            logE("SecurityException on pairDevice", e)
            false
        } catch (e: Exception) {
            logE("Exception on pairDevice", e)
            false
        }
    }

    fun connect(device: BluetoothDevice): Boolean {
        if (!hasPermission()) {
            logE("Missing BLUETOOTH_CONNECT permission on connect")
            return false
        }
        return try {
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                logD("Device not bonded, initiating pairing")
                pairDevice(device)
            }
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            bluetoothGatt != null
        } catch (e: SecurityException) {
            logE("SecurityException on connect", e)
            false
        } catch (e: Exception) {
            logE("Exception on connect", e)
            false
        }
    }

    fun disconnect() {
        if (hasPermission()) {
            try {
                bluetoothGatt?.disconnect()
            } catch (e: SecurityException) {
                logE("SecurityException on disconnect", e)
            } catch (e: Exception) {
                logE("Exception on disconnect", e)
            }
        } else {
            logE("Missing BLUETOOTH_CONNECT permission on disconnect")
        }
    }

    @Suppress("DEPRECATION")
    fun sendData(data: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val rx = rxCharacteristic ?: return false
        
        if (!hasPermission()) {
            logE("Missing BLUETOOTH_CONNECT permission on sendData")
            return false
        }

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val status = gatt.writeCharacteristic(rx, data, rx.writeType)
                status == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                rx.value = data
                gatt.writeCharacteristic(rx)
            }
        } catch (e: SecurityException) {
            logE("SecurityException on sendData", e)
            false
        } catch (e: Exception) {
            logE("Exception on sendData", e)
            false
        }
    }

    fun close() {
        if (hasPermission()) {
            try {
                bluetoothGatt?.close()
            } catch (e: SecurityException) {
                logE("SecurityException on close", e)
            } catch (e: Exception) {
                logE("Exception on close", e)
            }
        } else if (context != null) {
            logE("Missing BLUETOOTH_CONNECT permission on close")
        }
        bluetoothGatt = null
        rxCharacteristic = null
        txCharacteristic = null
    }
}
