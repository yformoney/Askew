package com.yy.askew.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.askew.bluetooth.data.BluetoothConnectionState
import com.yy.askew.bluetooth.data.BluetoothDeviceInfo
import com.yy.askew.bluetooth.data.BluetoothScanState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 蓝牙功能的ViewModel
 */
class BluetoothViewModel : ViewModel() {
    
    private var bluetoothManager: BluetoothManager? = null
    
    // UI状态
    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()
    
    // 权限状态
    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()
    
    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun initBluetooth(bluetoothManager: BluetoothManager) {
        this.bluetoothManager = bluetoothManager
        
        // 监听蓝牙状态变化
        viewModelScope.launch {
            bluetoothManager.scanState.collect { scanState ->
                _uiState.value = _uiState.value.copy(scanState = scanState)
            }
        }
        
        viewModelScope.launch {
            bluetoothManager.discoveredDevices.collect { devices ->
                _uiState.value = _uiState.value.copy(discoveredDevices = devices)
            }
        }
        
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { connectionState ->
                _uiState.value = _uiState.value.copy(connectionState = connectionState)
            }
        }
        
        viewModelScope.launch {
            bluetoothManager.connectedDevice.collect { device ->
                _uiState.value = _uiState.value.copy(connectedDevice = device)
            }
        }
        
        viewModelScope.launch {
            bluetoothManager.receivedData.collect { data ->
                if (data.isNotEmpty()) {
                    val currentMessages = _uiState.value.messages.toMutableList()
                    currentMessages.add("接收: $data")
                    _uiState.value = _uiState.value.copy(messages = currentMessages)
                }
            }
        }
    }
    
    fun updatePermissionStatus(hasPermissions: Boolean) {
        _hasPermissions.value = hasPermissions
    }
    
    fun startScan() {
        if (!_hasPermissions.value) {
            _errorMessage.value = "需要蓝牙权限才能扫描设备"
            return
        }
        
        if (!BluetoothPermissionHelper.isBluetoothAvailable()) {
            _errorMessage.value = "设备不支持蓝牙"
            return
        }
        
        if (!BluetoothPermissionHelper.isBluetoothEnabled()) {
            _errorMessage.value = "请先开启蓝牙"
            return
        }
        
        val success = bluetoothManager?.startScan() ?: false
        if (!success) {
            _errorMessage.value = "开始扫描失败"
        }
    }
    
    fun stopScan() {
        bluetoothManager?.stopScan()
    }
    
    fun connectToDevice(device: BluetoothDeviceInfo) {
        if (!_hasPermissions.value) {
            _errorMessage.value = "需要蓝牙权限才能连接设备"
            return
        }
        
        // connectToDevice 现在是异步的，不需要在viewModelScope中再次启动
        val success = bluetoothManager?.connectToDevice(device) ?: false
        if (!success) {
            _errorMessage.value = "连接设备失败: ${device.name}"
        }
    }
    
    fun disconnect() {
        bluetoothManager?.disconnect()
    }
    
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        val success = bluetoothManager?.sendData(message) ?: false
        if (success) {
            val currentMessages = _uiState.value.messages.toMutableList()
            currentMessages.add("发送: $message")
            _uiState.value = _uiState.value.copy(messages = currentMessages)
        } else {
            _errorMessage.value = "发送消息失败"
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothManager?.release()
    }
}

/**
 * 蓝牙UI状态数据类
 */
data class BluetoothUiState(
    val scanState: BluetoothScanState = BluetoothScanState.IDLE,
    val discoveredDevices: List<BluetoothDeviceInfo> = emptyList(),
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.DISCONNECTED,
    val connectedDevice: BluetoothDeviceInfo? = null,
    val messages: List<String> = emptyList()
)