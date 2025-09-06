package com.yy.askew.bluetooth.data

/**
 * 蓝牙设备数据模型
 */
data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val rssi: Int = 0,
    val isConnected: Boolean = false,
    val isPaired: Boolean = false,
    val deviceType: BluetoothDeviceType = BluetoothDeviceType.UNKNOWN
)

/**
 * 蓝牙设备类型
 */
enum class BluetoothDeviceType {
    PHONE,
    HEADSET,
    SPEAKER,
    COMPUTER,
    UNKNOWN
}

/**
 * 蓝牙连接状态
 */
enum class BluetoothConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

/**
 * 蓝牙扫描状态
 */
enum class BluetoothScanState {
    IDLE,
    SCANNING,
    STOPPED
}