# Implementation Plan

### Task 1.1: Initialize Android Studio project (Kotlin, Coroutines, Jetpack Compose, Dark Theme). [RESOLVED]
- **Target Files:** `app/build.gradle.kts`, `settings.gradle.kts`, `app/src/main/AndroidManifest.xml` (Create)
- **Core Technical Requirements:** Bootstrap a modern Android project targeting SDK 34/35. Setup Jetpack Compose and Coroutines dependencies.
- **Verification Criteria:** `./gradlew build`

### Task 1.2: Implement BluetoothHidDevice profile registration with SDP boot keyboard records. [RESOLVED]
- **Target Files:** `app/src/main/java/com/remote/bluetooth/HidDeviceManager.kt` (Create)
- **Core Technical Requirements:** Android platform implementation using `BluetoothHidDevice`. Must handle SDP records for standard keyboard.
- **Verification Criteria:** `./gradlew test`

### Task 1.3: Build UTF-8 character to standard USB HID Usage Tables (Page 0x07) translation engine. [RESOLVED]
- **Target Files:** `app/src/main/java/com/remote/bluetooth/KeyTranslator.kt` (Create)
- **Core Technical Requirements:** Map characters to HID page 0x07 bytes.
- **Verification Criteria:** `./gradlew test`

### Task 1.4: Implement precompose buffer UI and chunked text injection pipeline with adjustable inter-key delay. [EASY]
- **Target Files:** `app/src/main/java/com/remote/ui/PrecomposeBuffer.kt` (Create)
- **Core Technical Requirements:** Jetpack Compose UI with text buffer state and injection coroutine honoring interKeyDelayMs.
- **Verification Criteria:** `./gradlew lint`

### Task 1.5: Implement BLE GATT Client connection manager using Nordic UART Service (NUS) protocol. [RESOLVED]
- **Target Files:** `app/src/main/java/com/remote/bluetooth/BleGattManager.kt` (Create)
- **Core Technical Requirements:** Manage BLE connections to ESP32 dongle using NUS characteristics for RX/TX.
- **Verification Criteria:** `./gradlew test`

### Task 2.1: Bootstrap PlatformIO project with native TinyUSB stack for ESP32-S2/S3. [EASY]
- **Target Files:** `firmware/platformio.ini`, `firmware/src/main.cpp` (Create)
- **Core Technical Requirements:** Embedded C/C++ project setup.
- **Verification Criteria:** `cd firmware && pio run`

### Task 2.2: Implement USB HID Keyboard descriptor and report endpoints on native D-/D+ GPIOs. [RESOLVED]
- **Target Files:** `firmware/src/usb_hid.cpp`, `firmware/src/usb_hid.h` (Create)
- **Core Technical Requirements:** TinyUSB HID setup on native ESP32-S2/S3 USB pins.
- **Verification Criteria:** `cd firmware && pio check`

### Task 2.3: Implement NimBLE GATT NUS service receiving binary command frames. [RESOLVED]
- **Target Files:** `firmware/src/ble_nus.cpp`, `firmware/src/ble_nus.h` (Create)
- **Core Technical Requirements:** Expose Nordic UART Service via NimBLE and parse incoming packets.
- **Verification Criteria:** `cd firmware && pio check`

### Task 2.4: Implement thread-safe FIFO ring buffer bridging BLE payloads to USB HID endpoints. [HARD]
- **Target Files:** `firmware/src/ring_buffer.cpp`, `firmware/src/ring_buffer.h` (Create)
- **Core Technical Requirements:** FreeRTOS queue or ring buffer.
- **Verification Criteria:** `cd firmware && pio check`

### Task 2.5: Add hardware pairing bond security and static PIN authentication. [EASY]
- **Target Files:** `firmware/src/ble_sec.cpp` (Create)
- **Core Technical Requirements:** NimBLE security manager configuration.
- **Verification Criteria:** `cd firmware && pio check`

### Task 3.1: Construct Room entities (TargetDeviceEntity, ButtonMappingEntity) and DAOs. [EASY]
- **Target Files:** `app/src/main/java/com/remote/db/AppDatabase.kt`, `app/src/main/java/com/remote/db/Entities.kt` (Create)
- **Core Technical Requirements:** Room persistence schema based on design.
- **Verification Criteria:** `./gradlew build`

### Task 3.2: Implement single-link DeviceConnectionRouter using Disconnect-Before-Connect lifecycle. [HARD]
- **Target Files:** `app/src/main/java/com/remote/router/ConnectionRouter.kt` (Create)
- **Core Technical Requirements:** Coroutine-based state machine for exclusive link management.
- **Verification Criteria:** `./gradlew test`

