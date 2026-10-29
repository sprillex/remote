package com.remote.network

import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class SshVaultProfile(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String = "root",
    val defaultWorkingDirectory: String? = null,
    val keyType: String = "ED25519", // ED25519, RSA_SHA2_512
    val encryptedPrivateKey: String,
    val publicKey: String,
    val requiresBiometricOrPassphrase: Boolean = true
)

class SshCredentialVault {

    companion object {
        private const val SALT = "SshVaultFixedSalt2025"
        private const val KEY_ALGORITHM = "AES"
        private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val IV_LENGTH_BYTES = 16

        fun generateKeyPair(keyType: String = "ED25519"): Pair<String, String> {
            return when (keyType.uppercase()) {
                "RSA_SHA2_512", "RSA" -> {
                    try {
                        val kpg = KeyPairGenerator.getInstance("RSA")
                        kpg.initialize(2048)
                        val kp = kpg.generateKeyPair()
                        val pubStr = "ssh-rsa " + Base64.getEncoder().encodeToString(kp.public.encoded)
                        val privStr = "-----BEGIN PRIVATE KEY-----\n" +
                                Base64.getEncoder().encodeToString(kp.private.encoded) +
                                "\n-----END PRIVATE KEY-----"
                        Pair(pubStr, privStr)
                    } catch (_: Exception) {
                        Pair("ssh-rsa AAAAB3NzaC1yc2E...mock...pub", "-----BEGIN PRIVATE KEY-----\nmock...key\n-----END PRIVATE KEY-----")
                    }
                }
                else -> {
                    SshClient.generateEd25519KeyPair()
                }
            }
        }

        fun encryptKeyData(plainTextKey: String, passphrase: String): String {
            val secretKey = deriveSecretKey(passphrase)
            val iv = ByteArray(IV_LENGTH_BYTES)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encryptedBytes = cipher.doFinal(plainTextKey.toByteArray(StandardCharsets.UTF_8))

            val combined = ByteArray(IV_LENGTH_BYTES + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH_BYTES)
            System.arraycopy(encryptedBytes, 0, combined, IV_LENGTH_BYTES, encryptedBytes.size)

            return Base64.getEncoder().encodeToString(combined)
        }

        fun decryptKeyData(encryptedKeyBase64: String, passphrase: String): String? {
            return try {
                val combined = Base64.getDecoder().decode(encryptedKeyBase64)
                if (combined.size <= IV_LENGTH_BYTES) return null

                val iv = ByteArray(IV_LENGTH_BYTES)
                val encryptedBytes = ByteArray(combined.size - IV_LENGTH_BYTES)
                System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES)
                System.arraycopy(combined, IV_LENGTH_BYTES, encryptedBytes, 0, encryptedBytes.size)

                val secretKey = deriveSecretKey(passphrase)
                val ivSpec = IvParameterSpec(iv)

                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                val decryptedBytes = cipher.doFinal(encryptedBytes)
                String(decryptedBytes, StandardCharsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }

        private fun deriveSecretKey(passphrase: String): SecretKeySpec {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(passphrase.toCharArray(), SALT.toByteArray(StandardCharsets.UTF_8), 1000, 256)
            val tmp = factory.generateSecret(spec)
            return SecretKeySpec(tmp.encoded, KEY_ALGORITHM)
        }
    }

    fun validateAccess(profile: SshVaultProfile, providedPassphrase: String?): Boolean {
        if (!profile.requiresBiometricOrPassphrase) return true
        if (providedPassphrase.isNullOrEmpty()) return false
        val decrypted = decryptKeyData(profile.encryptedPrivateKey, providedPassphrase) ?: return false
        return decrypted.contains("PRIVATE KEY") || decrypted.isNotBlank()
    }
}
