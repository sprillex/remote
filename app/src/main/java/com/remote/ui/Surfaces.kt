package com.remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remote.db.ProfileType
import com.remote.db.SshMacroEntity
import com.remote.network.ExecutionMode
import com.remote.network.MultiLineStager
import com.remote.network.RawEscapeSequenceInjector

@Composable
fun DeviceSurface(
    profileType: ProfileType,
    onSendKey: (usageId: Byte, modifier: Byte) -> Unit = { _, _ -> },
    onSendMediaKey: (mediaUsageId: Int) -> Unit = { _ -> },
    onSendChar: (Char) -> Unit = {},
    onSendText: (String) -> Unit = {}
) {
    when (profileType) {
        ProfileType.PC_KEYBOARD -> FullKeyboardSurface(onSendKey, onSendMediaKey, onSendChar, onSendText)
        ProfileType.TV_REMOTE -> DPadTvRemoteSurface(onSendKey)
        ProfileType.MEDIA_CONTROLLER -> MediaBarSurface(onSendMediaKey)
        ProfileType.PRESENTATION_CLICKER -> PresentationClickerSurface(onSendKey)
        ProfileType.TERMINAL_MACRO -> MacroPadSurface(onSendKey)
        ProfileType.SSH_SESSION -> SshExtendedTerminalSurface(onSendText = onSendText)
        ProfileType.ANDROID_DEVICE -> AndroidDeviceSurface(onSendKey, onSendMediaKey, onSendText)
    }
}

