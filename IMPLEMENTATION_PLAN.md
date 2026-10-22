# Implementation Plan

### Task 1.1: Initialize Android Studio project (Kotlin, Coroutines, Jetpack Compose, Dark Theme). [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/Task1_1.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 1.2: Implement BluetoothHidDevice profile registration with SDP boot keyboard records. [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/Task1_2.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 1.3: Build UTF-8 character to standard USB HID Usage Tables (Page 0x07) translation engine. [HARD]
- **Target Files:** `app/src/main/java/com/example/remote/Task1_3.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 1.4: Implement precompose buffer UI and chunked text injection pipeline with adjustable inter-key delay. [HARD]
- **Target Files:** `app/src/main/java/com/example/remote/Task1_4.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 1.5: Implement BLE GATT Client connection manager using Nordic UART Service (NUS) protocol. [HARD]
- **Target Files:** `app/src/main/java/com/example/remote/Task1_5.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 2.1: Bootstrap PlatformIO project with native TinyUSB stack for ESP32-S2/S3. [EASY]
- **Target Files:** `firmware/esp32/src/task_2_1.c` (Create/Modify)
- **Core Technical Requirements:** Embedded C/C++ logic. Bound to ESP-IDF or PlatformIO hardware APIs.
- **Verification Criteria:** `pio run -e esp32-s3`

### Task 2.2: Implement USB HID Keyboard descriptor and report endpoints on native D-/D+ GPIOs. [HARD]
- **Target Files:** `firmware/esp32/src/task_2_2.c` (Create/Modify)
- **Core Technical Requirements:** Embedded C/C++ logic. Bound to ESP-IDF or PlatformIO hardware APIs.
- **Verification Criteria:** `pio run -e esp32-s3`

### Task 2.3: Implement NimBLE GATT NUS service receiving binary command frames. [HARD]
- **Target Files:** `firmware/esp32/src/task_2_3.c` (Create/Modify)
- **Core Technical Requirements:** Embedded C/C++ logic. Bound to ESP-IDF or PlatformIO hardware APIs.
- **Verification Criteria:** `pio run -e esp32-s3`

### Task 2.4: Implement thread-safe FIFO ring buffer bridging BLE payloads to USB HID endpoints. [HARD]
- **Target Files:** `firmware/esp32/src/task_2_4.c` (Create/Modify)
- **Core Technical Requirements:** Embedded C/C++ logic. Bound to ESP-IDF or PlatformIO hardware APIs.
- **Verification Criteria:** `pio run -e esp32-s3`

### Task 2.5: Add hardware pairing bond security and static PIN authentication. [HARD]
- **Target Files:** `firmware/esp32/src/task_2_5.c` (Create/Modify)
- **Core Technical Requirements:** Embedded C/C++ logic. Bound to ESP-IDF or PlatformIO hardware APIs.
- **Verification Criteria:** `pio run -e esp32-s3`

### Task 3.1: Construct Room entities (TargetDeviceEntity, ButtonMappingEntity) and DAOs. [HARD]
- **Target Files:** `app/src/main/java/com/example/remote/Task3_1.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 3.2: Implement single-link DeviceConnectionRouter using Disconnect-Before-Connect lifecycle. [HARD]
- **Target Files:** `app/src/main/java/com/example/remote/Task3_2.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 3.3: Implement DeviceSetupGuideRegistry providing contextual onboarding instructions. [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/Task3_3.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 3.4: Build dynamic Compose UI surfaces (Full Keyboard, D-Pad TV Remote, Media Bar, Macro Pad). [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/Task3_4.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 3.5: Build Device Manager screen for adding, editing, reordering, and deleting profiles. [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/Task3_5.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 4.1: Implement Roku ECP client over OkHttp3 (Port 8060 keypress, power, and Lit_ endpoints). [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/Task4_1.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 4.2: Implement SSDP UDP multicast discovery (ST: roku:ecp) with subnet sweep fallback. [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/Task4_2.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 4.3: Implement SSHJ client supporting Ed25519 key generation and known_hosts validation. [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/Task4_3.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 4.4: Support one-shot SSH Exec commands (for button macros) and continuous PTY Shell streaming. [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/Task4_4.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 4.5: Raspberry Pi Zero ConfigFS multi-function composite gadget script (HID + Mass Storage + RNDIS). [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/Task4_5.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 5.1: Implement ESP32 RMT hardware infrared driver using IRremoteESP8266 library. [HARD]
- **Target Files:** `firmware/esp32/src/task_5_1.c` (Create/Modify)
- **Core Technical Requirements:** Embedded C/C++ logic. Bound to ESP-IDF or PlatformIO hardware APIs.
- **Verification Criteria:** `pio run -e esp32-s3`

### Task 5.2: Update BLE frame specification to handle IR protocol opcodes (NEC, Sony, RC5, Raw). [EASY]
- **Target Files:** `src/Task5_2.txt` (Create/Modify)
- **Core Technical Requirements:** Implement Update BLE frame specification to handle IR protocol opcodes (NEC, Sony, RC5, Raw).
- **Verification Criteria:** `echo 'Done'`

### Task 5.3: Implement native Android ConsumerIrManager driver for devices with onboard blasters. [HARD]
- **Target Files:** `app/src/main/java/com/example/remote/Task5_3.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 5.4: Build AudioTrack 19kHz dual-channel phase-inverted carrier generator for 3.5mm adapters. [EASY]
- **Target Files:** `app/src/main/java/com/example/remote/Task5_4.kt` (Create/Modify)
- **Core Technical Requirements:** Android platform implementation. Must adhere to standard Android SDK boundaries and depend only on configured libraries.
- **Verification Criteria:** `./gradlew test && ./gradlew lint`

### Task 5.5: Implement USB Host CDC-ACM serial driver for USB-C IR dongles. [HARD]
- **Target Files:** `scripts/bridge_5_5.py` (Create/Modify)
- **Core Technical Requirements:** Scripting logic and integrations.
- **Verification Criteria:** `python3 -m unittest discover`

### Task 5.6: Implement Flipper Zero BLE serial bridge sending direct `ir tx` CLI commands. [HARD]
- **Target Files:** `scripts/bridge_5_6.py` (Create/Modify)
- **Core Technical Requirements:** Scripting logic and integrations.
- **Verification Criteria:** `python3 -m unittest discover`
