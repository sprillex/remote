#include <Arduino.h>
#include "ble_nus.h"

void setup() {
  Serial.begin(115200);
  setupBleNus();
}

void loop() {
  processBleTasks();
  delay(10);
}
