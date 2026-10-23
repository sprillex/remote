package com.remote.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class FlipperBridgeTest {

    @Test
    fun testFormatIrTxCommand() {
        val command = FlipperBridge.formatIrTxCommand("NEC", "0x00FF00FF")
        assertEquals("ir tx NEC 0x00FF00FF\r\n", command)
    }

    @Test
    fun testFormatIrTxCommandTrimsWhitespace() {
        val command = FlipperBridge.formatIrTxCommand("  Sony  ", "  0x1234  ")
        assertEquals("ir tx Sony 0x1234\r\n", command)
    }

    @Test
    fun testFormatIrTxCommandBytes() {
        val bytes = FlipperBridge.formatIrTxCommandBytes("RC5", "0x12")
        val expectedString = "ir tx RC5 0x12\r\n"
        val actualString = String(bytes, StandardCharsets.UTF_8)
        assertEquals(expectedString, actualString)
    }

    @Test
    fun testBridgeInitialState() {
        val bridge = FlipperBridge()
        // Default close should not throw exception
        bridge.close()
        assertTrue(true)
    }
}
