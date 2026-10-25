package com.remote.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.remote.db.ConnectionType
import com.remote.db.ProfileType
import com.remote.db.TargetDeviceEntity

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permission status handle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

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
    GUIDES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent() {
    var selectedTab by remember { mutableStateOf(NavTab.DEVICES) }
    val devices = remember {
        mutableStateListOf(
            TargetDeviceEntity(
                id = 1L,
                name = "PC Keyboard",
                connectionType = ConnectionType.DIRECT_BT_HID,
                profileType = ProfileType.PC_KEYBOARD
            ),
            TargetDeviceEntity(
                id = 2L,
                name = "Living Room TV",
                connectionType = ConnectionType.ROKU_ECP_WIFI,
                profileType = ProfileType.TV_REMOTE
            )
        )
    }
    var selectedDeviceId by remember { mutableStateOf<Long?>(1L) }
    val activeDevice = devices.find { it.id == selectedDeviceId } ?: devices.firstOrNull()

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
                        onSelectDevice = { selectedDeviceId = it.id },
                        onAddDevice = { newDevice ->
                            val newId = (devices.maxOfOrNull { it.id } ?: 0L) + 1L
                            devices.add(newDevice.copy(id = newId))
                            selectedDeviceId = newId
                        },
                        onUpdateDevice = { updatedDevice ->
                            val index = devices.indexOfFirst { it.id == updatedDevice.id }
                            if (index >= 0) {
                                devices[index] = updatedDevice
                            }
                        },
                        onDeleteDevice = { deviceToDelete ->
                            devices.removeIf { it.id == deviceToDelete.id }
                            if (selectedDeviceId == deviceToDelete.id) {
                                selectedDeviceId = devices.firstOrNull()?.id
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
                        Text(
                            text = "Active Target: ${activeDevice?.name ?: "None"}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        DeviceSurface(profileType = activeDevice?.profileType ?: ProfileType.PC_KEYBOARD)
                    }
                }
                NavTab.GUIDES -> {
                    DeviceSetupGuideView(
                        connectionType = activeDevice?.connectionType ?: ConnectionType.DIRECT_BT_HID
                    )
                }
            }
        }
    }
}
