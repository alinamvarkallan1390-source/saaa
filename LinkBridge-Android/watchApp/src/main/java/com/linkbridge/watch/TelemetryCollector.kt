package com.linkbridge.watch
import android.app.ActivityManager
import android.content.*
import android.os.*
import android.os.StatFs
import android.provider.Settings
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class TelemetryCollector @Inject constructor(@ApplicationContext private val c:Context){
 fun collect():String{val battery=c.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED));val level=battery?.getIntExtra(BatteryManager.EXTRA_LEVEL,0)?:0;val charging=(battery?.getIntExtra(BatteryManager.EXTRA_STATUS,0)?:0) in listOf(BatteryManager.BATTERY_STATUS_CHARGING,BatteryManager.BATTERY_STATUS_FULL);val am=c.getSystemService(ActivityManager::class.java);val mem=ActivityManager.MemoryInfo().also(am::getMemoryInfo);val stat=StatFs(Environment.getDataDirectory().path);val temp=(battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)?:0)/10f;return listOf("TELEMETRY",Build.MODEL,Build.VERSION.RELEASE,Build.DISPLAY,level,charging,temp,mem.availMem/1048576,stat.availableBytes/1048576,System.currentTimeMillis()).joinToString("|")}
}
