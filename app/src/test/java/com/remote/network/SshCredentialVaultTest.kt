package com.remote.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SshCredentialVaultTest {

    @Test
    fun testGenerateEd25519KeyPair() {
        val (pub, priv) = SshCredentialVault.generateKeyPair("ED25519")
        assertNotNull(pub)
        assertNotNull(priv)
        assertTrue(pub.startsWith("ssh-ed25519"))
        assertTrue(priv.contains("PRIVATE KEY"))
    }

    @Test
    fun testGenerateRsaKeyPair() {
        val (pub, priv) = SshCredentialVault.generateKeyPair("RSA_SHA2_512")
        assertNotNull(pub)
        assertNotNull(priv)
        assertTrue(pub.contains("ssh-rsa"))
        assertTrue(priv.contains("PRIVATE KEY"))
    }

    @Test
    fun testEncryptAndDecryptKeyData() {
        val plainKey = "-----BEGIN OPENSSH PRIVATE KEY-----\ntestkey\n-----END OPENSSH PRIVATE KEY-----"
        val passphrase = "SecretPassphrase123"

        val encrypted = SshCredentialVault.encryptKeyData(plainKey, passphrase)
        assertNotNull(encrypted)
        assertTrue(encrypted.isNotEmpty())

        val decrypted = SshCredentialVault.decryptKeyData(encrypted, passphrase)
        assertEquals(plainKey, decrypted)
    }

    @Test
    fun testValidateAccessWithPassphraseGating() {
        val vault = SshCredentialVault()
        val plainKey = "-----BEGIN OPENSSH PRIVATE KEY-----\ntestkey\n-----END OPENSSH PRIVATE KEY-----"
        val passphrase = "MyVaultPassphrase"
        val encrypted = SshCredentialVault.encryptKeyData(plainKey, passphrase)

        val profile = SshVaultProfile(
            id = 10,
            name = "Production Server",
            host = "192.168.1.50",
            port = 22,
            username = "admin",
            encryptedPrivateKey = encrypted,
            publicKey = "ssh-ed25519 AAA...",
            requiresBiometricOrPassphrase = true
        )

        assertTrue(vault.validateAccess(profile, passphrase))
        assertFalse(vault.validateAccess(profile, ""))
    }
}
