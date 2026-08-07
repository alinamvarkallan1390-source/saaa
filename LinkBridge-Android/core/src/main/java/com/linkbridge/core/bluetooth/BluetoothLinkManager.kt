package com.linkbridge.core.bluetooth
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.linkbridge.core.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission") @Singleton class BluetoothLinkManager @Inject constructor(@ApplicationContext private val context:Context){
 companion object { val SERVICE_UUID:UUID=UUID.fromString("738afc10-3c28-4ea5-9b43-5fb276a6c321");val RX_UUID:UUID=UUID.fromString("738afc11-3c28-4ea5-9b43-5fb276a6c321");val TX_UUID:UUID=UUID.fromString("738afc12-3c28-4ea5-9b43-5fb276a6c321") }
 private val manager=context.getSystemService(BluetoothManager::class.java);private val adapter get()=manager.adapter;private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO);private val _state=MutableStateFlow(LinkSnapshot());val state:StateFlow<LinkSnapshot> = _state.asStateFlow();private val _incoming=MutableSharedFlow<ByteArray>(extraBufferCapacity=64);val incoming=_incoming.asSharedFlow();private var gatt:BluetoothGatt?=null;private var scanJob:Job?=null
 private val scanCallback=object:ScanCallback(){override fun onScanResult(type:Int,r:ScanResult){ if(r.device.name?.contains("TC4G",true)==true || r.scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID))==true){adapter.bluetoothLeScanner?.stopScan(this);connect(r.device)}} override fun onScanFailed(e:Int){_state.value=LinkSnapshot(LinkState.ERROR,message="BLE scan error $e")}}
 fun startSmartScan(){if(!adapter.isEnabled){_state.value=LinkSnapshot(LinkState.ERROR,message="Bluetooth خاموش است");return};if(gatt!=null)return;scanJob?.cancel();scanJob=scope.launch{var backoff=2_000L;while(isActive&&gatt==null){_state.value=LinkSnapshot(LinkState.SCANNING,message="جستجوی هوشمند");val filters=listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build(),ScanFilter.Builder().setDeviceName("TC4G").build());adapter.bluetoothLeScanner.startScan(filters,ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(),scanCallback);delay(10_000);adapter.bluetoothLeScanner?.stopScan(scanCallback);delay(backoff);backoff=(backoff*2).coerceAtMost(120_000)}}}
 private fun connect(d:BluetoothDevice){_state.value=LinkSnapshot(LinkState.CONNECTING,message=d.address);gatt=d.connectGatt(context,false,callback,BluetoothDevice.TRANSPORT_LE)}
 private val callback=object:BluetoothGattCallback(){override fun onConnectionStateChange(g:BluetoothGatt,status:Int,newState:Int){if(status==BluetoothGatt.GATT_SUCCESS&&newState==BluetoothProfile.STATE_CONNECTED){gatt=g;_state.value=LinkSnapshot(LinkState.AUTHENTICATING,DeviceTelemetry(name=g.device.name?:"TC4G"));g.requestMtu(247);g.discoverServices()}else{g.close();gatt=null;_state.value=LinkSnapshot(LinkState.RETRYING,message="ارتباط قطع شد");startSmartScan()}} override fun onServicesDiscovered(g:BluetoothGatt,status:Int){if(status==BluetoothGatt.GATT_SUCCESS){g.getService(SERVICE_UUID)?.getCharacteristic(TX_UUID)?.let{g.setCharacteristicNotification(it,true);it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))?.also{d->d.value=BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;g.writeDescriptor(d)}};_state.value=LinkSnapshot(LinkState.CONNECTED,DeviceTelemetry(name=g.device.name?:"TC4G",rssi=-60,syncedAt=System.currentTimeMillis()),"امن و متصل");g.readRemoteRssi()}} override fun onCharacteristicChanged(g:BluetoothGatt,c:BluetoothGattCharacteristic){_incoming.tryEmit(c.value)} override fun onReadRemoteRssi(g:BluetoothGatt,rssi:Int,status:Int){if(status==0)_state.update{it.copy(device=it.device.copy(rssi=rssi))};scope.launch{delay(15_000);g.readRemoteRssi()}}}
 fun send(data:ByteArray):Boolean {val c=gatt?.getService(SERVICE_UUID)?.getCharacteristic(RX_UUID)?:return false;c.writeType=BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;c.value=data;return gatt?.writeCharacteristic(c)==true}
 fun disconnect(){scanJob?.cancel();adapter.bluetoothLeScanner?.stopScan(scanCallback);gatt?.disconnect();gatt?.close();gatt=null;_state.value=LinkSnapshot()}
}
