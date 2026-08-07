package com.linkbridge.watch
import android.app.*
import android.content.*
import android.media.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.WindowManager
import android.view.KeyEvent
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class WatchCommandDispatcher @Inject constructor(@ApplicationContext private val c:Context){
 private val audio=c.getSystemService<AudioManager>()!!
 private val vibrator=c.getSystemService<Vibrator>()!!
 fun execute(raw:ByteArray):String=runCatching{val command=raw.toString(Charsets.UTF_8);when{
  command=="FIND_DEVICE"->{vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0,500,250,500,250,1000),0));audio.adjustStreamVolume(AudioManager.STREAM_ALARM,AudioManager.ADJUST_RAISE,0);"FINDING"}
  command=="STOP_FIND"->{vibrator.cancel();"STOPPED"}
  command.startsWith("BRIGHTNESS|")->{val n=command.substringAfter('|').toInt().coerceIn(1,255);if(Settings.System.canWrite(c))Settings.System.putInt(c.contentResolver,Settings.System.SCREEN_BRIGHTNESS,n) else c.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:${c.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));"OK"}
  command.startsWith("VOLUME|")->{val n=command.substringAfter('|').toInt().coerceIn(0,audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));audio.setStreamVolume(AudioManager.STREAM_MUSIC,n,0);"OK"}
  command.startsWith("MEDIA|")->{val key=when(command.substringAfter('|')){"PLAY_PAUSE"->KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;"NEXT"->KeyEvent.KEYCODE_MEDIA_NEXT;else->KeyEvent.KEYCODE_MEDIA_PREVIOUS};audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,key));audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP,key));"OK"}
  command.startsWith("OPEN_APP|")->{val pkg=command.substringAfter('|');c.packageManager.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let(c::startActivity)?:error("App not found");"OK"}
  command=="OPEN_SETTINGS"->{c.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));"OK"}
  else->"UNSUPPORTED"
 }}.getOrElse{"ERROR|${it.message}"}
}
