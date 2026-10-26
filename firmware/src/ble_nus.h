#ifndef BLE_NUS_H
#define BLE_NUS_H

#include <Arduino.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include "ring_buffer.h"

// Protocol Opcodes
#define OPCODE_HID     0x01
#define OPCODE_IR_NEC  0x02
#define OPCODE_IR_SONY 0x03
#define OPCODE_IR_RC5  0x04
#define OPCODE_IR_RAW  0x05

struct NusFrame {
    uint8_t opcode;
    union {
        HidReport hid;
        struct {
            uint64_t data;
            uint16_t nbits;
        } ir;
        struct {
            uint8_t raw_opcode;
            size_t length;
        } unknown;
    } payload;
};

extern QueueHandle_t bleRxQueue;

void setupBleNus();
void processBleTasks();

#endif // BLE_NUS_H
