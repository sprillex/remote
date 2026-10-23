package com.remote.ui

import com.remote.db.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class SetupGuidesTest {

    @Test
    fun testAllConnectionTypesHaveValidGuides() {
        ConnectionType.entries.forEach { type ->
            val guide = DeviceSetupGuideRegistry.getGuide(type)
            assertNotNull("Guide for $type must not be null", guide)
            assertEquals("Guide connection type must match requested", type, guide.connectionType)
            assertFalse("Guide title for $type must not be blank", guide.title.isBlank())
            assertFalse("Guide steps for $type must not be empty", guide.steps.isEmpty())
            assertFalse("Guide warning for $type must not be blank", guide.warning.isBlank())
        }
    }

    @Test
    fun testRokuGuideSpecifics() {
        val rokuGuide = DeviceSetupGuideRegistry.getGuide(ConnectionType.ROKU_ECP_WIFI)
        assertEquals("Roku TV ECP Wi-Fi Setup", rokuGuide.title)
        assert(rokuGuide.warning.contains("Roku OS 14.1+"))
    }

    @Test
    fun testDirectBtHidGuideSpecifics() {
        val btGuide = DeviceSetupGuideRegistry.getGuide(ConnectionType.DIRECT_BT_HID)
        assertEquals("Direct Bluetooth HID Setup", btGuide.title)
        assertEquals(2, btGuide.steps.size)
    }
}
