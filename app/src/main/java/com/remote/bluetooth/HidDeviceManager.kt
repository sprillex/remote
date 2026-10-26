package com.remote.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class HidDeviceManager(
    private val context: Context,
    private val executor: Executor = Executors.newSingleThreadExecutor()
) {

    companion object {
        private const val TAG = "HidDeviceManager"

        const val SUBCLASS_KEYBOARD = 0x40.toByte()

        val KEYBOARD_DESCRIPTOR = byteArrayOf(
            // Standard 63-byte Boot Keyboard Descriptor (Report ID 0)
            0x05, 0x01,       // Usage Page (Generic Desktop)
            0x09, 0x06,       // Usage (Keyboard)
            0xA1.toByte(), 0x01, // Collection (Application)
            0x05, 0x07,       // Usage Page (Key Codes)
            0x19, 0xE0.toByte(), // Usage Minimum (224)
            0x29, 0xE7.toByte(), // Usage Maximum (231)
            0x15, 0x00,       // Logical Minimum (0)
            0x25, 0x01,       // Logical Maximum (1)
            0x75, 0x01,       // Report Size (1)
            0x95.toByte(), 0x08, // Report Count (8)
            0x81.toByte(), 0x02, // Input (Data, Variable, Absolute)
            0x95.toByte(), 0x01, // Report Count (1)
            0x75, 0x08,       // Report Size (8)
            0x81.toByte(), 0x01, // Input (Constant) reserved byte(1)
            0x95.toByte(), 0x05, // Report Count (5)
            0x75, 0x01,       // Report Size (1)
            0x05, 0x08,       // Usage Page (Page# for LEDs)
            0x19, 0x01,       // Usage Minimum (1)
            0x29, 0x05,       // Usage Maximum (5)
            0x91.toByte(), 0x02, // Output (Data, Variable, Absolute), Led report
            0x95.toByte(), 0x01, // Report Count (1)
            0x75, 0x03,       // Report Size (3)
            0x91.toByte(), 0x01, // Output (Data, Variable, Absolute), Led report padding
            0x95.toByte(), 0x06, // Report Count (6)
            0x75, 0x08,       // Report Size (8)
            0x15, 0x00,       // Logical Minimum (0)
            0x25, 0x65,       // Logical Maximum (101)
            0x05, 0x07,       // Usage Page (Key codes)
            0x19, 0x00,       // Usage Minimum (0)
            0x29, 0x65,       // Usage Maximum (101)
            0x81.toByte(), 0x00, // Input (Data, Array) Key array(6 bytes)
            0xC0.toByte()      // End Collection (Application)
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

        fun buildConsumerReport(mediaUsageId: Int): ByteArray {
            val report = ByteArray(2)
            report[0] = (mediaUsageId and 0xFF).toByte()
            report[1] = ((mediaUsageId shr 8) and 0xFF).toByte()
            return report
        }
    }

    fun hasPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        }
        return true
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

    @RequiresApi(Build.VERSION_CODES.P)
    fun connect(device: BluetoothDevice): Boolean {
        if (!hasPermission()) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission to connect HID device")
            return false
        }
        return try {
            hidDevice?.connect(device) ?: false
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException on connect", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception on connect", e)
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun disconnect(device: BluetoothDevice): Boolean {
        if (!hasPermission()) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission to disconnect HID device")
            return false
        }
        return try {
            hidDevice?.disconnect(device) ?: false
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException on disconnect", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception on disconnect", e)
            false
        }
    }

    fun getBondedDevices(): Set<BluetoothDevice> {
        if (!hasPermission()) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission to get bonded devices")
            return emptySet()
        }
        return try {
            @Suppress("DEPRECATION")
            val adapter = try { android.bluetooth.BluetoothAdapter.getDefaultAdapter() } catch (_: Throwable) { null }
            adapter?.bondedDevices ?: emptySet()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException on getBondedDevices", e)
            emptySet()
        } catch (e: Exception) {
            Log.e(TAG, "Exception on getBondedDevices", e)
            emptySet()
        } catch (_: Throwable) {
            emptySet()
        }
    }

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null

    val sdpSettings by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            BluetoothHidDeviceAppSdpSettings(
                "RemoteKeyboard",
                "Android Bluetooth HID",
                "Google",
                SUBCLASS_KEYBOARD,
                KEYBOARD_DESCRIPTOR
            )
        } else null
    }

    private val callback by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            object : BluetoothHidDevice.Callback() {
                override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                    super.onAppStatusChanged(pluggedDevice, registered)
                    Log.d(TAG, "onAppStatusChanged: registered=$registered")
                }

                override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
                    super.onConnectionStateChanged(device, state)
                    Log.d(TAG, "onConnectionStateChanged: device=${device.address}, state=$state")
                    if (state == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                        connectedDevice = device
                    } else if (state == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                        if (connectedDevice?.address == device.address) {
                            connectedDevice = null
                        }
                    }
                }
            }
        } else null
    }

    fun getTargetDevice(): BluetoothDevice? {
        if (connectedDevice != null) return connectedDevice
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hasPermission()) {
            try {
                return hidDevice?.getDevicesMatchingConnectionStates(intArrayOf(android.bluetooth.BluetoothProfile.STATE_CONNECTED))?.firstOrNull()
            } catch (_: Exception) {} catch (_: Throwable) {}
        }
        return null
    }

    fun sendKey(usageId: Byte, modifier: Byte = 0): Boolean {
        val device = getTargetDevice() ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val keyDown = buildKeyboardReport(modifier, listOf(usageId))
            val sentDown = sendReport(device, 0, keyDown)
            val keyUp = buildKeyboardReport(0, emptyList())
            val sentUp = sendReport(device, 0, keyUp)
            return sentDown && sentUp
        }
        return false
    }

    fun sendMediaKey(mediaUsageId: Int): Boolean {
        val (usageId, modifier) = when (mediaUsageId) {
            MediaKey.VOLUME_UP -> Pair(0x80.toByte(), 0.toByte())
            MediaKey.VOLUME_DOWN -> Pair(0x81.toByte(), 0.toByte())
            MediaKey.MUTE -> Pair(0x7F.toByte(), 0.toByte())
            MediaKey.PLAY_PAUSE -> Pair(0x2C.toByte(), 0.toByte())
            MediaKey.NEXT_TRACK -> Pair(0x4F.toByte(), 0.toByte())
            MediaKey.PREV_TRACK -> Pair(0x50.toByte(), 0.toByte())
            MediaKey.STOP -> Pair(0x29.toByte(), 0.toByte())
            MediaKey.FAST_FORWARD -> Pair(0x4F.toByte(), 0.toByte())
            MediaKey.REWIND -> Pair(0x50.toByte(), 0.toByte())
            MediaKey.HOME -> Pair(0x4A.toByte(), 0.toByte())
            MediaKey.BACK -> Pair(0x29.toByte(), 0.toByte())
            MediaKey.RECENTS -> Pair(0x2B.toByte(), 0x04.toByte()) // Alt+Tab
            MediaKey.NOTIFICATIONS -> Pair(0x43.toByte(), 0.toByte()) // F10
            MediaKey.POWER -> Pair(0x66.toByte(), 0.toByte())
            MediaKey.SEARCH -> Pair(0x06.toByte(), 0x08.toByte()) // GUI+c
            else -> Pair((mediaUsageId and 0xFF).toByte(), 0.toByte())
        }
        return sendKey(usageId, modifier)
    }

    fun sendChar(char: Char): Boolean {
        val report = KeyTranslator.translate(char) ?: return false
        return sendKey(report.usageId, report.modifier)
    }

    fun registerHidApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (!hasPermission()) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission to get HID profile proxy")
                return
            }
            if (this.hidDevice != null) {
                registerApp(this.hidDevice!!)
                return
            }
            try {
                @Suppress("DEPRECATION")
                val adapter = try { android.bluetooth.BluetoothAdapter.getDefaultAdapter() } catch (_: Throwable) { null }
                adapter?.getProfileProxy(context, object : android.bluetooth.BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: android.bluetooth.BluetoothProfile) {
                        if (profile == android.bluetooth.BluetoothProfile.HID_DEVICE) {
                            Log.d(TAG, "BluetoothProfile.HID_DEVICE connected")
                            val hidDeviceProxy = proxy as BluetoothHidDevice
                            registerApp(hidDeviceProxy)
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == android.bluetooth.BluetoothProfile.HID_DEVICE) {
                            Log.d(TAG, "BluetoothProfile.HID_DEVICE disconnected")
                            hidDevice = null
                        }
                    }
                }, android.bluetooth.BluetoothProfile.HID_DEVICE)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException on getProfileProxy", e)
            } catch (e: Exception) {
                Log.e(TAG, "Exception on getProfileProxy", e)
            } catch (_: Throwable) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun registerApp(proxy: BluetoothHidDevice) {
        if (!hasPermission()) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission to register app")
            return
        }

        this.hidDevice = proxy
        try {
            proxy.registerApp(sdpSettings, null, null, executor, callback)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException on registerApp", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun unregisterApp() {
        if (hasPermission()) {
            try {
                hidDevice?.unregisterApp()
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException on unregisterApp", e)
            }
        } else {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun sendReport(device: BluetoothDevice, id: Int, report: ByteArray): Boolean {
        if (hasPermission()) {
            try {
                return hidDevice?.sendReport(device, id, report) ?: false
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException on sendReport", e)
                return false
            }
        } else {
             Log.e(TAG, "Missing BLUETOOTH_CONNECT permission")
             return false
        }
    }
}
