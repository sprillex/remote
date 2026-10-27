package com.remote.network

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class SshSessionTest {

    private class MockSshChannel(output: String, override val exitStatus: Int? = 0) : SshChannel {
        override val inputStream: InputStream = ByteArrayInputStream(output.toByteArray())
        override fun join() {}
    }

    private class MockSshSessionWrapper(
        private val execOutput: String,
        private val execExitStatus: Int = 0,
        private val shellOutput: String = ""
    ) : SshSessionWrapper {
        var closed = false
        var ptyAllocated = false

        override fun exec(command: String): SshChannel {
            return MockSshChannel(execOutput, execExitStatus)
        }

        override fun allocateDefaultPTY() {
            ptyAllocated = true
        }

        override fun startShell(): SshChannel {
            return MockSshChannel(shellOutput)
        }

        override fun close() {
            closed = true
        }
    }

    private class MockSshClientWrapper(
        override val isConnected: Boolean,
        private val sessionWrapper: SshSessionWrapper = MockSshSessionWrapper("")
    ) : SshClientWrapper {
        override fun startSession(): SshSessionWrapper = sessionWrapper
    }

    @Test
    fun `executeCommand returns success when connected and command succeeds`() = runBlocking {
        val sessionWrapper = MockSshSessionWrapper(execOutput = "hello world\n")
        val client = MockSshClientWrapper(isConnected = true, sessionWrapper = sessionWrapper)
        val sshSession = SshSession(client)

        val result = sshSession.executeCommand("echo 'hello world'")

        assertTrue(result.isSuccess)
        assertEquals("hello world", result.getOrNull())
        assertTrue(sessionWrapper.closed)
    }

    @Test
    fun `executeCommand returns failure when not connected`() = runBlocking {
        val client = MockSshClientWrapper(isConnected = false)
        val sshSession = SshSession(client)

        val result = sshSession.executeCommand("ls")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `executeCommand returns failure when command fails`() = runBlocking {
        val sessionWrapper = MockSshSessionWrapper(execOutput = "command not found", execExitStatus = 127)
        val client = MockSshClientWrapper(isConnected = true, sessionWrapper = sessionWrapper)
        val sshSession = SshSession(client)

        val result = sshSession.executeCommand("invalid_cmd")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
        assertTrue(result.exceptionOrNull()?.message?.contains("127") == true)
        assertTrue(sessionWrapper.closed)
    }

    @Test
    fun `startPtyShell emits output and closes session`() = runBlocking {
        val sessionWrapper = MockSshSessionWrapper(execOutput = "", shellOutput = "prompt$ ")
        val client = MockSshClientWrapper(isConnected = true, sessionWrapper = sessionWrapper)
        val sshSession = SshSession(client)

        val flowOutput = sshSession.startPtyShell().toList()

        assertEquals(listOf("prompt$ "), flowOutput)
        assertTrue(sessionWrapper.ptyAllocated)
        assertTrue(sessionWrapper.closed)
    }

    @Test(expected = java.lang.IllegalStateException::class)
    fun `startPtyShell throws when not connected`() {
        runBlocking {
            val client = MockSshClientWrapper(isConnected = false)
            val sshSession = SshSession(client)

            // Flow should throw on collection
            sshSession.startPtyShell().toList()
        }
    }
}
