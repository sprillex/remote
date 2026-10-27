package com.remote.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remote.bluetooth.HidDeviceManager
import com.remote.bluetooth.KeyTranslator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

data class InjectionResult(
    val totalChars: Int,
    val injectedChars: Int,
    val skippedChars: Int
)

object PrecomposeBufferInjector {
    private val keyUpReport = ByteArray(8)

    suspend fun injectText(
        text: String,
        interKeyDelayMs: Long,
        sendReport: suspend (ByteArray) -> Boolean,
        onProgress: ((Int, Int) -> Unit)? = null
    ): InjectionResult {
        var injected = 0
        var skipped = 0
        val total = text.length

        // Reusable array for key down to prevent hot-loop allocations
        val keyDownReport = ByteArray(8)

        for (i in text.indices) {
            if (!coroutineContext.isActive) {
                break
            }

            val char = text[i]
            val reportInfo = KeyTranslator.translate(char)

            if (reportInfo != null) {
                // Key down
                keyDownReport[0] = reportInfo.modifier
                keyDownReport[2] = reportInfo.usageId
                sendReport(keyDownReport)

                if (interKeyDelayMs > 0) {
                    delay(interKeyDelayMs)
                }

                // Key up
                sendReport(keyUpReport)

                if (interKeyDelayMs > 0) {
                    delay(interKeyDelayMs)
                }

                // Clear the used usageId for the next character
                keyDownReport[2] = 0

                injected++
            } else {
                skipped++
            }

            onProgress?.invoke(i + 1, total)
        }

        return InjectionResult(
            totalChars = total,
            injectedChars = injected,
            skippedChars = skipped
        )
    }
}

@Composable
fun PrecomposeBufferScreen(
    modifier: Modifier = Modifier,
    hidDeviceManager: HidDeviceManager? = null,
    targetDevice: BluetoothDevice? = null,
    onInjectOverride: (suspend (String, Long, (Int, Int) -> Unit) -> InjectionResult)? = null
) {
    var textBuffer by remember { mutableStateOf("") }
    var interKeyDelayMs by remember { mutableFloatStateOf(20f) }
    var isInjecting by remember { mutableStateOf(false) }
    var progressCurrent by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var injectionJob by remember { mutableStateOf<Job?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Precompose Buffer & Text Injection",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = textBuffer,
            onValueChange = { textBuffer = it },
            label = { Text("Text Buffer") },
            placeholder = { Text("Enter text to inject...") },
            enabled = !isInjecting,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Inter-Key Delay: ${interKeyDelayMs.toInt()} ms",
            style = MaterialTheme.typography.bodyMedium
        )

        Slider(
            value = interKeyDelayMs,
            onValueChange = { interKeyDelayMs = it },
            valueRange = 0f..200f,
            steps = 19,
            enabled = !isInjecting,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isInjecting && progressTotal > 0) {
            LinearProgressIndicator(
                progress = { progressCurrent.toFloat() / progressTotal.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Injecting $progressCurrent / $progressTotal characters...",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (textBuffer.isEmpty()) return@Button
                    isInjecting = true
                    progressCurrent = 0
                    progressTotal = textBuffer.length
                    statusMessage = "Injecting..."

                    injectionJob = coroutineScope.launch {
                        try {
                            val delayMs = interKeyDelayMs.toLong()
                            val result = if (onInjectOverride != null) {
                                onInjectOverride(textBuffer, delayMs) { cur, tot ->
                                    progressCurrent = cur
                                    progressTotal = tot
                                }
                            } else {
                                PrecomposeBufferInjector.injectText(
                                    text = textBuffer,
                                    interKeyDelayMs = delayMs,
                                    sendReport = { report ->
                                        if (hidDeviceManager != null && targetDevice != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                            hidDeviceManager.sendReport(targetDevice, 0, report)
                                        } else {
                                            false
                                        }
                                    },
                                    onProgress = { cur, tot ->
                                        progressCurrent = cur
                                        progressTotal = tot
                                    }
                                )
                            }
                            statusMessage = "Injection complete: ${result.injectedChars} injected, ${result.skippedChars} skipped."
                        } catch (e: CancellationException) {
                            statusMessage = "Injection cancelled."
                        } catch (e: Exception) {
                            statusMessage = "Error: ${e.message}"
                        } finally {
                            isInjecting = false
                        }
                    }
                },
                enabled = !isInjecting && textBuffer.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Inject Text")
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isInjecting) {
                OutlinedButton(
                    onClick = {
                        injectionJob?.cancel()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            } else {
                OutlinedButton(
                    onClick = {
                        textBuffer = ""
                        statusMessage = ""
                        progressCurrent = 0
                        progressTotal = 0
                    },
                    enabled = textBuffer.isNotEmpty() || statusMessage.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }
        }
    }
}
