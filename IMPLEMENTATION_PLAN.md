# Atomic Implementation Plan

This document outlines the sequential execution plan for implementing the Multi-Target Input & Injection System.

### Task 1.1: Initialize Android Studio project (Kotlin, Coroutines, Jetpack Compose, Dark Theme) [EASY]
- **Target Files:** `build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`
- **Core Technical Requirements:** Implement initialize android studio project (kotlin, coroutines, jetpack compose, dark theme) ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 1.2: Implement BluetoothHidDevice profile registration with SDP boot keyboard records [HARD]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Implement bluetoothhiddevice profile registration with sdp boot keyboard records ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 1.3: Build UTF-8 character to standard USB HID Usage Tables (Page 0x07) translation engine [HARD]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Build utf-8 character to standard usb hid usage tables (page 0x07) translation engine ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 1.4: Implement precompose buffer UI and chunked text injection pipeline with adjustable inter-key delay [EASY]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Implement precompose buffer ui and chunked text injection pipeline with adjustable inter-key delay ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 1.5: Implement BLE GATT Client connection manager using Nordic UART Service (NUS) protocol [HARD]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Implement ble gatt client connection manager using nordic uart service (nus) protocol ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 2.1: Bootstrap PlatformIO project with native TinyUSB stack for ESP32-S2/S3 [EASY]
- **Target Files:** `firmware/platformio.ini`, `firmware/src/main.cpp`
- **Core Technical Requirements:** Bootstrap platformio project with native tinyusb stack for esp32-s2/s3 ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `pio run -e esp32-s3 && pio test`

### Task 2.2: Implement USB HID Keyboard descriptor and report endpoints on native D-/D+ GPIOs [HARD]
- **Target Files:** `firmware/platformio.ini`, `firmware/src/main.cpp`
- **Core Technical Requirements:** Implement usb hid keyboard descriptor and report endpoints on native d-/d+ gpios ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `pio run -e esp32-s3 && pio test`

### Task 2.3: Implement NimBLE GATT NUS service receiving binary command frames [HARD]
- **Target Files:** `firmware/platformio.ini`, `firmware/src/main.cpp`
- **Core Technical Requirements:** Implement nimble gatt nus service receiving binary command frames ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `pio run -e esp32-s3 && pio test`

### Task 2.4: Implement thread-safe FIFO ring buffer bridging BLE payloads to USB HID endpoints [HARD]
- **Target Files:** `firmware/platformio.ini`, `firmware/src/main.cpp`
- **Core Technical Requirements:** Implement thread-safe fifo ring buffer bridging ble payloads to usb hid endpoints ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `pio run -e esp32-s3 && pio test`

### Task 2.5: Add hardware pairing bond security and static PIN authentication [EASY]
- **Target Files:** `firmware/platformio.ini`, `firmware/src/main.cpp`
- **Core Technical Requirements:** Add hardware pairing bond security and static pin authentication ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `pio run -e esp32-s3 && pio test`

### Task 3.1: Construct Room entities (TargetDeviceEntity, ButtonMappingEntity) and DAOs [HARD]
- **Target Files:** `app/src/main/java/com/example/remote/data/AppDatabase.kt`, `app/src/main/java/com/example/remote/data/entities/*`
- **Core Technical Requirements:** Construct room entities (targetdeviceentity, buttonmappingentity) and daos ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 3.2: Implement single-link DeviceConnectionRouter using Disconnect-Before-Connect lifecycle [HARD]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Implement single-link deviceconnectionrouter using disconnect-before-connect lifecycle ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 3.3: Implement DeviceSetupGuideRegistry providing contextual onboarding instructions [EASY]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Implement devicesetupguideregistry providing contextual onboarding instructions ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 3.4: Build dynamic Compose UI surfaces (Full Keyboard, D-Pad TV Remote, Media Bar, Macro Pad) [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/ui/screens/*`
- **Core Technical Requirements:** Build dynamic compose ui surfaces (full keyboard, d-pad tv remote, media bar, macro pad) ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 3.5: Build Device Manager screen for adding, editing, reordering, and deleting profiles [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/ui/screens/*`
- **Core Technical Requirements:** Build device manager screen for adding, editing, reordering, and deleting profiles ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 4.1: Implement Roku ECP client over OkHttp3 (Port 8060 keypress, power, and Lit_ endpoints) [EASY]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Implement roku ecp client over okhttp3 (port 8060 keypress, power, and lit_ endpoints) ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 4.2: Implement SSDP UDP multicast discovery (ST: roku:ecp) with subnet sweep fallback [EASY]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Implement ssdp udp multicast discovery (st: roku:ecp) with subnet sweep fallback ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 4.3: Implement SSHJ client supporting Ed25519 key generation and known_hosts validation [EASY]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Implement sshj client supporting ed25519 key generation and known_hosts validation ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 4.4: Support one-shot SSH Exec commands (for button macros) and continuous PTY Shell streaming [EASY]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Support one-shot ssh exec commands (for button macros) and continuous pty shell streaming ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 4.5: Raspberry Pi Zero ConfigFS multi-function composite gadget script (HID + Mass Storage + RNDIS) [EASY]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Implement Raspberry pi zero configfs multi-function composite gadget script (hid + mass storage + rndis) ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `flake8 scripts/ && pytest scripts/`

### Task 5.1: Implement ESP32 RMT hardware infrared driver using IRremoteESP8266 library [HARD]
- **Target Files:** `firmware/platformio.ini`, `firmware/src/main.cpp`
- **Core Technical Requirements:** Implement esp32 rmt hardware infrared driver using irremoteesp8266 library ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `pio run -e esp32-s3 && pio test`

### Task 5.2: Update BLE frame specification to handle IR protocol opcodes (NEC, Sony, RC5, Raw) [EASY]
- **Target Files:** `src/main/...`
- **Core Technical Requirements:** Update ble frame specification to handle ir protocol opcodes (nec, sony, rc5, raw) ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 5.3: Implement native Android ConsumerIrManager driver for devices with onboard blasters [HARD]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Implement native android consumerirmanager driver for devices with onboard blasters ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 5.4: Build AudioTrack 19kHz dual-channel phase-inverted carrier generator for 3.5mm adapters [EASY]
- **Target Files:** `app/build.gradle.kts`, `app/src/main/...`
- **Core Technical Requirements:** Build audiotrack 19khz dual-channel phase-inverted carrier generator for 3.5mm adapters ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `./gradlew test lint`

### Task 5.5: Implement USB Host CDC-ACM serial driver for USB-C IR dongles [HARD]
- **Target Files:** `scripts/gadget_setup.sh`, `scripts/bridge.py`
- **Core Technical Requirements:** Implement usb host cdc-acm serial driver for usb-c ir dongles ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `flake8 scripts/ && pytest scripts/`

### Task 5.6: Implement Flipper Zero BLE serial bridge sending direct `ir tx` CLI commands [HARD]
- **Target Files:** `scripts/gadget_setup.sh`, `scripts/bridge.py`
- **Core Technical Requirements:** Implement flipper zero ble serial bridge sending direct `ir tx` cli commands ensuring proper encapsulation, dependency injection where applicable, and adherence to project architecture.
- **Verification Criteria:** `flake8 scripts/ && pytest scripts/`

