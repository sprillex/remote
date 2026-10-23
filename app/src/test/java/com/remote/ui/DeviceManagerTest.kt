package com.remote.ui

import com.remote.db.ConnectionType
import com.remote.db.ProfileType
import com.remote.db.TargetDeviceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceManagerTest {

    @Test
    fun testDeviceEntityCreationForUI() {
        val device = TargetDeviceEntity(
            id = 1L,
            name = "Living Room TV",
            connectionType = ConnectionType.ROKU_ECP_WIFI,
            profileType = ProfileType.TV_REMOTE,
            hostAddress = "192.168.1.100",
            port = 8060
        )

        assertEquals("Living Room TV", device.name)
        assertEquals(ConnectionType.ROKU_ECP_WIFI, device.connectionType)
        assertEquals(ProfileType.TV_REMOTE, device.profileType)
        assertEquals("192.168.1.100", device.hostAddress)
        assertEquals(8060, device.port)
        assertNull(device.macAddress)
    }

    @Test
    fun testDeviceSelectionListFiltering() {
        val dev1 = TargetDeviceEntity(id = 1L, name = "PC", connectionType = ConnectionType.DIRECT_BT_HID, profileType = ProfileType.PC_KEYBOARD)
        val dev2 = TargetDeviceEntity(id = 2L, name = "Roku", connectionType = ConnectionType.ROKU_ECP_WIFI, profileType = ProfileType.TV_REMOTE)

        val devices = listOf(dev1, dev2)
        val selectedId = 2L

        val selected = devices.find { it.id == selectedId }
        assertEquals("Roku", selected?.name)
    }
}
