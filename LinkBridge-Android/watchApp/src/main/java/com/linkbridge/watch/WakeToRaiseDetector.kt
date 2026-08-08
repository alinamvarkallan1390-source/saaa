package com.linkbridge.watch

import android.content.Context
<<<<<<< HEAD
=======
import android.content.Intent
>>>>>>> 98dfe04 (fix: wake-to-raise for TC4G with gravity sensor + find fix + real telemetry)
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
<<<<<<< HEAD
import android.os.PowerManager
import android.os.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Wake to Rise - fixed for Android 8.1 watches without gravity sensor
 * Uses Accelerometer + Gyroscope (fallback to accelerometer only)
=======
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Wake To Raise - Production fix for Telzeal TC4G with Gravity sensor
 * Works WITHOUT phone - standalone
 * Supports Android 8.1 (API 27)
>>>>>>> 98dfe04 (fix: wake-to-raise for TC4G with gravity sensor + find fix + real telemetry)
 */
@Singleton
class WakeToRaiseDetector @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

<<<<<<< HEAD
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
=======
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private var gravity = FloatArray(3) { 0f }
    private var accel = FloatArray(3) { 0f }
    private var lastGravityZ = 0f
    private var lastAccelY = 0f
    private var lastMovementTime = 0L
    private var lastTrigger = 0L

    // History for debounce
    private var wasFaceDown = false
    private var faceDownTime = 0L

    // Sensor references
    private var hasGravity = false
    private var hasGyro = false
    private var hasAccel = false

    fun start() {
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val linearSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        hasGravity = gravitySensor != null
        hasAccel = accelSensor != null
        hasGyro = gyroSensor != null

        Log.d("WakeToRaise", "Sensors - Gravity: $hasGravity, Accel: $hasAccel, Gyro: $hasGyro")

        // Use GAME delay for faster response (20ms) instead of NORMAL (200ms)
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        linearSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        // Fallback: if no gravity, accelerometer will provide filtered gravity
        if (!hasGravity && hasAccel) {
            Log.d("WakeToRaise", "No gravity sensor, using accelerometer filter")
>>>>>>> 98dfe04 (fix: wake-to-raise for TC4G with gravity sensor + find fix + real telemetry)
        }
    }

    fun stop() {
<<<<<<< HEAD
        sensorManager.unregisterListener(this)
=======
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {}
>>>>>>> 98dfe04 (fix: wake-to-raise for TC4G with gravity sensor + find fix + real telemetry)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                gravity = event.values.clone()
<<<<<<< HEAD
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
=======
                handleGravityChange(event.values)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accel = event.values.clone()
                // If no gravity sensor, low-pass filter accelerometer to get gravity
                if (!hasGravity) {
                    val alpha = 0.85f
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
                    // Also handle as gravity change
                    handleGravityChange(gravity)
                }
                handleAccelChange(event.values)
            }
            Sensor.TYPE_GYROSCOPE -> {
                val mag = sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2])
                if (mag > 0.8f) {
                    lastMovementTime = SystemClock.elapsedRealtime()
                }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val mag = sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2])
                if (mag > 1.2f) {
                    lastMovementTime = SystemClock.elapsedRealtime()
                }
            }
        }
    }

    private fun handleGravityChange(values: FloatArray) {
        val now = SystemClock.elapsedRealtime()
        val z = values[2]
        val y = values[1]
        val x = values[0]

        // For TC4G watch on wrist:
        // Arm down (watch face to side): gravity Y ~ -8, Z ~ 0-2
        // Arm raised to see time: gravity Z ~ -7 to -9.8, or X high
        // This matches real watch behavior

        val isFaceDown = z > 4.0f || (y > 7.0f) // Screen facing down / arm down
        val isFaceUp = z < -3.5f || (y < -5.0f && abs(x) < 6) // Screen facing user - raised

        if (isFaceDown) {
            wasFaceDown = true
            faceDownTime = now
        }

        // Detect transition: was face down, now face up within 1.5 sec, with recent movement
        if (wasFaceDown && isFaceUp) {
            val timeSinceFaceDown = now - faceDownTime
            val timeSinceMovement = now - lastMovementTime

            // Must have been face down recently (0.2-2000ms ago) and recent movement
            if (timeSinceFaceDown in 200..2000 && timeSinceMovement < 1000) {
                if (now - lastTrigger > 2500) { // Debounce 2.5 sec (not 4)
                    Log.d("WakeToRaise", "WRIST RAISE DETECTED! wasFaceDown $timeSinceFaceDown ms ago, movement $timeSinceMovement ms ago, gravity Z $z Y $y")
                    triggerWake()
                    lastTrigger = now
                    wasFaceDown = false
                }
            } else if (timeSinceFaceDown > 2000) {
                // Too long since face down, reset
                wasFaceDown = false
            }
        }

        // Also direct raise without face-down history: strong Z negative + movement
        if (isFaceUp && (now - lastMovementTime) < 800) {
            if (now - lastTrigger > 2500) {
                // Check if Z changed quickly
                val zDiff = abs(z - lastGravityZ)
                if (zDiff > 4.0f) {
                    Log.d("WakeToRaise", "Direct raise detected! Z diff $zDiff, current Z $z")
                    triggerWake()
                    lastTrigger = now
                }
            }
        }

        lastGravityZ = z
    }

    private fun handleAccelChange(values: FloatArray) {
        val y = values[1]
        val now = SystemClock.elapsedRealtime()

        // For accelerometer fallback (when no gravity sensor)
        // Detect Y going from positive/high to negative
        if (lastAccelY > 2.0f && y < -3.0f) {
            val timeSinceMovement = now - lastMovementTime
            if (timeSinceMovement < 1000 && now - lastTrigger > 2500) {
                Log.d("WakeToRaise", "Accel Y flip detected: $lastAccelY -> $y")
>>>>>>> 98dfe04 (fix: wake-to-raise for TC4G with gravity sensor + find fix + real telemetry)
                triggerWake()
                lastTrigger = now
            }
        }
<<<<<<< HEAD
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
=======
        lastAccelY = y
    }

    private fun triggerWake() {
        Log.d("WakeToRaise", "TRIGGERING WAKE!")
        try {
            // Method 1: WakeLock with bright + cause wakeup
            try {
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE,
                    "LinkBridge:WristWake"
                )
                wakeLock.acquire(4000)
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (wakeLock.isHeld) wakeLock.release()
                    } catch (_: Exception) {}
                }, 4000)
                Log.d("WakeToRaise", "WakeLock acquired")
            } catch (e: Exception) {
                Log.e("WakeToRaise", "WakeLock failed, trying FULL", e)
                try {
                    @Suppress("DEPRECATION")
                    val fullWakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK or
                                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                                PowerManager.ON_AFTER_RELEASE,
                        "LinkBridge:FullWake"
                    )
                    fullWakeLock.acquire(4000)
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            if (fullWakeLock.isHeld) fullWakeLock.release()
                        } catch (_: Exception) {}
                    }, 4000)
                } catch (e2: Exception) {
                    Log.e("WakeToRaise", "Full WakeLock also failed", e2)
                }
            }

            // Method 2: Start WatchActivity with turn screen on flags (works on Android 8.1)
            try {
                val intent = Intent(context, WatchActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("WAKE_TO_RAISE", true)
                }
                context.startActivity(intent)
                Log.d("WakeToRaise", "Started WatchActivity to wake")
            } catch (e: Exception) {
                Log.e("WakeToRaise", "Failed to start activity", e)
            }

            // Method 3: For Android 8.1+ turnScreenOn attribute should help if activity already in foreground

        } catch (e: Exception) {
            Log.e("WakeToRaise", "triggerWake failed", e)
>>>>>>> 98dfe04 (fix: wake-to-raise for TC4G with gravity sensor + find fix + real telemetry)
        }
    }
}
