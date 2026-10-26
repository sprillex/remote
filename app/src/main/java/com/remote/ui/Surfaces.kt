package com.remote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remote.db.ProfileType

@Composable
fun DeviceSurface(
    profileType: ProfileType,
    onSendKey: (usageId: Byte, modifier: Byte) -> Unit = { _, _ -> },
    onSendChar: (Char) -> Unit = {},
    onSendText: (String) -> Unit = {}
) {
    when (profileType) {
        ProfileType.PC_KEYBOARD -> FullKeyboardSurface(onSendKey, onSendChar, onSendText)
        ProfileType.TV_REMOTE -> DPadTvRemoteSurface(onSendKey)
        ProfileType.MEDIA_CONTROLLER -> MediaBarSurface(onSendKey)
        ProfileType.PRESENTATION_CLICKER -> PresentationClickerSurface(onSendKey)
        ProfileType.TERMINAL_MACRO -> MacroPadSurface(onSendKey)
        ProfileType.SSH_SESSION -> MacroPadSurface(onSendKey)
    }
}

@Composable
fun FullKeyboardSurface(
    onSendKey: (usageId: Byte, modifier: Byte) -> Unit = { _, _ -> },
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
    onSendKey: (usageId: Byte, modifier: Byte) -> Unit = { _, _ -> }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { onSendKey(0x50, 0) }) { Text("⏮ Prev") }
        Button(onClick = { onSendKey(0x2C, 0) }) { Text("⏯ Play/Pause") }
        Button(onClick = { onSendKey(0x4F, 0) }) { Text("Next ⏭") }
        Column {
            OutlinedButton(onClick = { onSendKey(0x80.toByte(), 0) }) { Text("Vol +") }
            OutlinedButton(onClick = { onSendKey(0x81.toByte(), 0) }) { Text("Vol -") }
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
