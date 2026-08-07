package com.linkbridge.phone
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
@HiltAndroidApp class PhoneApplication:Application(){override fun onCreate(){super.onCreate();RecoveryWorker.schedule(this)}}
