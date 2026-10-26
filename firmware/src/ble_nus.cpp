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

        NusFrame frame = {0};
        frame.opcode = opcode;

        if (opcode == OPCODE_HID) {
            size_t payload_len = rxValue.length() - 1;
            if (payload_len > sizeof(HidReport)) {
                payload_len = sizeof(HidReport);
            }
            if (payload_len > 0) {
                memcpy(&frame.payload.hid, rxValue.data() + 1, payload_len);
                xQueueSend(bleRxQueue, &frame, 0);
            }
        } else if (opcode == OPCODE_IR_NEC || opcode == OPCODE_IR_SONY ||
                   opcode == OPCODE_IR_RC5) {
            if (rxValue.length() >= 9) { // 1 byte opcode + 8 bytes data
                uint64_t data = 0;
                memcpy(&data, rxValue.data() + 1, 8);
                uint16_t nbits = (opcode == OPCODE_IR_SONY) ? 12 : (opcode == OPCODE_IR_RC5 ? 12 : 32);
                frame.payload.ir.data = data;
                frame.payload.ir.nbits = nbits;
                xQueueSend(bleRxQueue, &frame, 0);
            }
        } else if (opcode == OPCODE_IR_RAW) {
            frame.payload.unknown.raw_opcode = opcode;
            frame.payload.unknown.length = rxValue.length();
            xQueueSend(bleRxQueue, &frame, 0);
        } else {
            frame.payload.unknown.raw_opcode = opcode;
            frame.payload.unknown.length = rxValue.length();
            xQueueSend(bleRxQueue, &frame, 0);
        }
      }
    }
};

void setupBleNus() {
  bleRxQueue = xQueueCreate(10, sizeof(NusFrame));

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
  NusFrame frame;
  if (xQueueReceive(bleRxQueue, &frame, 0) == pdPASS) {
    if (frame.opcode == OPCODE_HID) {
      Serial.println("*********");
      Serial.print("Received Value: ");
      uint8_t* rawData = (uint8_t*)&frame.payload.hid;
      for (size_t i = 0; i < sizeof(HidReport); i++) {
        Serial.print(rawData[i], HEX);
        Serial.print(" ");
      }
      Serial.println();
      Serial.println("*********");
    } else if (frame.opcode == OPCODE_IR_NEC || frame.opcode == OPCODE_IR_SONY ||
               frame.opcode == OPCODE_IR_RC5) {
      sendIrCommand(frame.opcode, frame.payload.ir.data, frame.payload.ir.nbits);
    } else if (frame.opcode == OPCODE_IR_RAW) {
      Serial.printf("Received IR raw opcode: 0x%02X, length: %d\n", frame.payload.unknown.raw_opcode, (int)frame.payload.unknown.length);
    } else {
      Serial.printf("Received unknown opcode: 0x%02X\n", frame.payload.unknown.raw_opcode);
    }
  }
}
