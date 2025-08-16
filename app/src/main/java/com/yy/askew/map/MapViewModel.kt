package com.yy.askew.map

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yy.askew.map.data.Location
import com.yy.askew.map.data.LocationPermissionState
import com.yy.askew.map.data.MapRepository
import com.yy.askew.map.data.MapState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = MapRepository(application)
    val mapState: StateFlow<MapState> = repository.mapState
    
    init {
        checkLocationPermission()
    }
    
    private fun checkLocationPermission() {
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasFineLocation = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        when {
            hasCoarseLocation && hasFineLocation -> {
                repository.updatePermissionState(LocationPermissionState.Granted)
                getCurrentLocation()
            }
            else -> {
                repository.updatePermissionState(LocationPermissionState.Denied)
            }
        }
    }
    
    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                // 强制获取新的GPS位置，而不是使用缓存的模拟位置
                val currentLocation = repository.getCurrentLocation()
                currentLocation?.let { location ->
                    // 如果成功获取到真实GPS位置，清除模拟位置标记
                    repository.clearSimulatedLocation()
                }
            } catch (e: Exception) {
                // 错误处理已在repository中实现
            }
        }
    }
    
    fun updateStartLocation(latitude: Double, longitude: Double, address: String = "") {
        viewModelScope.launch {
            val actualAddress = if (address.isEmpty()) {
                repository.getAddressFromLocation(latitude, longitude) ?: ""
            } else {
                address
            }
            
            val location = Location(latitude, longitude, actualAddress)
            repository.updateStartLocation(location)
        }
    }
    
    fun updateEndLocation(latitude: Double, longitude: Double, address: String = "") {
        viewModelScope.launch {
            val actualAddress = if (address.isEmpty()) {
                repository.getAddressFromLocation(latitude, longitude) ?: ""
            } else {
                address
            }
            
            val location = Location(latitude, longitude, actualAddress)
            repository.updateEndLocation(location)
            
            // 如果起点和终点都已设置，计算路线
            val startLocation = mapState.value.startLocation
            if (startLocation != null) {
                calculateRoute(startLocation, location)
            }
        }
    }
    
    private fun calculateRoute(start: Location, end: Location) {
        viewModelScope.launch {
            try {
                repository.calculateRoute(start, end)
            } catch (e: Exception) {
                // 错误处理已在repository中实现
            }
        }
    }
    
    fun setStartLocationAsCurrentLocation() {
        val userLocation = mapState.value.userLocation
        if (userLocation != null) {
            repository.updateStartLocation(userLocation)
        } else {
            getCurrentLocation()
        }
    }
    
    fun onPermissionGranted() {
        repository.updatePermissionState(LocationPermissionState.Granted)
        getCurrentLocation()
    }
    
    fun onPermissionDenied() {
        repository.updatePermissionState(LocationPermissionState.Denied)
    }
    
    fun clearError() {
        repository.clearError()
    }
    
    // 添加模拟定位功能（用于测试）
    fun setSimulatedLocation(latitude: Double, longitude: Double, address: String) {
        val simulatedLocation = Location(latitude, longitude, address, "模拟位置")
        repository.setSimulatedLocation(simulatedLocation)
    }
    
    // 确保有起点并计算路线
    fun ensureStartLocationAndCalculateRoute() {
        val currentState = mapState.value
        
        // 如果没有起点，设置当前位置为起点
        if (currentState.startLocation == null) {
            currentState.userLocation?.let { userLocation ->
                repository.updateStartLocation(userLocation)
            }
        }
        
        // 如果现在有起点和终点，计算路线
        val startLoc = currentState.startLocation ?: currentState.userLocation
        val endLoc = currentState.endLocation
        
        if (startLoc != null && endLoc != null) {
            viewModelScope.launch {
                repository.calculateRoute(startLoc, endLoc)
            }
        }
    }
    
    // 定位到当前位置并居中显示
    fun centerOnCurrentLocation() {
        viewModelScope.launch {
            try {
                val currentLocation = repository.getCurrentLocation()
                currentLocation?.let { location ->
                    // 清除模拟位置标记，确保使用真实GPS
                    repository.clearSimulatedLocation()
                    // 触发地图重新居中（通过状态更新）
                    repository.triggerMapRecenter()
                }
            } catch (e: Exception) {
                // 错误处理已在repository中实现
            }
        }
    }
    
    // 重置地图居中标记
    fun resetMapCenterFlag() {
        repository.resetMapCenterFlag()
    }
    
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}