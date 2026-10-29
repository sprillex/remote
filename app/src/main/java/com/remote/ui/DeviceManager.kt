package com.remote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.remote.db.ConnectionType
import com.remote.db.ProfileType
import com.remote.db.TargetDeviceEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagerScreen(
    devices: List<TargetDeviceEntity>,
    selectedDeviceId: Long? = null,
    onSelectDevice: (TargetDeviceEntity) -> Unit = {},
    onAddDevice: (TargetDeviceEntity) -> Unit = {},
    onUpdateDevice: (TargetDeviceEntity) -> Unit = {},
    onDeleteDevice: (TargetDeviceEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<TargetDeviceEntity?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingDevice = null
                    showAddEditDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Device")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Device Profiles",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (devices.isEmpty()) {
                Text(
                    text = "No devices added yet. Tap + to add a target device profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices, key = { it.id }) { device ->
                        DeviceCard(
                            device = device,
                            isSelected = device.id == selectedDeviceId,
                            onSelect = { onSelectDevice(device) },
                            onEdit = {
                                editingDevice = device
                                showAddEditDialog = true
                            },
                            onDelete = { onDeleteDevice(device) }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        DeviceAddEditDialog(
            initialDevice = editingDevice,
            onDismiss = { showAddEditDialog = false },
            onSave = { device ->
                if (editingDevice == null) {
                    onAddDevice(device)
                } else {
                    onUpdateDevice(device)
                }
                showAddEditDialog = false
            }
        )
    }
}

@Composable
fun DeviceCard(
    device: TargetDeviceEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${device.connectionType} • ${device.profileType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!device.macAddress.isNullOrEmpty()) {
                    Text(
                        text = "MAC: ${device.macAddress}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (!device.hostAddress.isNullOrEmpty()) {
                    Text(
                        text = "Host: ${device.authUsername ?: "root"}@${device.hostAddress}:${device.port}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Device")
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Device")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceAddEditDialog(
    initialDevice: TargetDeviceEntity?,
    onDismiss: () -> Unit,
    onSave: (TargetDeviceEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialDevice?.name ?: "") }
    var connectionType by remember { mutableStateOf(initialDevice?.connectionType ?: ConnectionType.DIRECT_BT_HID) }
    var profileType by remember { mutableStateOf(initialDevice?.profileType ?: ProfileType.PC_KEYBOARD) }
    var macAddress by remember { mutableStateOf(initialDevice?.macAddress ?: "") }
    var hostAddress by remember { mutableStateOf(initialDevice?.hostAddress ?: "") }
    var port by remember { mutableStateOf(initialDevice?.port?.toString() ?: "22") }
    var authUsername by remember { mutableStateOf(initialDevice?.authUsername ?: "root") }
    var authType by remember { mutableStateOf(initialDevice?.authType ?: "KEY") }
    var interKeyDelayMs by remember { mutableStateOf(initialDevice?.interKeyDelayMs?.toString() ?: "10") }

    var connDropdownExpanded by remember { mutableStateOf(false) }
    var profileDropdownExpanded by remember { mutableStateOf(false) }
    var pairedDevicesDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isBluetoothType = connectionType == ConnectionType.DIRECT_BT_HID ||
            connectionType == ConnectionType.ESP32_BLE_BRIDGE ||
            connectionType == ConnectionType.FLIPPER_ZERO_BRIDGE

    val pairedDevices = remember(isBluetoothType) {
        if (isBluetoothType) {
            try {
                @Suppress("DEPRECATION")
                val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                adapter?.bondedDevices?.toList() ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialDevice == null) "Add Device Profile" else "Edit Device Profile") },
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
                    label = { Text("Device Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Connection Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = connDropdownExpanded,
                    onExpandedChange = { connDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = connectionType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Connection Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = connDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = connDropdownExpanded,
                        onDismissRequest = { connDropdownExpanded = false }
                    ) {
                        ConnectionType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    connectionType = type
                                    if (type == ConnectionType.SSH_NETWORK) {
                                        profileType = ProfileType.SSH_SESSION
                                    }
                                    connDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Profile Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = profileDropdownExpanded,
                    onExpandedChange = { profileDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = profileType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Profile Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profileDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = profileDropdownExpanded,
                        onDismissRequest = { profileDropdownExpanded = false }
                    ) {
                        ProfileType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    profileType = type
                                    profileDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (isBluetoothType) {
                    if (pairedDevices.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = pairedDevicesDropdownExpanded,
                            onExpandedChange = { pairedDevicesDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = pairedDevices.find { it.address == macAddress }?.let { "${it.name ?: "Unknown"} (${it.address})" } ?: macAddress.ifEmpty { "Select Paired Device" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Paired Bluetooth Devices") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pairedDevicesDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = pairedDevicesDropdownExpanded,
                                onDismissRequest = { pairedDevicesDropdownExpanded = false }
                            ) {
                                pairedDevices.forEach { dev ->
                                    val displayName = try { dev.name ?: dev.address } catch (_: SecurityException) { dev.address }
                                    DropdownMenuItem(
                                        text = { Text("$displayName (${dev.address})") },
                                        onClick = {
                                            macAddress = dev.address
                                            if (name.isBlank()) {
                                                name = displayName
                                            }
                                            pairedDevicesDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                    putExtra(android.bluetooth.BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Make Phone Discoverable for Pairing")
                    }
                }

                OutlinedTextField(
                    value = macAddress,
                    onValueChange = { macAddress = it },
                    label = { Text("MAC Address (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hostAddress,
                    onValueChange = { hostAddress = it },
                    label = { Text("Host / IP Address (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (connectionType == ConnectionType.SSH_NETWORK) {
                    OutlinedTextField(
                        value = authUsername,
                        onValueChange = { authUsername = it },
                        label = { Text("SSH Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = authType,
                        onValueChange = { authType = it },
                        label = { Text("SSH Auth Type (KEY / PASSWORD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = interKeyDelayMs,
                    onValueChange = { interKeyDelayMs = it },
                    label = { Text("Inter-Key Delay (ms)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val device = TargetDeviceEntity(
                        id = initialDevice?.id ?: 0L,
                        name = name.trim(),
                        connectionType = connectionType,
                        profileType = profileType,
                        macAddress = macAddress.ifBlank { null },
                        hostAddress = hostAddress.ifBlank { null },
                        port = port.toIntOrNull() ?: 22,
                        authUsername = authUsername.ifBlank { "root" },
                        authType = authType.ifBlank { "KEY" },
                        interKeyDelayMs = interKeyDelayMs.toIntOrNull() ?: 10
                    )
                    onSave(device)
                },
                enabled = name.isNotBlank()
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
