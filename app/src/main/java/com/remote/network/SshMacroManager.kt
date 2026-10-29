package com.remote.network

import com.remote.db.SshMacroEntity

data class MacroExecutionLog(
    val id: Long = System.currentTimeMillis(),
    val macroName: String,
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timestamp: Long = System.currentTimeMillis()
)

class SshMacroManager {

    private val executionLogs = mutableListOf<MacroExecutionLog>()

    fun getLogs(): List<MacroExecutionLog> = executionLogs.toList()

    fun clearLogs() {
        executionLogs.clear()
    }

    fun logExecution(
        macroName: String,
        command: String,
        exitCode: Int,
        stdout: String,
        stderr: String,
        onCopyClipboard: ((String) -> Unit)? = null,
        onNonZeroExitNotification: ((String) -> Unit)? = null
    ): MacroExecutionLog {
        val log = MacroExecutionLog(
            macroName = macroName,
            command = command,
            exitCode = exitCode,
            stdout = stdout,
            stderr = stderr
        )
        executionLogs.add(0, log)

        if (onCopyClipboard != null && stdout.isNotEmpty()) {
            onCopyClipboard(stdout)
        }

        if (exitCode != 0 && onNonZeroExitNotification != null) {
            val errorMsg = stderr.ifEmpty { stdout }.ifEmpty { "Command failed with exit code $exitCode" }
            onNonZeroExitNotification("Macro '$macroName' failed ($exitCode): $errorMsg")
        }

        return log
    }

    suspend fun executeMacroWithSafety(
        macro: SshMacroEntity,
        isUserConfirmed: Boolean,
        session: SshSession,
        workingDir: String? = macro.workingDirectory,
        onCopyClipboard: ((String) -> Unit)? = null,
        onNonZeroExitNotification: ((String) -> Unit)? = null
    ): Result<MacroExecutionLog> {
        if (macro.requireConfirmation && !isUserConfirmed) {
            val dryRunMsg = "[DRY-RUN CONFIRMATION REQUIRED] Macro '${macro.name}' (Command: '${macro.command}') requires safety confirmation before execution."
            return Result.failure(IllegalStateException(dryRunMsg))
        }

        val effectiveDir = workingDir ?: macro.workingDirectory
        val result = session.executeCommand(
            command = macro.command,
            workingDir = effectiveDir,
            mode = ExecutionMode.BUFFERED_EXEC
        )

        return if (result.isSuccess) {
            val stdout = result.getOrNull() ?: ""
            val log = logExecution(
                macroName = macro.name,
                command = macro.command,
                exitCode = 0,
                stdout = stdout,
                stderr = "",
                onCopyClipboard = onCopyClipboard,
                onNonZeroExitNotification = onNonZeroExitNotification
            )
            Result.success(log)
        } else {
            val exception = result.exceptionOrNull()
            val stderr = exception?.message ?: "Unknown execution error"
            logExecution(
                macroName = macro.name,
                command = macro.command,
                exitCode = -1,
                stdout = "",
                stderr = stderr,
                onCopyClipboard = onCopyClipboard,
                onNonZeroExitNotification = onNonZeroExitNotification
            )
            Result.failure(exception ?: RuntimeException(stderr))
        }
    }
}
