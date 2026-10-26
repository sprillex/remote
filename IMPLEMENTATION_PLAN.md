# Implementation Plan

### Task 5.4: Build AudioTrack 19kHz dual-channel phase-inverted carrier generator for 3.5mm adapters. [HARD]

- **Target Files:** `app/src/main/java/com/remote/ir/AudioIr.kt` (Create)
- **Core Technical Requirements:** PCM byte array generation for IR modulation via headphone jack.
- **Verification Criteria:** `./gradlew test`

### Task 5.5: Implement USB Host CDC-ACM serial driver for USB-C IR dongles. [HARD]

- **Target Files:** `app/src/main/java/com/remote/usb/UsbCdcDriver.kt` (Create)
- **Core Technical Requirements:** Android USB Host API integration for external serial adapters.
- **Verification Criteria:** `./gradlew test`
