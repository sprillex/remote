#include <NimBLEDevice.h>
#include <Arduino.h>

class SecurityCallbacks : public NimBLESecurityCallbacks {
    uint32_t onPassKeyRequest(){
        Serial.println("PassKeyRequest");
        // Secure dynamic passkey generation using hardware RNG (esp_random)
        uint32_t passkey = esp_random() % 1000000;
        Serial.printf("Generated passkey: %06" PRIu32 "\n", passkey);
        return passkey;
    }

    void onPassKeyNotify(uint32_t pass_key){
        Serial.printf("The passkey is %06" PRIu32 "\n", pass_key);
    }

    bool onConfirmPIN(uint32_t pass_key){
        Serial.printf("The passkey is %06" PRIu32 "\n", pass_key);
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
    NimBLEDevice::setSecurityIOCap(BLE_HS_IO_DISPLAY_ONLY);
    NimBLEDevice::setSecurityCallbacks(new SecurityCallbacks());
}
