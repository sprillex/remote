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

    @Test
    fun testSavedDeviceSelectionPersistenceFallback() {
        val dev1 = TargetDeviceEntity(id = 10L, name = "PC Keyboard", connectionType = ConnectionType.DIRECT_BT_HID, profileType = ProfileType.PC_KEYBOARD)
        val dev2 = TargetDeviceEntity(id = 20L, name = "Android Phone", connectionType = ConnectionType.DIRECT_BT_HID, profileType = ProfileType.ANDROID_DEVICE)
        val devices = listOf(dev1, dev2)

        // Case 1: Saved ID exists in list -> returns matching device
        var savedId: Long? = 20L
        var activeDevice = devices.find { it.id == savedId } ?: devices.firstOrNull()
        assertEquals("Android Phone", activeDevice?.name)

        // Case 2: Saved ID is invalid/deleted -> falls back to first device in list
        savedId = 999L
        activeDevice = devices.find { it.id == savedId } ?: devices.firstOrNull()
        assertEquals("PC Keyboard", activeDevice?.name)

        // Case 3: Empty list -> returns null
        savedId = 10L
        val emptyDevices = emptyList<TargetDeviceEntity>()
        activeDevice = emptyDevices.find { it.id == savedId } ?: emptyDevices.firstOrNull()
        assertNull(activeDevice)
    }

    @Test
    fun testConnectionStatusHeaderStateLogic() {
        val dev1 = TargetDeviceEntity(id = 1L, name = "Target A", connectionType = ConnectionType.DIRECT_BT_HID, profileType = ProfileType.PC_KEYBOARD)
        val dev2 = TargetDeviceEntity(id = 2L, name = "Target B", connectionType = ConnectionType.DIRECT_BT_HID, profileType = ProfileType.PC_KEYBOARD)

        // Selected device matching routed device -> Connected
        var selectedDevice: TargetDeviceEntity? = dev1
        var routedDevice: TargetDeviceEntity? = dev1
        var isConnected = selectedDevice != null && routedDevice?.id == selectedDevice?.id
        assertEquals(true, isConnected)

        // Selected device different from routed device -> Disconnected
        routedDevice = dev2
        isConnected = selectedDevice != null && routedDevice?.id == selectedDevice?.id
        assertEquals(false, isConnected)

        // No active routed device -> Disconnected
        routedDevice = null
        isConnected = selectedDevice != null && routedDevice?.id == selectedDevice?.id
        assertEquals(false, isConnected)
    }
}
