package com.remote.network

import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.PublicKey

class SshClientTest {

    private val tempDir = File("build/tmp/ssh_test")

    @Before
    fun setup() {
        tempDir.mkdirs()
    }

    @After
    fun teardown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `generateEd25519Key creates parseable private and public key files`() {
        val privKey = File(tempDir, "id_ed25519")
        val pubKey = File(tempDir, "id_ed25519.pub")

        SshClientImpl.generateEd25519Key(privKey, pubKey)

        assertTrue("Private key file should exist", privKey.exists())
        assertTrue("Public key file should exist", pubKey.exists())

        val privContent = privKey.readText()
        assertTrue("Private key should contain PKCS8 header", privContent.contains("BEGIN PRIVATE KEY"))

        val pubContent = pubKey.readText()
        assertTrue("Public key should contain header", pubContent.contains("ssh-ed25519"))
    }
}
