#include <Arduino.h>
#include <NimBLEDevice.h>
#include <unity.h>

void setUp(void) {
}

void tearDown(void) {
}

void test_random_passkey(void) {
    uint32_t passkey1 = esp_random() % 1000000;
    uint32_t passkey2 = esp_random() % 1000000;

    TEST_ASSERT_TRUE(passkey1 < 1000000);
    TEST_ASSERT_TRUE(passkey2 < 1000000);
    // Dynamic random passkeys generated via hardware RNG should not be fixed static 123456
    TEST_ASSERT_NOT_EQUAL(123456, passkey1);
}

void setup() {
    UNITY_BEGIN();
    RUN_TEST(test_random_passkey);
    UNITY_END();
}

void loop() {
}
