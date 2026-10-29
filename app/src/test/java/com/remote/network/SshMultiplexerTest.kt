package com.remote.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class SshMultiplexerTest {

    private class MockSshChannel(output: String, override val exitStatus: Int? = 0) : SshChannel {
        override val inputStream: InputStream = ByteArrayInputStream(output.toByteArray())
        override fun join() {}
    }

    private class MockSshSessionWrapper(private val execOutput: String) : SshSessionWrapper {
        override fun exec(command: String): SshChannel = MockSshChannel(execOutput)
        override fun allocateDefaultPTY() {}
        override fun startShell(): SshChannel = MockSshChannel(execOutput)
        override fun close() {}
    }

    private class MockSshClientWrapper(private val sessionWrapper: SshSessionWrapper) : SshClientWrapper {
        override val isConnected: Boolean = true
        override fun startSession(): SshSessionWrapper = sessionWrapper
    }

    @Test
    fun testRegisterAndBroadcastExecute() = runBlocking {
        val multiplexer = SshMultiplexer()

        val session1 = SshSession(MockSshClientWrapper(MockSshSessionWrapper("host1 ok")))
        val session2 = SshSession(MockSshClientWrapper(MockSshSessionWrapper("host2 ok")))

        multiplexer.registerSession("host1.local", session1)
        multiplexer.registerSession("host2.local", session2)

        assertEquals(2, multiplexer.getActiveSessionIds().size)

        val results = multiplexer.broadcastExecute("uptime")

        assertEquals(2, results.size)
        val r1 = results.find { it.sessionId == "host1.local" }
        val r2 = results.find { it.sessionId == "host2.local" }

        assertTrue(r1?.result?.isSuccess == true)
        assertEquals("host1 ok", r1?.result?.getOrNull())

        assertTrue(r2?.result?.isSuccess == true)
        assertEquals("host2 ok", r2?.result?.getOrNull())
    }
}
