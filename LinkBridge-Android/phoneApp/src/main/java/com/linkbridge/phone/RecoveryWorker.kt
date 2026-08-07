package com.linkbridge.phone
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.linkbridge.phone.service.LinkService
import java.util.concurrent.TimeUnit

class RecoveryWorker(c:Context, p:WorkerParameters):CoroutineWorker(c,p){
 override suspend fun doWork():Result=runCatching{ applicationContext.startForegroundService(Intent(applicationContext,LinkService::class.java));Result.success() }.getOrElse{Result.retry()}
 companion object { fun schedule(c:Context)=WorkManager.getInstance(c).enqueueUniquePeriodicWork("link-health",ExistingPeriodicWorkPolicy.UPDATE,PeriodicWorkRequestBuilder<RecoveryWorker>(15,TimeUnit.MINUTES).setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()).build()) }
}
