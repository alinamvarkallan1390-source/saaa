package com.linkbridge.core.bluetooth
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.linkbridge.core.model.TransferProgress
import com.linkbridge.core.security.CryptoEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission") @Singleton class ClassicTransferManager @Inject constructor(private val crypto:CryptoEngine){
 private val jobs=ConcurrentHashMap<String,Job>();private val _progress=MutableSharedFlow<TransferProgress>(extraBufferCapacity=64);val progress=_progress.asSharedFlow();private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 fun send(id:String,name:String,total:Long,input:InputStream,device:BluetoothDevice){jobs[id]=scope.launch{var sent=0L;val started=System.nanoTime();try{device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")).use{s->s.connect();val out=s.outputStream;val buf=ByteArray(48*1024);while(isActive){val n=input.read(buf);if(n<0)break;val packet=crypto.encrypt(buf.copyOf(n),id.toByteArray());out.write(packet.size ushr 24);out.write(packet.size ushr 16);out.write(packet.size ushr 8);out.write(packet.size);out.write(packet);sent+=n;val sec=((System.nanoTime()-started)/1e9).coerceAtLeast(.1);_progress.emit(TransferProgress(id,name,total,sent,(sent/sec).toLong()))};out.flush();_progress.emit(TransferProgress(id,name,total,sent,0,done=true))}}catch(t:Throwable){_progress.emit(TransferProgress(id,name,total,sent,0,error=t.message))}finally{input.close();jobs.remove(id)}}}
 fun cancel(id:String){jobs.remove(id)?.cancel()}
}
