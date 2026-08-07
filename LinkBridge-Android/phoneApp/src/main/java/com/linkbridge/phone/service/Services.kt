package com.linkbridge.phone.service
import android.app.*
import android.content.*
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.linkbridge.core.bluetooth.BluetoothLinkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*

@AndroidEntryPoint class LinkService:Service(){@Inject lateinit var link:BluetoothLinkManager;@Inject lateinit var finder:FindPhoneController;private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default);override fun onCreate(){super.onCreate();val c=NotificationChannel("link","ارتباط ساعت",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager::class.java).createNotificationChannel(c);startForeground(100,NotificationCompat.Builder(this,"link").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle("LinkBridge فعال است").setContentText("پایش کم‌مصرف ارتباط").setOngoing(true).build());link.startSmartScan();scope.launch{link.incoming.collect{when(it.toString(Charsets.UTF_8)){"FIND_PHONE"->finder.start();"STOP_FIND_PHONE"->finder.stop()}}}}override fun onBind(i:Intent?):IBinder?=null;override fun onDestroy(){scope.cancel();finder.stop();link.disconnect();super.onDestroy()}}
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(i.action==Intent.ACTION_BOOT_COMPLETED||i.action==Intent.ACTION_MY_PACKAGE_REPLACED)c.startForegroundService(Intent(c,LinkService::class.java))}}
@AndroidEntryPoint class PhoneNotificationListener:NotificationListenerService(){@Inject lateinit var link:BluetoothLinkManager;override fun onNotificationPosted(s:StatusBarNotification){val title=s.notification.extras.getCharSequence("android.title")?.toString().orEmpty();val text=s.notification.extras.getCharSequence("android.text")?.toString().orEmpty();link.send("NOTIFICATION|${s.packageName}|$title|$text".take(4000).toByteArray())}}
