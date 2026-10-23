# Implementation Plan

### Task 2.2: Implement USB HID Keyboard descriptor and report endpoints on native D-/D+ GPIOs. [HARD]
- **Target Files:** `firmware/src/usb_hid.cpp`, `firmware/src/usb_hid.h` (Create)
- **Core Technical Requirements:** TinyUSB HID setup on native ESP32-S2/S3 USB pins.
- **Verification Criteria:** `cd firmware && pio check`

### Task 2.4: Implement thread-safe FIFO ring buffer bridging BLE payloads to USB HID endpoints. [HARD]
- **Target Files:** `firmware/src/ring_buffer.cpp`, `firmware/src/ring_buffer.h` (Create)
- **Core Technical Requirements:** FreeRTOS queue or ring buffer bridging NimBLE and TinyUSB.
- **Verification Criteria:** `cd firmware && pio check`

### Task 2.5: Add hardware pairing bond security and static PIN authentication. [EASY]
- **Target Files:** `firmware/src/ble_sec.cpp` (Create)
- **Core Technical Requirements:** NimBLE security manager configuration for encrypted connections.
- **Verification Criteria:** `cd firmware && pio check`

### Task 3.2: Implement single-link DeviceConnectionRouter using Disconnect-Before-Connect lifecycle. [HARD]
- **Target Files:** `app/src/main/java/com/remote/router/ConnectionRouter.kt` (Create)
- **Core Technical Requirements:** Coroutine-based state machine for exclusive link management.
- **Verification Criteria:** `./gradlew test`

### Task 3.4: Build dynamic Compose UI surfaces (Full Keyboard, D-Pad TV Remote, Media Bar, Macro Pad). [HARD]
- **Target Files:** `app/src/main/java/com/remote/ui/Surfaces.kt` (Create)
- **Core Technical Requirements:** Adaptive UI grids and buttons for different connection types.
- **Verification Criteria:** `./gradlew lint`

### Task 4.2: Implement SSDP UDP multicast discovery (ST: roku:ecp) with subnet sweep fallback. [HARD]
- **Target Files:** `app/src/main/java/com/remote/network/SsdpDiscovery.kt` (Create)
- **Core Technical Requirements:** Multicast UDP sockets for auto-discovering Roku devices.
- **Verification Criteria:** `./gradlew test`

### Task 4.3: Implement SSHJ client supporting Ed25519 key generation and known_hosts validation. [HARD]
- **Target Files:** `app/src/main/java/com/remote/network/SshClient.kt` (Create)
- **Core Technical Requirements:** SSH connection via net.schmizz:sshj with local key management.
- **Verification Criteria:** `./gradlew test`

### Task 4.4: Support one-shot SSH Exec commands (for button macros) and continuous PTY Shell streaming. [EASY]
- **Target Files:** `app/src/main/java/com/remote/network/SshSession.kt` (Create)
- **Core Technical Requirements:** Channel execution for remote ssh targets.
- **Verification Criteria:** `./gradlew test`

### Task 4.5: Raspberry Pi Zero ConfigFS multi-function composite gadget script (HID + Mass Storage + RNDIS). [HARD]
- **Target Files:** `scripts/pi_gadget.sh` (Create)
- **Core Technical Requirements:** Bash script configuring Linux ConfigFS.
- **Verification Criteria:** `shellcheck scripts/pi_gadget.sh`

### Task 5.1: Implement ESP32 RMT hardware infrared driver using IRremoteESP8266 library. [HARD]
- **Target Files:** `firmware/src/ir_tx.cpp` (Create)
- **Core Technical Requirements:** RMT peripheral integration for 38kHz infrared transmission.
- **Verification Criteria:** `cd firmware && pio check`

### Task 5.2: Update BLE frame specification to handle IR protocol opcodes (NEC, Sony, RC5, Raw). [EASY]
- **Target Files:** `firmware/src/ble_nus.cpp` (Modify)
- **Core Technical Requirements:** Protocol expansion for IR packets.
- **Verification Criteria:** `cd firmware && pio check`

### Task 5.4: Build AudioTrack 19kHz dual-channel phase-inverted carrier generator for 3.5mm adapters. [HARD]
- **Target Files:** `app/src/main/java/com/remote/ir/AudioIr.kt` (Create)
- **Core Technical Requirements:** PCM byte array generation for IR modulation via headphone jack.
- **Verification Criteria:** `./gradlew test`

### Task 5.5: Implement USB Host CDC-ACM serial driver for USB-C IR dongles. [HARD]
- **Target Files:** `app/src/main/java/com/remote/usb/UsbCdcDriver.kt` (Create)
- **Core Technical Requirements:** Android USB Host API integration for external serial adapters.
- **Verification Criteria:** `./gradlew test`

