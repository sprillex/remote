package com.remote.script

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadUsbScriptExecutorTest {

    @Test
    fun testParseKeyCommandSingleKey() {
        val executor = BadUsbScriptExecutor()

        val (usageId, modifier) = executor.parseKeyCommand("ENTER")
        assertEquals(0x28.toByte(), usageId)
        assertEquals(0.toByte(), modifier)

        val (escUsage, escMod) = executor.parseKeyCommand("ESC")
        assertEquals(0x29.toByte(), escUsage)
        assertEquals(0.toByte(), escMod)
    }

    @Test
    fun testParseKeyCommandModifiers() {
        val executor = BadUsbScriptExecutor()

        val (rUsage, guiMod) = executor.parseKeyCommand("GUI r")
        assertEquals(0x15.toByte(), rUsage) // 'r'
        assertEquals(0x08.toByte(), guiMod) // GUI / WIN modifier

        val (cUsage, ctrlMod) = executor.parseKeyCommand("CTRL c")
        assertEquals(0x06.toByte(), cUsage) // 'c'
        assertEquals(0x01.toByte(), ctrlMod) // Ctrl modifier

        val (tabUsage, altMod) = executor.parseKeyCommand("ALT TAB")
        assertEquals(0x2B.toByte(), tabUsage) // Tab
        assertEquals(0x04.toByte(), altMod) // Alt modifier

        val (escUsage, ctrlShiftMod) = executor.parseKeyCommand("CTRL SHIFT ESC")
        assertEquals(0x29.toByte(), escUsage) // ESC
        assertEquals((0x01 or 0x02).toByte(), ctrlShiftMod) // Ctrl + Shift
    }

    @Test
    fun testExecuteScriptStringAndKeys() = runBlocking {
        val executor = BadUsbScriptExecutor()
        val script = """
            REM Test Script
            DEFAULT_DELAY 10
            STRING Hello World
            ENTER
        """.trimIndent()

        val sentKeys = mutableListOf<Pair<Byte, Byte>>()
        val sentChars = mutableListOf<Char>()
        val progressList = mutableListOf<String>()

        executor.executeScript(
            scriptContent = script,
            onSendKey = { usage, mod -> sentKeys.add(Pair(usage, mod)) },
            onSendChar = { ch -> sentChars.add(ch) },
            onProgress = { line, total, cmd -> progressList.add(cmd) }
        )

        assertEquals("Hello World", sentChars.joinToString(""))
        assertEquals(1, sentKeys.size)
        assertEquals(0x28.toByte(), sentKeys[0].first) // Enter key
        assertTrue(progressList.isNotEmpty())
    }

    @Test
    fun testExecuteScriptRepeatCommand() = runBlocking {
        val executor = BadUsbScriptExecutor()
        val script = """
            ENTER
            REPEAT 3
        """.trimIndent()

        val sentKeys = mutableListOf<Pair<Byte, Byte>>()

        executor.executeScript(
            scriptContent = script,
            onSendKey = { usage, mod -> sentKeys.add(Pair(usage, mod)) },
            onSendChar = {}
        )

        // 1 initial enter + 3 repeats = 4 total enter key presses
        assertEquals(4, sentKeys.size)
        for (key in sentKeys) {
            assertEquals(0x28.toByte(), key.first)
        }
    }

    @Test
    fun testExecuteScriptStopControl() = runBlocking {
        val executor = BadUsbScriptExecutor()
        val script = """
            STRING line1
            DELAY 5000
            STRING line2
        """.trimIndent()

        val sentChars = mutableListOf<Char>()

        @Suppress("DEPRECATION")
        val job = GlobalScope.launch(Dispatchers.Default) {
            executor.executeScript(
                scriptContent = script,
                onSendKey = { _, _ -> },
                onSendChar = { ch -> sentChars.add(ch) }
            )
        }

        delay(100)
        executor.stop()
        job.join()

        assertEquals(BadUsbScriptExecutor.State.STOPPED, executor.currentState)
    }
}
