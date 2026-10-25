#include "ir_tx.h"
#include <IRsend.h>
#include <Arduino.h>

#define IR_TX_PIN 4
#define IR_RAW_FREQ 38 // 38 kHz for RAW

#include "protocol.h"

QueueHandle_t irTxQueue;
IRsend irsend(IR_TX_PIN);

void setupIrTx() {
    irTxQueue = xQueueCreate(10, sizeof(IrReport));
    irsend.begin();
}

void processIrTasks() {
    IrReport report;
    if (xQueueReceive(irTxQueue, &report, 0) == pdPASS) {
        if (report.length > MAX_IR_PAYLOAD_SIZE) {
            report.length = MAX_IR_PAYLOAD_SIZE;
        }

        switch (report.opcode) {
            case OPCODE_IR_NEC:
                if (report.length >= 4) {
                    uint32_t data = 0;
                    memcpy(&data, report.data, 4);
                    irsend.sendNEC(data, 32);
                    Serial.printf("Sent NEC: %08X\n", data);
                }
                break;
            case OPCODE_IR_SONY:
                if (report.length >= 4) {
                    uint32_t data = 0;
                    memcpy(&data, report.data, 4);
                    // Sony is typically 12, 15, or 20 bits. We'll default to 12.
                    irsend.sendSony(data, 12);
                    Serial.printf("Sent Sony: %08X\n", data);
                }
                break;
            case OPCODE_IR_RC5:
                if (report.length >= 4) {
                    uint32_t data = 0;
                    memcpy(&data, report.data, 4);
                    // RC5 is typically 12, 14, or 20. We'll default to 12.
                    irsend.sendRC5(data, 12);
                    Serial.printf("Sent RC5: %08X\n", data);
                }
                break;
            case OPCODE_IR_RAW: {
                if (report.length > 0 && (report.length % 2 == 0)) {
                    uint16_t num_elements = report.length / 2;
                    uint16_t raw_data[MAX_IR_PAYLOAD_SIZE / 2];
                    for (int i = 0; i < num_elements; i++) {
                        // Assuming little-endian over the wire
                        raw_data[i] = (report.data[i * 2 + 1] << 8) | report.data[i * 2];
                    }
                    irsend.sendRaw(raw_data, num_elements, IR_RAW_FREQ);
                    Serial.printf("Sent RAW (%d elements)\n", num_elements);
                }
                break;
            }
            default:
                Serial.printf("Unknown IR opcode: %02X\n", report.opcode);
                break;
        }
    }
}
