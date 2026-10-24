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

void setup() {
    UNITY_BEGIN();
    RUN_TEST(test_hidreport_queue_size_and_type);
    UNITY_END();
}

void loop() {
}
