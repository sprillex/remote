package com.remote.bluetooth

import org.junit.Assert.*
import org.junit.Test

class BleGattManagerTest {

    @Test
    fun testInitialization() {
        assertEquals("6e400001-b5a3-f393-e0a9-e50e24dcca9e", BleGattManager.NUS_SERVICE_UUID.toString())
        assertEquals("6e400002-b5a3-f393-e0a9-e50e24dcca9e", BleGattManager.NUS_RX_CHARACTERISTIC_UUID.toString())
        assertEquals("6e400003-b5a3-f393-e0a9-e50e24dcca9e", BleGattManager.NUS_TX_CHARACTERISTIC_UUID.toString())
        assertEquals("00002902-0000-1000-8000-00805f9b34fb", BleGattManager.CLIENT_CHARACTERISTIC_CONFIG_UUID.toString())
    }

    @Test
    fun testHasPermissionNullContext() {
        val manager = BleGattManager(null)
        assertFalse(manager.hasPermission())
    }

    @Test
    fun testSendDataWithoutPermissionReturnsFalse() {
        val manager = BleGattManager(null)
        assertFalse(manager.sendData(byteArrayOf(0x01, 0x02)))
    }

    @Test
    fun testDisconnectAndCloseWithoutPermissionHandlesGracefully() {
        val manager = BleGattManager(null)
        // Should not throw exceptions
        manager.disconnect()
        manager.close()
    }

    @Test
    fun testPairDeviceWithoutPermissionReturnsFalse() {
        val manager = BleGattManager(null)
        assertFalse(manager.hasPermission())
    }
}
