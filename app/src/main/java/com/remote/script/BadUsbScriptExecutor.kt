package com.remote.script

import com.remote.bluetooth.KeyTranslator
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

class BadUsbScriptExecutor {

    enum class State {
        IDLE,
        RUNNING,
        PAUSED,
        STOPPED
    }

    private val isPausedState = AtomicBoolean(false)
    private val isStoppedState = AtomicBoolean(false)

    var currentState: State = State.IDLE
        private set

    fun pause() {
        if (currentState == State.RUNNING) {
            isPausedState.set(true)
            currentState = State.PAUSED
        }
    }

    fun resume() {
        if (currentState == State.PAUSED) {
            isPausedState.set(false)
            currentState = State.RUNNING
        }
    }

    fun stop() {
        isStoppedState.set(true)
        isPausedState.set(false)
        currentState = State.STOPPED
    }

    suspend fun executeScript(
        scriptContent: String,
        onSendKey: (usageId: Byte, modifier: Byte) -> Unit,
        onSendChar: (Char) -> Unit,
        onProgress: (currentLine: Int, totalLines: Int, currentCommand: String) -> Unit = { _, _, _ -> }
    ) {
        isPausedState.set(false)
        isStoppedState.set(false)
        currentState = State.RUNNING

        val lines = scriptContent.lines()
            .map { it.trim() }
            .filter { line -> line.isNotEmpty() && !line.startsWith("REM") && !line.startsWith("#") && !line.startsWith("ID") && !line.startsWith("BT_ID") }

        var defaultDelayMs = 0L
        var lastCommand: String? = null

        for ((index, rawLine) in lines.withIndex()) {
            while (isPausedState.get() && !isStoppedState.get()) {
                delay(100)
            }
            if (isStoppedState.get()) {
                currentState = State.STOPPED
                return
            }

            val line = rawLine.trim()
            if (line.isEmpty()) continue

            onProgress(index + 1, lines.size, line)

            executeSingleLine(
                line = line,
                defaultDelayMs = defaultDelayMs,
                onSendKey = onSendKey,
                onSendChar = onSendChar,
                onSetDefaultDelay = { defaultDelayMs = it },
                getLastCommand = { lastCommand },
                setLastCommand = { lastCommand = it }
            )

            if (defaultDelayMs > 0 && !line.startsWith("DELAY") && !line.startsWith("DEFAULT_DELAY") && !line.startsWith("DEFAULTDELAY")) {
                delay(defaultDelayMs)
            }
        }

        currentState = State.IDLE
    }

    private suspend fun executeSingleLine(
        line: String,
        defaultDelayMs: Long,
        onSendKey: (usageId: Byte, modifier: Byte) -> Unit,
        onSendChar: (Char) -> Unit,
        onSetDefaultDelay: (Long) -> Unit,
        getLastCommand: () -> String?,
        setLastCommand: (String) -> Unit
    ) {
        val parts = line.split("\\s+".toRegex(), limit = 2)
        val command = parts[0].uppercase()
        val arg = if (parts.size > 1) parts[1] else ""

        when (command) {
            "DEFAULT_DELAY", "DEFAULTDELAY" -> {
                val ms = arg.toLongOrNull() ?: 0L
                onSetDefaultDelay(ms)
            }

            "DELAY" -> {
                val ms = arg.toLongOrNull() ?: 0L
                if (ms > 0) delay(ms)
            }

            "STRING" -> {
                setLastCommand(line)
                for (ch in arg) {
                    if (isStoppedState.get()) return
                    onSendChar(ch)
                    delay(10)
                }
            }

            "STRING_DELAY" -> {
                setLastCommand(line)
                val delayAndText = arg.split("\\s+".toRegex(), limit = 2)
                val charDelay = delayAndText.getOrNull(0)?.toLongOrNull() ?: 10L
                val text = if (delayAndText.size > 1) delayAndText[1] else ""
                for (ch in text) {
                    if (isStoppedState.get()) return
                    onSendChar(ch)
                    delay(charDelay)
                }
            }

            "ALTSTRING" -> {
                setLastCommand(line)
                for (ch in arg) {
                    if (isStoppedState.get()) return
                    onSendChar(ch)
                    delay(10)
                }
            }

            "REPEAT" -> {
                val count = arg.toIntOrNull() ?: 1
                val prevCmd = getLastCommand()
                if (prevCmd != null && prevCmd != line) {
                    for (i in 0 until count) {
                        if (isStoppedState.get()) return
                        executeSingleLine(
                            line = prevCmd,
                            defaultDelayMs = defaultDelayMs,
                            onSendKey = onSendKey,
                            onSendChar = onSendChar,
                            onSetDefaultDelay = onSetDefaultDelay,
                            getLastCommand = getLastCommand,
                            setLastCommand = {}
                        )
                        delay(20)
                    }
                }
            }

            else -> {
                setLastCommand(line)
                val (usageId, modifier) = parseKeyCommand(line)
                if (usageId != null || modifier != 0.toByte()) {
                    onSendKey(usageId ?: 0, modifier)
                }
            }
        }
    }

    fun parseKeyCommand(line: String): Pair<Byte?, Byte> {
        val tokens = line.split("\\s+".toRegex())
        var modifierFlags: Byte = 0
        var keyUsageId: Byte? = null

        for (token in tokens) {
            val upper = token.uppercase()
            when (upper) {
                "CTRL", "CONTROL" -> modifierFlags = (modifierFlags.toInt() or 0x01).toByte()
                "SHIFT" -> modifierFlags = (modifierFlags.toInt() or 0x02).toByte()
                "ALT" -> modifierFlags = (modifierFlags.toInt() or 0x04).toByte()
                "GUI", "WINDOWS", "COMMAND" -> modifierFlags = (modifierFlags.toInt() or 0x08).toByte()
                else -> {
                    keyUsageId = parseKeyNameToUsageId(upper)
                }
            }
        }

        return Pair(keyUsageId, modifierFlags)
    }

    private fun parseKeyNameToUsageId(keyName: String): Byte? {
        return when (keyName) {
            "ENTER", "RETURN" -> 0x28
            "ESC", "ESCAPE" -> 0x29
            "BACKSPACE" -> 0x2A
            "TAB" -> 0x2B
            "SPACE" -> 0x2C
            "CAPSLOCK" -> 0x39
            "F1" -> 0x3A
            "F2" -> 0x3B
            "F3" -> 0x3C
            "F4" -> 0x3D
            "F5" -> 0x3E
            "F6" -> 0x3F
            "F7" -> 0x40
            "F8" -> 0x41
            "F9" -> 0x42
            "F10" -> 0x43
            "F11" -> 0x44
            "F12" -> 0x45
            "PRINTSCREEN" -> 0x46
            "SCROLLLOCK" -> 0x47
            "PAUSE" -> 0x48
            "INSERT" -> 0x49
            "HOME" -> 0x4A
            "PAGEUP" -> 0x4B
            "DELETE", "DEL" -> 0x4C
            "END" -> 0x4D
            "PAGEDOWN" -> 0x4E
            "RIGHT", "RIGHTARROW" -> 0x4F
            "LEFT", "LEFTARROW" -> 0x50
            "DOWN", "DOWNARROW" -> 0x51
            "UP", "UPARROW" -> 0x52
            else -> {
                if (keyName.length == 1) {
                    val ch = keyName[0]
                    KeyTranslator.translate(ch.lowercaseChar())?.usageId
                } else null
            }
        }
    }
}
