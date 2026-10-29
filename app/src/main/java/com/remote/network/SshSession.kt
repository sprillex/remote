package com.remote.network

import com.remote.db.SshMacroEntity
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

enum class ExecutionMode {
    INTERACTIVE_PTY,
    BUFFERED_EXEC
}

// Interface wrapper to handle SSH channels and streams
interface SshChannel {
    val inputStream: InputStream
    val outputStream: OutputStream?
        get() = null
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

    val macroManager = SshMacroManager()
    val keepAliveManager = SshKeepAliveManager()
    val duckyInterpreter = SshDuckyInterpreter()
    val multiplexer = SshMultiplexer()

    suspend fun executeCommand(
        command: String,
        workingDir: String? = null,
        mode: ExecutionMode = ExecutionMode.BUFFERED_EXEC
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!sshClient.isConnected) {
            return@withContext Result.failure(IllegalStateException("SSHClient is not connected"))
        }
        var session: SshSessionWrapper? = null
        try {
            session = sshClient.startSession()
            val effectiveCmd = if (!workingDir.isNullOrBlank()) {
                "cd $workingDir && $command"
            } else {
                command
            }

            if (mode == ExecutionMode.BUFFERED_EXEC) {
                val cmd = session.exec(effectiveCmd)
                val output = runInterruptible {
                    cmd.join()
                    cmd.inputStream.bufferedReader().use { it.readText() }
                }
                if (cmd.exitStatus != null && cmd.exitStatus != 0) {
                    Result.failure(RuntimeException("Command failed with exit status ${cmd.exitStatus}: $output"))
                } else {
                    Result.success(output.trim())
                }
            } else {
                session.allocateDefaultPTY()
                val shell = session.startShell()
                shell.outputStream?.let { out ->
                    out.write("$effectiveCmd\n".toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                val output = runInterruptible {
                    val buf = ByteArray(2048)
                    val readBytes = shell.inputStream.read(buf)
                    if (readBytes > 0) String(buf, 0, readBytes) else ""
                }
                Result.success(output.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            session?.close()
        }
    }

    suspend fun executeMacro(
        macro: SshMacroEntity,
        isUserConfirmed: Boolean,
        onCopyClipboard: ((String) -> Unit)? = null,
        onNonZeroNotification: ((String) -> Unit)? = null
    ): Result<MacroExecutionLog> {
        return macroManager.executeMacroWithSafety(
            macro = macro,
            isUserConfirmed = isUserConfirmed,
            session = this,
            workingDir = macro.workingDirectory,
            onCopyClipboard = onCopyClipboard,
            onNonZeroExitNotification = onNonZeroNotification
        )
    }

    suspend fun executeDuckyScript(
        scriptContent: String,
        mode: ExecutionMode = ExecutionMode.BUFFERED_EXEC,
        onProgress: (currentLine: Int, totalLines: Int, currentCommand: String) -> Unit = { _, _, _ -> }
    ): Result<Unit> {
        return try {
            duckyInterpreter.executeScript(
                scriptContent = scriptContent,
                onSendOutput = { output ->
                    executeCommand(output, mode = mode)
                },
                onProgress = onProgress
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startPtyShell(workingDir: String? = null): Flow<String> = flow {
        if (!sshClient.isConnected) {
            throw IllegalStateException("SSHClient is not connected")
        }
        var session: SshSessionWrapper? = null
        try {
            session = sshClient.startSession()
            session.allocateDefaultPTY()
            val shell = session.startShell()

            if (!workingDir.isNullOrBlank()) {
                shell.outputStream?.let { out ->
                    out.write("cd $workingDir\n".toByteArray(Charsets.UTF_8))
                    out.flush()
                }
            }

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
    }
    .catch { e ->
        if (e is IllegalStateException) {
            throw e
        } else {
            emit("Error: ${e.message}")
        }
    }
    .flowOn(Dispatchers.IO)
}
