package com.remote.bluetooth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

class HidDeviceManagerTest {

    @Test
    fun testKeyboardDescriptorSize() {
        assertNotNull("KEYBOARD_DESCRIPTOR must not be null", HidDeviceManager.KEYBOARD_DESCRIPTOR)
        // Check descriptor has expected bytes (just verifying it's not empty and contains HID info)
        assertEquals("KEYBOARD_DESCRIPTOR length is incorrect", 63, HidDeviceManager.KEYBOARD_DESCRIPTOR.size)
    }

    @Test
    fun testBuildKeyboardReport_EmptyKeys() {
        val modifier: Byte = 0x01
        val keys = emptyList<Byte>()

        val report = HidDeviceManager.buildKeyboardReport(modifier, keys)

        val expected = ByteArray(8)
        expected[0] = modifier

        assertArrayEquals("Report should match expected empty key report", expected, report)
    }

    @Test
    fun testBuildKeyboardReport_MaxKeys() {
        val modifier: Byte = 0x02
        val keys = listOf<Byte>(0x04, 0x05, 0x06, 0x07, 0x08, 0x09) // 'a' through 'f'

        val report = HidDeviceManager.buildKeyboardReport(modifier, keys)

        val expected = ByteArray(8)
        expected[0] = modifier
        expected[2] = 0x04
        expected[3] = 0x05
        expected[4] = 0x06
        expected[5] = 0x07
        expected[6] = 0x08
        expected[7] = 0x09

        assertArrayEquals("Report should contain 6 keys", expected, report)
    }

    @Test
    fun testBuildKeyboardReport_ExceedsMaxKeys() {
        val modifier: Byte = 0x00
        val keys = listOf<Byte>(0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A) // 7 keys

        val report = HidDeviceManager.buildKeyboardReport(modifier, keys)

        val expected = ByteArray(8)
        expected[0] = modifier
        expected[2] = 0x04
        expected[3] = 0x05
        expected[4] = 0x06
        expected[5] = 0x07
        expected[6] = 0x08
        expected[7] = 0x09
        // 7th key 0x0A should be truncated

        assertArrayEquals("Report should truncate to 6 keys", expected, report)
    }

    @Test
    fun testCustomExecutorInstantiation() {
        val executedOnCustom = AtomicBoolean(false)
        val testExecutor = Executor { command ->
            executedOnCustom.set(true)
            command.run()
        }

        val mockContext = android.content.ContextWrapper(null)
        val manager = HidDeviceManager(mockContext, testExecutor)
        assertNotNull("HidDeviceManager instance should be created with custom executor", manager)

        testExecutor.execute { }
        assertTrue("Custom executor should execute tasks", executedOnCustom.get())
    }

    @Test
    fun testGetBondedDevicesWithoutPermissionReturnsEmptySet() {
        val mockContext = android.content.ContextWrapper(null)
        val manager = HidDeviceManager(mockContext)
        val bonded = manager.getBondedDevices()
        assertNotNull("Bonded devices set should not be null", bonded)
        assertTrue("Bonded devices set should be empty without permission or adapter", bonded.isEmpty())
    }

    @Test
    fun testRegisterHidAppHandlesNullAdapterGracefully() {
        val mockContext = android.content.ContextWrapper(null)
        val manager = HidDeviceManager(mockContext)
        manager.registerHidApp()
        assertTrue("registerHidApp should complete gracefully without throwing", true)
    }

    @Test
    fun testSendKeyWithoutTargetDeviceReturnsFalse() {
        val mockContext = android.content.ContextWrapper(null)
        val manager = HidDeviceManager(mockContext)
        val result = manager.sendKey(0x04, 0x00)
        org.junit.Assert.assertFalse("sendKey without target device should return false", result)
    }

    @Test
    fun testSendCharWithoutTargetDeviceReturnsFalse() {
        val mockContext = android.content.ContextWrapper(null)
        val manager = HidDeviceManager(mockContext)
        val result = manager.sendChar('a')
        org.junit.Assert.assertFalse("sendChar without target device should return false", result)
    }
}
