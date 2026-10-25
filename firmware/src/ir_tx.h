#ifndef IR_TX_H
#define IR_TX_H

#include <stdint.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>

#define MAX_IR_PAYLOAD_SIZE 64

struct IrReport {
    uint8_t opcode;
    uint8_t length;
    uint8_t data[MAX_IR_PAYLOAD_SIZE];
};

extern QueueHandle_t irTxQueue;

void setupIrTx();
void processIrTasks();

#endif // IR_TX_H
