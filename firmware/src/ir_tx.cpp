#include <Arduino.h>
#include <IRremoteESP8266.h>
#include <IRsend.h>

#ifndef IR_TX_PIN
#define IR_TX_PIN 4
#endif

IRsend irsend(IR_TX_PIN);

extern "C" {
    void initIR() {
        irsend.begin();
    }

    static uint64_t payloadToUint64(const uint8_t* payload, size_t len) {
        uint64_t data = 0;
        size_t max_len = len > 8 ? 8 : len;
        for (size_t i = 0; i < max_len; i++) {
            data = (data << 8) | payload[i];
        }
        return data;
    }

    void sendIR_NEC(const uint8_t* payload, size_t len) {
        uint64_t data = payloadToUint64(payload, len);
        uint16_t nbits = len * 8;
        irsend.sendNEC(data, nbits);
    }

    void sendIR_SONY(const uint8_t* payload, size_t len) {
        uint64_t data = payloadToUint64(payload, len);
        uint16_t nbits = len * 8;
        irsend.sendSony(data, nbits, 2); // Sony standard generally requires repeat=2
    }

    void sendIR_RC5(const uint8_t* payload, size_t len) {
        uint64_t data = payloadToUint64(payload, len);
        uint16_t nbits = len * 8;
        irsend.sendRC5(data, nbits);
    }

    void sendIR_RAW(const uint8_t* payload, size_t len) {
        // Raw timing data is generally an array of 16-bit integers
        uint16_t num_elements = len / 2;
        irsend.sendRaw((const uint16_t*)payload, num_elements, 38);
    }
}
