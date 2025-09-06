package com.yy.askew.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager as AndroidLocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 位置管理器，负责获取设备的GPS位置信息
 */
class LocationManager private constructor(private val context: Context) {
    
    private val androidLocationManager = 
        context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
    
    // 当前位置状态
    private val _currentLocation = MutableStateFlow<LocationInfo?>(null)
    val currentLocation: StateFlow<LocationInfo?> = _currentLocation.asStateFlow()
    
    // 位置更新状态
    private val _isLocationUpdating = MutableStateFlow(false)
    val isLocationUpdating: StateFlow<Boolean> = _isLocationUpdating.asStateFlow()
    
    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // 位置监听器
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val locationInfo = LocationInfo(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestamp = System.currentTimeMillis(),
                provider = location.provider ?: "unknown"
            )
            _currentLocation.value = locationInfo
            Log.i(TAG, "Location updated: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m)")
        }
        
        override fun onProviderEnabled(provider: String) {
            Log.i(TAG, "Location provider enabled: $provider")
        }
        
        override fun onProviderDisabled(provider: String) {
            Log.w(TAG, "Location provider disabled: $provider")
            _errorMessage.value = "位置服务已关闭，请开启GPS"
        }
    }
    
    /**
     * 检查是否有位置权限
     */
    fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 获取需要的位置权限列表
     */
    fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    /**
     * 开始位置更新
     */
    @Suppress("MissingPermission")
    fun startLocationUpdates(): Boolean {
        if (!hasLocationPermissions()) {
            _errorMessage.value = "需要位置权限"
            return false
        }
        
        if (!isLocationEnabled()) {
            _errorMessage.value = "请开启位置服务"
            return false
        }
        
        try {
            _isLocationUpdating.value = true
            
            // 优先使用GPS提供者
            if (androidLocationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)) {
                androidLocationManager.requestLocationUpdates(
                    AndroidLocationManager.GPS_PROVIDER,
                    MIN_UPDATE_TIME,
                    MIN_UPDATE_DISTANCE,
                    locationListener
                )
                Log.i(TAG, "Started GPS location updates")
            }
            
            // 备用网络提供者
            if (androidLocationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)) {
                androidLocationManager.requestLocationUpdates(
                    AndroidLocationManager.NETWORK_PROVIDER,
                    MIN_UPDATE_TIME,
                    MIN_UPDATE_DISTANCE,
                    locationListener
                )
                Log.i(TAG, "Started Network location updates")
            }
            
            // 获取最后已知位置作为初始位置
            getLastKnownLocation()
            
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting location updates", e)
            _errorMessage.value = "位置权限被拒绝"
            _isLocationUpdating.value = false
            return false
        }
    }
    
    /**
     * 停止位置更新
     */
    fun stopLocationUpdates() {
        androidLocationManager.removeUpdates(locationListener)
        _isLocationUpdating.value = false
        Log.i(TAG, "Stopped location updates")
    }
    
    /**
     * 获取最后已知位置
     */
    @Suppress("MissingPermission")
    private fun getLastKnownLocation() {
        if (!hasLocationPermissions()) return
        
        try {
            val providers = listOf(
                AndroidLocationManager.GPS_PROVIDER,
                AndroidLocationManager.NETWORK_PROVIDER
            )
            
            providers.forEach { provider ->
                androidLocationManager.getLastKnownLocation(provider)?.let { location ->
                    val locationInfo = LocationInfo(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = location.time,
                        provider = provider
                    )
                    _currentLocation.value = locationInfo
                    Log.i(TAG, "Got last known location from $provider")
                    return@forEach
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when getting last known location", e)
        }
    }
    
    /**
     * 检查位置服务是否开启
     */
    private fun isLocationEnabled(): Boolean {
        return androidLocationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) ||
               androidLocationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stopLocationUpdates()
    }
    
    companion object {
        private const val TAG = "LocationManager"
        private const val MIN_UPDATE_TIME = 5000L  // 5秒
        private const val MIN_UPDATE_DISTANCE = 2f // 2米
        
        @Volatile
        private var INSTANCE: LocationManager? = null
        
        fun getInstance(context: Context): LocationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * 位置信息数据类
 */
data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val provider: String
)