package com.yy.askew.location

import kotlin.math.*

/**
 * 距离计算工具类
 * 提供多种距离测量方法
 */
object DistanceCalculator {
    
    /**
     * 使用Haversine公式计算两个GPS坐标点之间的距离
     * 
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 距离（米）
     */
    fun calculateGpsDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // 地球半径（米）
        
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLatRad = Math.toRadians(lat2 - lat1)
        val deltaLonRad = Math.toRadians(lon2 - lon1)
        
        val a = sin(deltaLatRad / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * 根据RSSI值估算蓝牙设备距离
     * 使用对数路径损耗模型
     * 
     * @param rssi 接收信号强度指示器（dBm）
     * @param txPower 发射功率（dBm，默认-59为1米处的RSSI值）
     * @param pathLoss 路径损耗指数（默认2.0为自由空间，室内通常2-4）
     * @return 估算距离（米）
     */
    fun calculateBluetoothDistance(
        rssi: Int, 
        txPower: Double = -59.0, 
        pathLoss: Double = 2.0
    ): Double {
        if (rssi == 0) {
            return -1.0 // 无法计算
        }
        
        // RSSI = TxPower - 10 * n * log10(distance)
        // 重新整理得到: distance = 10^((TxPower - RSSI) / (10 * n))
        val ratio = rssi * 1.0 / txPower
        
        return when {
            ratio < 1.0 -> 10.0.pow((txPower - rssi) / (10 * pathLoss))
            else -> {
                val accuracy = (0.89976) * ratio.pow(7.7095) + 0.111
                accuracy
            }
        }
    }
    
    /**
     * 根据RSSI值获取距离等级
     */
    fun getRssiDistanceLevel(rssi: Int): DistanceLevel {
        return when {
            rssi >= -30 -> DistanceLevel.IMMEDIATE  // 0-0.5米
            rssi >= -50 -> DistanceLevel.NEAR       // 0.5-3米
            rssi >= -70 -> DistanceLevel.FAR        // 3-10米
            else -> DistanceLevel.VERY_FAR          // >10米
        }
    }
    
    /**
     * 格式化距离显示
     */
    fun formatDistance(distance: Double): String {
        return when {
            distance < 0 -> "未知距离"
            distance < 1 -> "${(distance * 100).roundToInt()} 厘米"
            distance < 1000 -> "${distance.roundToInt()} 米"
            else -> "${"%.2f".format(distance / 1000)} 公里"
        }
    }
    
    /**
     * 综合GPS和蓝牙距离，返回最准确的距离
     * 
     * @param gpsDistance GPS计算的距离（米）
     * @param bluetoothDistance 蓝牙RSSI估算的距离（米）
     * @param gpsAccuracy GPS精度（米）
     * @return 综合距离结果
     */
    fun getCombinedDistance(
        gpsDistance: Double?, 
        bluetoothDistance: Double?, 
        gpsAccuracy: Float?
    ): DistanceResult {
        return when {
            // 如果只有蓝牙距离
            gpsDistance == null && bluetoothDistance != null -> {
                DistanceResult(
                    distance = bluetoothDistance,
                    method = DistanceMethod.BLUETOOTH,
                    accuracy = EstimatedAccuracy.MEDIUM,
                    description = "基于蓝牙信号强度"
                )
            }
            
            // 如果只有GPS距离
            bluetoothDistance == null && gpsDistance != null -> {
                val accuracy = when {
                    gpsAccuracy != null && gpsAccuracy <= 5 -> EstimatedAccuracy.HIGH
                    gpsAccuracy != null && gpsAccuracy <= 15 -> EstimatedAccuracy.MEDIUM
                    else -> EstimatedAccuracy.LOW
                }
                DistanceResult(
                    distance = gpsDistance,
                    method = DistanceMethod.GPS,
                    accuracy = accuracy,
                    description = "基于GPS定位"
                )
            }
            
            // 如果两种距离都有
            gpsDistance != null && bluetoothDistance != null -> {
                // 近距离（<50米）优先使用蓝牙，远距离优先使用GPS
                val useGps = gpsDistance > 50 || (gpsAccuracy != null && gpsAccuracy <= 10)
                
                if (useGps) {
                    val accuracy = when {
                        gpsAccuracy != null && gpsAccuracy <= 5 -> EstimatedAccuracy.HIGH
                        gpsAccuracy != null && gpsAccuracy <= 15 -> EstimatedAccuracy.MEDIUM
                        else -> EstimatedAccuracy.LOW
                    }
                    DistanceResult(
                        distance = gpsDistance,
                        method = DistanceMethod.COMBINED_GPS_PRIORITY,
                        accuracy = accuracy,
                        description = "GPS定位 (蓝牙辅助: ${formatDistance(bluetoothDistance)})"
                    )
                } else {
                    DistanceResult(
                        distance = bluetoothDistance,
                        method = DistanceMethod.COMBINED_BLUETOOTH_PRIORITY,
                        accuracy = EstimatedAccuracy.MEDIUM,
                        description = "蓝牙估算 (GPS参考: ${formatDistance(gpsDistance)})"
                    )
                }
            }
            
            // 都没有
            else -> DistanceResult(
                distance = -1.0,
                method = DistanceMethod.UNKNOWN,
                accuracy = EstimatedAccuracy.UNKNOWN,
                description = "无法计算距离"
            )
        }
    }
}

/**
 * 距离等级枚举
 */
enum class DistanceLevel(val description: String) {
    IMMEDIATE("极近距离"),
    NEAR("近距离"),
    FAR("中等距离"),
    VERY_FAR("远距离")
}

/**
 * 距离计算方法枚举
 */
enum class DistanceMethod {
    GPS,
    BLUETOOTH,
    COMBINED_GPS_PRIORITY,
    COMBINED_BLUETOOTH_PRIORITY,
    UNKNOWN
}

/**
 * 预估精度枚举
 */
enum class EstimatedAccuracy(val description: String) {
    HIGH("高精度"),
    MEDIUM("中等精度"),
    LOW("低精度"),
    UNKNOWN("精度未知")
}

/**
 * 距离计算结果数据类
 */
data class DistanceResult(
    val distance: Double,           // 距离（米）
    val method: DistanceMethod,     // 计算方法
    val accuracy: EstimatedAccuracy, // 预估精度
    val description: String         // 描述信息
) {
    /**
     * 格式化显示距离
     */
    fun getFormattedDistance(): String = DistanceCalculator.formatDistance(distance)
    
    /**
     * 是否是有效距离
     */
    fun isValid(): Boolean = distance >= 0
}