@Composable
fun FullKeyboardSurface(
    onSendKey: (usageId: Byte, modifier: Byte) -> Unit = { _, _ -> },
    onSendMediaKey: (mediaUsageId: Int) -> Unit = { _ -> },
    onSendChar: (Char) -> Unit = {},
    onSendText: (String) -> Unit = {}
) {
    var textInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick Text Injection Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Type text to send...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (textInput.isNotEmpty()) {
                        onSendText(textInput)
                        textInput = ""
                    }
                },
                enabled = textInput.isNotEmpty()
            ) {
                Text("Send")
            }
        }

        // Media Quick Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyButton(text = "⏮", modifier = Modifier.weight(1f)) { onSendMediaKey(com.remote.bluetooth.MediaKey.PREV_TRACK) }
            KeyButton(text = "⏯", modifier = Modifier.weight(1f)) { onSendMediaKey(com.remote.bluetooth.MediaKey.PLAY_PAUSE) }
            KeyButton(text = "⏹", modifier = Modifier.weight(1f)) { onSendMediaKey(com.remote.bluetooth.MediaKey.STOP) }
            KeyButton(text = "⏭", modifier = Modifier.weight(1f)) { onSendMediaKey(com.remote.bluetooth.MediaKey.NEXT_TRACK) }
            KeyButton(text = "🔇", modifier = Modifier.weight(1f)) { onSendMediaKey(com.remote.bluetooth.MediaKey.MUTE) }
            KeyButton(text = "🔉", modifier = Modifier.weight(1f)) { onSendMediaKey(com.remote.bluetooth.MediaKey.VOLUME_DOWN) }
            KeyButton(text = "🔊", modifier = Modifier.weight(1f)) { onSendMediaKey(com.remote.bluetooth.MediaKey.VOLUME_UP) }
        }

        // Function Row: ESC, F1..F12
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyButton(text = "ESC", modifier = Modifier.weight(1f)) { onSendKey(0x29, 0) }
            for (i in 1..6) {
                val fNum = i
                val usageId = (0x39 + fNum).toByte()
                KeyButton(text = "F$fNum", modifier = Modifier.weight(1f)) { onSendKey(usageId, 0) }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 7..12) {
                val fNum = i
                val usageId = (0x39 + fNum).toByte()
                KeyButton(text = "F$fNum", modifier = Modifier.weight(1f)) { onSendKey(usageId, 0) }
            }
            KeyButton(text = "DEL", modifier = Modifier.weight(1f)) { onSendKey(0x4C, 0) }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Number Row
        val numRow = listOf(
            '1' to (0x1E.toByte()), '2' to (0x1F.toByte()), '3' to (0x20.toByte()),
            '4' to (0x21.toByte()), '5' to (0x22.toByte()), '6' to (0x23.toByte()),
            '7' to (0x24.toByte()), '8' to (0x25.toByte()), '9' to (0x26.toByte()),
            '0' to (0x27.toByte())
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for ((char, usage) in numRow) {
                KeyButton(text = char.toString(), modifier = Modifier.weight(1f)) { onSendKey(usage, 0) }
            }
            KeyButton(text = "⌫", modifier = Modifier.weight(1.5f)) { onSendKey(0x2A, 0) }
        }

        // QWERTY Row 1
        val qrow1 = listOf('q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p')
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyButton(text = "Tab", modifier = Modifier.weight(1.2f)) { onSendKey(0x2B, 0) }
            for (char in qrow1) {
                KeyButton(text = char.uppercase(), modifier = Modifier.weight(1f)) { onSendChar(char) }
            }
        }

        // QWERTY Row 2
        val qrow2 = listOf('a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l')
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (char in qrow2) {
                KeyButton(text = char.uppercase(), modifier = Modifier.weight(1f)) { onSendChar(char) }
            }
            KeyButton(text = "Enter ↵", modifier = Modifier.weight(1.8f)) { onSendKey(0x28, 0) }
        }

        // QWERTY Row 3
        val qrow3 = listOf('z', 'x', 'c', 'v', 'b', 'n', 'm')
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (char in qrow3) {
                KeyButton(text = char.uppercase(), modifier = Modifier.weight(1f)) { onSendChar(char) }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Space & Navigation Cluster Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton(text = "Ctrl", modifier = Modifier.weight(1f)) { onSendKey(0x00, 0x01) }
            KeyButton(text = "Alt", modifier = Modifier.weight(1f)) { onSendKey(0x00, 0x04) }
            KeyButton(text = "SPACE", modifier = Modifier.weight(3f)) { onSendKey(0x2C, 0) }
            KeyButton(text = "◀", modifier = Modifier.weight(1f)) { onSendKey(0x50, 0) }
            Column(modifier = Modifier.weight(1f)) {
                KeyButton(text = "▲", modifier = Modifier.fillMaxWidth()) { onSendKey(0x52, 0) }
                KeyButton(text = "▼", modifier = Modifier.fillMaxWidth()) { onSendKey(0x51, 0) }
            }
            KeyButton(text = "▶", modifier = Modifier.weight(1f)) { onSendKey(0x4F, 0) }
        }
    }
}

