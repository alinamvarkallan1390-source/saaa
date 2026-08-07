package com.linkbridge.watch
import android.content.Context
import android.hardware.*
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*
@Singleton class WakeToRaiseDetector @Inject constructor(@ApplicationContext private val c:Context):SensorEventListener{private val sm=c.getSystemService(SensorManager::class.java);private var gravity=FloatArray(3);private var gyro=FloatArray(3);private var last=0L;fun start(){sm.getDefaultSensor(Sensor.TYPE_GRAVITY)?.let{sm.registerListener(this,it,SensorManager.SENSOR_DELAY_NORMAL)};sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let{sm.registerListener(this,it,SensorManager.SENSOR_DELAY_NORMAL)}}fun stop()=sm.unregisterListener(this);override fun onAccuracyChanged(s:Sensor?,a:Int){};override fun onSensorChanged(e:SensorEvent){when(e.sensor.type){Sensor.TYPE_GRAVITY->gravity=e.values.clone();Sensor.TYPE_GYROSCOPE->gyro=e.values.clone()};val now=System.currentTimeMillis();val faceUp=gravity[2] < -5.5f;val rotation=sqrt(gyro.sumOf{(it*it).toDouble()}) in 0.6..3.8;if(faceUp&&rotation&&now-last>5000){last=now;c.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,"linkbridge:wrist").apply{acquire(2500)}}}}
