# Implementation Plan

### Task 1.0: Implement ESP32 RMT hardware infrared driver using IRremoteESP8266 library. [RESOLVED]
- **Target Files:** `firmware/src/ir_tx.cpp`
- **Core Technical Requirements:** Use IRremoteESP8266 library to implement ESP32 RMT hardware infrared driver.
- **Verification Criteria:** `cd firmware && pio test --without-uploading`

### Task 2.0: Fix Swallowed Network Exceptions [EASY]
- **Target Files:** `app/src/main/java/com/remote/network/RokuClient.kt`
- **Core Technical Requirements:** Catching generic `Exception` without logging hides critical connection failures (e.g., timeouts, unreachable hosts). Failure state: Silent failure of remote commands. Proposed fix: Log exception details and return a distinct error type (Result/Either).
- **Verification Criteria:** `./gradlew test`

### Task 3.0: Fix Unused Connection Parameters [EASY]
- **Target Files:** `app/src/main/java/com/remote/router/ConnectionRouter.kt`
- **Core Technical Requirements:** The `device` parameter in `disconnectInternal` and `connectInternal` is unused, meaning the router does not actually delegate properly to protocol managers. Failure state: Stale or incorrect connections during routing. Proposed fix: Pass the device configuration correctly to the protocol handlers.
- **Verification Criteria:** `./gradlew test`

### Task 4.0: Fix Unhandled SSH Flow Exceptions [RESOLVED]
- **Target Files:** `app/src/main/java/com/remote/network/SshSession.kt`
- **Core Technical Requirements:** `executeCommand` and `startPtyShell` flows do not comprehensively handle connection drops midway, which could result in dangling Coroutine scopes. Trigger condition: Dropped network connection during SSH shell. Proposed fix: Implement robust exception catching and flow `.catch` operators.
- **Verification Criteria:** `./gradlew test`

### Task 5.0: Fix Blocking Socket Connect Thread Leak [HARD]
- **Target Files:** `app/src/main/java/com/remote/network/SsdpDiscovery.kt`
- **Core Technical Requirements:** `Socket.connect` is blocking and non-cancellable by coroutine timeouts (`withTimeoutOrNull`). Trigger condition: Unreachable IPs during subnet sweep block IO dispatcher threads until OS TCP timeout. Proposed fix: Use nio `SocketChannel` or run inside `runInterruptible` block to ensure thread cancellation.
- **Verification Criteria:** `./gradlew test`

### Task 6.0: Fix Blocking I/O in Coroutine Flow [HARD]
- **Target Files:** `app/src/main/java/com/remote/network/SshSession.kt`
- **Core Technical Requirements:** The `startPtyShell` Flow blocks the current dispatcher thread with a synchronous `while(true)` read loop `inputStream.read(buffer)` without checking for cancellation or yielding. Trigger condition: Long-running SSH shell blocks the IO dispatcher. Proposed fix: Use `yield()` or `isActive` check within the loop to allow coroutine cancellation, or wrap blocking reads appropriately.
- **Verification Criteria:** `./gradlew test`

### Task 7.0: Fix High Allocation Rate on Key Injection [HARD]
- **Target Files:** `app/src/main/java/com/remote/network/RokuClient.kt`
- **Core Technical Requirements:** Creating a new `HttpURLConnection` for each individual keystroke in `sendTextString` creates an allocation hot-loop and resource exhaustion. Description: Connection setup overhead limits injection speed and wastes memory. Remediation: Migrate to a connection-pooled HTTP client like OkHttp for keep-alive connections.
- **Verification Criteria:** `./gradlew test`

### Task 8.0: Fix Dynamic Allocation in BLE Interrupt [RESOLVED]
- **Target Files:** `firmware/src/ble_nus.cpp`
- **Core Technical Requirements:** `new std::string(rxValue)` inside `onWrite` callback allocates heap memory dynamically in an ISR/BLE task context. Description: Unbounded heap allocations in tasks can cause memory fragmentation and exhaustion. Remediation: Use a static buffer or pre-allocated FreeRTOS queue for `HidReport` objects instead of dynamic strings.
- **Verification Criteria:** `cd firmware && pio test --without-uploading`

