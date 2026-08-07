package com.linkbridge.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.os.SystemClock
import com.linkbridge.core.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class BluetoothLinkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("738afc10-3c28-4ea5-9b43-5fb276a6c321")
        val RX_UUID: UUID = UUID.fromString("738afc11-3c28-4ea5-9b43-5fb276a6c321")
        val TX_UUID: UUID = UUID.fromString("738afc12-3c28-4ea5-9b43-5fb276a6c321")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val manager = context.getSystemService(BluetoothManager::class.java)
    private val adapter get() = manager.adapter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(LinkSnapshot())
    val state: StateFlow<LinkSnapshot> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val incoming = _incoming.asSharedFlow()

    private var gatt: BluetoothGatt? = null
    private var scanJob: Job? = null
    private var rssiJob: Job? = null
    private val scannedMap = ConcurrentHashMap<String, ScannedDevice>()

    // For latency measurement
    private var lastPingTime = 0L

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name?.ifBlank { null } ?: result.scanRecord?.deviceName?.ifBlank { null } ?: "Unknown"
            val address = device.address
            val isOurService = result.scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true ||
                    name.contains("TC4G", true) ||
                    name.contains("Poco", true) ||
                    name.contains("Watch", true) ||
                    name.contains("LinkBridge", true)

            val scanned = ScannedDevice(
                name = name,
                address = address,
                rssi = result.rssi,
                isOurService = isOurService,
                lastSeen = System.currentTimeMillis()
            )
            scannedMap[address] = scanned
            // Update state with sorted list (our service first, then by rssi)
            _state.update {
                val sorted = scannedMap.values.sortedWith(
                    compareByDescending<ScannedDevice> { it.isOurService }.thenByDescending { it.rssi }
                )
                it.copy(scanned = sorted, state = if (it.state == LinkState.IDLE) LinkState.SCANNING else it.state)
            }

            // Auto-connect if it's our service and we are scanning automatically
            if (isOurService && gatt == null && _state.value.state == LinkState.SCANNING) {
                if (name.contains("TC4G", true) || result.scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true) {
                    adapter.bluetoothLeScanner?.stopScan(this)
                    connect(device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _state.update { it.copy(state = LinkState.ERROR, message = "خطای اسکن BLE: $errorCode") }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }
    }

    fun startSmartScan() {
        if (!adapter.isEnabled) {
            _state.update { it.copy(state = LinkState.ERROR, message = "بلوتوث خاموش است - لطفاً روشن کنید") }
            return
        }
        if (gatt != null) return

        scannedMap.clear()
        scanJob?.cancel()
        scanJob = scope.launch {
            var backoff = 2_000L
            while (isActive && gatt == null) {
                _state.update {
                    it.copy(state = LinkState.SCANNING, message = "جستجوی ساعت‌های اطراف...")
                }
                try {
                    // Scan without filter first to get all real devices with names
                    val settings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setReportDelay(0)
                        .build()
                    adapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
                    delay(12_000)
                    adapter.bluetoothLeScanner?.stopScan(scanCallback)
                } catch (e: Exception) {
                    _state.update { it.copy(state = LinkState.ERROR, message = "خطا در اسکن: ${e.message}") }
                }

                if (gatt != null) break

                // If no device found, retry with backoff
                if (scannedMap.isEmpty()) {
                    delay(backoff)
                    backoff = (backoff * 1.5).toLong().coerceAtMost(15_000L)
                } else {
                    // Keep showing scanned list, wait a bit before rescan
                    delay(5_000)
                }
            }
        }
    }

    fun stopScan() {
        try {
            adapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        scanJob?.cancel()
        if (_state.value.state == LinkState.SCANNING) {
            _state.update { it.copy(state = if (it.scanned.isNotEmpty()) it.state else LinkState.IDLE) }
        }
    }

    fun connectTo(address: String) {
        val device = adapter.getRemoteDevice(address) ?: return
        connect(device)
    }

    private fun connect(device: BluetoothDevice) {
        stopScan()
        _state.update {
            it.copy(
                state = LinkState.CONNECTING,
                message = "در حال اتصال به ${device.name ?: device.address}...",
                device = it.device.copy(name = device.name ?: "ساعت", address = device.address)
            )
        }
        try {
            // TRANSPORT_LE for watches
            gatt = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
        } catch (e: Exception) {
            _state.update { it.copy(state = LinkState.ERROR, message = "خطا در اتصال: ${e.message}") }
            startSmartScan()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                this@BluetoothLinkManager.gatt = gatt
                _state.update {
                    it.copy(
                        state = LinkState.AUTHENTICATING,
                        message = "در حال احراز هویت...",
                        device = it.device.copy(
                            name = gatt.device.name ?: it.device.name,
                            address = gatt.device.address,
                            syncedAt = System.currentTimeMillis()
                        )
                    )
                }
                scope.launch {
                    delay(200)
                    gatt.requestMtu(247)
                    delay(300)
                    gatt.discoverServices()
                    startRssiMonitoring(gatt)
                }
            } else {
                gatt.close()
                this@BluetoothLinkManager.gatt = null
                rssiJob?.cancel()
                _state.update {
                    it.copy(
                        state = LinkState.RETRYING,
                        message = if (status != 0) "ارتباط قطع شد (کد $status)" else "ارتباط قطع شد - تلاش مجدد...",
                        retryInMs = 3000
                    )
                }
                scope.launch {
                    delay(3000)
                    startSmartScan()
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // MTU changed
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    // Service not found, maybe not our device, but keep connected for generic
                    _state.update {
                        it.copy(
                            state = LinkState.CONNECTED,
                            message = "متصل (سرویس عمومی)",
                            device = it.device.copy(syncedAt = System.currentTimeMillis())
                        )
                    }
                    gatt.readRemoteRssi()
                    return
                }
                val txChar = service.getCharacteristic(TX_UUID)
                txChar?.let { char ->
                    gatt.setCharacteristicNotification(char, true)
                    val descriptor = char.getDescriptor(CCCD_UUID)
                    descriptor?.let { d ->
                        d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(d)
                    }
                }
                _state.update {
                    it.copy(
                        state = LinkState.CONNECTED,
                        message = "متصل و امن ✅",
                        device = it.device.copy(
                            name = gatt.device.name ?: it.device.name,
                            address = gatt.device.address,
                            rssi = it.device.rssi,
                            syncedAt = System.currentTimeMillis()
                        )
                    )
                }
                gatt.readRemoteRssi()
                // Send initial ping for latency
                sendPing()
            } else {
                _state.update { it.copy(state = LinkState.ERROR, message = "سرویس‌ها یافت نشد") }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value ?: return
            handleIncomingData(data)
            _incoming.tryEmit(data)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value ?: return
                _incoming.tryEmit(data)
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _state.update {
                    it.copy(device = it.device.copy(rssi = rssi, syncedAt = System.currentTimeMillis()))
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Notification enabled
            }
        }
    }

    private fun handleIncomingData(data: ByteArray) {
        val str = data.toString(Charsets.UTF_8)
        // Real telemetry parsing from watch
        if (str.startsWith("TELEMETRY|")) {
            // Format: TELEMETRY|MODEL|RELEASE|DISPLAY|level|charging|temp|ramAvail|storageFree|timestamp
            try {
                val parts = str.split("|")
                if (parts.size >= 10) {
                    val model = parts[1]
                    val androidVersion = parts[2]
                    val firmware = parts[3]
                    val battery = parts[4].toIntOrNull() ?: 0
                    val charging = parts[5].toBooleanStrictOrNull() ?: false
                    val temp = parts[6].toFloatOrNull()
                    val ram = parts[7].toLongOrNull() ?: 0
                    val storageFree = parts[8].toLongOrNull() ?: 0
                    val timestamp = parts[9].toLongOrNull() ?: System.currentTimeMillis()

                    _state.update {
                        it.copy(
                            device = it.device.copy(
                                model = model,
                                androidVersion = androidVersion,
                                firmware = firmware,
                                battery = battery,
                                charging = charging,
                                temperatureC = temp,
                                ramUsedMb = ram,
                                storageFreeMb = storageFree,
                                syncedAt = timestamp
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        } else if (str.startsWith("PONG|")) {
            // Latency measurement
            val sentTime = str.substringAfter("PONG|").toLongOrNull() ?: return
            val latency = SystemClock.elapsedRealtime() - sentTime
            _state.update { it.copy(device = it.device.copy(latencyMs = latency)) }
        }
    }

    private fun startRssiMonitoring(g: BluetoothGatt) {
        rssiJob?.cancel()
        rssiJob = scope.launch {
            while (isActive) {
                delay(5000)
                try {
                    g.readRemoteRssi()
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun sendPing() {
        lastPingTime = SystemClock.elapsedRealtime()
        send("PING|$lastPingTime".toByteArray())
    }

    fun send(data: ByteArray): Boolean {
        val g = gatt ?: return false
        val service = g.getService(SERVICE_UUID) ?: run {
            // For generic devices, try to send via first writable characteristic
            return false
        }
        val char = service.getCharacteristic(RX_UUID) ?: return false
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        char.value = data

        // Also measure latency if it's ping
        if (data.toString(Charsets.UTF_8).startsWith("PING|")) {
            lastPingTime = SystemClock.elapsedRealtime()
        }

        return try {
            g.writeCharacteristic(char)
        } catch (_: Exception) {
            false
        }
    }

    fun sendFindCommand() {
        send("FIND_DEVICE".toByteArray())
        // Also periodic ping
        scope.launch {
            repeat(5) {
                delay(1000)
                sendPing()
            }
        }
    }

    fun disconnect() {
        scanJob?.cancel()
        rssiJob?.cancel()
        try {
            adapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        scannedMap.clear()
        _state.value = LinkSnapshot(state = LinkState.IDLE, message = "قطع شد")
    }

    fun clearScanned() {
        scannedMap.clear()
        _state.update { it.copy(scanned = emptyList()) }
    }
}
