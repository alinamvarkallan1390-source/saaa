package com.linkbridge.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.os.SystemClock
<<<<<<< HEAD
=======
import android.util.Log
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
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

<<<<<<< HEAD
    private val manager = context.getSystemService(BluetoothManager::class.java)
=======
    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
    private val adapter get() = manager.adapter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(LinkSnapshot())
    val state: StateFlow<LinkSnapshot> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val incoming = _incoming.asSharedFlow()

    private var gatt: BluetoothGatt? = null
    private var scanJob: Job? = null
    private var rssiJob: Job? = null
<<<<<<< HEAD
    private val scannedMap = ConcurrentHashMap<String, ScannedDevice>()

    // For latency measurement
=======
    private var telemetryReadJob: Job? = null
    private val scannedMap = ConcurrentHashMap<String, ScannedDevice>()

>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
    private var lastPingTime = 0L

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
<<<<<<< HEAD
            val device = result.device
            val name = device.name?.ifBlank { null } ?: result.scanRecord?.deviceName?.ifBlank { null } ?: "Unknown"
            val address = device.address
            val isOurService = result.scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true ||
                    name.contains("TC4G", true) ||
                    name.contains("Poco", true) ||
                    name.contains("Watch", true) ||
                    name.contains("LinkBridge", true)
=======
            val device = result.device ?: return
            val name = try {
                device.name?.ifBlank { null }
            } catch (_: Exception) { null }
                ?: result.scanRecord?.deviceName?.ifBlank { null }
                ?: "Unknown (${device.address.takeLast(5)})"

            val address = device.address ?: return
            val isOurService = result.scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true ||
                    name.contains("TC4G", true) ||
                    name.contains("TC5G", true) ||
                    name.contains("Poco", true) ||
                    name.contains("Watch", true) ||
                    name.contains("LinkBridge", true) ||
                    name.contains("Telzeal", true)
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)

            val scanned = ScannedDevice(
                name = name,
                address = address,
                rssi = result.rssi,
                isOurService = isOurService,
                lastSeen = System.currentTimeMillis()
            )
            scannedMap[address] = scanned
<<<<<<< HEAD
            // Update state with sorted list (our service first, then by rssi)
=======
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
            _state.update {
                val sorted = scannedMap.values.sortedWith(
                    compareByDescending<ScannedDevice> { it.isOurService }.thenByDescending { it.rssi }
                )
                it.copy(scanned = sorted, state = if (it.state == LinkState.IDLE) LinkState.SCANNING else it.state)
            }

