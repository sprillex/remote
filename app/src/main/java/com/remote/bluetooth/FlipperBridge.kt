package com.remote.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.nio.charset.StandardCharsets
import java.util.UUID

@SuppressLint("MissingPermission")
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
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected")
                close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(FLIPPER_SERIAL_SERVICE_UUID)
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(FLIPPER_SERIAL_RX_CHAR_UUID)
                    txCharacteristic = service.getCharacteristic(FLIPPER_SERIAL_TX_CHAR_UUID)
                    txCharacteristic?.let { tx ->
                        gatt.setCharacteristicNotification(tx, true)
                    }
                } else {
                    Log.w(TAG, "Flipper serial service not found")
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic.uuid == FLIPPER_SERIAL_TX_CHAR_UUID) {
                val text = String(characteristic.value ?: byteArrayOf(), StandardCharsets.UTF_8)
                _receivedResponse.tryEmit(text)
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        context?.let { ctx ->
            bluetoothGatt = device.connectGatt(ctx, false, gattCallback)
        }
    }

    fun sendIrCommand(protocol: String, hexCode: String): Boolean {
        val payload = formatIrTxCommandBytes(protocol, hexCode)
        return sendRawSerialData(payload)
    }

    fun sendRawSerialData(data: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        val rx = rxCharacteristic ?: return false

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(rx, data, rx.writeType) == android.bluetooth.BluetoothStatusCodes.SUCCESS
        } else {
            rx.value = data
            gatt.writeCharacteristic(rx)
        }
    }

    fun close() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        rxCharacteristic = null
        txCharacteristic = null
    }
}
