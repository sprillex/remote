#include "ring_buffer.h"

RingBuffer hidRingBuffer(32); // Default capacity of 32 elements

RingBuffer::RingBuffer(size_t capacity) {
    queue = xQueueCreate(capacity, sizeof(HidReport));
}

RingBuffer::~RingBuffer() {
    if (queue != NULL) {
        vQueueDelete(queue);
    }
}

bool RingBuffer::push(const HidReport& report) {
    if (queue == NULL) return false;
    return xQueueSend(queue, &report, 0) == pdTRUE;
}

bool RingBuffer::pop(HidReport& report) {
    if (queue == NULL) return false;
    return xQueueReceive(queue, &report, 0) == pdTRUE;
}

bool RingBuffer::pushFromISR(const HidReport& report, BaseType_t *pxHigherPriorityTaskWoken) {
    if (queue == NULL) return false;
    return xQueueSendFromISR(queue, &report, pxHigherPriorityTaskWoken) == pdTRUE;
}

bool RingBuffer::popFromISR(HidReport& report, BaseType_t *pxHigherPriorityTaskWoken) {
    if (queue == NULL) return false;
    return xQueueReceiveFromISR(queue, &report, pxHigherPriorityTaskWoken) == pdTRUE;
}

bool RingBuffer::isEmpty() const {
    if (queue == NULL) return true;
    return uxQueueMessagesWaiting(queue) == 0;
}

bool RingBuffer::isFull() const {
    if (queue == NULL) return false;
    return uxQueueSpacesAvailable(queue) == 0;
}
