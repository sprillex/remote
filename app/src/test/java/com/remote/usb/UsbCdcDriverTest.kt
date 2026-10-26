package com.remote.usb

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsbCdcDriverTest {

    @Test
    fun testLineCodingPayloadGeneration_default115200() {
        val config = UsbCdcConfig(
            baudRate = 115200,
            dataBits = 8,
            stopBits = UsbCdcConfig.STOP_BITS_1,
            parity = UsbCdcConfig.PARITY_NONE
        )
        val payload = UsbCdcProtocol.createLineCodingPayload(config)

        assertEquals(7, payload.size)
        // 115200 in hexadecimal = 0x0001C200 -> [0x00, 0xC2, 0x01, 0x00]
        assertEquals(0x00.toByte(), payload[0])
        assertEquals(0xC2.toByte(), payload[1])
        assertEquals(0x01.toByte(), payload[2])
        assertEquals(0x00.toByte(), payload[3])
        assertEquals(0.toByte(), payload[4]) // 1 stop bit
        assertEquals(0.toByte(), payload[5]) // no parity
        assertEquals(8.toByte(), payload[6]) // 8 data bits
    }

    @Test
    fun testLineCodingPayloadGeneration_customBaudAndParity() {
        val config = UsbCdcConfig(
            baudRate = 9600,
            dataBits = 7,
            stopBits = UsbCdcConfig.STOP_BITS_2,
            parity = UsbCdcConfig.PARITY_EVEN
        )
        val payload = UsbCdcProtocol.createLineCodingPayload(config)

        assertEquals(7, payload.size)
        // 9600 in hexadecimal = 0x00002580 -> [0x80, 0x25, 0x00, 0x00]
        assertEquals(0x80.toByte(), payload[0])
        assertEquals(0x25.toByte(), payload[1])
        assertEquals(0x00.toByte(), payload[2])
        assertEquals(0x00.toByte(), payload[3])
        assertEquals(2.toByte(), payload[4]) // 2 stop bits
        assertEquals(2.toByte(), payload[5]) // even parity
        assertEquals(7.toByte(), payload[6]) // 7 data bits
    }

    @Test
    fun testControlLineStateValue() {
        assertEquals(0x00, UsbCdcProtocol.createControlLineStateValue(dtr = false, rts = false))
        assertEquals(0x01, UsbCdcProtocol.createControlLineStateValue(dtr = true, rts = false))
        assertEquals(0x02, UsbCdcProtocol.createControlLineStateValue(dtr = false, rts = true))
        assertEquals(0x03, UsbCdcProtocol.createControlLineStateValue(dtr = true, rts = true))
    }

    @Test
    fun testDriverInitialStateAndUnconnectedOperations() {
        val driver = UsbCdcDriver()
        assertFalse(driver.isConnected)

        val writeResult = driver.write(byteArrayOf(0x01, 0x02))
        assertEquals(-1, writeResult)

        val buffer = ByteArray(10)
        val readResult = driver.read(buffer)
        assertEquals(-1, readResult)

        driver.close()
        assertFalse(driver.isConnected)
    }

    @Test
    fun testOpenDeviceNullManagerFailsSafely() {
        val driver = UsbCdcDriver(usbManager = null)
        assertFalse(driver.isConnected)
    }
}
