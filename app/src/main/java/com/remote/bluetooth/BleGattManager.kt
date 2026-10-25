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

class BleGattManager(private val context: Context) {
    companion object {
        private const val TAG = "BleGattManager"
        // Nordic UART Service UUIDs
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val NUS_RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val NUS_TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private fun hasPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
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
            Log.d(TAG, "Connection state changed: $newState")
            _connectionState.tryEmit(newState)
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Attempting to start service discovery")
                if (hasPermission()) {
                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException on discoverServices", e)
                    }
                } else {
                    Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server")
                if (hasPermission()) {
                    try {
                        close()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException on close", e)
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed with status: $status")
                return
            }

            if (!hasPermission()) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission in onServicesDiscovered")
                return
            }

            val service = gatt.getService(NUS_SERVICE_UUID)
            if (service == null) {
                Log.w(TAG, "NUS service not found")
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
                    Log.e(TAG, "SecurityException configuring characteristics", e)
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
            if (characteristic.uuid == NUS_TX_CHARACTERISTIC_UUID) {
                try {
                    val value = characteristic.value
                    if (value != null) {
                        _incomingData.tryEmit(value)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception on onCharacteristicChanged", e)
                }
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        if (hasPermission()) {
            try {
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException on connect", e)
            }
        } else {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
        }
    }

    @Suppress("DEPRECATION")
    fun sendData(data: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val rx = rxCharacteristic ?: return false
        
        if (hasPermission()) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val status = gatt.writeCharacteristic(rx, data, rx.writeType)
                    return status == android.bluetooth.BluetoothStatusCodes.SUCCESS
                } else {
                    rx.value = data
                    return gatt.writeCharacteristic(rx)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException on sendData", e)
                return false
            } catch (e: Exception) {
                Log.e(TAG, "Exception on sendData", e)
                return false
            }
        }

        Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
        return false
    }

    fun close() {
        if (hasPermission()) {
            try {
                bluetoothGatt?.close()
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException on close", e)
            }
        } else {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
        }
        bluetoothGatt = null
        rxCharacteristic = null
        txCharacteristic = null
    }
}
