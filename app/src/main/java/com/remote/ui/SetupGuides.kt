package com.remote.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remote.db.ConnectionType

data class SetupGuideStep(
    val stepNumber: Int,
    val instruction: String
)

data class DeviceSetupGuide(
    val connectionType: ConnectionType,
    val title: String,
    val steps: List<SetupGuideStep>,
    val warning: String
)

object DeviceSetupGuideRegistry {

    private val guides = mapOf(
        ConnectionType.DIRECT_BT_HID to DeviceSetupGuide(
            connectionType = ConnectionType.DIRECT_BT_HID,
            title = "Direct Bluetooth HID Setup",
            steps = listOf(
                SetupGuideStep(1, "Prompt system discoverability via system Bluetooth settings."),
                SetupGuideStep(2, "Navigate to target device Bluetooth settings and select phone to pair.")
            ),
            warning = "Target OS must complete the pairing handshake. No host software is required."
        ),
        ConnectionType.ESP32_BLE_BRIDGE to DeviceSetupGuide(
            connectionType = ConnectionType.ESP32_BLE_BRIDGE,
            title = "ESP32 Dongle BLE Bridge Setup",
            steps = listOf(
                SetupGuideStep(1, "Insert ESP32-S2/S3 dongle into native USB port of target host."),
                SetupGuideStep(2, "Pair phone to dongle BLE advertising name via app scanner.")
            ),
            warning = "Ensure board uses ESP32-S2/S3 native USB GPIOs (GPIO 19/20), not USB-to-UART bridge."
        ),
        ConnectionType.ESP32_IR_EMITTER to DeviceSetupGuide(
            connectionType = ConnectionType.ESP32_IR_EMITTER,
            title = "ESP32 Hardware IR Setup",
            steps = listOf(
                SetupGuideStep(1, "Connect ESP32 IR emitter dongle to power."),
                SetupGuideStep(2, "Point IR emitter LED towards target appliance.")
            ),
            warning = "Requires 940nm IR LED driven via transistor on RMT peripheral GPIO pin."
        ),
        ConnectionType.ESP32_HYBRID to DeviceSetupGuide(
            connectionType = ConnectionType.ESP32_HYBRID,
            title = "ESP32 Hybrid (USB HID + IR) Setup",
            steps = listOf(
                SetupGuideStep(1, "Insert ESP32 dongle into target host USB port."),
                SetupGuideStep(2, "Position IR emitter facing secondary IR targets.")
            ),
            warning = "Handles both USB keystrokes and IR signals over a single BLE NUS bridge link."
        ),
        ConnectionType.ROKU_ECP_WIFI to DeviceSetupGuide(
            connectionType = ConnectionType.ROKU_ECP_WIFI,
            title = "Roku TV ECP Wi-Fi Setup",
            steps = listOf(
                SetupGuideStep(1, "Verify phone and Roku TV share the same Wi-Fi subnet."),
                SetupGuideStep(2, "On Roku TV, navigate to Settings → System → Advanced system settings → Control by mobile apps → Network access and set to 'Enabled'.")
            ),
            warning = "Roku OS 14.1+ Requirement: Network access must be explicitly set to 'Enabled'."
        ),
        ConnectionType.SSH_NETWORK to DeviceSetupGuide(
            connectionType = ConnectionType.SSH_NETWORK,
            title = "SSH Host Remote Setup",
            steps = listOf(
                SetupGuideStep(1, "Enter target Linux host IP address and SSH port (default 22)."),
                SetupGuideStep(2, "Generate or copy public key and append to target ~/.ssh/authorized_keys file.")
            ),
            warning = "Ensure SSH service is running and key or password authentication is allowed."
        ),
        ConnectionType.AUDIO_JACK_IR to DeviceSetupGuide(
            connectionType = ConnectionType.AUDIO_JACK_IR,
            title = "3.5mm Headphone Jack IR Setup",
            steps = listOf(
                SetupGuideStep(1, "Plug passive audio IR blaster adapter into 3.5mm audio jack."),
                SetupGuideStep(2, "Set phone media volume to 100%.")
            ),
            warning = "Volume Warning: Disable system equalizers, Dolby Atmos, and hearing-safety limiters to prevent wave clipping."
        ),
        ConnectionType.FLIPPER_ZERO_BRIDGE to DeviceSetupGuide(
            connectionType = ConnectionType.FLIPPER_ZERO_BRIDGE,
            title = "Flipper Zero BLE Bridge Setup",
            steps = listOf(
                SetupGuideStep(1, "Enable Bluetooth on Flipper Zero via Settings → Bluetooth."),
                SetupGuideStep(2, "Pair phone with Flipper Zero via app BLE scanner.")
            ),
            warning = "Ensure Flipper is on home desktop screen (CLI serial session is blocked if stuck in sub-menus)."
        ),
        ConnectionType.INTERNAL_IR to DeviceSetupGuide(
            connectionType = ConnectionType.INTERNAL_IR,
            title = "Internal IR Blaster Setup",
            steps = listOf(
                SetupGuideStep(1, "Ensure device has built-in IR hardware transmitter."),
                SetupGuideStep(2, "Point phone top edge toward target receiver.")
            ),
            warning = "Requires hardware IR transmitter support (ConsumerIrManager)."
        ),
        ConnectionType.USB_IR_DONGLE to DeviceSetupGuide(
            connectionType = ConnectionType.USB_IR_DONGLE,
            title = "USB-C IR Dongle Setup",
            steps = listOf(
                SetupGuideStep(1, "Connect USB-C IR blaster dongle to phone."),
                SetupGuideStep(2, "Grant USB device host permission when prompted.")
            ),
            warning = "Requires USB Host OTG support on smartphone."
        )
    )

    fun getGuide(connectionType: ConnectionType): DeviceSetupGuide {
        return guides[connectionType] ?: DeviceSetupGuide(
            connectionType = connectionType,
            title = "${connectionType.name} Setup",
            steps = listOf(SetupGuideStep(1, "Follow target device manufacturer pairing instructions.")),
            warning = "Ensure target device is powered on and within range."
        )
    }
}

@Composable
fun DeviceSetupGuideView(
    connectionType: ConnectionType,
    modifier: Modifier = Modifier
) {
    val guide = DeviceSetupGuideRegistry.getGuide(connectionType)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = guide.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Setup Steps:",
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        guide.steps.forEach { step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "${step.stepNumber}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = guide.warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
