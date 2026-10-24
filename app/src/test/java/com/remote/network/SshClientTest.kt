package com.remote.network

import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator

class SshClientTest {

    @Test
    fun testKeyGeneration() {
        val client = SshClient("localhost", 22, "user")
        val keyPair = client.generateEd25519KeyPair()
        assertNotNull(keyPair)
        assertEquals("EdDSA", keyPair.private.algorithm)
    }

    @Test
    fun testConnectWithoutHostKey() {
        val client = SshClient("localhost", 22, "user")
        client.connect()
        assertTrue(client.isConnected)
        client.disconnect()
        assertFalse(client.isConnected)
    }

    @Test
    fun testConnectWithValidHostKey() {
        val expectedKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair().public
        // Mocking: We override validateKnownHost for this test to pretend server returned the right key
        val client = object : SshClient("localhost", 22, "user", expectedKey) {
            override fun validateKnownHost(serverKey: java.security.PublicKey): Boolean {
                return true
            }
        }
        client.connect()
        assertTrue(client.isConnected)
    }

    @Test
    fun testConnectWithInvalidHostKey() {
        val expectedKey = KeyPairGenerator.getInstance("Ed25519").generateKeyPair().public
        val client = SshClient("localhost", 22, "user", expectedKey)
        try {
            client.connect()
            fail("Expected SecurityException")
        } catch (e: SecurityException) {
            assertEquals("Host key verification failed.", e.message)
        }
    }
}
