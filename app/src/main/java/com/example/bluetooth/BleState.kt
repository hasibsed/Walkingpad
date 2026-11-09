package com.example.bluetooth

enum class ConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class BeltState {
    STOPPED,
    STARTING,
    RUNNING,
    PAUSED
}

data class DiscoveredBleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isWalkingPad: Boolean
)

data class LiveTreadmillData(
    val instantaneousSpeedKmh: Float = 0.0f,
    val averageSpeedKmh: Float = 0.0f,
    val totalDistanceMeters: Float = 0.0f,
    val inclinationPercent: Float = 0.0f,
    val elapsedTimeSeconds: Int = 0,
    val totalSteps: Int = 0,
    val stepCadenceSpm: Int = 0,
    val instantaneousKcal: Float = 0.0f,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
