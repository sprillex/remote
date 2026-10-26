#include <Arduino.h>
#include "ble_nus.h"
#include "ir_tx.h"

void setup() {
  Serial.begin(115200);
  setupIrTx();
  setupBleNus();
}

void loop() {
  processBleTasks();
  delay(10);
}
