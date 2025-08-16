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
                repository.getCurrentLocation()
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
    
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}