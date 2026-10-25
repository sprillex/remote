package com.remote.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.junit.Assert.assertTrue
import kotlinx.coroutines.cancelAndJoin

class SsdpDiscoveryTest {

    private val discovery = SsdpDiscovery()

    @Test
    fun testParseLocationIp_ValidHeader() {
        val response = """
            HTTP/1.1 200 OK
            Cache-Control: max-age=3600
            ST: roku:ecp
            USN: uuid:roku:ecp:1234567890
            Ext:
            Server: Roku UPnP/1.0 MiniUPnPd/1.4
            LOCATION: http://192.168.1.15:8060/
        """.trimIndent()

        val ip = discovery.parseLocationIp(response)
        assertEquals("192.168.1.15", ip)
    }

    @Test
    fun testParseLocationIp_ValidHeader_MixedCase() {
        val response = """
            HTTP/1.1 200 OK
            Location: http://10.0.0.5:8060/
        """.trimIndent()

        val ip = discovery.parseLocationIp(response)
        assertEquals("10.0.0.5", ip)
    }

    @Test
    fun testParseLocationIp_InvalidOrMissingHeader() {
        val response = """
            HTTP/1.1 200 OK
            Cache-Control: max-age=3600
            ST: roku:ecp
            Server: Roku UPnP/1.0
        """.trimIndent()

        val ip = discovery.parseLocationIp(response)
        assertNull(ip)
    }

    @Test
    fun testGetSubnetAddresses_ValidIp() {
        val ips = discovery.getSubnetAddresses("192.168.1.100")
        assertEquals(254, ips.size)
        assertEquals("192.168.1.1", ips.first())
        assertEquals("192.168.1.254", ips.last())
        assertEquals("192.168.1.15", ips[14])
    }

    @Test
    fun testGetSubnetAddresses_InvalidIp() {
        val ips = discovery.getSubnetAddresses("invalid_ip")
        assertEquals(0, ips.size)

        val ips2 = discovery.getSubnetAddresses("192.168.1")
        assertEquals(0, ips2.size)
    }

    @Test
    fun testPortOpenCancellation() = runBlocking {
        // Create an instance of SsdpDiscovery with 0ms ssdp and 5000ms sweep timeout
        val testDiscovery = SsdpDiscovery(ssdpTimeoutMs = 0, sweepTimeoutMs = 5000)

        val time = measureTimeMillis {
            val job = launch(Dispatchers.IO) {
                // Give it a blackhole IP subnet. The sweep will try to connect to 10.255.255.1-254
                // Each would block for 5000ms if not cancelled.
                testDiscovery.discoverRokuDevices("10.255.255.10")
            }
            delay(500)
            job.cancelAndJoin()
        }

        // Ensure it took less time than the timeout
        assertTrue("Cancellation should stop connection promptly, took $time ms", time < 2000)
    }
}
