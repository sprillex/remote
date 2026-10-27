package com.remote.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

// We define a stub interface to avoid a hard dependency on sshj since we cannot add dependencies.
// The real implementation would wrap the actual net.schmizz.sshj.SSHClient or similar.
interface SshChannel {
    val inputStream: InputStream
    val exitStatus: Int?
    fun join()
}

interface SshSessionWrapper {
    fun exec(command: String): SshChannel
    fun allocateDefaultPTY()
    fun startShell(): SshChannel
    fun close()
}

interface SshClientWrapper {
    val isConnected: Boolean
    fun startSession(): SshSessionWrapper
}

class SshSession(private val sshClient: SshClientWrapper) {

    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        if (!sshClient.isConnected) {
            return@withContext Result.failure(IllegalStateException("SSHClient is not connected"))
        }
        var session: SshSessionWrapper? = null
        try {
            session = sshClient.startSession()
            val cmd = session.exec(command)
            val output = runInterruptible {
                cmd.join()
                cmd.inputStream.bufferedReader().use { it.readText() }
            }
            if (cmd.exitStatus != null && cmd.exitStatus != 0) {
                Result.failure(RuntimeException("Command failed with exit status ${cmd.exitStatus}: $output"))
            } else {
                Result.success(output.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            session?.close()
        }
    }

    fun startPtyShell(): Flow<String> = flow {
        if (!sshClient.isConnected) {
            throw IllegalStateException("SSHClient is not connected")
        }
        var session: SshSessionWrapper? = null
        try {
            session = sshClient.startSession()
            session.allocateDefaultPTY()
            val shell = session.startShell()

            val inputStream = shell.inputStream
            val buffer = ByteArray(1024)
            while (currentCoroutineContext().isActive) {
                yield()

                val bytesRead = runInterruptible {
                    inputStream.read(buffer)
                }

                if (bytesRead == -1) break
                emit(String(buffer, 0, bytesRead))
            }
        } finally {
            session?.close()
        }
    }.catch { e ->
        if (e is IllegalStateException && e.message == "SSHClient is not connected") {
            throw e
        } else {
            emit("Connection dropped: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}
