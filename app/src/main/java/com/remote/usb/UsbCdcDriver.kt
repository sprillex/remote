package com.remote.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager

/**
 * Configuration parameters for USB CDC-ACM serial communication.
 */
data class UsbCdcConfig(
    val baudRate: Int = 115200,
    val dataBits: Int = 8,
    val stopBits: Int = STOP_BITS_1,
    val parity: Int = PARITY_NONE,
    val dtr: Boolean = true,
    val rts: Boolean = true
) {
    companion object {
        const val STOP_BITS_1 = 0
        const val STOP_BITS_1_5 = 1
        const val STOP_BITS_2 = 2

        const val PARITY_NONE = 0
        const val PARITY_ODD = 1
        const val PARITY_EVEN = 2
        const val PARITY_MARK = 3
        const val PARITY_SPACE = 4
    }
}

/**
 * Utilities for USB CDC-ACM protocol control requests and binary frame generation.
 */
object UsbCdcProtocol {
    const val SET_LINE_CODING = 0x20
    const val GET_LINE_CODING = 0x21
    const val SET_CONTROL_LINE_STATE = 0x22

    const val REQ_TYPE_SET_LINE_CODING = 0x21 // USB_DIR_OUT | USB_TYPE_CLASS | USB_RECIP_INTERFACE
    const val REQ_TYPE_SET_CONTROL_LINE_STATE = 0x21

    /**
     * Generates a 7-byte CDC SET_LINE_CODING payload.
     * Bytes 0..3: Baud rate (32-bit int, Little-Endian)
     * Byte 4: Stop bits (0=1, 1=1.5, 2=2)
     * Byte 5: Parity (0=None, 1=Odd, 2=Even, 3=Mark, 4=Space)
     * Byte 6: Data bits (e.g. 8)
     */
    fun createLineCodingPayload(config: UsbCdcConfig): ByteArray {
        val payload = ByteArray(7)
        val baud = config.baudRate
        payload[0] = (baud and 0xFF).toByte()
        payload[1] = ((baud shr 8) and 0xFF).toByte()
        payload[2] = ((baud shr 16) and 0xFF).toByte()
        payload[3] = ((baud shr 24) and 0xFF).toByte()
        payload[4] = config.stopBits.toByte()
        payload[5] = config.parity.toByte()
        payload[6] = config.dataBits.toByte()
        return payload
    }

    /**
     * Converts DTR and RTS flags into a 16-bit value for SET_CONTROL_LINE_STATE.
     * Bit 0: DTR
     * Bit 1: RTS
     */
    fun createControlLineStateValue(dtr: Boolean, rts: Boolean): Int {
        var value = 0
        if (dtr) value = value or 0x01
        if (rts) value = value or 0x02
        return value
    }
}

/**
 * USB CDC-ACM Serial Driver for USB-C IR dongles.
 */
class UsbCdcDriver(
    private val usbManager: UsbManager? = null,
    val config: UsbCdcConfig = UsbCdcConfig()
) {
    private var deviceConnection: UsbDeviceConnection? = null
    private var claimedInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null

    var isConnected: Boolean = false
        private set

    /**
     * Opens connection to the given [UsbDevice], claims the data interface, and initializes line coding.
     */
    fun openDevice(device: UsbDevice): Boolean {
        if (usbManager == null) return false

        val connection = usbManager.openDevice(device) ?: return false

        // Locate CDC endpoints (Bulk OUT and Bulk IN) across interfaces
        var targetInterface: UsbInterface? = null
        var epIn: UsbEndpoint? = null
        var epOut: UsbEndpoint? = null

        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            for (j in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(j)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN && epIn == null) {
                        epIn = ep
                    } else if (ep.direction == UsbConstants.USB_DIR_OUT && epOut == null) {
                        epOut = ep
                    }
                }
            }
            if (epIn != null || epOut != null) {
                targetInterface = iface
                if (epIn != null && epOut != null) break
            }
        }

        if (targetInterface == null || epOut == null) {
            connection.close()
            return false
        }

        if (!connection.claimInterface(targetInterface, true)) {
            connection.close()
            return false
        }

        this.deviceConnection = connection
        this.claimedInterface = targetInterface
        this.endpointIn = epIn
        this.endpointOut = epOut

        val interfaceNum = targetInterface.id
        if (!configureLineCoding(connection, interfaceNum, config)) {
            close()
            return false
        }

        this.isConnected = true
        return true
    }

    /**
     * Manually connects using pre-created [UsbDeviceConnection], [UsbInterface], and endpoints.
     * Useful for testing or custom connection initialization.
     */
    fun openWithConnection(
        connection: UsbDeviceConnection,
        iface: UsbInterface,
        outEndpoint: UsbEndpoint,
        inEndpoint: UsbEndpoint? = null
    ): Boolean {
        this.deviceConnection = connection
        this.claimedInterface = iface
        this.endpointOut = outEndpoint
        this.endpointIn = inEndpoint

        if (!configureLineCoding(connection, iface.id, config)) {
            close()
            return false
        }

        this.isConnected = true
        return true
    }

    private fun configureLineCoding(
        connection: UsbDeviceConnection,
        interfaceNum: Int,
        config: UsbCdcConfig
    ): Boolean {
        val payload = UsbCdcProtocol.createLineCodingPayload(config)
        val transferResult = connection.controlTransfer(
            UsbCdcProtocol.REQ_TYPE_SET_LINE_CODING,
            UsbCdcProtocol.SET_LINE_CODING,
            0,
            interfaceNum,
            payload,
            payload.size,
            1000
        )
        if (transferResult < 0) return false

        val controlValue = UsbCdcProtocol.createControlLineStateValue(config.dtr, config.rts)
        connection.controlTransfer(
            UsbCdcProtocol.REQ_TYPE_SET_CONTROL_LINE_STATE,
            UsbCdcProtocol.SET_CONTROL_LINE_STATE,
            controlValue,
            interfaceNum,
            null,
            0,
            1000
        )

        return true
    }

    /**
     * Writes binary payload to the connected USB CDC serial port.
     * Returns total bytes transferred, or -1 on error.
     */
    fun write(data: ByteArray, timeoutMs: Int = 1000): Int {
        if (!isConnected) return -1
        val connection = deviceConnection ?: return -1
        val epOut = endpointOut ?: return -1

        return connection.bulkTransfer(epOut, data, data.size, timeoutMs)
    }

    /**
     * Reads incoming binary payload into [buffer] from the connected USB CDC serial port.
     * Returns total bytes read, or -1 on error.
     */
    fun read(buffer: ByteArray, timeoutMs: Int = 1000): Int {
        if (!isConnected) return -1
        val connection = deviceConnection ?: return -1
        val epIn = endpointIn ?: return -1

        return connection.bulkTransfer(epIn, buffer, buffer.size, timeoutMs)
    }

    /**
     * Closes the active USB connection and releases claimed interfaces.
     */
    fun close() {
        try {
            val iface = claimedInterface
            val conn = deviceConnection
            if (conn != null && iface != null) {
                conn.releaseInterface(iface)
            }
            conn?.close()
        } catch (_: Exception) {
        } finally {
            deviceConnection = null
            claimedInterface = null
            endpointIn = null
            endpointOut = null
            isConnected = false
        }
    }
}
