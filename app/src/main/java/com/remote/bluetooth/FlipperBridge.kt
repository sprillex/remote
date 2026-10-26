package com.remote.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.nio.charset.StandardCharsets
import java.util.UUID

class FlipperBridge(private val context: Context? = null) {

    companion object {
        private const val TAG = "FlipperBridge"

        // Flipper Zero Serial Service & Characteristic UUIDs
        val FLIPPER_SERIAL_SERVICE_UUID: UUID = UUID.fromString("30823271-4641-4320-9f81-18a6a23d19a0")
        val FLIPPER_SERIAL_TX_CHAR_UUID: UUID = UUID.fromString("e7329ca1-11e6-42f0-aa1e-0a5601242371")
        val FLIPPER_SERIAL_RX_CHAR_UUID: UUID = UUID.fromString("e7329ca2-11e6-42f0-aa1e-0a5601242371")

        fun formatIrTxCommand(protocol: String, hexCode: String): String {
            val cleanProtocol = protocol.trim()
            val cleanHex = hexCode.trim()
            return "ir tx $cleanProtocol $cleanHex\r\n"
        }

        fun formatIrTxCommandBytes(protocol: String, hexCode: String): ByteArray {
            return formatIrTxCommand(protocol, hexCode).toByteArray(StandardCharsets.UTF_8)
        }
    }

    private fun hasPermission(): Boolean {
        if (context == null) return false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val connectionState = _connectionState.asSharedFlow()

    private val _receivedResponse = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val receivedResponse = _receivedResponse.asSharedFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.d(TAG, "Connection state changed: $newState")
            _connectionState.tryEmit(newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Discovering services...")
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
                Log.d(TAG, "Disconnected")
                close()
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

            val service = gatt.getService(FLIPPER_SERIAL_SERVICE_UUID)
            if (service == null) {
                Log.w(TAG, "Flipper serial service not found")
                return
            }

            rxCharacteristic = service.getCharacteristic(FLIPPER_SERIAL_RX_CHAR_UUID)
            txCharacteristic = service.getCharacteristic(FLIPPER_SERIAL_TX_CHAR_UUID)

            val tx = txCharacteristic
            if (tx != null) {
                try {
                    gatt.setCharacteristicNotification(tx, true)
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException on setCharacteristicNotification", e)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (characteristic.uuid == FLIPPER_SERIAL_TX_CHAR_UUID) {
                val text = String(value, StandardCharsets.UTF_8)
                _receivedResponse.tryEmit(text)
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic.uuid == FLIPPER_SERIAL_TX_CHAR_UUID) {
                try {
                    val value = characteristic.value ?: byteArrayOf()
                    val text = String(value, StandardCharsets.UTF_8)
                    _receivedResponse.tryEmit(text)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception on onCharacteristicChanged", e)
                }
            }
        }
    }

    fun pairDevice(device: BluetoothDevice): Boolean {
        if (!hasPermission()) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission to pair device")
            return false
        }
        return try {
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                device.createBond()
            } else {
                true
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException on pairDevice", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception on pairDevice", e)
            false
        }
    }

    fun connect(device: BluetoothDevice) {
        context?.let { ctx ->
            if (hasPermission()) {
                try {
                    if (device.bondState == BluetoothDevice.BOND_NONE) {
                        Log.d(TAG, "Device not bonded, initiating pairing")
                        pairDevice(device)
                    }
                    bluetoothGatt = device.connectGatt(ctx, false, gattCallback)
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException on connect", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception on connect", e)
                }
            } else {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
            }
        }
    }

    fun sendIrCommand(protocol: String, hexCode: String): Boolean {
        val payload = formatIrTxCommandBytes(protocol, hexCode)
        return sendRawSerialData(payload)
    }

    @Suppress("DEPRECATION")
    fun sendRawSerialData(data: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val rx = rxCharacteristic ?: return false

        if (hasPermission()) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    return gatt.writeCharacteristic(rx, data, rx.writeType) == android.bluetooth.BluetoothStatusCodes.SUCCESS
                } else {
                    rx.value = data
                    return gatt.writeCharacteristic(rx)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException on sendData", e)
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
        } else if (context != null) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission for closing")
        }
        bluetoothGatt = null
        rxCharacteristic = null
        txCharacteristic = null
    }
}
