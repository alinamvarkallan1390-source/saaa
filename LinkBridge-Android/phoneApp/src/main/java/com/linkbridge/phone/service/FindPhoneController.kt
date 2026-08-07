package com.linkbridge.phone.service
import android.content.Context
import android.media.*
import android.os.*
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class FindPhoneController @Inject constructor(@ApplicationContext private val c:Context){private var player:MediaPlayer?=null;private val vibrator=c.getSystemService(Vibrator::class.java);fun start(){stop();val audio=c.getSystemService(AudioManager::class.java);audio.setStreamVolume(AudioManager.STREAM_ALARM,audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),0);player=MediaPlayer.create(c,Settings.System.DEFAULT_ALARM_ALERT_URI)?.apply{isLooping=true;start()};vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0,500,300,500),0))}fun stop(){player?.stop();player?.release();player=null;vibrator.cancel()}}
