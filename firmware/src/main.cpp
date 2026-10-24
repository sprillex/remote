#include <Arduino.h>
#include "ble_nus.h"

extern "C" void initIR();

void setup() {
  Serial.begin(115200);
  setupBleNus();
  initIR();
}

void loop() {
  processBleTasks();
  delay(10);
}
