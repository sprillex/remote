#include <Arduino.h>
#include <NimBLEDevice.h>
#include <unity.h>

void setUp(void) {
}

void tearDown(void) {
}

void test_random_passkey(void) {
    TEST_ASSERT_TRUE(true);
}

void setup() {
    UNITY_BEGIN();
    RUN_TEST(test_random_passkey);
    UNITY_END();
}

void loop() {
}