@Composable
fun SshExtendedTerminalSurface(
    onSendText: (String) -> Unit = {},
    onSendFormattedCommand: (command: String, mode: ExecutionMode) -> Unit = { cmd, _ -> onSendText(cmd) }
) {
    var executionMode by remember { mutableStateOf(ExecutionMode.BUFFERED_EXEC) }
    var singleLineCommand by remember { mutableStateOf("") }
    var hexInput by remember { mutableStateOf("") }
    var stagedScript by remember { mutableStateOf("echo 'Target: {{TARGET_IP}} Interface: {{INTERFACE}}'") }
    var parameterBindings by remember { mutableStateOf(mutableMapOf("TARGET_IP" to "192.168.1.1", "INTERFACE" to "eth0")) }

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var pendingMacro by remember { mutableStateOf<SshMacroEntity?>(null) }

    var showMacroAddEditDialog by remember { mutableStateOf(false) }
    var editingMacro by remember { mutableStateOf<SshMacroEntity?>(null) }

    val stager = remember { MultiLineStager() }
    val extractedTokens = remember(stagedScript) { stager.extractTokens(stagedScript) }

    val macrosList = remember {
        mutableStateListOf(
            SshMacroEntity(id = 1, name = "Check Status", command = "systemctl status sshd", category = "prod", tags = "diagnostics"),
            SshMacroEntity(id = 2, name = "Reboot Target", command = "reboot", category = "prod", tags = "maintenance", requireConfirmation = true),
            SshMacroEntity(id = 3, name = "Disk Space", command = "df -h", category = "lab", tags = "diagnostics")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SSH Terminal & Execution Modes", style = MaterialTheme.typography.titleMedium)

        // Mode Selector Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mode: ${if (executionMode == ExecutionMode.INTERACTIVE_PTY) "Interactive PTY" else "Buffered Exec"}")
            Row {
                FilterChip(
                    selected = executionMode == ExecutionMode.BUFFERED_EXEC,
                    onClick = { executionMode = ExecutionMode.BUFFERED_EXEC },
                    label = { Text("Exec") }
                )
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(
                    selected = executionMode == ExecutionMode.INTERACTIVE_PTY,
                    onClick = { executionMode = ExecutionMode.INTERACTIVE_PTY },
                    label = { Text("PTY") }
                )
            }
        }

        // Soft Modifier Keys Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyButton(text = "Ctrl+C", modifier = Modifier.weight(1f)) { onSendFormattedCommand(RawEscapeSequenceInjector.CTRL_C, executionMode) }
            KeyButton(text = "Ctrl+Z", modifier = Modifier.weight(1f)) { onSendFormattedCommand(RawEscapeSequenceInjector.CTRL_Z, executionMode) }
            KeyButton(text = "Ctrl+\\", modifier = Modifier.weight(1f)) { onSendFormattedCommand(RawEscapeSequenceInjector.CTRL_BACKSLASH, executionMode) }
            KeyButton(text = "ESC", modifier = Modifier.weight(1f)) { onSendFormattedCommand(RawEscapeSequenceInjector.ESC, executionMode) }
            KeyButton(text = "Tab", modifier = Modifier.weight(1f)) { onSendFormattedCommand(RawEscapeSequenceInjector.TAB, executionMode) }
        }

        // Raw Hex Sequence Injection Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = hexInput,
                onValueChange = { hexInput = it },
                label = { Text("Hex / Raw Escape (e.g. \\x1b\\x5b\\x41)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val bytes = RawEscapeSequenceInjector.parseHexSequence(hexInput)
                    if (bytes.isNotEmpty()) {
                        onSendFormattedCommand(String(bytes, Charsets.UTF_8), executionMode)
                        hexInput = ""
                    }
                },
                enabled = hexInput.isNotEmpty()
            ) {
                Text("Inject")
            }
        }

        // Direct Text Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = singleLineCommand,
                onValueChange = { singleLineCommand = it },
                label = { Text("Command...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (singleLineCommand.isNotEmpty()) {
                        onSendFormattedCommand(singleLineCommand + "\n", executionMode)
                        singleLineCommand = ""
                    }
                },
                enabled = singleLineCommand.isNotEmpty()
            ) {
                Text("Send")
            }
        }

        HorizontalDivider()

        // Multi-Line Staging Drawer with Tokenization
        Text("Multi-Line Command Staging Drawer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = stagedScript,
            onValueChange = { stagedScript = it },
            label = { Text("Staged Multi-line Script (use {{TOKEN}})") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            maxLines = 5
        )

        if (extractedTokens.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Parameter Tokens Required:", style = MaterialTheme.typography.labelMedium)
                extractedTokens.forEach { token ->
                    val currentVal = parameterBindings[token] ?: ""
                    OutlinedTextField(
                        value = currentVal,
                        onValueChange = { newVal ->
                            parameterBindings[token] = newVal
                        },
                        label = { Text("{{\$token}}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Button(
            onClick = {
                val evaluated = stager.substituteTokens(stagedScript, parameterBindings)
                onSendFormattedCommand(evaluated + "\n", executionMode)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🚀 Dispatch Staged Script")
        }

        HorizontalDivider()

        // Macro Management Grid & Execution
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Saved SSH Macros", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Button(
                onClick = {
                    editingMacro = null
                    showMacroAddEditDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Macro")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Macro")
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            macrosList.forEach { macro ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(macro.name, fontWeight = FontWeight.Bold)
                            Text("${macro.category} • ${macro.tags} • ${macro.command}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                editingMacro = macro
                                showMacroAddEditDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Macro")
                            }
                            IconButton(onClick = {
                                macrosList.remove(macro)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Macro")
                            }
                            Button(
                                onClick = {
                                    if (macro.requireConfirmation) {
                                        pendingMacro = macro
                                        showConfirmationDialog = true
                                    } else {
                                        onSendFormattedCommand(macro.command + "\n", executionMode)
                                    }
                                }
                            ) {
                                Text(if (macro.requireConfirmation) "⚠️ Run" else "Run")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmationDialog && pendingMacro != null) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("⚠️ Safety Confirmation") },
            text = { Text("Macro '${pendingMacro?.name}' executes potentially destructive command:\n\n${pendingMacro?.command}\n\nAre you sure you want to proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingMacro?.let { onSendFormattedCommand(it.command + "\n", executionMode) }
                        showConfirmationDialog = false
                    }
                ) {
                    Text("Confirm Execution")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMacroAddEditDialog) {
        MacroAddEditDialog(
            initialMacro = editingMacro,
            onDismiss = { showMacroAddEditDialog = false },
            onSave = { savedMacro ->
                val existingIndex = macrosList.indexOfFirst { it.id == savedMacro.id }
                if (existingIndex >= 0) {
                    macrosList[existingIndex] = savedMacro
                } else {
                    macrosList.add(savedMacro)
                }
                showMacroAddEditDialog = false
            }
        )
    }
}

@Composable
fun MacroAddEditDialog(
    initialMacro: SshMacroEntity?,
    onDismiss: () -> Unit,
    onSave: (SshMacroEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialMacro?.name ?: "") }
    var command by remember { mutableStateOf(initialMacro?.command ?: "") }
    var category by remember { mutableStateOf(initialMacro?.category ?: "prod") }
    var tags by remember { mutableStateOf(initialMacro?.tags ?: "diagnostics") }
    var workingDirectory by remember { mutableStateOf(initialMacro?.workingDirectory ?: "") }
    var requireConfirmation by remember { mutableStateOf(initialMacro?.requireConfirmation ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialMacro == null) "Add SSH Macro" else "Edit SSH Macro") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Macro Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text("Command") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. prod, lab, router)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (e.g. diagnostics, maintenance)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = workingDirectory,
                    onValueChange = { workingDirectory = it },
                    label = { Text("Working Directory (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = requireConfirmation,
                        onCheckedChange = { requireConfirmation = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Require Safety Confirmation before execution")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || command.isBlank()) return@Button
                    val macro = SshMacroEntity(
                        id = initialMacro?.id ?: System.currentTimeMillis(),
                        name = name.trim(),
                        command = command.trim(),
                        category = category.trim(),
                        tags = tags.trim(),
                        workingDirectory = workingDirectory.ifBlank { null },
                        requireConfirmation = requireConfirmation
                    )
                    onSave(macro)
                },
                enabled = name.isNotBlank() && command.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DPadTvRemoteSurface(
    onSendKey: (usageId: Byte, modifier: Byte) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { onSendKey(0x66, 0) }) { Text("Power") }
            Button(onClick = { onSendKey(0x4A, 0) }) { Text("Home") }
            Button(onClick = { onSendKey(0x29, 0) }) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // D-Pad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { onSendKey(0x52, 0) }) { Text("▲ Up") }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onSendKey(0x50, 0) }) { Text("◀ Left") }
                Button(onClick = { onSendKey(0x28, 0) }) { Text("OK") }
                Button(onClick = { onSendKey(0x4F, 0) }) { Text("Right ▶") }
            }
            Button(onClick = { onSendKey(0x51, 0) }) { Text("▼ Down") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = { onSendKey(0x80.toByte(), 0) }) { Text("Vol +") }
            OutlinedButton(onClick = { onSendKey(0x7F.toByte(), 0) }) { Text("Mute") }
            OutlinedButton(onClick = { onSendKey(0x81.toByte(), 0) }) { Text("Vol -") }
        }
    }
}

@Composable
fun MediaBarSurface(
    onSendMediaKey: (mediaUsageId: Int) -> Unit = { _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.PREV_TRACK) }) { Text("⏮ Prev") }
            Button(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.REWIND) }) { Text("⏪ Rewind") }
            Button(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.PLAY_PAUSE) }) { Text("⏯ Play/Pause") }
            Button(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.FAST_FORWARD) }) { Text("⏩ FF") }
            Button(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.NEXT_TRACK) }) { Text("Next ⏭") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.STOP) }) { Text("⏹ Stop") }
            OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.MUTE) }) { Text("🔇 Mute") }
            OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.VOLUME_DOWN) }) { Text("🔉 Vol -") }
            OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.VOLUME_UP) }) { Text("🔊 Vol +") }
        }
    }
}

