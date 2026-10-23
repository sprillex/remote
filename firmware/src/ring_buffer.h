#ifndef RING_BUFFER_H
#define RING_BUFFER_H

#include <stdint.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>

struct HidReport {
    uint8_t modifiers;
    uint8_t key1;
    uint8_t key2;
    uint8_t key3;
    uint8_t key4;
    uint8_t key5;
    uint8_t key6;
};

class RingBuffer {
private:
    QueueHandle_t queue;

public:
    RingBuffer(size_t capacity);
    ~RingBuffer();

    bool push(const HidReport& report);
    bool pop(HidReport& report);
    bool pushFromISR(const HidReport& report, BaseType_t *pxHigherPriorityTaskWoken);
    bool popFromISR(HidReport& report, BaseType_t *pxHigherPriorityTaskWoken);
    bool isEmpty() const;
    bool isFull() const;
};

extern RingBuffer hidRingBuffer;

#endif // RING_BUFFER_H
