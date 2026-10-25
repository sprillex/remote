package com.remote.network

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.connection.channel.direct.Session.Command
import net.schmizz.sshj.connection.channel.direct.Session.Shell
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts
import java.io.File
import java.io.InputStream
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64
import java.nio.ByteBuffer
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile

class SshClientImpl : SshClientWrapper {

    private val client = SSHClient()

    override val isConnected: Boolean
        get() = client.isConnected && client.isAuthenticated

    suspend fun connect(host: String, username: String, keyFile: File, knownHostsFile: File) {
        withContext(Dispatchers.IO) {
            if (!knownHostsFile.exists()) {
                knownHostsFile.parentFile?.mkdirs()
                knownHostsFile.createNewFile()
                // Strict validation mode: do NOT automatically trust. The known_hosts file is
                // created but empty. The SSHJ client will throw an exception if the host key
                // is not in the known_hosts file (or user can implement TOFU via UI wrapper later).
            }
            client.addHostKeyVerifier(OpenSSHKnownHosts(knownHostsFile))
            client.connect(host)

            // Note: Since sshj version 0.10.0 relies on an older BouncyCastle, it expects PKCS8 formatted files
            // For standard OpenSSH key parsing in sshj 0.10.0 it uses bouncycastle PEMParser, which
            // does not support 'OPENSSH PRIVATE KEY' format natively in the version bundled with android/sshj.
            // As a fallback it will load PKCS8 keys. However, we'll write them in standard PKCS8 PEM format
            // that older BouncyCastle can read.
            val keyProvider = OpenSSHKeyFile()
            keyProvider.init(keyFile)
            client.authPublickey(username, keyProvider)
        }
    }

    fun disconnect() {
        client.disconnect()
    }

    override fun startSession(): SshSessionWrapper {
        val session = client.startSession()
        return SshSessionWrapperImpl(session)
    }

    companion object {
        fun generateEd25519Key(privateKeyFile: File, publicKeyFile: File) {
            val kpg = KeyPairGenerator.getInstance("Ed25519")
            val kp = kpg.generateKeyPair()

            val privEncoded = kp.private.encoded // PKCS#8 format
            val pubEncoded = kp.public.encoded // X.509 format

            val pubBytes = pubEncoded.copyOfRange(pubEncoded.size - 32, pubEncoded.size)

            // To ensure compatibility with sshj 0.10.0 and its internal BouncyCastle PEMParser,
            // we write the private key as standard PKCS#8 PEM rather than the newer OpenSSH v1 format.
            // sshj 0.10.0 cannot parse "BEGIN OPENSSH PRIVATE KEY" without a newer BouncyCastle provider.
            val b64Priv = Base64.getEncoder().encodeToString(privEncoded)
            val formattedPriv = b64Priv.chunked(64).joinToString("\n")
            val pkcs8Pem = "-----BEGIN PRIVATE KEY-----\n$formattedPriv\n-----END PRIVATE KEY-----\n"

            // The public key can be written in standard ssh-ed25519 format for authorized_keys
            val opensshPubKey = createOpenSshPublicKey(pubBytes)

            privateKeyFile.writeText(pkcs8Pem)
            publicKeyFile.writeText(opensshPubKey)
        }

        private fun createOpenSshPublicKey(pubBytes: ByteArray): String {
            val type = "ssh-ed25519"
            val buffer = ByteBuffer.allocate(4 + type.length + 4 + pubBytes.size)
            buffer.putInt(type.length)
            buffer.put(type.toByteArray())
            buffer.putInt(pubBytes.size)
            buffer.put(pubBytes)
            return "$type ${Base64.getEncoder().encodeToString(buffer.array())} generated-key\n"
        }
    }
}

class SshSessionWrapperImpl(private val session: Session) : SshSessionWrapper {
    override fun exec(command: String): SshChannel {
        return SshCommandChannelImpl(session.exec(command))
    }

    override fun allocateDefaultPTY() {
        session.allocateDefaultPTY()
    }

    override fun startShell(): SshChannel {
        return SshShellChannelImpl(session.startShell())
    }

    override fun close() {
        session.close()
    }
}

class SshCommandChannelImpl(private val command: Command) : SshChannel {
    override val inputStream: InputStream
        get() = command.inputStream

    override val exitStatus: Int?
        get() = command.exitStatus

    override fun join() {
        command.join()
    }
}

class SshShellChannelImpl(private val shell: Shell) : SshChannel {
    override val inputStream: InputStream
        get() = shell.inputStream

    override val exitStatus: Int?
        get() = null

    override fun join() {
        shell.join()
    }
}