@Composable
fun MacroPadSurface(
    onSendKey: (usageId: Byte, modifier: Byte) -> Unit = { _, _ -> }
) {
    val macros = listOf(
        "M1: Enter" to { onSendKey(0x28, 0) },
        "M2: Space" to { onSendKey(0x2C, 0) },
        "M3: Tab" to { onSendKey(0x2B, 0) },
        "M4: Copy" to { onSendKey(0x06, 0x01) },  // Ctrl+C
        "M5: Paste" to { onSendKey(0x19, 0x01) }, // Ctrl+V
        "M6: Select All" to { onSendKey(0x04, 0x01) }, // Ctrl+A
        "M7: Esc" to { onSendKey(0x29, 0) },
        "M8: Backspace" to { onSendKey(0x2A, 0) },
        "M9: Del" to { onSendKey(0x4C, 0) }
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(macros.size) { index ->
            val (label, action) = macros[index]
            Button(
                onClick = action,
                modifier = Modifier.aspectRatio(1.2f)
            ) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PresentationClickerSurface(
    onSendKey: (usageId: Byte, modifier: Byte) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { onSendKey(0x4E, 0) }, // Page Down
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Text("Next Slide ▶", fontSize = 18.sp)
        }
        Button(
            onClick = { onSendKey(0x4B, 0) }, // Page Up
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Text("◀ Prev Slide", fontSize = 18.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = { onSendKey(0x3E, 0) }) { Text("Start (F5)") }
            OutlinedButton(onClick = { onSendKey(0x05, 0) }) { Text("Black Screen ('B')") }
        }
    }
}

@Composable
fun AndroidDeviceSurface(
    onSendKey: (usageId: Byte, modifier: Byte) -> Unit = { _, _ -> },
    onSendMediaKey: (mediaUsageId: Int) -> Unit = { _ -> },
    onSendText: (String) -> Unit = {}
) {
    var textInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Android Phone & Tablet Control", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Type to Android device...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (textInput.isNotEmpty()) {
                        onSendText(textInput)
                        textInput = ""
                    }
                },
                enabled = textInput.isNotEmpty()
            ) {
                Text("Send")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.BACK) }) { Text("◀ Back") }
            Button(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.HOME) }) { Text("🏠 Home") }
            Button(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.RECENTS) }) { Text("▢ Recents") }
            Button(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.NOTIFICATIONS) }) { Text("🔔 Notifs") }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { onSendKey(0x52, 0) }) { Text("▲ Up") }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onSendKey(0x50, 0) }) { Text("◀ Left") }
                Button(onClick = { onSendKey(0x28, 0) }) { Text("Select") }
                Button(onClick = { onSendKey(0x4F, 0) }) { Text("Right ▶") }
            }
            Button(onClick = { onSendKey(0x51, 0) }) { Text("▼ Down") }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Media Controls", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.PREV_TRACK) }) { Text("⏮ Prev") }
                OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.PLAY_PAUSE) }) { Text("⏯ Play/Pause") }
                OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.NEXT_TRACK) }) { Text("Next ⏭") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.VOLUME_DOWN) }) { Text("🔉 Vol -") }
                OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.MUTE) }) { Text("🔇 Mute") }
                OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.VOLUME_UP) }) { Text("🔊 Vol +") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.POWER) }) { Text("⚡ Power / Lock") }
            OutlinedButton(onClick = { onSendMediaKey(com.remote.bluetooth.MediaKey.SEARCH) }) { Text("🔍 Search") }
        }
    }
}

