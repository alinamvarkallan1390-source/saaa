package com.linkbridge.watch

import android.app.*
import android.content.*
import android.media.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchCommandDispatcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audio = context.getSystemService<AudioManager>()!!
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        val vm = context.getSystemService(VibratorManager::class.java)
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var findJob: Job? = null

    fun execute(raw: ByteArray): String = runCatching {
        val command = raw.toString(Charsets.UTF_8).trim()
        when {
            command == "FIND_DEVICE" || command == "FIND_PHONE" || command == "FIND" -> {
                startFinding()
                "FINDING"
            }
            command == "STOP_FIND" || command == "STOP_FIND_PHONE" || command == "STOP" -> {
                stopFinding()
                "STOPPED"
            }
            command.startsWith("BRIGHTNESS|") -> {
                val n = command.substringAfter('|').toIntOrNull()?.coerceIn(1, 255) ?: 128
                try {
                    if (Settings.System.canWrite(context)) {
                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, n)
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    "OK"
                } catch (e: Exception) {
                    "ERROR:${e.message}"
                }
            }
            command.startsWith("VOLUME|") -> {
                val n = command.substringAfter('|').toIntOrNull()?.coerceIn(0, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) ?: 0
                try {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, n, 0)
                    "OK"
                } catch (e: Exception) {
                    "ERROR:${e.message}"
                }
            }
            command.startsWith("MEDIA|") -> {
                val key = when (command.substringAfter('|')) {
                    "PLAY_PAUSE" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    "NEXT" -> KeyEvent.KEYCODE_MEDIA_NEXT
                    else -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                }
                try {
                    audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
                    audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
                    "OK"
                } catch (e: Exception) {
                    "ERROR:${e.message}"
                }
            }
            command.startsWith("OPEN_APP|") -> {
                val pkg = command.substringAfter('|')
                try {
                    context.packageManager.getLaunchIntentForPackage(pkg)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ?.let(context::startActivity) ?: error("App not found")
                    "OK"
                } catch (e: Exception) {
                    "ERROR:${e.message}"
                }
            }
            command == "OPEN_SETTINGS" -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "OK"
            }
            else -> "UNSUPPORTED:$command"
        }
    }.getOrElse {
        "ERROR|${it.message}"
    }

    private fun startFinding() {
        stopFinding()
        try {
            // Max volume for alarm
            try {
                audio.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    0
                )
            } catch (_: Exception) {}

            // Vibration - works on Android 8.1
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= 26) {
                    // Waveform: wait 0, vibrate 600, pause 300, vibrate 600, pause 300, vibrate 1000, repeat
                    val pattern = longArrayOf(0, 600, 300, 600, 300, 1000)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                    try {
                        vib.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
                    } catch (e: Exception) {
                        // Fallback for devices that don't support amplitudes
                        try {
                            vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
                        } catch (_: Exception) {
                            @Suppress("DEPRECATION")
                            vib.vibrate(pattern, 0)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(longArrayOf(0, 600, 300, 600, 300, 1000), 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopFinding() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }
}

// End
