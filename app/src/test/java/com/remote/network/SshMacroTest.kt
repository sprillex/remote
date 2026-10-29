package com.remote.network

import com.remote.db.SshMacroEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class SshMacroTest {

    private class MockSshChannel(output: String, override val exitStatus: Int? = 0) : SshChannel {
        override val inputStream: InputStream = ByteArrayInputStream(output.toByteArray())
        override fun join() {}
    }

    private class MockSshSessionWrapper(private val execOutput: String, private val exitCode: Int = 0) : SshSessionWrapper {
        override fun exec(command: String): SshChannel = MockSshChannel(execOutput, exitCode)
        override fun allocateDefaultPTY() {}
        override fun startShell(): SshChannel = MockSshChannel(execOutput)
        override fun close() {}
    }

    private class MockSshClientWrapper(private val sessionWrapper: SshSessionWrapper) : SshClientWrapper {
        override val isConnected: Boolean = true
        override fun startSession(): SshSessionWrapper = sessionWrapper
    }

    @Test
    fun testExecuteMacroWithSafetyConfirmed() = runBlocking {
        val manager = SshMacroManager()
        val mockClient = MockSshClientWrapper(MockSshSessionWrapper("sshd is active"))
        val session = SshSession(mockClient)

        val macro = SshMacroEntity(
            id = 1,
            name = "Check SSH",
            command = "systemctl status sshd",
            requireConfirmation = true
        )

        var clipboardText = ""
        val result = manager.executeMacroWithSafety(
            macro = macro,
            isUserConfirmed = true,
            session = session,
            onCopyClipboard = { clipboardText = it }
        )

        assertTrue(result.isSuccess)
        val log = result.getOrNull()
        assertEquals("Check SSH", log?.macroName)
        assertEquals("sshd is active", log?.stdout)
        assertEquals("sshd is active", clipboardText)
    }

    @Test
    fun testExecuteMacroWithSafetyUnconfirmedFailsDryRun() = runBlocking {
        val manager = SshMacroManager()
        val mockClient = MockSshClientWrapper(MockSshSessionWrapper(""))
        val session = SshSession(mockClient)

        val macro = SshMacroEntity(
            id = 2,
            name = "Reboot Host",
            command = "reboot",
            requireConfirmation = true
        )

        val result = manager.executeMacroWithSafety(
            macro = macro,
            isUserConfirmed = false,
            session = session
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(result.exceptionOrNull()?.message?.contains("DRY-RUN CONFIRMATION REQUIRED") == true)
    }

    @Test
    fun testNonZeroExitNotificationTrigger() = runBlocking {
        val manager = SshMacroManager()
        val mockClient = MockSshClientWrapper(MockSshSessionWrapper("Permission denied", exitCode = 1))
        val session = SshSession(mockClient)

        val macro = SshMacroEntity(
            id = 3,
            name = "Failed Task",
            command = "cat /etc/shadow",
            requireConfirmation = false
        )

        var notification = ""
        val result = manager.executeMacroWithSafety(
            macro = macro,
            isUserConfirmed = true,
            session = session,
            onNonZeroExitNotification = { notification = it }
        )

        assertTrue(result.isFailure)
        assertTrue(notification.contains("Macro 'Failed Task' failed"))
    }
}