### Task 3.3: Implement DeviceSetupGuideRegistry providing contextual onboarding instructions. [EASY]
- **Target Files:** `app/src/main/java/com/remote/ui/SetupGuides.kt` (Create)
- **Core Technical Requirements:** UI registry matching Device Type.
- **Verification Criteria:** `./gradlew lint`

### Task 3.4: Build dynamic Compose UI surfaces (Full Keyboard, D-Pad TV Remote, Media Bar, Macro Pad). [HARD]
- **Target Files:** `app/src/main/java/com/remote/ui/Surfaces.kt` (Create)
- **Core Technical Requirements:** Adaptive UI grids and buttons.
- **Verification Criteria:** `./gradlew lint`

### Task 3.5: Build Device Manager screen for adding, editing, reordering, and deleting profiles. [EASY]
- **Target Files:** `app/src/main/java/com/remote/ui/DeviceManager.kt` (Create)
- **Core Technical Requirements:** CRUD UI bound to Room.
- **Verification Criteria:** `./gradlew lint`

### Task 4.1: Implement Roku ECP client over OkHttp3 (Port 8060 keypress, power, and Lit_ endpoints). [EASY]
- **Target Files:** `app/src/main/java/com/remote/network/RokuClient.kt` (Create)
- **Core Technical Requirements:** REST client handling ECP spec.
- **Verification Criteria:** `./gradlew test`

### Task 4.2: Implement SSDP UDP multicast discovery (ST: roku:ecp) with subnet sweep fallback. [HARD]
- **Target Files:** `app/src/main/java/com/remote/network/SsdpDiscovery.kt` (Create)
- **Core Technical Requirements:** Multicast UDP sockets.
- **Verification Criteria:** `./gradlew test`

### Task 4.3: Implement SSHJ client supporting Ed25519 key generation and known_hosts validation. [HARD]
- **Target Files:** `app/src/main/java/com/remote/network/SshClient.kt` (Create)
- **Core Technical Requirements:** SSH connection via net.schmizz:sshj.
- **Verification Criteria:** `./gradlew test`

### Task 4.4: Support one-shot SSH Exec commands (for button macros) and continuous PTY Shell streaming. [EASY]
- **Target Files:** `app/src/main/java/com/remote/network/SshSession.kt` (Create)
- **Core Technical Requirements:** Channel execution.
- **Verification Criteria:** `./gradlew test`

### Task 4.5: Raspberry Pi Zero ConfigFS multi-function composite gadget script (HID + Mass Storage + RNDIS). [HARD]
- **Target Files:** `scripts/pi_gadget.sh` (Create)
- **Core Technical Requirements:** Bash script configuring ConfigFS.
- **Verification Criteria:** `shellcheck scripts/pi_gadget.sh`

### Task 5.1: Implement ESP32 RMT hardware infrared driver using IRremoteESP8266 library. [HARD]
- **Target Files:** `firmware/src/ir_tx.cpp` (Create)
- **Core Technical Requirements:** RMT peripheral integration.
- **Verification Criteria:** `cd firmware && pio check`

### Task 5.2: Update BLE frame specification to handle IR protocol opcodes (NEC, Sony, RC5, Raw). [EASY]
- **Target Files:** `firmware/src/ble_nus.cpp` (Modify)
- **Core Technical Requirements:** Protocol expansion for IR packets.
- **Verification Criteria:** `cd firmware && pio check`

### Task 5.3: Implement native Android ConsumerIrManager driver for devices with onboard blasters. [EASY]
- **Target Files:** `app/src/main/java/com/remote/ir/ConsumerIr.kt` (Create)
- **Core Technical Requirements:** Android system IR API wrapper.
- **Verification Criteria:** `./gradlew lint`

### Task 5.4: Build AudioTrack 19kHz dual-channel phase-inverted carrier generator for 3.5mm adapters. [HARD]
- **Target Files:** `app/src/main/java/com/remote/ir/AudioIr.kt` (Create)
- **Core Technical Requirements:** PCM byte array generation for IR modulation.
- **Verification Criteria:** `./gradlew test`

### Task 5.5: Implement USB Host CDC-ACM serial driver for USB-C IR dongles. [HARD]
- **Target Files:** `app/src/main/java/com/remote/usb/UsbCdcDriver.kt` (Create)
- **Core Technical Requirements:** Android USB Host API integration.
- **Verification Criteria:** `./gradlew test`

### Task 5.6: Implement Flipper Zero BLE serial bridge sending direct `ir tx` CLI commands. [EASY]
- **Target Files:** `app/src/main/java/com/remote/bluetooth/FlipperBridge.kt` (Create)
- **Core Technical Requirements:** Connect to Flipper's serial BLE service.
- **Verification Criteria:** `./gradlew lint`
