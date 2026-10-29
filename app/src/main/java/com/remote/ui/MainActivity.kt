package com.remote.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.remote.bluetooth.HidDeviceManager
import com.remote.db.AppDatabase
import com.remote.db.ConnectionType
import com.remote.db.ProfileType
import com.remote.db.TargetDeviceEntity
import com.remote.router.ConnectionRouter
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permission status handle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        try {
            com.remote.bluetooth.HidDeviceManager(this).registerHidApp()
        } catch (_: Exception) {}

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent()
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            val needed = permissions.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                requestPermissionLauncher.launch(needed.toTypedArray())
            }
        }
    }
}

enum class NavTab {
    DEVICES,
    REMOTE,
    BADUSB,
    GUIDES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val prefs = remember(context) { context.getSharedPreferences("remote_control_prefs", Context.MODE_PRIVATE) }
    val hidDeviceManager = remember(context) { HidDeviceManager(context).apply { registerHidApp() } }
    val connectionRouter = remember(context) { ConnectionRouter(hidManager = hidDeviceManager) }

    var selectedTab by remember { mutableStateOf(NavTab.DEVICES) }
    val devices by db.targetDeviceDao().getAllDevices().collectAsState(initial = emptyList())

    var selectedDeviceId by remember {
        val savedId = prefs.getLong("selected_device_id", -1L)
        mutableStateOf<Long?>(if (savedId != -1L) savedId else null)
    }

    // Ensure selectedDeviceId stays valid relative to loaded devices
    LaunchedEffect(devices) {
        if (devices.isNotEmpty()) {
            if (selectedDeviceId == null || devices.none { it.id == selectedDeviceId }) {
                val fallbackId = devices.first().id
                selectedDeviceId = fallbackId
                prefs.edit().putLong("selected_device_id", fallbackId).apply()
            }
        } else {
            selectedDeviceId = null
            prefs.edit().remove("selected_device_id").apply()
        }
    }

    val selectedDevice = devices.find { it.id == selectedDeviceId }
    val routedDevice by connectionRouter.activeDevice.collectAsState()

    // Attempt connection when entering REMOTE or BADUSB tabs or when selectedDevice changes
    LaunchedEffect(selectedTab, selectedDevice) {
        if ((selectedTab == NavTab.REMOTE || selectedTab == NavTab.BADUSB) && selectedDevice != null) {
            connectionRouter.routeTo(selectedDevice)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Universal Remote") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == NavTab.DEVICES,
                    onClick = { selectedTab = NavTab.DEVICES },
                    icon = { Text("📱") },
                    label = { Text("Devices") }
                )
                NavigationBarItem(
                    selected = selectedTab == NavTab.REMOTE,
                    onClick = { selectedTab = NavTab.REMOTE },
                    icon = { Text("🎮") },
                    label = { Text("Remote") }
                )
                NavigationBarItem(
                    selected = selectedTab == NavTab.BADUSB,
                    onClick = { selectedTab = NavTab.BADUSB },
                    icon = { Text("⚡") },
                    label = { Text("BadUSB") }
                )
                NavigationBarItem(
                    selected = selectedTab == NavTab.GUIDES,
                    onClick = { selectedTab = NavTab.GUIDES },
                    icon = { Text("📖") },
                    label = { Text("Guides") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                NavTab.DEVICES -> {
                    DeviceManagerScreen(
                        devices = devices,
                        selectedDeviceId = selectedDeviceId,
                        onSelectDevice = { device ->
                            selectedDeviceId = device.id
                            prefs.edit().putLong("selected_device_id", device.id).apply()
                        },
                        onAddDevice = { newDevice ->
                            coroutineScope.launch {
                                val newId = db.targetDeviceDao().insertDevice(newDevice)
                                selectedDeviceId = newId
                                prefs.edit().putLong("selected_device_id", newId).apply()
                            }
                        },
                        onUpdateDevice = { updatedDevice ->
                            coroutineScope.launch {
                                db.targetDeviceDao().updateDevice(updatedDevice)
                            }
                        },
                        onDeleteDevice = { deviceToDelete ->
                            coroutineScope.launch {
                                db.targetDeviceDao().deleteDevice(deviceToDelete)
                                if (selectedDeviceId == deviceToDelete.id) {
                                    val remaining = devices.filter { it.id != deviceToDelete.id }
                                    val nextId = remaining.firstOrNull()?.id
                                    selectedDeviceId = nextId
                                    if (nextId != null) {
                                        prefs.edit().putLong("selected_device_id", nextId).apply()
                                    } else {
                                        prefs.edit().remove("selected_device_id").apply()
                                    }
                                }
                            }
                        }
                    )
                }
                NavTab.REMOTE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        ConnectionStatusHeader(
                            selectedDevice = selectedDevice,
                            routedDevice = routedDevice,
                            onConnect = {
                                selectedDevice?.let { dev ->
                                    coroutineScope.launch {
                                        connectionRouter.routeTo(dev)
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        DeviceSurface(
                            profileType = selectedDevice?.profileType ?: ProfileType.PC_KEYBOARD,
                            onSendKey = { usageId, modifier ->
                                hidDeviceManager.sendKey(usageId, modifier)
                            },
                            onSendMediaKey = { mediaUsageId ->
                                hidDeviceManager.sendMediaKey(mediaUsageId)
                            },
                            onSendChar = { char ->
                                hidDeviceManager.sendChar(char)
                            },
                            onSendText = { text ->
                                for (ch in text) {
                                    hidDeviceManager.sendChar(ch)
                                }
                            }
                        )
                    }
                }
                NavTab.BADUSB -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        ConnectionStatusHeader(
                            selectedDevice = selectedDevice,
                            routedDevice = routedDevice,
                            onConnect = {
                                selectedDevice?.let { dev ->
                                    coroutineScope.launch {
                                        connectionRouter.routeTo(dev)
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        BadUsbScriptSurface(
                            onSendKey = { usageId, modifier ->
                                hidDeviceManager.sendKey(usageId, modifier)
                            },
                            onSendChar = { char ->
                                hidDeviceManager.sendChar(char)
                            }
                        )
                    }
                }
                NavTab.GUIDES -> {
                    DeviceSetupGuideView(
                        connectionType = selectedDevice?.connectionType ?: ConnectionType.DIRECT_BT_HID
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusHeader(
    selectedDevice: TargetDeviceEntity?,
    routedDevice: TargetDeviceEntity?,
    onConnect: () -> Unit
) {
    val isConnected = selectedDevice != null && routedDevice?.id == selectedDevice.id

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedDevice != null) {
                            if (isConnected) "Connected to ${selectedDevice.name} (${selectedDevice.profileType})"
                            else "Disconnected from ${selectedDevice.name}"
                        } else {
                            "No device selected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isConnected) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                if (!isConnected && selectedDevice != null) {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Connect")
                    }
                }
            }

            if (!isConnected && selectedDevice != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ Warning: Device is not connected. Commands may not be delivered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
