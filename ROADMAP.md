# Multi-Target Input & Injection System

Comprehensive architecture and phased engineering roadmap for a multi-profile remote control, keystroke injection, and device management ecosystem targeting the Pixel 8a, ESP32 hardware dongles, network targets, and infrared emitters.

## 1. System Architecture Overview

```
                               ┌────────────────────────────────────────┐
                               │       Pixel 8a Control Hub App         │
                               │  - Jetpack Compose Dynamic Surfaces    │
                               │  - Room Persistence (Profiles/Mappings)│
                               │  - Single-Link State Machine Router    │
                               │  - Setup & Onboarding Guide Registry   │
                               └────────────────────┬───────────────────┘
                                                    │
         ┌─────────────────────────┬────────────────┼─────────────────────────┬─────────────────────────┐
         │ (Direct BT HID)         │ (BLE Bridge)   │ (HTTP REST)             │ (TCP / SSH)             │ (IR Transports)
         ▼                         ▼                ▼                         ▼                         ▼
┌──────────────────┐     ┌───────────────────┐ ┌─────────────┐       ┌─────────────────┐       ┌─────────────────┐
│   Target Host    │     │ ESP32-S2/S3 Dongle│ │ Roku TV ECP │       │ Linux Host/NAS  │       │ Target Appliance│
│ (PC / Mac / TV)  │     │ - TinyUSB HID     │ │ (Port 8060) │       │ (Port 22 Shell/ │       │ (TV / AV / AC)  │
│ Serverless Input │     │ - IR Transmitter  │ └─────────────┘       │  Exec Commands) │       └────────┬────────┘
└──────────────────┘     └─────────┬─────────┘                       └─────────────────┘                │
                                   │                                                                    │
                                   ├─ USB OTG (Host Keystrokes)                                         │
                                   └─ Hardware IR (38kHz Carrier) ──────────────────────────────────────┘
                                                                                                        │
                                 ┌──────────────────────────────────────────────────────────────────────┤
                                 │ Alternate IR Transports:                                             │
                                 ├─ Internal Blaster (ConsumerIrManager)                                │
                                 ├─ USB-C CDC Blaster Dongle                                            │
                                 ├─ 3.5mm Headphone Jack (AudioTrack 19kHz Out-of-Phase Carrier)        │
                                 └─ Flipper Zero Bridge (BLE Serial CLI `ir tx`)                        │
                                                                                                        │
                                 ┌──────────────────────────────────────────────────────────────────────┘
                                 │ Low-Priority Composite Expansion:
                                 ▼
                         ┌───────────────────┐
                         │ Raspberry Pi Zero │
                         │ (dwc2 ConfigFS)   │
                         │ - /dev/hidg0      │
                         │ - Mass Storage    │
                         │ - RNDIS / Network │
                         └───────────────────┘
```

## 2. Technical Stack & Hardware Requirements

### Mobile Application

- **Platform:** Android 14+ (Target SDK 34/35), optimized for Pixel 8a.
- **UI Toolkit:** Jetpack Compose with dynamic Material 3 design and dark theme.
- **Database:** SQLite via Android Room with reactive Kotlin Coroutines/Flow.
- **Protocols & APIs:**
  - **Bluetooth Classic / BLE:** Android `BluetoothHidDevice`, `BluetoothLeAdvertiser`, `BluetoothGatt`.
  - **HTTP / LAN:** `OkHttp3` for Roku External Control Protocol (ECP) REST requests.
  - **SSH:** `net.schmizz:sshj:0.38.0` for encrypted command execution and interactive sessions.
  - **Native Infrared:** Android `ConsumerIrManager`.
  - **Audio Modulation:** Android `AudioTrack` (PCM 16-bit 48kHz stereo) for passive 3.5mm emitters.

### Microcontroller Dongle

- **SoC:** ESP32-S2 or ESP32-S3 (Classic ESP32-WROOM/WROVER lack native USB OTG and cannot be used).
- **Firmware Framework:** ESP-IDF or PlatformIO / Arduino Core with `Adafruit_TinyUSB` and `IRremoteESP8266`.
- **Hardware Interfacing:**
  - USB Host communication via native GPIO 19 (`D-`) and GPIO 20 (`D+`).
  - Infrared transmission via onboard Remote Control Peripheral (RMT) driving a 940nm IR LED through a 2N2222/2N3904 NPN transistor and 10–22Ω current-limiting resistor.

## 3. Database Schema Design (Room Persistence)

