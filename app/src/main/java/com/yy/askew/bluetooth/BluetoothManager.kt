package com.yy.askew.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.yy.askew.bluetooth.data.BluetoothConnectionState
import com.yy.askew.bluetooth.data.BluetoothDeviceInfo
import com.yy.askew.bluetooth.data.BluetoothDeviceType
import com.yy.askew.bluetooth.data.BluetoothScanState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import com.yy.askew.location.LocationManager
import com.yy.askew.location.DistanceCalculator
import com.yy.askew.location.DistanceResult
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * 蓝牙管理类
 * 负责蓝牙设备的扫描、连接和数据传输
 */
@SuppressLint("MissingPermission")
class BluetoothManager private constructor(private val context: Context) {
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val locationManager = LocationManager.getInstance(context)
    
    // 状态流
    private val _scanState = MutableStateFlow(BluetoothScanState.IDLE)
    val scanState: StateFlow<BluetoothScanState> = _scanState.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()
    
    private val _connectionState = MutableStateFlow(BluetoothConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()
    
    private val _connectedDevice = MutableStateFlow<BluetoothDeviceInfo?>(null)
    val connectedDevice: StateFlow<BluetoothDeviceInfo?> = _connectedDevice.asStateFlow()
    
    private val _receivedData = MutableStateFlow<String>("")
    val receivedData: StateFlow<String> = _receivedData.asStateFlow()
    
    // 距离计算状态
    private val _distanceResult = MutableStateFlow<DistanceResult?>(null)
    val distanceResult: StateFlow<DistanceResult?> = _distanceResult.asStateFlow()
    
    // 连接相关
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    // 蓝牙设备发现广播接收器
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0).toInt()
                    
