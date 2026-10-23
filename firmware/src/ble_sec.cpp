#include <NimBLEDevice.h>
#include <Arduino.h>

class SecurityCallbacks : public NimBLESecurityCallbacks {
    uint32_t onPassKeyRequest(){
        Serial.println("PassKeyRequest");
        return 123456;
    }

    void onPassKeyNotify(uint32_t pass_key){
        Serial.printf("The passkey is %" PRIu32 "\n", pass_key);
    }

    bool onConfirmPIN(uint32_t pass_key){
        Serial.printf("The passkey is %" PRIu32 "\n", pass_key);
        return true;
    }

    bool onSecurityRequest(){
        Serial.println("SecurityRequest");
        return true;
    }

    void onAuthenticationComplete(ble_gap_conn_desc* desc){
        if(!desc->sec_state.encrypted) {
            Serial.println("Encrypt connection failed - disconnecting");
            NimBLEDevice::getServer()->disconnect(desc->conn_handle);
            return;
        }
        Serial.println("Security completed");
    }
};

void setupBleSecurity() {
    NimBLEDevice::setSecurityAuth(true, true, true);
    NimBLEDevice::setSecurityPasskey(123456);
    NimBLEDevice::setSecurityIOCap(BLE_HS_IO_DISPLAY_ONLY);
    NimBLEDevice::setSecurityCallbacks(new SecurityCallbacks());
}
