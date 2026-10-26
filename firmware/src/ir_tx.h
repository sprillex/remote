#pragma once
#include <stdint.h>

void setupIrTx();
void sendIrCommand(uint8_t opcode, uint64_t data, uint16_t nbits);