                    device?.let {
                        val deviceInfo = BluetoothDeviceInfo(
                            name = it.name ?: "未知设备",
                            address = it.address,
                            rssi = rssi,
                            isPaired = it.bondState == BluetoothDevice.BOND_BONDED,
                            deviceType = getDeviceType(it)
                        )
                        
                        Log.d(TAG, "Device found: ${deviceInfo.name} (${deviceInfo.address}) RSSI: ${rssi}dBm")
                        addDiscoveredDevice(deviceInfo)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.i(TAG, "Discovery started")
                    _scanState.value = BluetoothScanState.SCANNING
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i(TAG, "Discovery finished")
                    _scanState.value = BluetoothScanState.STOPPED
                }
            }
        }
    }
    
    init {
        // 注册广播接收器
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }
    
    /**
     * 开始扫描蓝牙设备
     */
    fun startScan(): Boolean {
        if (!BluetoothPermissionHelper.hasAllPermissions(context)) {
            Log.w(TAG, "Missing bluetooth permissions for scan")
            return false
        }
        
        if (!BluetoothPermissionHelper.isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth is not enabled")
            return false
        }
        
        Log.i(TAG, "Starting bluetooth device scan")
        
        // 清空之前的发现列表
        _discoveredDevices.value = emptyList()
        
        // 添加已配对设备
        val bondedDevices = bluetoothAdapter?.bondedDevices
        Log.i(TAG, "Found ${bondedDevices?.size ?: 0} bonded devices")
        
        bondedDevices?.forEach { device ->
            val deviceInfo = BluetoothDeviceInfo(
                name = device.name ?: "未知设备",
                address = device.address,
                isPaired = true,
                deviceType = getDeviceType(device)
            )
            addDiscoveredDevice(deviceInfo)
            Log.d(TAG, "Added bonded device: ${device.name} (${device.address})")
        }
        
        // 开始发现新设备
        val result = bluetoothAdapter?.startDiscovery() ?: false
        Log.i(TAG, "Discovery started: $result")
        return result
    }
    
    /**
     * 停止扫描
     */
    fun stopScan(): Boolean {
        return bluetoothAdapter?.cancelDiscovery() ?: false
    }
    
    /**
     * 连接到指定设备
     */
    fun connectToDevice(deviceInfo: BluetoothDeviceInfo): Boolean {
        if (!BluetoothPermissionHelper.hasAllPermissions(context)) {
            Log.w(TAG, "Missing bluetooth permissions")
            return false
        }
        
        val device = bluetoothAdapter?.getRemoteDevice(deviceInfo.address)
        if (device == null) {
            Log.e(TAG, "Cannot get remote device for address: ${deviceInfo.address}")
            return false
        }
        
        // 在后台线程执行连接操作
        scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    _connectionState.value = BluetoothConnectionState.CONNECTING
                }
                
                Log.i(TAG, "Starting connection to device: ${deviceInfo.name} (${deviceInfo.address})")
                
                // 停止扫描以释放资源
                bluetoothAdapter?.cancelDiscovery()
                
                // 尝试多种连接方法
                var socket: BluetoothSocket? = null
                var connected = false
                val connectionMethods = listOf(
                    { device.createRfcommSocketToServiceRecord(MY_UUID) },
                    { device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")) },
                    // 反射方法，适用于某些设备
                    { 
                        try {
                            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                            method.invoke(device, 1) as BluetoothSocket
                        } catch (e: Exception) {
                            Log.w(TAG, "Reflection method failed", e)
                            device.createRfcommSocketToServiceRecord(MY_UUID)
                        }
                    }
                )
                
                for ((index, createSocket) in connectionMethods.withIndex()) {
                    try {
                        socket = createSocket()
                        bluetoothSocket = socket
                        Log.i(TAG, "Trying connection method ${index + 1}")
                        socket.connect()
                        connected = true
                        Log.i(TAG, "Connection successful with method ${index + 1}")
                        break
                    } catch (e: IOException) {
                        Log.w(TAG, "Connection method ${index + 1} failed", e)
                        socket?.close()
                        socket = null
                        if (index == connectionMethods.size - 1) {
                            // 所有连接方法都失败了，但我们仍然可以基于RSSI显示距离
                            Log.w(TAG, "All connection methods failed, will show distance based on RSSI only")
                            withContext(Dispatchers.Main) {
                                _connectionState.value = BluetoothConnectionState.DISCONNECTED
                                // 设置一个虚拟连接状态，用于显示RSSI距离
                                _connectedDevice.value = deviceInfo.copy(isConnected = false)
                            }
                            // 立即计算RSSI距离
                            calculateRssiDistance(deviceInfo.rssi)
                            return@launch
                        }
                    }
                }
                
                if (!connected) {
                    return@launch
                }
                
                // 获取输入输出流
                inputStream = socket?.inputStream
                outputStream = socket?.outputStream
                
                withContext(Dispatchers.Main) {
                    _connectionState.value = BluetoothConnectionState.CONNECTED
                    _connectedDevice.value = deviceInfo.copy(isConnected = true)
                }
                
                Log.i(TAG, "Successfully connected to device: ${deviceInfo.name}")
                
                // 开始监听数据
                startListening()
                
            } catch (e: IOException) {
                Log.e(TAG, "Failed to connect to device: ${deviceInfo.name}", e)
                withContext(Dispatchers.Main) {
                    _connectionState.value = BluetoothConnectionState.DISCONNECTED
                }
                disconnect()
            }
        }
        
        return true
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    _connectionState.value = BluetoothConnectionState.DISCONNECTING
                }
                
                Log.i(TAG, "Disconnecting bluetooth connection")
                
                inputStream?.close()
                outputStream?.close()
                bluetoothSocket?.close()
                
                inputStream = null
                outputStream = null
                bluetoothSocket = null
                
                withContext(Dispatchers.Main) {
                    _connectionState.value = BluetoothConnectionState.DISCONNECTED
                    _connectedDevice.value = null
                }
                
                Log.i(TAG, "Bluetooth disconnected successfully")
            } catch (e: IOException) {
                Log.w(TAG, "Error during disconnect", e)
                withContext(Dispatchers.Main) {
                    _connectionState.value = BluetoothConnectionState.DISCONNECTED
                    _connectedDevice.value = null
                }
            }
        }
    }
    
    /**
     * 发送数据
     */
    fun sendData(data: String): Boolean {
        return try {
            outputStream?.write(data.toByteArray())
            outputStream?.flush()
            true
        } catch (e: IOException) {
            false
        }
    }
    
    /**
     * 开始监听接收数据
     */
    private fun startListening() {
        Thread {
            val buffer = ByteArray(1024)
            
            while (_connectionState.value == BluetoothConnectionState.CONNECTED) {
                try {
                    val bytes = inputStream?.read(buffer) ?: 0
                    if (bytes > 0) {
                        val receivedMessage = String(buffer, 0, bytes)
                        _receivedData.value = receivedMessage
                    }
                } catch (e: IOException) {
                    break
                }
            }
        }.start()
    }
    
    /**
     * 添加发现的设备到列表
     */
    private fun addDiscoveredDevice(deviceInfo: BluetoothDeviceInfo) {
        val currentList = _discoveredDevices.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.address == deviceInfo.address }
        
        if (existingIndex >= 0) {
            currentList[existingIndex] = deviceInfo
        } else {
            currentList.add(deviceInfo)
        }
        
        _discoveredDevices.value = currentList
    }
    
    /**
     * 根据蓝牙设备判断设备类型
     */
    private fun getDeviceType(device: BluetoothDevice): BluetoothDeviceType {
        return when (device.bluetoothClass?.majorDeviceClass) {
            0x0200 -> BluetoothDeviceType.PHONE
            0x0400 -> BluetoothDeviceType.HEADSET
            0x0100 -> BluetoothDeviceType.COMPUTER
            else -> BluetoothDeviceType.UNKNOWN
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        try {
            disconnect()
            stopScan()
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // 忽略释放时的异常
        }
    }
    
    /**
     * 开始位置更新和距离计算
     */
    fun startLocationUpdates(): Boolean {
        return locationManager.startLocationUpdates()
    }
    
    /**
     * 停止位置更新
     */
    fun stopLocationUpdates() {
        locationManager.stopLocationUpdates()
    }
    
    /**
     * 仅基于RSSI计算距离
     */
    private fun calculateRssiDistance(rssi: Int) {
        scope.launch {
            try {
                val bluetoothDistance = if (rssi != 0) {
                    DistanceCalculator.calculateBluetoothDistance(rssi)
                } else {
                    -1.0
                }
                
                val result = DistanceResult(
                    distance = bluetoothDistance,
                    method = com.yy.askew.location.DistanceMethod.BLUETOOTH,
                    accuracy = com.yy.askew.location.EstimatedAccuracy.MEDIUM,
                    description = "基于蓝牙信号强度估算 (RSSI: ${rssi}dBm)"
                )
                
                withContext(Dispatchers.Main) {
                    _distanceResult.value = result
                }
                
                Log.d(TAG, "RSSI distance calculated: ${result.getFormattedDistance()} (RSSI: ${rssi}dBm)")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating RSSI distance", e)
            }
        }
    }
    
    /**
     * 计算与连接设备的距离
     */
    fun calculateDistance(targetLocation: com.yy.askew.location.LocationInfo? = null) {
        scope.launch {
            try {
                val currentLocation = locationManager.currentLocation.value
                val connectedDeviceInfo = _connectedDevice.value
                
                // GPS距离计算
                val gpsDistance = if (currentLocation != null && targetLocation != null) {
                    DistanceCalculator.calculateGpsDistance(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        targetLocation.latitude,
                        targetLocation.longitude
                    )
                } else null
                
                // 蓝牙RSSI距离估算
                val bluetoothDistance = connectedDeviceInfo?.rssi?.let { rssi ->
                    DistanceCalculator.calculateBluetoothDistance(rssi)
                }
                
                // 综合距离结果
                val result = DistanceCalculator.getCombinedDistance(
                    gpsDistance = gpsDistance,
                    bluetoothDistance = bluetoothDistance,
                    gpsAccuracy = currentLocation?.accuracy
                )
                
                withContext(Dispatchers.Main) {
                    _distanceResult.value = result
                }
                
                Log.d(TAG, "Distance calculated: ${result.getFormattedDistance()} (${result.method})")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating distance", e)
            }
        }
    }
    
    /**
     * 获取位置权限检查
     */
    fun hasLocationPermissions(): Boolean {
        return locationManager.hasLocationPermissions()
    }
    
    /**
     * 获取位置权限列表
     */
    fun getLocationPermissions(): Array<String> {
        return locationManager.getRequiredPermissions()
    }
    
    companion object {
        private const val TAG = "BluetoothManager"
        // 用于RFCOMM连接的UUID
        private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        @Volatile
        private var INSTANCE: BluetoothManager? = null
        
        fun getInstance(context: Context): BluetoothManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}