```kotlin
enum class ConnectionType {
    DIRECT_BT_HID,
    ESP32_BLE_BRIDGE,
    ESP32_IR_EMITTER,
    ESP32_HYBRID,
    ROKU_ECP_WIFI,
    INTERNAL_IR,
    USB_IR_DONGLE,
    AUDIO_JACK_IR,
    FLIPPER_ZERO_BRIDGE,
    SSH_NETWORK
}

enum class ProfileType {
    PC_KEYBOARD,
    TV_REMOTE,
    MEDIA_CONTROLLER,
    PRESENTATION_CLICKER,
    TERMINAL_MACRO,
    SSH_SESSION
}

@Entity(
    tableName = "target_devices",
    indices = [Index(value = ["mac_address"], unique = true)]
)
data class TargetDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "connection_type") val connectionType: ConnectionType,
    @ColumnInfo(name = "profile_type") val profileType: ProfileType,
    @ColumnInfo(name = "mac_address") val macAddress: String? = null,
    @ColumnInfo(name = "host_address") val hostAddress: String? = null, // IP or Hostname
    @ColumnInfo(name = "port") val port: Int = 22,
    @ColumnInfo(name = "auth_username") val authUsername: String? = null,
    @ColumnInfo(name = "auth_type") val authType: String? = "KEY", // "KEY" or "PASSWORD"
    @ColumnInfo(name = "carrier_frequency") val carrierFrequency: Int = 38000,
    @ColumnInfo(name = "inter_key_delay_ms") val interKeyDelayMs: Int = 10,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "button_mappings",
    foreignKeys = [
        ForeignKey(
            entity = TargetDeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["device_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["device_id", "button_identifier"], unique = true)]
)
data class ButtonMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "device_id") val deviceId: Long,
    @ColumnInfo(name = "button_identifier") val buttonIdentifier: String, // e.g., "KEY_ENTER", "VOL_UP", "REBOOT"
    @ColumnInfo(name = "label") val label: String,
    // HID & Media Mappings
    @ColumnInfo(name = "hid_page") val hidPage: Int = 0x07, // 0x07 = Keyboard, 0x0C = Consumer
    @ColumnInfo(name = "usage_id") val usageId: Int = 0,
    @ColumnInfo(name = "modifier_flags") val modifierFlags: Byte = 0,
    // Infrared Data
    @ColumnInfo(name = "ir_protocol") val irProtocol: String? = null, // "NEC", "RC5", "RAW"
    @ColumnInfo(name = "ir_hex_code") val irHexCode: String? = null,
    @ColumnInfo(name = "ir_raw_timings") val irRawTimings: String? = null,
    // Network / SSH / ECP Command Payload
    @ColumnInfo(name = "command_payload") val commandPayload: String? = null
)
```

## 4. Contextual Onboarding & Setup Instructions Engine

| Target Type       | In-App Setup Step                                                                                                          | Critical Warnings & Specific Verification Checks                                                                                                              |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Direct BT HID** | 1. Prompt system discoverability via `ACTION_REQUEST_DISCOVERABLE`.<br>2. Direct user to target device Bluetooth settings. | Target OS must complete pairing handshake. No host software required.                                                                                         |
| **ESP32 Dongle**  | 1. Insert dongle into native USB port.<br>2. Pair phone to dongle BLE advertising name.                                    | Ensure board uses ESP32-S2/S3 native pins (GPIO 19/20), not UART bridge.                                                                                      |
| **Roku TV (ECP)** | 1. Verify phone and TV share same Wi-Fi subnet.<br>2. Enable network control in Roku settings.                             | **Roku OS 14.1+ Requirement:** Navigate to `Settings → System → Advanced system settings → Control by mobile apps → Network access` and set to **"Enabled"**. |
| **SSH Host**      | 1. Prompt user for target IP and port.<br>2. In-app Ed25519 key generation.                                                | Provide 1-tap "Copy Public Key" button for appending to target `~/.ssh/authorized_keys`.                                                                      |
| **3.5mm IR Jack** | 1. Plug passive audio IR blaster into 3.5mm adapter.<br>2. Set media volume to 100%.                                       | **Volume Warning:** Disable system equalizers, Dolby Atmos, and hearing-safety limiters to prevent wave clipping.                                             |
| **Flipper Zero**  | 1. Enable Bluetooth on Flipper (`Settings → Bluetooth`).<br>2. Pair via app BLE scanner.                                   | Ensure Flipper is on home screen (CLI session blocked if stuck in sub-menus).                                                                                 |

## 5. Phased Engineering Roadmap

