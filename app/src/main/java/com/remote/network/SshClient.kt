package com.remote.network

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class SshClient : SshClientWrapper {
    private val client = SSHClient()
    private var connected = false

    override val isConnected: Boolean
        get() = connected && client.isConnected

    override fun startSession(): SshSessionWrapper {
        if (!isConnected) {
            throw IllegalStateException("Cannot start session when not connected")
        }
        val session = client.startSession()
        return RealSshSessionWrapper(session)
    }

    fun connect(host: String, knownHostsPath: String?, privateKeyPath: String? = null) {
        if (knownHostsPath != null) {
            val file = File(knownHostsPath)
            if (file.exists()) {
                client.loadKnownHosts(file)
            } else {
                client.addHostKeyVerifier(PromiscuousVerifier())
            }
        } else {
            client.addHostKeyVerifier(PromiscuousVerifier())
        }

        client.connect(host)

        if (privateKeyPath != null) {
            client.authPublickey("root", privateKeyPath)
        }

        connected = true
    }

    fun disconnect() {
        if (client.isConnected) {
            client.disconnect()
        }
        connected = false
    }

    companion object {
        fun generateEd25519KeyPair(): Pair<String, String> {
            val publicKey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAI...mock...pub"
            val privateKey = "-----BEGIN OPENSSH PRIVATE KEY-----\nb3BlbnNzaC...mock...key\n-----END OPENSSH PRIVATE KEY-----"
            return Pair(publicKey, privateKey)
        }
    }

    private class RealSshSessionWrapper(val session: net.schmizz.sshj.connection.channel.direct.Session) : SshSessionWrapper {
        override fun exec(command: String): SshChannel {
            val cmd = session.exec(command)
            return RealCommandChannel(cmd)
        }

        override fun allocateDefaultPTY() {
            session.allocateDefaultPTY()
        }

        override fun startShell(): SshChannel {
            val shell = session.startShell()
            return RealShellChannel(shell)
        }

        override fun close() {
            session.close()
        }
    }

    private class RealCommandChannel(val channel: net.schmizz.sshj.connection.channel.direct.Session.Command) : SshChannel {
        override val inputStream: InputStream
            get() = channel.inputStream

        override val outputStream: OutputStream
            get() = channel.outputStream

        override val exitStatus: Int?
            get() = channel.exitStatus

        override fun join() {
            channel.join()
        }
    }

    private class RealShellChannel(val channel: net.schmizz.sshj.connection.channel.direct.Session.Shell) : SshChannel {
        override val inputStream: InputStream
            get() = channel.inputStream

        override val outputStream: OutputStream
            get() = channel.outputStream

        override val exitStatus: Int?
            get() = null

        override fun join() {
            channel.join()
        }
    }
}
