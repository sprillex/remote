#include <Arduino.h>
#include <unity.h>
#include "ble_nus.h"
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>

QueueHandle_t bleRxQueue;

void setUp(void) {
    bleRxQueue = xQueueCreate(10, sizeof(NusFrame));
}

void tearDown(void) {
    if (bleRxQueue != NULL) {
        vQueueDelete(bleRxQueue);
        bleRxQueue = NULL;
    }
}

void test_nusframe_hid_queue_and_parse(void) {
    NusFrame frame = {0};
    frame.opcode = OPCODE_HID;
    frame.payload.hid.modifiers = 0x01;
    frame.payload.hid.key1 = 0x02;

    BaseType_t res = xQueueSend(bleRxQueue, &frame, 0);
    TEST_ASSERT_EQUAL(pdPASS, res);

    NusFrame rxFrame = {0};
    res = xQueueReceive(bleRxQueue, &rxFrame, 0);
    TEST_ASSERT_EQUAL(pdPASS, res);

    TEST_ASSERT_EQUAL(OPCODE_HID, rxFrame.opcode);
    TEST_ASSERT_EQUAL(0x01, rxFrame.payload.hid.modifiers);
    TEST_ASSERT_EQUAL(0x02, rxFrame.payload.hid.key1);
}

void test_nusframe_ir_queue_and_parse(void) {
    NusFrame frame = {0};
    frame.opcode = OPCODE_IR_NEC;
    frame.payload.ir.data = 0x1234567887654321ULL;
    frame.payload.ir.nbits = 32;

    BaseType_t res = xQueueSend(bleRxQueue, &frame, 0);
    TEST_ASSERT_EQUAL(pdPASS, res);

    NusFrame rxFrame = {0};
    res = xQueueReceive(bleRxQueue, &rxFrame, 0);
    TEST_ASSERT_EQUAL(pdPASS, res);

    TEST_ASSERT_EQUAL(OPCODE_IR_NEC, rxFrame.opcode);
    TEST_ASSERT_EQUAL_UINT64(0x1234567887654321ULL, rxFrame.payload.ir.data);
    TEST_ASSERT_EQUAL(32, rxFrame.payload.ir.nbits);
}

void test_nusframe_unknown_and_raw_opcode_queueing(void) {
    NusFrame frame = {0};
    frame.opcode = 0xFF; // Unknown opcode
    frame.payload.unknown.raw_opcode = 0xFF;
    frame.payload.unknown.length = 12;

    BaseType_t res = xQueueSend(bleRxQueue, &frame, 0);
    TEST_ASSERT_EQUAL(pdPASS, res);

    NusFrame rxFrame = {0};
    res = xQueueReceive(bleRxQueue, &rxFrame, 0);
    TEST_ASSERT_EQUAL(pdPASS, res);

    TEST_ASSERT_EQUAL(0xFF, rxFrame.opcode);
    TEST_ASSERT_EQUAL(0xFF, rxFrame.payload.unknown.raw_opcode);
    TEST_ASSERT_EQUAL(12, rxFrame.payload.unknown.length);
}

void setup() {
    UNITY_BEGIN();
    RUN_TEST(test_nusframe_hid_queue_and_parse);
    RUN_TEST(test_nusframe_ir_queue_and_parse);
    RUN_TEST(test_nusframe_unknown_and_raw_opcode_queueing);
    UNITY_END();
}

void loop() {
}
