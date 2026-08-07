package com.linkbridge.core.model

data class DeviceTelemetry(val name:String="Unknown",val model:String="Unknown",val androidVersion:String="",val firmware:String="",val battery:Int=0,val charging:Boolean=false,val temperatureC:Float?=null,val ramUsedMb:Long=0,val cpuPercent:Float=0f,val storageFreeMb:Long=0,val rssi:Int=-127,val latencyMs:Long=0,val syncedAt:Long=0)
enum class LinkState { IDLE, SCANNING, CONNECTING, AUTHENTICATING, CONNECTED, RETRYING, ERROR }
data class LinkSnapshot(val state:LinkState=LinkState.IDLE,val device:DeviceTelemetry=DeviceTelemetry(),val message:String="",val retryInMs:Long=0) { val quality:String get()=when(device.rssi){in -59..0->"عالی";in -69..-60->"خوب";in -79..-70->"متوسط";else->"ضعیف"}; val distanceMeters:Double get()=kotlin.math.pow(10.0,(-59-device.rssi)/20.0) }
data class TransferProgress(val id:String,val fileName:String,val total:Long,val sent:Long,val bytesPerSecond:Long,val paused:Boolean=false,val done:Boolean=false,val error:String?=null){val percent:Int get()=if(total==0L)0 else ((sent*100)/total).toInt();val etaSeconds:Long get()=if(bytesPerSecond<=0)0 else (total-sent)/bytesPerSecond}
