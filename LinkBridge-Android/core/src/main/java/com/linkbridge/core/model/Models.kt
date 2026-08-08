package com.linkbridge.core.model

data class DeviceTelemetry(
    val name: String = "Unknown",
    val model: String = "Unknown",
    val androidVersion: String = "",
    val firmware: String = "",
    val battery: Int = 0,
    val charging: Boolean = false,
    val temperatureC: Float? = null,
    val ramUsedMb: Long = 0,
    val cpuPercent: Float = 0f,
    val storageFreeMb: Long = 0,
    val rssi: Int = -127,
    val latencyMs: Long = 0,
    val syncedAt: Long = 0,
    val address: String = ""
)

enum class LinkState { IDLE, SCANNING, CONNECTING, AUTHENTICATING, CONNECTED, RETRYING, ERROR }

data class ScannedDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isOurService: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

data class LinkSnapshot(
    val state: LinkState = LinkState.IDLE,
    val device: DeviceTelemetry = DeviceTelemetry(),
    val message: String = "",
    val retryInMs: Long = 0,
    val scanned: List<ScannedDevice> = emptyList()
) {
    val quality: String get() = when (device.rssi) {
        in -59..0 -> "عالی"
        in -69..-60 -> "خوب"
        in -79..-70 -> "متوسط"
        else -> "ضعیف"
    }
    // More realistic distance using log-distance path loss model
    val distanceMeters: Double get() {
        if (device.rssi == 0 || device.rssi == -127) return -1.0
        // txPower = -59 is typical for BLE at 1m
        val ratio = device.rssi * 1.0 / -59
        return if (ratio < 1.0) Math.pow(ratio, 10.0) else (0.89976 * Math.pow(ratio, 7.7095) + 0.111)
    }
}

data class TransferProgress(
    val id: String,
    val fileName: String,
    val total: Long,
    val sent: Long,
    val bytesPerSecond: Long,
    val paused: Boolean = false,
    val done: Boolean = false,
    val error: String? = null
) {
    val percent: Int get() = if (total == 0L) 0 else ((sent * 100) / total).toInt()
    val etaSeconds: Long get() = if (bytesPerSecond <= 0) 0 else (total - sent) / bytesPerSecond
}
