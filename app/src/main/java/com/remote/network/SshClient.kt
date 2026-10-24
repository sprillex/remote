package com.remote.network

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.interfaces.EdECPublicKey

// Define dummy SshClient class fulfilling task requirement for interface representation without actual sshj.

interface SshClientInterface {
    val host: String
    val port: Int
    val username: String

    fun generateEd25519KeyPair(): KeyPair
    fun connect()
    fun disconnect()
    val isConnected: Boolean
    fun validateKnownHost(serverKey: PublicKey): Boolean
}

open class SshClient(
    override val host: String,
    override val port: Int = 22,
    override val username: String,
    private val expectedHostKey: PublicKey? = null
) : SshClientInterface, SshClientWrapper {

    private var connected: Boolean = false

    override fun generateEd25519KeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("Ed25519")
        return kpg.generateKeyPair()
    }

    override fun connect() {
        // simulate known_hosts validation
        if (expectedHostKey != null) {
            val serverKey = generateEd25519KeyPair().public // Mocking server key retrieved
            if (!validateKnownHost(serverKey)) {
                throw SecurityException("Host key verification failed.")
            }
        }
        connected = true
    }

    override fun disconnect() {
        connected = false
    }

    override val isConnected: Boolean
        get() = connected

    override fun validateKnownHost(serverKey: PublicKey): Boolean {
        return expectedHostKey == null || expectedHostKey == serverKey
    }

    override fun startSession(): SshSessionWrapper {
        if (!connected) throw IllegalStateException("Not connected")
        return MockSessionWrapper()
    }

    private class MockSessionWrapper : SshSessionWrapper {
        override fun exec(command: String): SshChannel {
            return object : SshChannel {
                override val inputStream: java.io.InputStream = java.io.ByteArrayInputStream("".toByteArray())
                override val exitStatus: Int? = 0
                override fun join() {}
            }
        }
        override fun allocateDefaultPTY() {}
        override fun startShell(): SshChannel {
            return object : SshChannel {
                override val inputStream: java.io.InputStream = java.io.ByteArrayInputStream("".toByteArray())
                override val exitStatus: Int? = 0
                override fun join() {}
            }
        }
        override fun close() {}
    }
}
