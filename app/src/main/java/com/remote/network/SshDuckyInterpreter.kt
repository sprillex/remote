package com.remote.network

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

class SshDuckyInterpreter {

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
        onSendOutput: suspend (String) -> Unit,
        onProgress: (currentLine: Int, totalLines: Int, currentCommand: String) -> Unit = { _, _, _ -> }
    ) {
        isPausedState.set(false)
        isStoppedState.set(false)
        currentState = State.RUNNING

        val lines = scriptContent.lines()
            .map { it.trim() }
            .filter { line -> line.isNotEmpty() && !line.startsWith("REM") && !line.startsWith("#") }

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
                onSendOutput = onSendOutput,
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
        onSendOutput: suspend (String) -> Unit,
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
                onSendOutput(arg)
            }

            "STRING_DELAY" -> {
                setLastCommand(line)
                val delayAndText = arg.split("\\s+".toRegex(), limit = 2)
                val charDelay = delayAndText.getOrNull(0)?.toLongOrNull() ?: 10L
                val text = if (delayAndText.size > 1) delayAndText[1] else ""
                for (ch in text) {
                    if (isStoppedState.get()) return
                    onSendOutput(ch.toString())
                    delay(charDelay)
                }
            }

            "ENTER", "RETURN" -> {
                setLastCommand(line)
                onSendOutput("\n")
            }

            "TAB" -> {
                setLastCommand(line)
                onSendOutput("\t")
            }

            "ESC", "ESCAPE" -> {
                setLastCommand(line)
                onSendOutput("\u001b")
            }

            "BACKSPACE" -> {
                setLastCommand(line)
                onSendOutput("\u007f")
            }

            "UP", "UPARROW" -> {
                setLastCommand(line)
                onSendOutput("\u001b[A")
            }

            "DOWN", "DOWNARROW" -> {
                setLastCommand(line)
                onSendOutput("\u001b[B")
            }

            "RIGHT", "RIGHTARROW" -> {
                setLastCommand(line)
                onSendOutput("\u001b[C")
            }

            "LEFT", "LEFTARROW" -> {
                setLastCommand(line)
                onSendOutput("\u001b[D")
            }

            "GUI", "WINDOWS", "COMMAND" -> {
                setLastCommand(line)
                if (arg.isNotEmpty()) {
                    onSendOutput("\u001b" + arg)
                } else {
                    onSendOutput("\u001b")
                }
            }

            "CTRL-ALT" -> {
                setLastCommand(line)
                if (arg.isNotEmpty()) {
                    val ch = arg[0]
                    onSendOutput("\u001b" + RawEscapeSequenceInjector.getCtrlCode(ch))
                }
            }

            "CTRL", "CONTROL" -> {
                setLastCommand(line)
                if (arg.isNotEmpty()) {
                    val ch = arg[0]
                    onSendOutput(RawEscapeSequenceInjector.getCtrlCode(ch))
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
                            onSendOutput = onSendOutput,
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
                if (line.startsWith("CTRL-ALT") || line.startsWith("CTRL ALT")) {
                    val token = line.substringAfter("CTRL ALT").substringAfter("CTRL-ALT").trim()
                    if (token.isNotEmpty()) {
                        onSendOutput("\u001b" + RawEscapeSequenceInjector.getCtrlCode(token[0]))
                    }
                } else if (line.startsWith("CTRL ") || line.startsWith("CONTROL ")) {
                    val token = line.substringAfter(" ").trim()
                    if (token.isNotEmpty()) {
                        onSendOutput(RawEscapeSequenceInjector.getCtrlCode(token[0]))
                    }
                } else {
                    onSendOutput(line)
                }
            }
        }
    }
}
