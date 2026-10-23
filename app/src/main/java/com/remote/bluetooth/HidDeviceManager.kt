package com.remote.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.content.Context
import android.util.Log

class HidDeviceManager(private val context: Context) {

    companion object {
        private const val TAG = "HidDeviceManager"

        const val SUBCLASS_KEYBOARD = 0x40.toByte()

        val KEYBOARD_DESCRIPTOR = byteArrayOf(
            0x05, 0x01,       // Usage Page (Generic Desktop)
            0x09, 0x06,       // Usage (Keyboard)
            0xA1.toByte(), 0x01,       // Collection (Application)
            0x05, 0x07,       // Usage Page (Key Codes)
            0x19, 0xE0.toByte(),       // Usage Minimum (224)
            0x29, 0xE7.toByte(),       // Usage Maximum (231)
            0x15, 0x00,       // Logical Minimum (0)
            0x25, 0x01,       // Logical Maximum (1)
            0x75, 0x01,       // Report Size (1)
            0x95.toByte(), 0x08,       // Report Count (8)
            0x81.toByte(), 0x02,       // Input (Data, Variable, Absolute)
            0x95.toByte(), 0x01,       // Report Count (1)
            0x75, 0x08,       // Report Size (8)
            0x81.toByte(), 0x01,       // Input (Constant) reserved byte(1)
            0x95.toByte(), 0x05,       // Report Count (5)
            0x75, 0x01,       // Report Size (1)
            0x05, 0x08,       // Usage Page (Page# for LEDs)
            0x19, 0x01,       // Usage Minimum (1)
            0x29, 0x05,       // Usage Maximum (5)
            0x91.toByte(), 0x02,       // Output (Data, Variable, Absolute), Led report
            0x95.toByte(), 0x01,       // Report Count (1)
            0x75, 0x03,       // Report Size (3)
            0x91.toByte(), 0x01,       // Output (Data, Variable, Absolute), Led report padding
            0x95.toByte(), 0x06,       // Report Count (6)
            0x75, 0x08,       // Report Size (8)
            0x15, 0x00,       // Logical Minimum (0)
            0x25, 0x65,       // Logical Maximum (101)
            0x05, 0x07,       // Usage Page (Key codes)
            0x19, 0x00,       // Usage Minimum (0)
            0x29, 0x65,       // Usage Maximum (101)
            0x81.toByte(), 0x00,       // Input (Data, Array) Key array(6 bytes)
            0xC0.toByte()              // End Collection (Application)
        )

        fun buildKeyboardReport(modifier: Byte, keys: List<Byte>): ByteArray {
            val report = ByteArray(8)
            report[0] = modifier
            report[1] = 0 // Reserved

            // Add up to 6 keys
            for (i in 0 until minOf(keys.size, 6)) {
                report[2 + i] = keys[i]
            }
            return report
        }
    }

    private var hidDevice: BluetoothHidDevice? = null

    val sdpSettings = BluetoothHidDeviceAppSdpSettings(
        "RemoteKeyboard",
        "Android Bluetooth HID",
        "Google",
        SUBCLASS_KEYBOARD,
        KEYBOARD_DESCRIPTOR
    )

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            Log.d(TAG, "onAppStatusChanged: registered=$registered")
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            super.onConnectionStateChanged(device, state)
            Log.d(TAG, "onConnectionStateChanged: device=${device.address}, state=$state")
        }
    }

    @SuppressLint("MissingPermission")
    fun registerApp(proxy: BluetoothHidDevice) {
        this.hidDevice = proxy
        val outQos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            -1
        )
        val inQos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            -1
        )

        proxy.registerApp(sdpSettings, inQos, outQos, { it.run() }, callback)
    }

    @SuppressLint("MissingPermission")
    fun unregisterApp() {
        hidDevice?.unregisterApp()
    }
}
