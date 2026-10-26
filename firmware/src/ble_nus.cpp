#include "ble_nus.h"
#include <NimBLEDevice.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include "ring_buffer.h"
#include "ir_tx.h"
#include <IRremoteESP8266.h>

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" // UART service UUID
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

// Protocol Opcodes
#define OPCODE_HID     0x01
#define OPCODE_IR_NEC  0x02
#define OPCODE_IR_SONY 0x03
#define OPCODE_IR_RC5  0x04
#define OPCODE_IR_RAW  0x05

NimBLEServer *pServer = NULL;
NimBLECharacteristic * pTxCharacteristic;

QueueHandle_t bleRxQueue;

class MyServerCallbacks: public NimBLEServerCallbacks {
    void onConnect(NimBLEServer* pServer) {
      // Device connected
    }

    void onDisconnect(NimBLEServer* pServer) {
      // Device disconnected, restart advertising
      NimBLEDevice::startAdvertising();
    }
};

class MyCallbacks: public NimBLECharacteristicCallbacks {
    void onWrite(NimBLECharacteristic *pCharacteristic) {
      std::string rxValue = pCharacteristic->getValue();

      if (rxValue.length() > 0) {
        uint8_t opcode = rxValue[0];

        if (opcode == OPCODE_HID) {
            // Push incoming HID command frames to FreeRTOS queue
            HidReport report = {0};
            size_t payload_len = rxValue.length() - 1;
            if (payload_len > sizeof(HidReport)) {
                payload_len = sizeof(HidReport);
            }
            if (payload_len > 0) {
                memcpy(&report, rxValue.data() + 1, payload_len);
                if (xQueueSend(bleRxQueue, &report, 0) != pdPASS) {
                    // Queue full, drop packet
                }
            }
        } else if (opcode == OPCODE_IR_NEC || opcode == OPCODE_IR_SONY ||
                   opcode == OPCODE_IR_RC5) {
            // Safely parse IR frames and send
            if (rxValue.length() >= 9) { // 1 byte opcode + 8 bytes data
                uint64_t data = 0;
                memcpy(&data, rxValue.data() + 1, 8);
                uint16_t nbits = (opcode == OPCODE_IR_SONY) ? 12 : (opcode == OPCODE_IR_RC5 ? 12 : 32); // Default nbits based on protocol, should ideally be passed in
                sendIrCommand(opcode, data, nbits);
            }
        } else if (opcode == OPCODE_IR_RAW) {
            // Drop raw for now
            Serial.printf("Received IR raw opcode: 0x%02X, length: %d\n", opcode, rxValue.length());
        } else {
            // Unknown opcode, drop
            Serial.printf("Received unknown opcode: 0x%02X\n", opcode);
        }
      }
    }
};

void setupBleNus() {
  bleRxQueue = xQueueCreate(10, sizeof(HidReport));

  NimBLEDevice::init("NimBLE_NUS");

  pServer = NimBLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  NimBLEService *pService = pServer->createService(SERVICE_UUID);

  pTxCharacteristic = pService->createCharacteristic(
                                        CHARACTERISTIC_UUID_TX,
                                        NIMBLE_PROPERTY::NOTIFY
                                      );

  NimBLECharacteristic * pRxCharacteristic = pService->createCharacteristic(
                                             CHARACTERISTIC_UUID_RX,
                                             NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::WRITE_NR
                                           );

  pRxCharacteristic->setCallbacks(new MyCallbacks());

  pService->start();

  NimBLEAdvertising *pAdvertising = NimBLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->start();
}

void processBleTasks() {
  HidReport report;
  if (xQueueReceive(bleRxQueue, &report, 0) == pdPASS) {
    // Parse incoming binary command frames
    Serial.println("*********");
    Serial.print("Received Value: ");
    uint8_t* rawData = (uint8_t*)&report;
    for (size_t i = 0; i < sizeof(HidReport); i++) {
      Serial.print(rawData[i], HEX);
      Serial.print(" ");
    }
    Serial.println();
    Serial.println("*********");
  }
}
