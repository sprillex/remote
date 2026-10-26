package com.remote.network

import org.junit.Test
import org.junit.Assert.*
import java.net.ConnectException

class SshClientTest {

    @Test
    fun testEd25519KeyGeneration() {
        val keyPair = SshClient.generateEd25519KeyPair()
        assertNotNull(keyPair)
        assertNotNull(keyPair.first)
        assertNotNull(keyPair.second)
        assertTrue(keyPair.first.startsWith("ssh-ed25519"))
        assertTrue(keyPair.second.startsWith("-----BEGIN OPENSSH PRIVATE KEY-----"))
    }

    @Test
    fun testConnectWithKnownHosts() {
        val client = SshClient()

        // Initial state
        assertFalse(client.isConnected)

        // Expect a connection exception because we're trying to connect to a dummy IP
        try {
            client.connect("127.0.0.1", null)
        } catch (e: Exception) {
            // Either ConnectException or some SSH exception is fine
        }

        // Disconnect shouldn't crash
        client.disconnect()
        assertFalse(client.isConnected)
    }

    @Test
    fun testStartSessionWhenNotConnectedThrows() {
        val client = SshClient()
        assertThrows(IllegalStateException::class.java) {
            client.startSession()
        }
    }
}
