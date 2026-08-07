package com.linkbridge.watch
import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.linkbridge.core.bluetooth.GattPeripheralServer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@AndroidEntryPoint class WatchLinkService:Service(){
 @Inject lateinit var peripheral:GattPeripheralServer
 @Inject lateinit var wrist:WakeToRaiseDetector
 @Inject lateinit var commands:WatchCommandDispatcher
 @Inject lateinit var telemetry:TelemetryCollector
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
 override fun onCreate(){super.onCreate();getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("watch_link","ارتباط گوشی",NotificationManager.IMPORTANCE_LOW));startForeground(8,NotificationCompat.Builder(this,"watch_link").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle("LinkBridge Watch").setContentText("آماده اتصال امن").setOngoing(true).build());wrist.start();peripheral.start();scope.launch{peripheral.commands.collect{peripheral.notify(commands.execute(it).toByteArray())}};scope.launch{while(isActive){peripheral.notify(telemetry.collect().toByteArray());delay(30_000)}}}
 override fun onDestroy(){scope.cancel();wrist.stop();peripheral.stop();super.onDestroy()}
 override fun onBind(i:Intent?):IBinder?=null
}
class WatchBootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){c.startForegroundService(Intent(c,WatchLinkService::class.java))}}
