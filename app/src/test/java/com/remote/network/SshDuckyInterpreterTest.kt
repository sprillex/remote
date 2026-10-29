package com.remote.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SshDuckyInterpreterTest {

    @Test
    fun testStringAndEnterTranslation() = runBlocking {
        val interpreter = SshDuckyInterpreter()
        val script = """
            STRING echo hello
            ENTER
        """.trimIndent()

        val outputs = mutableListOf<String>()
        interpreter.executeScript(script, onSendOutput = { outputs.add(it) })

        assertEquals(listOf("echo hello", "\n"), outputs)
    }

    @Test
    fun testControlKeysAndEscapeSequences() = runBlocking {
        val interpreter = SshDuckyInterpreter()
        val script = """
            CTRL c
            TAB
            ESC
            UP
            DOWN
        """.trimIndent()

        val outputs = mutableListOf<String>()
        interpreter.executeScript(script, onSendOutput = { outputs.add(it) })

        assertEquals(listOf("\u0003", "\t", "\u001b", "\u001b[A", "\u001b[B"), outputs)
    }

    @Test
    fun testRepeatCommand() = runBlocking {
        val interpreter = SshDuckyInterpreter()
        val script = """
            STRING test
            REPEAT 2
        """.trimIndent()

        val outputs = mutableListOf<String>()
        interpreter.executeScript(script, onSendOutput = { outputs.add(it) })

        assertEquals(listOf("test", "test", "test"), outputs)
    }

    @Test
    fun testRawEscapeSequenceInjectorHexParsing() {
        val hexStr = "\\x1b\\x5b\\x41"
        val bytes = RawEscapeSequenceInjector.parseHexSequence(hexStr)

        assertEquals(3, bytes.size)
        assertEquals(0x1b.toByte(), bytes[0])
        assertEquals(0x5b.toByte(), bytes[1])
        assertEquals(0x41.toByte(), bytes[2])
    }
}
