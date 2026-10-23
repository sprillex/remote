package com.remote.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EntitiesTest {

    private val converters = Converters()

    @Test
    fun testConnectionTypeConverter() {
        val conn = ConnectionType.DIRECT_BT_HID
        val connStr = converters.fromConnectionType(conn)
        assertEquals("DIRECT_BT_HID", connStr)

        val restored = converters.toConnectionType(connStr)
        assertEquals(ConnectionType.DIRECT_BT_HID, restored)

        assertNull(converters.toConnectionType("INVALID_TYPE"))
        assertNull(converters.toConnectionType(null))
    }

    @Test
    fun testProfileTypeConverter() {
        val profile = ProfileType.PC_KEYBOARD
        val profileStr = converters.fromProfileType(profile)
        assertEquals("PC_KEYBOARD", profileStr)

        val restored = converters.toProfileType(profileStr)
        assertEquals(ProfileType.PC_KEYBOARD, restored)

        assertNull(converters.toProfileType("INVALID_PROFILE"))
        assertNull(converters.toProfileType(null))
    }

    @Test
    fun testTargetDeviceEntityDefaults() {
        val device = TargetDeviceEntity(
            name = "Test PC",
            connectionType = ConnectionType.DIRECT_BT_HID,
            profileType = ProfileType.PC_KEYBOARD,
            macAddress = "00:11:22:33:44:55"
        )

        assertEquals("Test PC", device.name)
        assertEquals(ConnectionType.DIRECT_BT_HID, device.connectionType)
        assertEquals(ProfileType.PC_KEYBOARD, device.profileType)
        assertEquals("00:11:22:33:44:55", device.macAddress)
        assertEquals(22, device.port)
        assertEquals(38000, device.carrierFrequency)
        assertEquals(10, device.interKeyDelayMs)
    }

    @Test
    fun testButtonMappingEntityDefaults() {
        val mapping = ButtonMappingEntity(
            deviceId = 1L,
            buttonIdentifier = "KEY_ENTER",
            label = "Enter",
            hidPage = 0x07,
            usageId = 0x28
        )

        assertEquals(1L, mapping.deviceId)
        assertEquals("KEY_ENTER", mapping.buttonIdentifier)
        assertEquals("Enter", mapping.label)
        assertEquals(0x07, mapping.hidPage)
        assertEquals(0x28, mapping.usageId)
    }
}
