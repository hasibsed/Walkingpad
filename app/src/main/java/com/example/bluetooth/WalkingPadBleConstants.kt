package com.example.bluetooth

import java.util.UUID

object WalkingPadBleConstants {
    // Bluetooth SIG Fitness Machine Service (FTMS)
    val SERVICE_FTMS: UUID = UUID.fromString("00001826-0000-1000-8000-00805f9b34fb")
    val CHAR_TREADMILL_DATA: UUID = UUID.fromString("00002acd-0000-1000-8000-00805f9b34fb")
    val CHAR_FITNESS_MACHINE_CONTROL_POINT: UUID = UUID.fromString("00002ad9-0000-1000-8000-00805f9b34fb")
    val CHAR_FITNESS_MACHINE_STATUS: UUID = UUID.fromString("00002ada-0000-1000-8000-00805f9b34fb")
    val CHAR_FITNESS_MACHINE_FEATURE: UUID = UUID.fromString("00002acc-0000-1000-8000-00805f9b34fb")

    // Standard Client Characteristic Configuration Descriptor
    val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Fallback KingSmith proprietary service (supported on some older models/firmwares)
    val SERVICE_KINGSMITH_PROPRIETARY: UUID = UUID.fromString("0000fe00-0000-1000-8000-00805f9b34fb")
    val CHAR_KINGSMITH_WRITE: UUID = UUID.fromString("0000fe01-0000-1000-8000-00805f9b34fb")
    val CHAR_KINGSMITH_NOTIFY: UUID = UUID.fromString("0000fe02-0000-1000-8000-00805f9b34fb")

    // FTMS Control Point Opcodes
    const val OPCODE_REQUEST_CONTROL: Byte = 0x00
    const val OPCODE_RESET: Byte = 0x01
    const val OPCODE_SET_TARGET_SPEED: Byte = 0x02
    const val OPCODE_START_OR_RESUME: Byte = 0x07
    const val OPCODE_STOP_OR_PAUSE: Byte = 0x08

    // Stop/Pause parameters for OPCODE_STOP_OR_PAUSE
    const val PARAM_STOP: Byte = 0x01
    const val PARAM_PAUSE: Byte = 0x02

    // Speed limits in km/h for WalkingPad MC21
    const val MIN_SPEED_KMH: Float = 0.5f
    const val MAX_SPEED_KMH: Float = 10.0f
    const val SPEED_STEP_FINE: Float = 0.1f
    const val SPEED_STEP_COARSE: Float = 0.5f

    // Device filtering patterns
    val DEVICE_NAME_PREFIXES = listOf("WalkingPad", "MC21", "KS-", "KingSmith", "WP-")
}
