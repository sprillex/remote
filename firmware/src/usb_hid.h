#pragma once

#include <stdint.h>

void setupUsbHid();
void sendKeyReport(uint8_t modifiers, uint8_t key1, uint8_t key2, uint8_t key3, uint8_t key4, uint8_t key5, uint8_t key6);
