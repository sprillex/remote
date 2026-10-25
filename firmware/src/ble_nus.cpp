#include "ble_nus.h"
#include <NimBLEDevice.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include "ring_buffer.h"
#include "ir_tx.h"

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" // UART service UUID
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

#include "protocol.h"
// Protocol Opcodes

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
                   opcode == OPCODE_IR_RC5 || opcode == OPCODE_IR_RAW) {
            IrReport report = {0};
            report.opcode = opcode;
            size_t payload_len = rxValue.length() - 1;
            if (payload_len > MAX_IR_PAYLOAD_SIZE) {
                payload_len = MAX_IR_PAYLOAD_SIZE;
            }
            report.length = payload_len;
            if (payload_len > 0) {
                memcpy(report.data, rxValue.data() + 1, payload_len);
            }
            if (xQueueSend(irTxQueue, &report, 0) != pdPASS) {
                // Queue full, drop packet
                Serial.println("IR Queue full, dropped packet");
            }
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
