package com.linkbridge.watch

import android.app.ActivityManager
import android.content.*
import android.os.*
import android.os.StatFs
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class TelemetryCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Real battery level - used in notification too
    fun getBatteryLevel(): Int {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
    }

    fun collect(): String {
        try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val batteryPct = (level * 100 / scale.toFloat()).roundToInt().coerceIn(0, 100)

            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempC = tempRaw / 10f

            val am = context.getSystemService(ActivityManager::class.java)
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val availMemMb = memInfo.availMem / (1024 * 1024)
            val totalMemMb = memInfo.totalMem / (1024 * 1024)
            val usedMemMb = totalMemMb - availMemMb

            val stat = StatFs(Environment.getDataDirectory().path)
            val freeBytes = stat.availableBytes
            val freeMb = freeBytes / (1024 * 1024)

            // Real device info
            val model = Build.MODEL ?: "Unknown"
            val release = Build.VERSION.RELEASE ?: "Unknown"
            val display = Build.DISPLAY ?: "Unknown"

            // Format: TELEMETRY|MODEL|RELEASE|DISPLAY|battery|charging|temp|ramUsed|storageFree|timestamp|totalRam
            // All real
            return listOf(
                "TELEMETRY",
                model,
                release,
                display,
                batteryPct,
                charging,
                tempC,
                usedMemMb,
                freeMb,
                System.currentTimeMillis(),
                totalMemMb
            ).joinToString("|")
        } catch (e: Exception) {
            // Fallback with real battery at least
            val lvl = getBatteryLevel()
            return "TELEMETRY|${Build.MODEL}|${Build.VERSION.RELEASE}|${Build.DISPLAY}|$lvl|false|0|0|0|${System.currentTimeMillis()}|0"
        }
    }
}
