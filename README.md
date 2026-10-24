# Multi-Target Input & Injection System

## 1. Project Overview

This repository contains a **Multi-Target Input & Injection System**, designed to seamlessly control a variety of hardware and software endpoints. The system acts as a universal remote and injection platform, utilizing a modern Android application (written in Kotlin and Jetpack Compose) to bridge commands to targets via multiple protocols.

Supported targets and protocols include:

- **Bluetooth HID (Human Interface Device):** Direct keyboard/mouse emulation.
- **ESP32 Dongles:** Wireless injection bridging Android BLE to USB via ESP32-S2/S3 hardware.
- **Roku ECP (External Control Protocol):** REST-based network control via UDP SSDP discovery and OkHttp.
- **SSH (Secure Shell):** Remote execution and shell sessions.
- **Infrared (IR) Emitters:** Legacy hardware control (RMT peripheral on ESP32 or AudioTrack dual-channel carriers).

## 2. Prerequisites

To build, test, and deploy this project, the following dependencies are required:

### Software

- **Android App:** JDK 17, Android SDK (API 34), Gradle.
- **Firmware:** Python 3.10+, PlatformIO Core.
- **System Packages:** `curl`, `git`, `python3-venv`.

### Hardware

- **Android Device:** API Level 26+ (API 28+ required for Bluetooth HID Device profiles).
- **Firmware Hardware:** ESP32-S3 DevKitC-1 (or equivalent ESP32-S2/S3 for native USB and BLE support).

## 3. Installation & Setup

Follow these exact steps to prepare the local development environment:

### Android Application

1. **Clone the repository:**
   ```bash
   git clone <repository_url>
   cd <repository_directory>
   ```
2. **Build the Android APK:**
   ```bash
   ./gradlew assembleDebug
   ```
3. **Run Lint and Tests:**
   ```bash
   ./gradlew lint
   ./gradlew test
   ```

### ESP32 Firmware

1. **Set up PlatformIO environment:**
   ```bash
   python3 -m venv .venv
   source .venv/bin/activate
   pip install platformio
   ```
2. **Compile the firmware:**
   ```bash
   cd firmware
   pio run
   ```
3. **Run Firmware Unit Tests (without hardware):**
   ```bash
   pio test --without-uploading
   ```

## 4. Configuration

There are no required dynamic `.env` files. Configuration is currently handled at compile-time:

- **Android Client:** API keys or static credentials (if any) are typically managed securely via Android Keystore or internal encrypted storage. Do not hardcode them.
- **BLE Service/Characteristics UUIDs:** Configured statically in `firmware/src/ble_nus.cpp` (e.g., `6E400001-B5A3-F393-E0A9-E50E24DCCA9E`).
- **Network Defaults:** `RokuClient` defaults to port `8060` with a 3000ms connection timeout (see `RokuClient.kt`).

## 5. Usage Guide

### Deploying the App

Install the built APK onto an Android device connected via ADB:

```bash
./gradlew installDebug
```

### Flashing the Firmware

Connect the ESP32-S3 to your workstation via USB and flash the firmware:

```bash
cd firmware
source ../.venv/bin/activate
pio run -t upload
```

### Operation

1. Pair your Android device with the ESP32 (SSID: `NimBLE_NUS`).
2. Ensure secure pairing is used; the firmware utilizes `esp_random()` for Passkey generation to avoid MITM attacks.
3. Once connected, commands from the App (UI surfaces like D-Pad or Keyboard) will be serialized over BLE NUS and bridged to the target endpoint.

## 6. Architecture & Subsystems

### Android Application (Client)

- **UI:** Jetpack Compose (Dynamic grids, Keyboard, Media Bar).
- **Concurrency:** Kotlin Coroutines (`Dispatchers.IO` for network, `Mutex` for router state).
- **Local Persistence:** Room Database (`AppDatabase.kt`) for caching discovered devices.
- **Networking:** `OkHttp` for REST ECP control (connection-pooled to prevent allocation exhaustion).
- **Routing:** `ConnectionRouter` enforces a Disconnect-Before-Connect lifecycle via coroutine state machines, ensuring exclusive links.

### Embedded Firmware (Bridge/Dongle)

- **Framework:** Arduino framework on ESP-IDF via PlatformIO.
- **BLE Stack:** `NimBLE-Arduino` (h2zero) provides a lightweight, highly efficient Bluetooth stack.
- **Task Management:** FreeRTOS ring buffers (`bleRxQueue`) queue incoming BLE UART frames for processing in the main application loop, preventing watchdog resets in interrupt/callback contexts.
- **Security:** Requires encrypted connections. The `SecurityCallbacks` class enforces out-of-band/random PIN matching.

## 7. Troubleshooting

| Symptom / Error                                      | Root Cause                                                                     | Actionable Recovery Step                                                                                                    |
| ---------------------------------------------------- | ------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------- |
| **App ANR during Bluetooth pairing/connect**         | Callbacks executing on the Binder thread blocking IPC.                         | Verify `HidDeviceManager` uses a dedicated `ExecutorService` rather than running inline blocks.                             |
| **ESP32 Watchdog Reset / Dropped Connections**       | Blocking I/O (`Serial.println`) inside NimBLE interrupt callbacks (`onWrite`). | Ensure payload is pushed to `bleRxQueue` and parsed in the `loop()` context.                                                |
| **High Memory Allocation / Slow Network Injections** | Creating a new `HttpURLConnection` per keypress.                               | Ensure `OkHttpClient` is instantiated as a singleton and reused for connection keep-alive (implemented in `RokuClient.kt`). |
| **`pio: command not found`**                         | Virtual environment not active.                                                | Run `source .venv/bin/activate` in the project root.                                                                        |
