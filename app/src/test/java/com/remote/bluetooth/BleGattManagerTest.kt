package com.remote.bluetooth

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class BleGattManagerTest {

    @Test
    fun testInitialization() {
        assertEquals("6e400001-b5a3-f393-e0a9-e50e24dcca9e", BleGattManager.NUS_SERVICE_UUID.toString())
        assertEquals("6e400002-b5a3-f393-e0a9-e50e24dcca9e", BleGattManager.NUS_RX_CHARACTERISTIC_UUID.toString())
        assertEquals("6e400003-b5a3-f393-e0a9-e50e24dcca9e", BleGattManager.NUS_TX_CHARACTERISTIC_UUID.toString())
    }
}
