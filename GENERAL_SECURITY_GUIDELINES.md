# General Security Guidelines & Vulnerabilities to Avoid

## Executive Summary
This document provides a general overview of common security vulnerabilities and best practices relevant to modern application development, particularly systems involving mobile applications (Android/Kotlin), network communication (REST, SSH), and hardware interfaces (Bluetooth, IR). It serves as a guide for developers to defensively design and implement features while avoiding common pitfalls.

## 1. Injection Vulnerabilities (P0 Level Risk)
Injection flaws occur when untrusted data is sent to an interpreter as part of a command or query.
- **SQL Injection (SQLi):** If using local databases (like Room on Android), ensure that raw queries use parameterized inputs. Avoid concatenating strings to build SQL queries.
- **Command Injection:** If the application executes system commands (e.g., via SSH or local shell), sanitize all inputs. Avoid passing user-controlled strings directly to `Runtime.getRuntime().exec()` or similar command execution APIs.

## 2. Authentication & Credential Management (P0 Level Risk)
Mishandling credentials can lead to unauthorized access and complete system compromise.
- **Hardcoded Secrets:** Never hardcode API keys, SSH passwords, or encryption keys in the source code. Use secure storage mechanisms (e.g., Android Keystore) or retrieve them dynamically over secure channels.
- **Insecure Data Storage:** Avoid storing sensitive information in plaintext in SharedPreferences or local files. Encrypt sensitive data at rest.
- **Weak Cryptography:** Avoid deprecated algorithms like MD5 or SHA-1. Use industry-standard algorithms (e.g., AES-GCM for symmetric encryption, SHA-256 or higher for hashing).

## 3. Network Security & Communication (P1 Level Risk)
Insecure communication exposes data to interception and tampering.
- **Man-in-the-Middle (MitM) Attacks:** Always use TLS/HTTPS for network communication (e.g., REST API calls). Avoid disabling SSL certificate validation or using custom, insecure trust managers.
- **SSH Security:** When connecting to targets via SSH, validate host keys to prevent connecting to malicious endpoints. Avoid accepting all host keys blindly.
- **Insecure Bluetooth (BLE) Communication:** Bluetooth communications can be intercepted. Ensure pairing uses secure methods (e.g., Just Works is vulnerable to MitM; prefer Numeric Comparison). Encrypt sensitive payloads at the application layer if the Bluetooth link layer encryption is insufficient.

## 4. Runtime Stability & Concurrency (P1/P2 Level Risk)
Bugs in this category can lead to application crashes, denial of service, or unpredictable behavior.
- **Data Races and Deadlocks:** When using multi-threading (e.g., Kotlin Coroutines), ensure shared mutable state is protected using synchronization primitives (Mutex) or thread-safe data structures.
- **Unhandled Exceptions:** Wrap risky operations (network calls, parsing, I/O) in `try-catch` blocks or use Result types to handle errors gracefully. Unhandled exceptions in background threads can crash the entire application.

## 5. Resource Management & Leaks (P2 Level Risk)
Improper resource management degrades performance and can lead to out-of-memory errors.
- **Memory Leaks:** In Android, avoid holding strong references to `Activity` or `Context` objects in long-lived background tasks or singletons.
- **Unclosed Resources:** Always close file descriptors, network sockets, and database cursors after use. Utilize try-with-resources (or Kotlin's `use` block) to ensure resources are cleaned up even if exceptions occur.

## 6. Input Validation & Logic Flaws (P3 Level Risk)
- **Missing Edge-case Validation:** Validate all inputs (length, format, type) before processing them. This applies to UI inputs, incoming network payloads, and Bluetooth read/write characteristics.
- **Insecure File Permissions:** Ensure files created by the application are not world-readable or world-writable unless absolutely necessary. On Android, use `MODE_PRIVATE` by default.

## Conclusion
Adhering to these general principles will significantly reduce the attack surface and improve the overall stability and security of the application. Regular security training and incorporating automated security testing into the CI/CD pipeline are recommended.