```
├── Phase 1: Android Core Bluetooth Engine & Text Pipeline (High Priority) [Audit Complete - Resolved]
│   ├── [x] Task 1.1: Initialize Android Studio project (Kotlin, Coroutines, Jetpack Compose, Dark Theme).
│   ├── [x] Task 1.2: Implement BluetoothHidDevice profile registration with SDP boot keyboard records.
│   ├── [x] Task 1.3: Build UTF-8 character to standard USB HID Usage Tables (Page 0x07) translation engine.
│   ├── [x] Task 1.4: Implement precompose buffer UI and chunked text injection pipeline with adjustable inter-key delay.
│   └── [x] Task 1.5: Implement BLE GATT Client connection manager using Nordic UART Service (NUS) protocol.
│
├── Phase 2: ESP32-S2/S3 Dongle Firmware (High Priority) [Audit Complete - Resolved]
│   ├── [x] Task 2.1: Bootstrap PlatformIO project with native TinyUSB stack for ESP32-S2/S3.
│   ├── [x] Task 2.2: Implement USB HID Keyboard descriptor and report endpoints on native D-/D+ GPIOs.
│   ├── [x] Task 2.3: Implement NimBLE GATT NUS service receiving binary command frames.
│   ├── [x] Task 2.4: Implement thread-safe FIFO ring buffer bridging BLE payloads to USB HID endpoints.
│   └── [x] Task 2.5: Add hardware pairing bond security and static PIN authentication.
│
├── Phase 3: Database, Multi-Profile Engine & UI Routing (High Priority) [Audit Complete - Resolved]
│   ├── [x] Task 3.1: Construct Room entities (TargetDeviceEntity, ButtonMappingEntity) and DAOs.
│   ├── [x] Task 3.2: Implement single-link DeviceConnectionRouter using Disconnect-Before-Connect lifecycle.
│   ├── [x] Task 3.3: Implement DeviceSetupGuideRegistry providing contextual onboarding instructions.
│   ├── [x] Task 3.4: Build dynamic Compose UI surfaces (Full Keyboard, D-Pad TV Remote, Media Bar, Macro Pad).
│   └── [x] Task 3.5: Build Device Manager screen for adding, editing, reordering, and deleting profiles.
│
├── Phase 4: Network Transport Extensions (Low Priority) [Audit Complete - Resolved]
│   ├── [x] Task 4.1: Implement Roku ECP client over OkHttp3 (Port 8060 keypress, power, and Lit_ endpoints).
│   ├── [x] Task 4.2: Implement SSDP UDP multicast discovery (ST: roku:ecp) with subnet sweep fallback.
│   ├── [x] Task 4.3: Implement SSHJ client supporting Ed25519 key generation and known_hosts validation.
│   ├── [x] Task 4.4: Support one-shot SSH Exec commands (for button macros) and continuous PTY Shell streaming.
│   └── [x] Task 4.5: Raspberry Pi Zero ConfigFS multi-function composite gadget script (HID + Mass Storage + RNDIS).
│
└── Phase 5: Infrared Subsystem Extensions (Low Priority) [Audit Complete - In Progress]
    ├── [ ] Task 5.1: Implement ESP32 RMT hardware infrared driver using IRremoteESP8266 library.
    ├── [x] Task 5.2: Update BLE frame specification to handle IR protocol opcodes (NEC, Sony, RC5, Raw).
    ├── [x] Task 5.3: Implement native Android ConsumerIrManager driver for devices with onboard blasters.
    ├── [x] Task 5.4: Build AudioTrack 19kHz dual-channel phase-inverted carrier generator for 3.5mm adapters.
    ├── [x] Task 5.5: Implement USB Host CDC-ACM serial driver for USB-C IR dongles.
    └── [x] Task 5.6: Implement Flipper Zero BLE serial bridge sending direct `ir tx` CLI commands.
```

## 6. Verification & Validation Checklists

### Milestone 1: Direct Bluetooth HID (Pixel 8a)

**Status:** [Audit Complete - Unstarted]

- [ ] Phone registers `BluetoothHidDevice` without crashing.
- [ ] Target host (PC/Mac/Linux) discovers phone as standard Bluetooth Keyboard.
- [ ] Precompose text injection sends 500 characters of mixed ASCII without dropped letters or stuck modifiers.

### Milestone 2: ESP32 Dongle Bridge

**Status:** [Audit Complete - Unstarted]

- [ ] ESP32-S2/S3 enumerates in target host BIOS/UEFI as standard USB HID Keyboard.
- [ ] Android app connects to ESP32 over BLE within 1.5 seconds of profile selection.
- [ ] Keystrokes sent over BLE pass through FIFO buffer to host with under 20ms perceived latency.

### Milestone 3: Profile State Machine

**Status:** [Audit Complete - In Progress]

- [x] Switching from Profile A (Direct BT) to Profile B (ESP32) cleanly severs Link A before initializing Link B.
- [ ] No residual Bluetooth locks or background connection conflicts occur when switching profiles rapidly.

### Milestone 4: Roku Network Remote

**Status:** [Audit Complete - Unstarted]

- [ ] App prompts user with Roku OS 14.1+ "Control by mobile apps" setup modal during device registration.
- [ ] App fires `POST http://<ROKU_IP>:8060/keypress/Home` and navigates TV interface instantly.
- [ ] App successfully types text using sequential `Lit_<char>` requests with proper URL encoding.

### Milestone 5: SSH & IR Modules

**Status:** [Audit Complete - Unstarted]

- [ ] App generates Ed25519 keypair and successfully authenticates against target SSH server.
- [ ] ESP32 RMT peripheral outputs calibrated 38kHz square wave driving IR LED to control target TV.
- [ ] AudioTrack module produces 19kHz stereo audio file without DAC distortion when volume is set to 100%.
