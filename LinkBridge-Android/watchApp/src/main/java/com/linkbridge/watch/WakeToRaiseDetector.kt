package com.linkbridge.watch

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Wake to Rise - fixed for Android 8.1 watches without gravity sensor
 * Uses Accelerometer + Gyroscope (fallback to accelerometer only)
 */
@Singleton
class WakeToRaiseDetector @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)

    private var accel = FloatArray(3) { 0f }
    private var gravity = FloatArray(3) { 0f }
    private var gyro = FloatArray(3) { 0f }

    private var hasGravitySensor = false
    private var hasGyro = false

    private var lastTrigger = 0L
    private var lastAccelMagnitude = 0f
    private var isScreenFaceDown = false

    fun start() {
        // Check available sensors
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        hasGravitySensor = gravitySensor != null
        hasGyro = gyroSensor != null

        if (hasGravitySensor) {
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        gyroSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Also try linear acceleration as extra
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                gravity = event.values.clone()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // Low-pass filter to separate gravity
                val alpha = 0.8f
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                accel = event.values.clone()
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyro = event.values.clone()
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                // Use linear accel to detect movement
                val mag = sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2])
                lastAccelMagnitude = mag
            }
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastTrigger < 4000) return // Debounce 4 seconds

        // Check if screen is face down initially, then raised
        // Gravity Z: device flat on table face up ~ +9.8, face down ~ -9.8
        // For watch on wrist, when arm down, gravity Y is high, when raised, Z becomes negative or X changes

        // Simple detection for Android 8.1 watches:
        // 1. Was relatively still
        // 2. Then acceleration spike (raising)
        // 3. Then gravity Z < -3 (screen facing user) or Y < -5
        val accelMag = sqrt(accel[0] * accel[0] + accel[1] * accel[1] + accel[2] * accel[2])
        val gyroMag = sqrt(gyro[0] * gyro[0] + gyro[1] * gyro[1] + gyro[2] * gyro[2])

        // Detect wrist raise: combination of gravity change and gyro
        val isFaceUp = gravity[2] < -4.0f || gravity[1] < -6.0f // Watch screen facing up when raised
        val isMoving = if (hasGyro) gyroMag in 0.8..6.0 else lastAccelMagnitude > 1.5f || accelMag > 12f

        // Alternative: accelerometer Y axis goes from ~0 to negative when raising
        val wristRaised = (accel[1] < -3f && accel[2] > -2f) || isFaceUp

        if (wristRaised && (isMoving || lastAccelMagnitude > 1.0f)) {
            // Additional check: avoid false positives when walking
            if (accelMag in 9.0..18.0) {
                triggerWake()
                lastTrigger = now
            }
        }
    }

    private fun triggerWake() {
        try {
            // Try to wake screen - works on Android 8.1
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "LinkBridge:WristWake"
            )
            wakeLock.acquire(3000)
            // Release after 3 sec is automatic via timeout, but also release explicitly after delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    if (wakeLock.isHeld) wakeLock.release()
                } catch (_: Exception) {}
            }, 3000)
        } catch (e: Exception) {
            // Fallback: try FULL_WAKE_LOCK for Android 8.1
            try {
                @Suppress("DEPRECATION")
                val fullWakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "LinkBridge:FullWake"
                )
                fullWakeLock.acquire(3000)
            } catch (_: Exception) {}
        }
    }
}
