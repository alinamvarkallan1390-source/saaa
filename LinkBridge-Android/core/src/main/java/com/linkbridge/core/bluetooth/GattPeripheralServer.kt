package com.linkbridge.core.bluetooth
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission") @Singleton class GattPeripheralServer @Inject constructor(@ApplicationContext private val context:Context){
 private val manager=context.getSystemService(BluetoothManager::class.java)
 private var server:BluetoothGattServer?=null
 private var connected:BluetoothDevice?=null
 private lateinit var tx:BluetoothGattCharacteristic
 private val _commands=MutableSharedFlow<ByteArray>(extraBufferCapacity=64)
 val commands=_commands.asSharedFlow()
 private val callback=object:BluetoothGattServerCallback(){
  override fun onConnectionStateChange(device:BluetoothDevice,status:Int,newState:Int){connected=if(newState==BluetoothProfile.STATE_CONNECTED)device else null}
  override fun onCharacteristicWriteRequest(device:BluetoothDevice,requestId:Int,characteristic:BluetoothGattCharacteristic,preparedWrite:Boolean,responseNeeded:Boolean,offset:Int,value:ByteArray){if(characteristic.uuid==BluetoothLinkManager.RX_UUID)_commands.tryEmit(value);if(responseNeeded)server?.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,0,null)}
  override fun onDescriptorWriteRequest(device:BluetoothDevice,requestId:Int,descriptor:BluetoothGattDescriptor,preparedWrite:Boolean,responseNeeded:Boolean,offset:Int,value:ByteArray){if(responseNeeded)server?.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,0,value)}
 }
 fun start(){if(server!=null)return;server=manager.openGattServer(context,callback);val service=BluetoothGattService(BluetoothLinkManager.SERVICE_UUID,BluetoothGattService.SERVICE_TYPE_PRIMARY);val rx=BluetoothGattCharacteristic(BluetoothLinkManager.RX_UUID,BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM);tx=BluetoothGattCharacteristic(BluetoothLinkManager.TX_UUID,BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM);tx.addDescriptor(BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE));service.addCharacteristic(rx);service.addCharacteristic(tx);server?.addService(service);manager.adapter.bluetoothLeAdvertiser?.startAdvertising(AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER).setConnectable(true).setTimeout(0).setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM).build(),AdvertiseData.Builder().setIncludeDeviceName(false).addServiceUuid(ParcelUuid(BluetoothLinkManager.SERVICE_UUID)).build(),object:AdvertiseCallback(){})}
 fun notify(payload:ByteArray):Boolean{val d=connected?:return false;tx.value=payload;return server?.notifyCharacteristicChanged(d,tx,false)==true}
 fun stop(){manager.adapter.bluetoothLeAdvertiser?.stopAdvertising(object:AdvertiseCallback(){});server?.close();server=null;connected=null}
}
