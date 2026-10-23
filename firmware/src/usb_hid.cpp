#include "usb_hid.h"
#include <Arduino.h>
#include "USB.h"
#include "USBHID.h"
#include "class/hid/hid_device.h"

static const uint8_t hid_report_descriptor[] = {
    TUD_HID_REPORT_DESC_KEYBOARD(HID_REPORT_ID(1))
};

USBHID hid;

class CustomUSBKeyboard : public USBHIDDevice {
public:
    CustomUSBKeyboard() {}

    void begin() {
        hid.addDevice(this, sizeof(hid_report_descriptor));
        hid.begin();
    }

    uint16_t _onGetDescriptor(uint8_t *buffer) {
        memcpy(buffer, hid_report_descriptor, sizeof(hid_report_descriptor));
        return sizeof(hid_report_descriptor);
    }

    void sendKeyReport(uint8_t modifiers, uint8_t key1, uint8_t key2, uint8_t key3, uint8_t key4, uint8_t key5, uint8_t key6) {
        uint8_t report[8] = { modifiers, 0, key1, key2, key3, key4, key5, key6 };
        hid.SendReport(1, report, sizeof(report));
    }
};

CustomUSBKeyboard CustomKeyboard;

void setupUsbHid() {
    CustomKeyboard.begin();
    USB.begin();
}

void sendKeyReport(uint8_t modifiers, uint8_t key1, uint8_t key2, uint8_t key3, uint8_t key4, uint8_t key5, uint8_t key6) {
    CustomKeyboard.sendKeyReport(modifiers, key1, key2, key3, key4, key5, key6);
}
