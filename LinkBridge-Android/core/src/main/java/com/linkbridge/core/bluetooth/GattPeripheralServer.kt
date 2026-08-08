package com.linkbridge.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class GattPeripheralServer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var server: BluetoothGattServer? = null
    private var connected: BluetoothDevice? = null
    private var lastDevice: BluetoothDevice? = null

    private lateinit var tx: BluetoothGattCharacteristic
    private lateinit var rx: BluetoothGattCharacteristic

    private var latestTelemetry: ByteArray = "TELEMETRY|Unknown|Unknown|Unknown|0|false|0|0|0|0|0".toByteArray()

    private val _commands = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val commands = _commands.asSharedFlow()
    val isConnected: Boolean get() = connected != null

    private val callback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.d("LinkBridge", "Peripheral onConnectionStateChange: ${device.address} status=$status newState=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected = device
                lastDevice = device
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (connected?.address == device.address) {
                    connected = null
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            Log.d("LinkBridge", "Peripheral onWrite: ${characteristic.uuid} value=${String(value, Charsets.UTF_8).take(100)} from ${device.address}")
            // Always save last device
            lastDevice = device
            connected = device

            if (characteristic.uuid == BluetoothLinkManager.RX_UUID) {
                val str = value.toString(Charsets.UTF_8).trim()
                Log.d("LinkBridge", "RX received: $str")
                if (str.startsWith("PING|")) {
                    val pingTime = str.substringAfter("PING|")
                    val pong = "PONG|$pingTime".toByteArray()
                    tx.value = pong
                    // Try notify to both connected and lastDevice
                    try {
                        connected?.let { server?.notifyCharacteristicChanged(it, tx, false) }
                        if (connected?.address != device.address) {
                            server?.notifyCharacteristicChanged(device, tx, false)
                        }
                    } catch (e: Exception) {
                        Log.e("LinkBridge", "Failed to send PONG", e)
                    }
                } else {
                    _commands.tryEmit(value)
                }
            }
            if (responseNeeded) {
                try {
                    server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                } catch (_: Exception) {}
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            Log.d("LinkBridge", "Peripheral onDescriptorWrite from ${device.address} value=${value.joinToString()}")
            if (responseNeeded) {
                try {
                    server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
                } catch (_: Exception) {}
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.d("LinkBridge", "Peripheral onRead: ${characteristic.uuid} from ${device.address}")
            lastDevice = device
            connected = device
            if (characteristic.uuid == BluetoothLinkManager.TX_UUID) {
                try {
                    // Return latest telemetry on read
                    server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, latestTelemetry)
                } catch (e: Exception) {
                    Log.e("LinkBridge", "Failed to send read response", e)
                    try {
                        server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
                    } catch (_: Exception) {}
                }
            } else {
                try {
                    server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.value)
                } catch (_: Exception) {}
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            Log.d("LinkBridge", "Peripheral onNotificationSent to ${device.address} status=$status")
        }
    }

    fun start() {
        if (server != null) return
        try {
            server = manager.openGattServer(context, callback)
        } catch (e: Exception) {
            Log.e("LinkBridge", "Failed to open GATT server", e)
            return
        }

        val service = BluetoothGattService(
            BluetoothLinkManager.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // FIX: Use plain PERMISSION_WRITE and PERMISSION_READ for Android 8.1 compatibility
        // Previously used ENCRYPTED_MITM which requires bonding and fails on many watches
        rx = BluetoothGattCharacteristic(
            BluetoothLinkManager.RX_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        tx = BluetoothGattCharacteristic(
            BluetoothLinkManager.TX_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val cccd = BluetoothGattDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        tx.addDescriptor(cccd)

        service.addCharacteristic(rx)
        service.addCharacteristic(tx)

        try {
            server?.addService(service)
        } catch (e: Exception) {
            Log.e("LinkBridge", "Failed to add service", e)
        }

        // Advertise with REAL device name
        try {
            val advertiser = manager.adapter?.bluetoothLeAdvertiser
            if (advertiser == null) {
                Log.e("LinkBridge", "BLE Advertiser null - device may not support peripheral mode")
                return
            }
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(ParcelUuid(BluetoothLinkManager.SERVICE_UUID))
                .build()

            val scanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()

            advertiser.startAdvertising(settings, data, scanResponse, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    Log.d("LinkBridge", "Advertising started success with real name: ${manager.adapter?.name}")
                }
                override fun onStartFailure(errorCode: Int) {
                    Log.e("LinkBridge", "Advertising failed: $errorCode")
                }
            })
        } catch (e: Exception) {
            Log.e("LinkBridge", "Advertising exception", e)
        }
    }

    fun notify(payload: ByteArray): Boolean {
        // Save latest for read requests
        latestTelemetry = payload

        // Try both connected and lastDevice
        var success = false
        val targets = listOfNotNull(connected, lastDevice).distinctBy { it.address }

        if (targets.isEmpty()) {
            Log.w("LinkBridge", "No connected device to notify, payload: ${String(payload).take(50)}")
            return false
        }

        for (device in targets) {
            try {
                tx.value = payload
                val result = server?.notifyCharacteristicChanged(device, tx, false) ?: false
                Log.d("LinkBridge", "Notify to ${device.address} result=$result payload=${String(payload).take(50)}")
                if (result) success = true
            } catch (e: Exception) {
                Log.e("LinkBridge", "Notify failed to ${device.address}", e)
            }
        }
        return success
    }

    fun stop() {
        try {
            manager.adapter?.bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        } catch (_: Exception) {}
        try {
            server?.close()
        } catch (_: Exception) {}
        server = null
        connected = null
        lastDevice = null
    }
}