### Task 9.0: Fix Hot-Loop Allocations in Key Injection [RESOLVED]
- **Target Files:** `app/src/main/java/com/remote/ui/PrecomposeBuffer.kt`
- **Core Technical Requirements:** `HidDeviceManager.buildKeyboardReport` and `emptyList()` create new objects on every keystroke during `injectText`. Allocation hot-loop description: Constant GC pressure during large payload injection. Remediation: Cache and reuse a static `keyUpReport` byte array and reuse mutable byte arrays for key down.
- **Verification Criteria:** `./gradlew test`

### Task 10.0: Fix Empty Catch Blocks in Discovery [RESOLVED]
- **Target Files:** `app/src/main/java/com/remote/network/SsdpDiscovery.kt`
- **Core Technical Requirements:** Catching generic exceptions (`try { performSsdpDiscovery() } catch (e: Exception) {}`) without logging during SSDP fallback sweeps. Trigger condition: Network failures during SSDP are silently ignored. Proposed fix: Log the exception to aid in debugging connection issues.
- **Verification Criteria:** `./gradlew test`

### Task 11.0: Fix Deprecated API Usage [EASY]
- **Target Files:** `app/src/main/java/com/remote/bluetooth/BleGattManager.kt`
- **Core Technical Requirements:** Accessing `characteristic.value` directly is deprecated in Android 13 (API 33). Linter warning. Remediation: Override the new `onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic, ByteArray)` callback signature introduced in TIRAMISU.
- **Verification Criteria:** `./gradlew test`

### Task 12.0: Fix Incomplete Edge Case Handling [EASY]
- **Target Files:** `app/src/main/java/com/remote/bluetooth/KeyTranslator.kt`
- **Core Technical Requirements:** Unsupported characters map to `null`, which are silently skipped in `PrecomposeBufferInjector`. Idiom deviation. Remediation: Fallback to an alternative input method or log explicit warnings for skipped inputs.
- **Verification Criteria:** `./gradlew test`

### Task 13.0: Fix Deprecated API Usage [EASY]
- **Target Files:** `app/src/main/java/com/remote/bluetooth/FlipperBridge.kt`
- **Core Technical Requirements:** Same as `BleGattManager.kt`, direct access of `characteristic.value` and old signature of `onCharacteristicChanged` triggers linter warnings for API 33+. Remediation: Suppress deprecation warning and override the newer Android 13+ signature.
- **Verification Criteria:** `./gradlew test`

### Task 14.0: Fix Deprecated Descriptor Write [EASY]
- **Target Files:** `app/src/main/java/com/remote/bluetooth/BleGattManager.kt`
- **Core Technical Requirements:** `descriptor.value` and `writeDescriptor(BluetoothGattDescriptor)` are deprecated in Android 13 (API 33). Linter warning. Remediation: Use `writeDescriptor(BluetoothGattDescriptor, ByteArray)`.
- **Verification Criteria:** `./gradlew test`

### Task 15.0: Fix Deprecated Characteristic Write [EASY]
- **Target Files:** `app/src/main/java/com/remote/bluetooth/BleGattManager.kt`
- **Core Technical Requirements:** `characteristic.value` and `writeCharacteristic(BluetoothGattCharacteristic)` are deprecated in Android 13 (API 33). Linter warning. Remediation: Use `writeCharacteristic(BluetoothGattCharacteristic, ByteArray, Int)`.
- **Verification Criteria:** `./gradlew test`

### Task 16.0: Fix Deprecated Characteristic Write (Flipper) [EASY]
- **Target Files:** `app/src/main/java/com/remote/bluetooth/FlipperBridge.kt`
- **Core Technical Requirements:** `characteristic.value` and `writeCharacteristic(BluetoothGattCharacteristic)` are deprecated in Android 13 (API 33). Linter warning. Remediation: Use `writeCharacteristic(BluetoothGattCharacteristic, ByteArray, Int)`.
- **Verification Criteria:** `./gradlew test`

### Task 17.0: Fix Unimplemented UI Actions [EASY]
- **Target Files:** `app/src/main/java/com/remote/ui/Surfaces.kt`
- **Core Technical Requirements:** Multiple `Button` click handlers are populated with `/* TODO */` empty lambdas. Description: Incomplete feature implementation. Remediation: Hook up buttons to `DeviceManager` or `ConnectionRouter` actions, or disable the buttons if unsupported.
- **Verification Criteria:** `./gradlew test`