<<<<<<< HEAD
            // Auto-connect if it's our service and we are scanning automatically
            if (isOurService && gatt == null && _state.value.state == LinkState.SCANNING) {
                if (name.contains("TC4G", true) || result.scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true) {
=======
            if (isOurService && gatt == null && _state.value.state == LinkState.SCANNING) {
                if (name.contains("TC4G", true) || result.scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true) {
                    Log.d("LinkBridge", "Auto-connecting to our service device: $name $address")
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                    adapter.bluetoothLeScanner?.stopScan(this)
                    connect(device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
<<<<<<< HEAD
=======
            Log.e("LinkBridge", "Scan failed: $errorCode")
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
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
<<<<<<< HEAD
        if (gatt != null) return
=======
        if (gatt != null) {
            Log.d("LinkBridge", "Already connected, not scanning")
            return
        }
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)

        scannedMap.clear()
        scanJob?.cancel()
        scanJob = scope.launch {
            var backoff = 2_000L
            while (isActive && gatt == null) {
                _state.update {
<<<<<<< HEAD
                    it.copy(state = LinkState.SCANNING, message = "جستجوی ساعت‌های اطراف...")
                }
                try {
                    // Scan without filter first to get all real devices with names
=======
                    it.copy(state = LinkState.SCANNING, message = "جستجوی ساعت‌های واقعی اطراف...")
                }
                try {
                    Log.d("LinkBridge", "Starting BLE scan (real devices)")
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                    val settings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .setReportDelay(0)
                        .build()
<<<<<<< HEAD
                    adapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
                    delay(12_000)
                    adapter.bluetoothLeScanner?.stopScan(scanCallback)
                } catch (e: Exception) {
=======
                    // Scan all devices to get real names
                    adapter.bluetoothLeScanner?.startScan(null, settings, scanCallback)
                    delay(12_000)
                    adapter.bluetoothLeScanner?.stopScan(scanCallback)
                    Log.d("LinkBridge", "Scan stopped, found ${scannedMap.size} devices")
                } catch (e: Exception) {
                    Log.e("LinkBridge", "Scan exception", e)
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                    _state.update { it.copy(state = LinkState.ERROR, message = "خطا در اسکن: ${e.message}") }
                }

                if (gatt != null) break

<<<<<<< HEAD
                // If no device found, retry with backoff
=======
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                if (scannedMap.isEmpty()) {
                    delay(backoff)
                    backoff = (backoff * 1.5).toLong().coerceAtMost(15_000L)
                } else {
<<<<<<< HEAD
                    // Keep showing scanned list, wait a bit before rescan
=======
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
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
<<<<<<< HEAD
            _state.update { it.copy(state = if (it.scanned.isNotEmpty()) it.state else LinkState.IDLE) }
=======
            _state.update { it.copy(state = if (it.scanned.isNotEmpty()) LinkState.SCANNING else LinkState.IDLE) }
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
        }
    }

    fun connectTo(address: String) {
<<<<<<< HEAD
        val device = adapter.getRemoteDevice(address) ?: return
        connect(device)
=======
        try {
            val device = adapter.getRemoteDevice(address)
            connect(device)
        } catch (e: Exception) {
            Log.e("LinkBridge", "Failed to get remote device $address", e)
            _state.update { it.copy(state = LinkState.ERROR, message = "دستگاه یافت نشد: $address") }
        }
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
    }

    private fun connect(device: BluetoothDevice) {
        stopScan()
<<<<<<< HEAD
=======
        Log.d("LinkBridge", "Connecting to ${device.name} ${device.address}")
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
        _state.update {
            it.copy(
                state = LinkState.CONNECTING,
                message = "در حال اتصال به ${device.name ?: device.address}...",
                device = it.device.copy(name = device.name ?: "ساعت", address = device.address)
            )
        }
        try {
<<<<<<< HEAD
            // TRANSPORT_LE for watches
=======
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
            gatt = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
        } catch (e: Exception) {
<<<<<<< HEAD
=======
            Log.e("LinkBridge", "connectGatt failed", e)
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
            _state.update { it.copy(state = LinkState.ERROR, message = "خطا در اتصال: ${e.message}") }
            startSmartScan()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
<<<<<<< HEAD
=======
            Log.d("LinkBridge", "onConnectionStateChange status=$status newState=$newState")
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                this@BluetoothLinkManager.gatt = gatt
                _state.update {
                    it.copy(
                        state = LinkState.AUTHENTICATING,
                        message = "در حال احراز هویت...",
                        device = it.device.copy(
<<<<<<< HEAD
                            name = gatt.device.name ?: it.device.name,
=======
                            name = try { gatt.device.name } catch (_: Exception) { it.device.name } ?: it.device.name,
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                            address = gatt.device.address,
                            syncedAt = System.currentTimeMillis()
                        )
                    )
                }
                scope.launch {
<<<<<<< HEAD
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
=======
                    delay(500)
                    try {
                        gatt.requestMtu(247)
                    } catch (_: Exception) {}
                    delay(500)
                    try {
                        gatt.discoverServices()
                    } catch (e: Exception) {
                        Log.e("LinkBridge", "discoverServices failed", e)
                    }
                    startRssiMonitoring(gatt)
                    startTelemetryReading(gatt)
                }
            } else {
                Log.w("LinkBridge", "Disconnected status=$status")
                try { gatt.close() } catch (_: Exception) {}
                this@BluetoothLinkManager.gatt = null
                rssiJob?.cancel()
                telemetryReadJob?.cancel()
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
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
<<<<<<< HEAD
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // MTU changed
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    // Service not found, maybe not our device, but keep connected for generic
=======
            Log.d("LinkBridge", "onMtuChanged mtu=$mtu status=$status")
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d("LinkBridge", "onServicesDiscovered status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.w("LinkBridge", "Our service not found, but keeping connection")
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                    _state.update {
                        it.copy(
                            state = LinkState.CONNECTED,
                            message = "متصل (سرویس عمومی)",
                            device = it.device.copy(syncedAt = System.currentTimeMillis())
                        )
                    }
<<<<<<< HEAD
                    gatt.readRemoteRssi()
=======
                    try { gatt.readRemoteRssi() } catch (_: Exception) {}
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                    return
                }
                val txChar = service.getCharacteristic(TX_UUID)
                txChar?.let { char ->
<<<<<<< HEAD
                    gatt.setCharacteristicNotification(char, true)
                    val descriptor = char.getDescriptor(CCCD_UUID)
                    descriptor?.let { d ->
                        d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(d)
=======
                    try {
                        gatt.setCharacteristicNotification(char, true)
                        val descriptor = char.getDescriptor(CCCD_UUID)
                        descriptor?.let { d ->
                            d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(d)
                            Log.d("LinkBridge", "Wrote CCCD to enable notifications")
                        }
                    } catch (e: Exception) {
                        Log.e("LinkBridge", "Failed to enable notifications", e)
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                    }
                }
                _state.update {
                    it.copy(
                        state = LinkState.CONNECTED,
<<<<<<< HEAD
                        message = "متصل و امن ✅",
                        device = it.device.copy(
                            name = gatt.device.name ?: it.device.name,
                            address = gatt.device.address,
                            rssi = it.device.rssi,
=======
                        message = "متصل و امن ✅ - داده واقعی",
                        device = it.device.copy(
                            name = try { gatt.device.name } catch (_: Exception) { it.device.name } ?: it.device.name,
                            address = gatt.device.address,
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                            syncedAt = System.currentTimeMillis()
                        )
                    )
                }
<<<<<<< HEAD
                gatt.readRemoteRssi()
                // Send initial ping for latency
                sendPing()
            } else {
                _state.update { it.copy(state = LinkState.ERROR, message = "سرویس‌ها یافت نشد") }
=======
                try {
                    gatt.readRemoteRssi()
                    txChar?.let { gatt.readCharacteristic(it) }
                } catch (_: Exception) {}
                sendPing()
            } else {
                _state.update { it.copy(state = LinkState.ERROR, message = "سرویس‌ها یافت نشد - کد $status") }
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value ?: return
<<<<<<< HEAD
=======
            Log.d("LinkBridge", "onCharacteristicChanged ${characteristic.uuid} len=${data.size} str=${String(data).take(100)}")
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
            handleIncomingData(data)
            _incoming.tryEmit(data)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
<<<<<<< HEAD
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value ?: return
=======
            Log.d("LinkBridge", "onCharacteristicRead ${characteristic.uuid} status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value ?: return
                Log.d("LinkBridge", "Read data len=${data.size} str=${String(data).take(100)}")
                handleIncomingData(data)
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                _incoming.tryEmit(data)
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
<<<<<<< HEAD
=======
                Log.d("LinkBridge", "RSSI: $rssi")
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                _state.update {
                    it.copy(device = it.device.copy(rssi = rssi, syncedAt = System.currentTimeMillis()))
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
<<<<<<< HEAD
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Notification enabled
=======
            Log.d("LinkBridge", "onDescriptorWrite status=$status")
            // After enabling notifications, try to read characteristic immediately for real data
            if (status == BluetoothGatt.GATT_SUCCESS) {
                scope.launch {
                    delay(500)
                    try {
                        val service = gatt.getService(SERVICE_UUID)
                        val txChar = service?.getCharacteristic(TX_UUID)
                        txChar?.let { gatt.readCharacteristic(it) }
                    } catch (_: Exception) {}
                }
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
            }
        }
    }

    private fun handleIncomingData(data: ByteArray) {
<<<<<<< HEAD
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
=======
        val str = data.toString(Charsets.UTF_8).trim()
        Log.d("LinkBridge", "handleIncoming: $str")
        if (str.startsWith("TELEMETRY|")) {
            try {
                val parts = str.split("|")
                // TELEMETRY|MODEL|RELEASE|DISPLAY|battery|charging|temp|ramUsed|free|timestamp|totalRam
                if (parts.size >= 10) {
                    val model = parts[1].ifBlank { "Unknown" }
                    val androidVersion = parts[2].ifBlank { "" }
                    val firmware = parts[3].ifBlank { "" }
                    val battery = parts[4].toIntOrNull() ?: 0
                    val charging = parts[5].toBooleanStrictOrNull() ?: parts[5].equals("true", true)
                    val temp = parts[6].toFloatOrNull()
                    val ramUsed = parts[7].toLongOrNull() ?: 0
                    val freeStorage = parts[8].toLongOrNull() ?: 0
                    val timestamp = parts[9].toLongOrNull() ?: System.currentTimeMillis()
                    val totalRam = if (parts.size > 10) parts[10].toLongOrNull() ?: 0 else 0

                    Log.d("LinkBridge", "Parsed telemetry: model=$model batt=$battery charging=$charging temp=$temp")

                    // Only update if battery is realistic (0-100) and model not empty
                    if (battery in 0..100 || model != "Unknown") {
                        _state.update {
                            it.copy(
                                device = it.device.copy(
                                    model = if (model != "Unknown" && model.isNotBlank()) model else it.device.model,
                                    androidVersion = if (androidVersion.isNotBlank()) androidVersion else it.device.androidVersion,
                                    firmware = if (firmware.isNotBlank()) firmware else it.device.firmware,
                                    battery = if (battery in 0..100) battery else it.device.battery,
                                    charging = charging,
                                    temperatureC = temp ?: it.device.temperatureC,
                                    ramUsedMb = if (ramUsed > 0) ramUsed else it.device.ramUsedMb,
                                    storageFreeMb = if (freeStorage > 0) freeStorage else it.device.storageFreeMb,
                                    syncedAt = timestamp
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LinkBridge", "Failed to parse telemetry: $str", e)
            }
        } else if (str.startsWith("PONG|")) {
            val sentTime = str.substringAfter("PONG|").toLongOrNull() ?: return
            val latency = SystemClock.elapsedRealtime() - sentTime
            Log.d("LinkBridge", "PONG latency: $latency ms (sent $sentTime)")
            if (latency in 0..10000) {
                _state.update { it.copy(device = it.device.copy(latencyMs = latency, syncedAt = System.currentTimeMillis())) }
            }
        } else {
            // Other messages like FIND_PHONE responses etc
            Log.d("LinkBridge", "Other incoming: $str")
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
        }
    }

    private fun startRssiMonitoring(g: BluetoothGatt) {
        rssiJob?.cancel()
        rssiJob = scope.launch {
            while (isActive) {
<<<<<<< HEAD
                delay(5000)
=======
                delay(4000)
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                try {
                    g.readRemoteRssi()
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

<<<<<<< HEAD
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
=======
    private fun startTelemetryReading(g: BluetoothGatt) {
        telemetryReadJob?.cancel()
        telemetryReadJob = scope.launch {
            // Read telemetry characteristic every 3 seconds for real data
            while (isActive) {
                delay(3000)
                try {
                    val service = g.getService(SERVICE_UUID)
                    val txChar = service?.getCharacteristic(TX_UUID)
                    if (txChar != null) {
                        g.readCharacteristic(txChar)
                        Log.d("LinkBridge", "Reading TX char for telemetry")
                    }
                    // Also send ping for latency
                    sendPing()
                } catch (e: Exception) {
                    Log.e("LinkBridge", "Telemetry read failed", e)
                }
            }
        }
    }

    private fun sendPing() {
        lastPingTime = SystemClock.elapsedRealtime()
        val pingStr = "PING|$lastPingTime"
        Log.d("LinkBridge", "Sending PING $lastPingTime")
        send(pingStr.toByteArray())
    }

    fun send(data: ByteArray): Boolean {
        val g = gatt ?: run {
            Log.w("LinkBridge", "send failed: gatt null")
            return false
        }
        val service = g.getService(SERVICE_UUID)
        if (service == null) {
            Log.w("LinkBridge", "send failed: service null, trying to find any writable")
            // Try to find any writable characteristic
            for (svc in g.services) {
                for (char in svc.characteristics) {
                    if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        char.value = data
                        return try {
                            g.writeCharacteristic(char)
                        } catch (_: Exception) { false }
                    }
                }
            }
            return false
        }
        val char = service.getCharacteristic(RX_UUID) ?: run {
            Log.w("LinkBridge", "RX char not found")
            return false
        }
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        char.value = data

        return try {
            val result = g.writeCharacteristic(char)
            Log.d("LinkBridge", "writeCharacteristic result=$result data=${String(data).take(50)}")
            result
        } catch (e: Exception) {
            Log.e("LinkBridge", "writeCharacteristic failed", e)
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
            false
        }
    }

    fun sendFindCommand() {
<<<<<<< HEAD
        send("FIND_DEVICE".toByteArray())
        // Also periodic ping
        scope.launch {
            repeat(5) {
                delay(1000)
=======
        Log.d("LinkBridge", "Sending FIND_DEVICE")
        send("FIND_DEVICE".toByteArray())
        scope.launch {
            repeat(3) {
                delay(800)
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
                sendPing()
            }
        }
    }

    fun disconnect() {
<<<<<<< HEAD
        scanJob?.cancel()
        rssiJob?.cancel()
        try {
            adapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        gatt?.disconnect()
        gatt?.close()
=======
        Log.d("LinkBridge", "Disconnecting")
        scanJob?.cancel()
        rssiJob?.cancel()
        telemetryReadJob?.cancel()
        try {
            adapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        try {
            gatt?.disconnect()
        } catch (_: Exception) {}
        try {
            gatt?.close()
        } catch (_: Exception) {}
>>>>>>> 750df09 (fix: critical bugs - telemetry real 0% and find and wake)
        gatt = null
        scannedMap.clear()
        _state.value = LinkSnapshot(state = LinkState.IDLE, message = "قطع شد")
    }

    fun clearScanned() {
        scannedMap.clear()
        _state.update { it.copy(scanned = emptyList()) }
    }
}
