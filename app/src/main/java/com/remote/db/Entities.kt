package com.remote.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class ConnectionType {
    DIRECT_BT_HID,
    ESP32_BLE_BRIDGE,
    ESP32_IR_EMITTER,
    ESP32_HYBRID,
    ROKU_ECP_WIFI,
    INTERNAL_IR,
    USB_IR_DONGLE,
    AUDIO_JACK_IR,
    FLIPPER_ZERO_BRIDGE,
    SSH_NETWORK
}

enum class ProfileType {
    PC_KEYBOARD,
    TV_REMOTE,
    MEDIA_CONTROLLER,
    PRESENTATION_CLICKER,
    TERMINAL_MACRO,
    SSH_SESSION,
    ANDROID_DEVICE
}

class Converters {
    @TypeConverter
    fun fromConnectionType(value: ConnectionType?): String? = value?.name

    @TypeConverter
    fun toConnectionType(value: String?): ConnectionType? =
        value?.let { runCatching { enumValueOf<ConnectionType>(it) }.getOrNull() }

    @TypeConverter
    fun fromProfileType(value: ProfileType?): String? = value?.name

    @TypeConverter
    fun toProfileType(value: String?): ProfileType? =
        value?.let { runCatching { enumValueOf<ProfileType>(it) }.getOrNull() }
}

@Entity(
    tableName = "target_devices",
    indices = [Index(value = ["mac_address"], unique = true)]
)
data class TargetDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "connection_type") val connectionType: ConnectionType,
    @ColumnInfo(name = "profile_type") val profileType: ProfileType,
    @ColumnInfo(name = "mac_address") val macAddress: String? = null,
    @ColumnInfo(name = "host_address") val hostAddress: String? = null,
    @ColumnInfo(name = "port") val port: Int = 22,
    @ColumnInfo(name = "auth_username") val authUsername: String? = null,
    @ColumnInfo(name = "auth_type") val authType: String? = "KEY",
    @ColumnInfo(name = "carrier_frequency") val carrierFrequency: Int = 38000,
    @ColumnInfo(name = "inter_key_delay_ms") val interKeyDelayMs: Int = 10,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "button_mappings",
    foreignKeys = [
        ForeignKey(
            entity = TargetDeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["device_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["device_id", "button_identifier"], unique = true)]
)
data class ButtonMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "device_id") val deviceId: Long,
    @ColumnInfo(name = "button_identifier") val buttonIdentifier: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "hid_page") val hidPage: Int = 0x07,
    @ColumnInfo(name = "usage_id") val usageId: Int = 0,
    @ColumnInfo(name = "modifier_flags") val modifierFlags: Byte = 0,
    @ColumnInfo(name = "ir_protocol") val irProtocol: String? = null,
    @ColumnInfo(name = "ir_hex_code") val irHexCode: String? = null,
    @ColumnInfo(name = "ir_raw_timings") val irRawTimings: String? = null,
    @ColumnInfo(name = "command_payload") val commandPayload: String? = null
)

@Entity(
    tableName = "ssh_macros"
)
data class SshMacroEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "command") val command: String,
    @ColumnInfo(name = "category") val category: String = "general",
    @ColumnInfo(name = "tags") val tags: String = "diagnostics",
    @ColumnInfo(name = "target_profile_id") val targetProfileId: Long? = null,
    @ColumnInfo(name = "require_confirmation") val requireConfirmation: Boolean = false,
    @ColumnInfo(name = "working_directory") val workingDirectory: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ssh_credentials"
)
data class SshCredentialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "key_type") val keyType: String = "ED25519",
    @ColumnInfo(name = "encrypted_key_data") val encryptedKeyData: String,
    @ColumnInfo(name = "public_key_data") val publicKeyData: String? = null,
    @ColumnInfo(name = "passphrase_protected") val passphraseProtected: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
