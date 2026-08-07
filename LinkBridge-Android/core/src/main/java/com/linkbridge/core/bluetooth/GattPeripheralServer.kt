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

@SuppressLint("MissingPermission")
@Singleton
class GattPeripheralServer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager = context.getSystemService(BluetoothManager::class.java)
    private var server: BluetoothGattServer? = null
    private var connected: BluetoothDevice? = null
    private lateinit var tx: BluetoothGattCharacteristic
    private lateinit var rx: BluetoothGattCharacteristic

    private val _commands = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val commands = _commands.asSharedFlow()

    private val callback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            connected = if (newState == BluetoothProfile.STATE_CONNECTED) device else null
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
            if (characteristic.uuid == BluetoothLinkManager.RX_UUID) {
                val str = value.toString(Charsets.UTF_8)
                // Handle real ping-pong for latency
                if (str.startsWith("PING|")) {
                    val pingTime = str.substringAfter("PING|")
                    // Respond with PONG same timestamp for real latency measurement
                    val pong = "PONG|$pingTime".toByteArray()
                    tx.value = pong
                    connected?.let { server?.notifyCharacteristicChanged(it, tx, false) }
                } else {
                    _commands.tryEmit(value)
                }
            }
            if (responseNeeded) {
                server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
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
            if (responseNeeded) {
                server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == BluetoothLinkManager.TX_UUID) {
                server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristic.value)
            }
        }
    }

    fun start() {
        if (server != null) return
        server = manager.openGattServer(context, callback)

        val service = BluetoothGattService(
            BluetoothLinkManager.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // RX characteristic for phone -> watch commands
        rx = BluetoothGattCharacteristic(
            BluetoothLinkManager.RX_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM or
                    BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // TX characteristic for watch -> phone data (telemetry, etc)
        tx = BluetoothGattCharacteristic(
            BluetoothLinkManager.TX_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM or
                    BluetoothGattCharacteristic.PERMISSION_READ
        )

        val cccd = BluetoothGattDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        tx.addDescriptor(cccd)

        service.addCharacteristic(rx)
        service.addCharacteristic(tx)
        server?.addService(service)

        // Advertise with REAL device name so phone can see it
        try {
            val advertiser = manager.adapter.bluetoothLeAdvertiser
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true) // Real name visible
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(ParcelUuid(BluetoothLinkManager.SERVICE_UUID))
                .build()

            advertiser?.startAdvertising(settings, data, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {}
                override fun onStartFailure(errorCode: Int) {}
            })
        } catch (_: Exception) {}
    }

    fun notify(payload: ByteArray): Boolean {
        val device = connected ?: return false
        tx.value = payload
        return try {
            server?.notifyCharacteristicChanged(device, tx, false) == true
        } catch (_: Exception) {
            false
        }
    }

    fun stop() {
        try {
            manager.adapter.bluetoothLeAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
        } catch (_: Exception) {}
        try {
            server?.close()
        } catch (_: Exception) {}
        server = null
        connected = null
    }
}