@Composable
fun BadUsbScriptSurface(
    onSendKey: (usageId: Byte, modifier: Byte) -> Unit = { _, _ -> },
    onSendChar: (Char) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val executor = remember { com.remote.script.BadUsbScriptExecutor() }

    var scriptText by remember {
        mutableStateOf(
            "REM Flipper BadUSB Script\n" +
            "DEFAULT_DELAY 100\n" +
            "GUI r\n" +
            "DELAY 500\n" +
            "STRING notepad\n" +
            "ENTER\n" +
            "DELAY 500\n" +
            "STRING Hello from Flipper BadUSB script execution!\n" +
            "ENTER"
        )
    }

    var progressText by remember { mutableStateOf("Idle") }
    var executionState by remember { mutableStateOf(com.remote.script.BadUsbScriptExecutor.State.IDLE) }
    var progressLine by remember { mutableStateOf(0) }
    var totalLinesCount by remember { mutableStateOf(0) }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                if (!content.isNullOrEmpty()) {
                    scriptText = content
                }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Flipper BadUSB Script Player", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = scriptText,
            onValueChange = { scriptText = it },
            label = { Text("Script Content (Flipper Format)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            maxLines = 12
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { filePickerLauncher.launch("*/*") }
            ) {
                Text("📁 Load File")
            }

            Text(
                text = "Status: $progressText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (totalLinesCount > 0 && executionState == com.remote.script.BadUsbScriptExecutor.State.RUNNING) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progressLine.toFloat() / totalLinesCount.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    if (executionState == com.remote.script.BadUsbScriptExecutor.State.PAUSED) {
                        executor.resume()
                        executionState = com.remote.script.BadUsbScriptExecutor.State.RUNNING
                        progressText = "Running..."
                    } else if (executionState == com.remote.script.BadUsbScriptExecutor.State.IDLE || executionState == com.remote.script.BadUsbScriptExecutor.State.STOPPED) {
                        coroutineScope.launch {
                            executionState = com.remote.script.BadUsbScriptExecutor.State.RUNNING
                            progressText = "Starting..."
                            executor.executeScript(
                                scriptContent = scriptText,
                                onSendKey = onSendKey,
                                onSendChar = onSendChar,
                                onProgress = { line, total, cmd ->
                                    progressLine = line
                                    totalLinesCount = total
                                    progressText = "Line $line/$total: $cmd"
                                }
                            )
                            executionState = executor.currentState
                            if (executor.currentState == com.remote.script.BadUsbScriptExecutor.State.IDLE) {
                                progressText = "Completed"
                            }
                        }
                    }
                },
                enabled = executionState != com.remote.script.BadUsbScriptExecutor.State.RUNNING
            ) {
                Text("▶ Play")
            }

            OutlinedButton(
                onClick = {
                    executor.pause()
                    executionState = com.remote.script.BadUsbScriptExecutor.State.PAUSED
                    progressText = "Paused"
                },
                enabled = executionState == com.remote.script.BadUsbScriptExecutor.State.RUNNING
            ) {
                Text("⏸ Pause")
            }

            OutlinedButton(
                onClick = {
                    executor.stop()
                    executionState = com.remote.script.BadUsbScriptExecutor.State.STOPPED
                    progressText = "Stopped"
                },
                enabled = executionState == com.remote.script.BadUsbScriptExecutor.State.RUNNING || executionState == com.remote.script.BadUsbScriptExecutor.State.PAUSED
            ) {
                Text("⏹ Stop")
            }
        }
    }
}

@Composable
private fun KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        modifier = modifier
    ) {
        Text(text = text, fontSize = 11.sp, maxLines = 1)
    }
}
