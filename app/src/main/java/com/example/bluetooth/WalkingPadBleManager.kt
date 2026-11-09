package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "WalkingPadBle"

class WalkingPadBleManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _connectedDeviceAddress = MutableStateFlow<String?>(null)
    val connectedDeviceAddress: StateFlow<String?> = _connectedDeviceAddress.asStateFlow()

    private val _beltState = MutableStateFlow(BeltState.STOPPED)
    val beltState: StateFlow<BeltState> = _beltState.asStateFlow()

    private val _liveData = MutableStateFlow(LiveTreadmillData())
    val liveData: StateFlow<LiveTreadmillData> = _liveData.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private var controlPointCharacteristic: BluetoothGattCharacteristic? = null
    private var treadmillDataCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false

    // BLE Scanner Callback
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = device.name ?: result.scanRecord?.deviceName ?: "WalkingPad / FTMS"
            val address = device.address

            val isWalkingPad = WalkingPadBleConstants.DEVICE_NAME_PREFIXES.any { prefix ->
                name.contains(prefix, ignoreCase = true)
            } || result.scanRecord?.serviceUuids?.any {
                it.uuid == WalkingPadBleConstants.SERVICE_FTMS ||
                it.uuid == WalkingPadBleConstants.SERVICE_KINGSMITH_PROPRIETARY
            } == true

            val discovered = DiscoveredBleDevice(
                name = name,
                address = address,
                rssi = result.rssi,
                isWalkingPad = isWalkingPad
            )

            val current = _discoveredDevices.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.address == address }
            if (existingIndex >= 0) {
                current[existingIndex] = discovered
            } else {
                current.add(discovered)
            }
            // Sort WalkingPad devices to the top, then by signal strength
            current.sortWith(compareByDescending<DiscoveredBleDevice> { it.isWalkingPad }.thenByDescending { it.rssi })
            _discoveredDevices.value = current
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE Scan failed with code: $errorCode")
            _connectionStatus.value = ConnectionStatus.ERROR
            _lastErrorMessage.value = "BLE scan failed (code $errorCode)"
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null || bluetoothAdapter?.isEnabled != true) {
            _lastErrorMessage.value = "Bluetooth is disabled or unavailable"
            _connectionStatus.value = ConnectionStatus.ERROR
            return
        }

        _discoveredDevices.value = emptyList()
        _connectionStatus.value = ConnectionStatus.SCANNING
        isScanning = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(WalkingPadBleConstants.SERVICE_FTMS))
                .build(),
            ScanFilter.Builder()
                .setDeviceName("WalkingPad")
                .build()
        )

        try {
            // Scan broadly without strict hardware filtering so unadvertised MC21 variants are found
            scanner.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan: ${e.message}")
            _lastErrorMessage.value = "Failed to start BLE scan: ${e.localizedMessage}"
            _connectionStatus.value = ConnectionStatus.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Exception stopping scan: ${e.message}")
        }
        isScanning = false
        if (_connectionStatus.value == ConnectionStatus.SCANNING) {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        stopScan()
        disconnect()

        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            _lastErrorMessage.value = "Device $address not found"
            _connectionStatus.value = ConnectionStatus.ERROR
            return
        }

        _connectionStatus.value = ConnectionStatus.CONNECTING
        _connectedDeviceAddress.value = address
        _connectedDeviceName.value = device.name ?: "WalkingPad MC21"

        try {
            bluetoothGatt = device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (e: Exception) {
            Log.e(TAG, "connectGatt error: ${e.message}")
            _connectionStatus.value = ConnectionStatus.ERROR
            _lastErrorMessage.value = "Failed to connect: ${e.localizedMessage}"
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting GATT: ${e.message}")
        }
        bluetoothGatt = null
        controlPointCharacteristic = null
        treadmillDataCharacteristic = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _beltState.value = BeltState.STOPPED
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server. Discovering services...")
                _connectionStatus.value = ConnectionStatus.CONNECTING
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server")
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                _beltState.value = BeltState.STOPPED
                controlPointCharacteristic = null
                treadmillDataCharacteristic = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed with status: $status")
                _connectionStatus.value = ConnectionStatus.ERROR
                _lastErrorMessage.value = "Service discovery failed (status $status)"
                return
            }

            val ftmsService = gatt.getService(WalkingPadBleConstants.SERVICE_FTMS)
            if (ftmsService != null) {
                Log.d(TAG, "Found FTMS Service: ${ftmsService.uuid}")
                treadmillDataCharacteristic = ftmsService.getCharacteristic(WalkingPadBleConstants.CHAR_TREADMILL_DATA)
                controlPointCharacteristic = ftmsService.getCharacteristic(WalkingPadBleConstants.CHAR_FITNESS_MACHINE_CONTROL_POINT)
                val statusCharacteristic = ftmsService.getCharacteristic(WalkingPadBleConstants.CHAR_FITNESS_MACHINE_STATUS)

                scope.launch {
                    // Staggered subscription sequence to avoid GATT descriptor collision
                    treadmillDataCharacteristic?.let { char ->
                        enableNotification(gatt, char)
                        delay(200)
                    }

                    statusCharacteristic?.let { char ->
                        enableNotification(gatt, char)
                        delay(200)
                    }

                    // Request Control on the Control Point
                    requestControl()
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                }
            } else {
                Log.w(TAG, "FTMS service not found. Checking KingSmith fallback...")
                val ksService = gatt.getService(WalkingPadBleConstants.SERVICE_KINGSMITH_PROPRIETARY)
                if (ksService != null) {
                    controlPointCharacteristic = ksService.getCharacteristic(WalkingPadBleConstants.CHAR_KINGSMITH_WRITE)
                    val notifyChar = ksService.getCharacteristic(WalkingPadBleConstants.CHAR_KINGSMITH_NOTIFY)
                    notifyChar?.let { enableNotification(gatt, it) }
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                } else {
                    _connectionStatus.value = ConnectionStatus.CONNECTED // Connected generically
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            handleCharacteristicUpdate(characteristic.uuid, value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicUpdate(characteristic.uuid, value)
        }
    }

    private fun handleCharacteristicUpdate(uuid: UUID, value: ByteArray) {
        when (uuid) {
            WalkingPadBleConstants.CHAR_TREADMILL_DATA -> {
                parseTreadmillData(value)
            }
            WalkingPadBleConstants.CHAR_FITNESS_MACHINE_STATUS -> {
                parseMachineStatus(value)
            }
            WalkingPadBleConstants.CHAR_KINGSMITH_NOTIFY -> {
                parseKingSmithLegacyNotify(value)
            }
        }
    }

    /**
     * Parses standard FTMS Treadmill Data (0x2ACD) with KingSmith bit-13 step count extension.
     */
    private fun parseTreadmillData(data: ByteArray) {
        if (data.size < 2) return

        try {
            val flags = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
            var offset = 2

            // Bit 0: More Data (0 = Instantaneous Speed Present)
            var instSpeedKmh = _liveData.value.instantaneousSpeedKmh
            if ((flags and 0x0001) == 0 && data.size >= offset + 2) {
                val rawSpeed = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
                instSpeedKmh = rawSpeed / 100.0f
                offset += 2
            }

            // Bit 1: Average Speed Present
            var avgSpeedKmh = _liveData.value.averageSpeedKmh
            if ((flags and 0x0002) != 0 && data.size >= offset + 2) {
                val rawAvg = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
                avgSpeedKmh = rawAvg / 100.0f
                offset += 2
            }

            // Bit 2: Total Distance Present (UInt24, 1m resolution)
            var totalDistanceMeters = _liveData.value.totalDistanceMeters
            if ((flags and 0x0004) != 0 && data.size >= offset + 3) {
                val rawDist = (data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8) or
                        ((data[offset + 2].toInt() and 0xFF) shl 16)
                totalDistanceMeters = rawDist.toFloat()
                offset += 3
            }

            // Bit 3: Inclination and Ramp Angle Setting Present
            var inclination = _liveData.value.inclinationPercent
            if ((flags and 0x0008) != 0 && data.size >= offset + 4) {
                val rawInc = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt()) shl 8)
                inclination = rawInc / 10.0f
                offset += 4
            }

            // Bit 4: Elevation Gain Present
            if ((flags and 0x0010) != 0 && data.size >= offset + 4) {
                offset += 4
            }

            // Bit 5: Instantaneous Pace Present
            if ((flags and 0x0020) != 0 && data.size >= offset + 1) {
                offset += 1
            }

            // Bit 6: Average Pace Present
            if ((flags and 0x0040) != 0 && data.size >= offset + 1) {
                offset += 1
            }

            // Bit 7: Expended Energy Present
            var instKcal = _liveData.value.instantaneousKcal
            if ((flags and 0x0080) != 0 && data.size >= offset + 2) {
                val rawEnergy = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
                instKcal = rawEnergy.toFloat()
                offset += 2
                // energy per hour (2 bytes) + energy per minute (1 byte) if present
                if (data.size >= offset + 3) offset += 3
            }

            // Bit 8: Heart Rate Present
            if ((flags and 0x0100) != 0 && data.size >= offset + 1) {
                offset += 1
            }

            // Bit 9: Metabolic Equivalent Present
            if ((flags and 0x0200) != 0 && data.size >= offset + 1) {
                offset += 1
            }

            // Bit 10: Elapsed Time Present (UInt16, seconds)
            var elapsedTime = _liveData.value.elapsedTimeSeconds
            if ((flags and 0x0400) != 0 && data.size >= offset + 2) {
                elapsedTime = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
                offset += 2
            }

            // Bit 11: Remaining Time Present
            if ((flags and 0x0800) != 0 && data.size >= offset + 2) {
                offset += 2
            }

            // Bit 12: Force on Belt / Power Output
            if ((flags and 0x1000) != 0 && data.size >= offset + 4) {
                offset += 4
            }

            // Bit 13: KingSmith Step Count Extension
            var totalSteps = _liveData.value.totalSteps
            var cadenceSpm = _liveData.value.stepCadenceSpm
            if ((flags and 0x2000) != 0 && data.size >= offset + 2) {
                val newSteps = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
                if (newSteps > 0) {
                    val stepDiff = newSteps - totalSteps
                    if (stepDiff in 1..20 && elapsedTime > 0) {
                        cadenceSpm = (stepDiff * 60).coerceIn(40, 190)
                    }
                    totalSteps = newSteps
                }
            } else if (data.size >= offset + 2) {
                // Some KingSmith firmwares append steps unconditionally at packet tail
                val tailSteps = (data[data.size - 2].toInt() and 0xFF) or ((data[data.size - 1].toInt() and 0xFF) shl 8)
                if (tailSteps in 1..200000) {
                    totalSteps = tailSteps
                }
            }

            // Estimate step cadence from speed if sensor reports 0
            if (cadenceSpm == 0 && instSpeedKmh > 0.5f) {
                // Empirical formula for walking cadence: ~18-22 steps/min per km/h
                cadenceSpm = (instSpeedKmh * 20.0f).toInt().coerceIn(30, 180)
            }

            // Infer belt state from speed
            if (instSpeedKmh > 0.1f) {
                _beltState.value = BeltState.RUNNING
            } else if (_beltState.value == BeltState.RUNNING) {
                _beltState.value = BeltState.PAUSED
            }

            _liveData.value = LiveTreadmillData(
                instantaneousSpeedKmh = instSpeedKmh,
                averageSpeedKmh = avgSpeedKmh,
                totalDistanceMeters = totalDistanceMeters,
                inclinationPercent = inclination,
                elapsedTimeSeconds = elapsedTime,
                totalSteps = totalSteps,
                stepCadenceSpm = cadenceSpm,
                instantaneousKcal = instKcal,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding FTMS treadmill packet: ${e.message}")
        }
    }

    private fun parseMachineStatus(data: ByteArray) {
        if (data.isEmpty()) return
        val opcode = data[0].toInt() and 0xFF
        Log.d(TAG, "FTMS Machine Status Opcode: 0x%02X".format(opcode))
        when (opcode) {
            0x01 -> _beltState.value = BeltState.STOPPED
            0x02 -> _beltState.value = BeltState.PAUSED
            0x03, 0x04 -> _beltState.value = BeltState.RUNNING
        }
    }

    private fun parseKingSmithLegacyNotify(data: ByteArray) {
        // Fallback for legacy KingSmith serial packet format
        if (data.size >= 10 && data[0] == 0xF7.toByte()) {
            val stateCode = data[2].toInt() and 0xFF
            val speedRaw = data[3].toInt() and 0xFF
            val speedKmh = speedRaw / 10.0f
            when (stateCode) {
                0 -> _beltState.value = BeltState.STOPPED
                1 -> _beltState.value = BeltState.RUNNING
                else -> _beltState.value = BeltState.PAUSED
            }
            _liveData.value = _liveData.value.copy(
                instantaneousSpeedKmh = speedKmh,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(WalkingPadBleConstants.CLIENT_CHARACTERISTIC_CONFIG)
        if (descriptor != null) {
            val isIndicate = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(
                    descriptor,
                    if (isIndicate) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                )
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = if (isIndicate) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    /**
     * Send FTMS Request Control opcode [0x00]
     */
    fun requestControl() {
        writeControlPoint(byteArrayOf(WalkingPadBleConstants.OPCODE_REQUEST_CONTROL))
    }

    /**
     * Start / Resume belt [0x07]
     */
    fun startOrResume() {
        _beltState.value = BeltState.STARTING
        writeControlPoint(byteArrayOf(WalkingPadBleConstants.OPCODE_START_OR_RESUME))
    }

    /**
     * Pause belt [0x08, 0x02]
     */
    fun pause() {
        _beltState.value = BeltState.PAUSED
        writeControlPoint(byteArrayOf(WalkingPadBleConstants.OPCODE_STOP_OR_PAUSE, WalkingPadBleConstants.PARAM_PAUSE))
    }

    /**
     * Emergency Stop belt [0x08, 0x01]
     */
    fun emergencyStop() {
        _beltState.value = BeltState.STOPPED
        writeControlPoint(byteArrayOf(WalkingPadBleConstants.OPCODE_STOP_OR_PAUSE, WalkingPadBleConstants.PARAM_STOP))
    }

    /**
     * Set target speed in km/h.
     * Encoded as [0x02, LSB, MSB] with resolution of 0.01 km/h (raw = speed * 100).
     */
    fun setTargetSpeed(speedKmh: Float) {
        val clamped = speedKmh.coerceIn(WalkingPadBleConstants.MIN_SPEED_KMH, WalkingPadBleConstants.MAX_SPEED_KMH)
        val raw = (clamped * 100.0f).toInt()
        val lsb = (raw and 0xFF).toByte()
        val msb = ((raw shr 8) and 0xFF).toByte()
        writeControlPoint(byteArrayOf(WalkingPadBleConstants.OPCODE_SET_TARGET_SPEED, lsb, msb))
    }

    @SuppressLint("MissingPermission")
    private fun writeControlPoint(payload: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val char = controlPointCharacteristic ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    char,
                    payload,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                @Suppress("DEPRECATION")
                char.value = payload
                @Suppress("DEPRECATION")
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
        } catch (e: Exception) {
            Log.e(TAG, "writeControlPoint exception: ${e.message}")
            _lastErrorMessage.value = "Failed to send command to WalkingPad"
        }
    }
}
