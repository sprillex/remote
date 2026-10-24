package com.remote.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RokuClientTest {

    private val client = RokuClient()

    @Test
    fun testBuildUrl_DefaultPort() {
        val url = client.buildUrl("192.168.1.100", path = "keypress/Home")
        assertEquals("http://192.168.1.100:8060/keypress/Home", url)
    }

    @Test
    fun testBuildUrl_CustomPort() {
        val url = client.buildUrl("192.168.1.100", port = 8080, path = "keypress/Select")
        assertEquals("http://192.168.1.100:8080/keypress/Select", url)
    }

    @Test
    fun testRokuKeyConstants() {
        assertEquals("Home", RokuClient.KEY_HOME)
        assertEquals("Back", RokuClient.KEY_BACK)
        assertEquals("Select", RokuClient.KEY_SELECT)
        assertEquals("Power", RokuClient.KEY_POWER)
        assertEquals("Play", RokuClient.KEY_PLAY)
        assertEquals("VolumeUp", RokuClient.KEY_VOLUME_UP)
    }

    @Test
    fun testSendKeypress_InvalidHost_ReturnsFalse() = runBlocking {
        // A non-routable IP with a short timeout will fail quickly and verify exception handling
        val shortTimeoutClient = RokuClient(connectTimeoutMs = 10, readTimeoutMs = 10)
        val result = shortTimeoutClient.sendKeypress("10.255.255.255", RokuClient.KEY_HOME)
        assertTrue("Expected failure for invalid host", result.isFailure)
    }
}
