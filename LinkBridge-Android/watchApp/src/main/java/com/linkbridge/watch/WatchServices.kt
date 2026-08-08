package com.linkbridge.watch

import android.app.*
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.linkbridge.core.bluetooth.GattPeripheralServer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@AndroidEntryPoint
class WatchLinkService : Service() {

    @Inject lateinit var peripheral: GattPeripheralServer
    @Inject lateinit var wrist: WakeToRaiseDetector
    @Inject lateinit var commands: WatchCommandDispatcher
    @Inject lateinit var telemetry: TelemetryCollector

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        var instance: WatchLinkService? = null
        private const val CHANNEL_ID = "watch_link"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ارتباط گوشی",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ارتباط امن با گوشی"
            }
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("LinkBridge Watch")
            .setContentText("آماده اتصال امن - ${getBluetoothName()}")
            .setOngoing(true)
            .build()

        startForeground(8, notification)

        // Start wrist detection - fixed for Android 8.1
        wrist.start()

        // Start BLE peripheral with real name
        peripheral.start()

        // Handle commands from phone (FIND_DEVICE etc)
        scope.launch {
            peripheral.commands.collect { raw ->
                val response = commands.execute(raw)
                // Send response back
                peripheral.notify(response.toByteArray())
                // If FIND command, also send telemetry immediately
                val cmdStr = raw.toString(Charsets.UTF_8)
                if (cmdStr.contains("FIND")) {
                    delay(500)
                    peripheral.notify(telemetry.collect().toByteArray())
                }
            }
        }

        // Send telemetry every 5 seconds (real data) - not 30 sec, so phone gets real values fast
        scope.launch {
            while (isActive) {
                delay(2000) // Initial delay 2 sec after service start
                val data = telemetry.collect()
                val sent = peripheral.notify(data.toByteArray())
                // If not connected, still try - will fail silently
                // Log for debugging (not in release)
                delay(5000) // Every 5 seconds for real-time updates
            }
        }

        // Update notification with real battery periodically
        scope.launch {
            while (isActive) {
                delay(10000)
                val batt = telemetry.getBatteryLevel()
                val notif = NotificationCompat.Builder(this@WatchLinkService, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                    .setContentTitle("LinkBridge Watch - ${batt}%")
                    .setContentText("متصل: ${peripheral.isConnected} - ${getBluetoothName()}")
                    .setOngoing(true)
                    .build()
                nm.notify(8, notif)
            }
        }
    }

    private fun getBluetoothName(): String {
        return try {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bm.adapter?.name ?: "Watch"
        } catch (_: Exception) {
            "Watch"
        }
    }

    override fun onDestroy() {
        instance = null
        scope.cancel()
        wrist.stop()
        peripheral.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Called from WatchActivity to send FIND_PHONE
    fun sendFindPhone() {
        scope.launch {
            peripheral.notify("FIND_PHONE".toByteArray())
        }
    }

    fun sendStopFind() {
        scope.launch {
            peripheral.notify("STOP_FIND_PHONE".toByteArray())
        }
    }
}

class WatchBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(Intent(context, WatchLinkService::class.java))
                } else {
                    context.startService(Intent(context, WatchLinkService::class.java))
                }
            } catch (_: Exception) {}
        }
    }
}
