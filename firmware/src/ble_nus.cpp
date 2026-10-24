#include "ble_nus.h"
#include <NimBLEDevice.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>

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
        // Push incoming binary command frames to FreeRTOS queue
        std::string* queuedValue = new std::string(rxValue);
        if (xQueueSend(bleRxQueue, &queuedValue, 0) != pdPASS) {
            // Queue full, drop packet
            delete queuedValue;
        }
      }
    }
};

void setupBleNus() {
  bleRxQueue = xQueueCreate(10, sizeof(std::string*));

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
  std::string* rxValue;
  if (xQueueReceive(bleRxQueue, &rxValue, 0) == pdPASS) {
    // Parse incoming binary command frames
    Serial.println("*********");
    Serial.print("Received Value: ");
    for (int i = 0; i < rxValue->length(); i++) {
      Serial.print((uint8_t)(*rxValue)[i], HEX);
      Serial.print(" ");
    }
    Serial.println();
    Serial.println("*********");
    delete rxValue;
  }
}
