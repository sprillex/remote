#include <Arduino.h>
#include <IRremoteESP8266.h>
#include <IRsend.h>

const uint16_t kIrLed = 4; // Use GPIO 4 for IR LED
IRsend irsend(kIrLed);

void setupIrTx() {
  irsend.begin();
}

void sendIrCommand(uint8_t opcode, uint64_t data, uint16_t nbits) {
  switch (opcode) {
    case 0x02: // OPCODE_IR_NEC
      irsend.sendNEC(data, nbits);
      break;
    case 0x03: // OPCODE_IR_SONY
      irsend.sendSony(data, nbits);
      break;
    case 0x04: // OPCODE_IR_RC5
      irsend.sendRC5(data, nbits);
      break;
    default:
      Serial.printf("Unknown IR opcode: 0x%02X\n", opcode);
      break;
  }
}
