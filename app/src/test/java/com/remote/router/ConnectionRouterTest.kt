package com.remote.router

import com.remote.db.ConnectionType
import com.remote.db.ProfileType
import com.remote.db.TargetDeviceEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ConnectionRouterTest {

    private lateinit var router: ConnectionRouter

    @Before
    fun setup() {
        router = ConnectionRouter()
    }

    @Test
    fun `initial state has null active device`() = runBlocking {
        assertNull(router.activeDevice.value)
    }

    @Test
    fun `routeTo connects to new device`() = runBlocking {
        val device = createDevice(1, ConnectionType.DIRECT_BT_HID)

        router.routeTo(device)

        assertEquals(device, router.activeDevice.value)
    }

    @Test
    fun `routeTo same device does not change active device instance`() = runBlocking {
        val device = createDevice(1, ConnectionType.DIRECT_BT_HID)

        router.routeTo(device)
        val initialInstance = router.activeDevice.value

        val sameDeviceDifferentInstance = createDevice(1, ConnectionType.DIRECT_BT_HID)
        router.routeTo(sameDeviceDifferentInstance)

        // Assert it did not update the value with the new instance
        assertEquals(initialInstance, router.activeDevice.value)
    }

    @Test
    fun `routeTo different device cleanly switches devices`() = runBlocking {
        val deviceA = createDevice(1, ConnectionType.DIRECT_BT_HID)
        val deviceB = createDevice(2, ConnectionType.ESP32_BLE_BRIDGE)

        router.routeTo(deviceA)
        assertEquals(deviceA, router.activeDevice.value)

        router.routeTo(deviceB)
        assertEquals(deviceB, router.activeDevice.value)
    }

    @Test
    fun `disconnect clears active device`() = runBlocking {
        val device = createDevice(1, ConnectionType.DIRECT_BT_HID)

        router.routeTo(device)
        assertEquals(device, router.activeDevice.value)

        router.disconnect()
        assertNull(router.activeDevice.value)
    }

    @Test
    fun `routeTo Bluetooth types succeeds without error`() = runBlocking {
        val btHid = createDevice(1, ConnectionType.DIRECT_BT_HID)
        val esp32Ble = createDevice(2, ConnectionType.ESP32_BLE_BRIDGE)
        val flipperBle = createDevice(3, ConnectionType.FLIPPER_ZERO_BRIDGE)

        router.routeTo(btHid)
        assertEquals(btHid, router.activeDevice.value)

        router.routeTo(esp32Ble)
        assertEquals(esp32Ble, router.activeDevice.value)

        router.routeTo(flipperBle)
        assertEquals(flipperBle, router.activeDevice.value)
    }

    private fun createDevice(id: Long, type: ConnectionType): TargetDeviceEntity {
        return TargetDeviceEntity(
            id = id,
            name = "Test Device $id",
            connectionType = type,
            profileType = ProfileType.PC_KEYBOARD,
            createdAt = 123456789L
        )
    }
}
