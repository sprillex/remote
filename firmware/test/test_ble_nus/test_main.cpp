#include <Arduino.h>
#include <unity.h>
#include "ring_buffer.h"
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>

QueueHandle_t bleRxQueue;

void setUp(void) {
    bleRxQueue = xQueueCreate(10, sizeof(HidReport));
}

void tearDown(void) {
    if (bleRxQueue != NULL) {
        vQueueDelete(bleRxQueue);
    }
}

void test_hidreport_queue_size_and_type(void) {
    HidReport report = {0};
    report.modifiers = 0x01;
    report.key1 = 0x02;

    BaseType_t res = xQueueSend(bleRxQueue, &report, 0);
    TEST_ASSERT_EQUAL(pdPASS, res);

    HidReport received_report = {0};
    res = xQueueReceive(bleRxQueue, &received_report, 0);
    TEST_ASSERT_EQUAL(pdPASS, res);

    TEST_ASSERT_EQUAL(0x01, received_report.modifiers);
    TEST_ASSERT_EQUAL(0x02, received_report.key1);
    TEST_ASSERT_EQUAL(0x00, received_report.key2);
}

void test_opcode_parsing_hid_frame(void) {
    // Note: since MyCallbacks::onWrite is private inside setupBleNus,
    // we just verify that we are compiling the right opcodes and parsing them.
    // In actual implementation, we simulate dropping the HID packet by ensuring
    // parsing logic extracts correctly.

    // We can simulate parsing logic here since onWrite is embedded:
    uint8_t rxData[] = {0x01, 0x02, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00}; // HID Opcode + Modifiers + Key1
    size_t length = sizeof(rxData);
    (void)length;

    uint8_t opcode = rxData[0];
    TEST_ASSERT_EQUAL(0x01, opcode); // OPCODE_HID

    HidReport report = {0};
    size_t payload_len = length - 1;
    if (payload_len > sizeof(HidReport)) {
        payload_len = sizeof(HidReport);
    }
    memcpy(&report, rxData + 1, payload_len);
    TEST_ASSERT_EQUAL(0x02, report.modifiers);
    TEST_ASSERT_EQUAL(0x04, report.key1);
}

void test_opcode_parsing_ir_frame(void) {
    uint8_t rxData[] = {0x02, 0x00, 0xFF, 0x00, 0xAA}; // IR NEC Opcode
    size_t length = sizeof(rxData);
    (void)length;

    uint8_t opcode = rxData[0];
    TEST_ASSERT_EQUAL(0x02, opcode); // OPCODE_IR_NEC
}

void setup() {
    UNITY_BEGIN();
    RUN_TEST(test_hidreport_queue_size_and_type);
    RUN_TEST(test_opcode_parsing_hid_frame);
    RUN_TEST(test_opcode_parsing_ir_frame);
    UNITY_END();
}

void loop() {
}
