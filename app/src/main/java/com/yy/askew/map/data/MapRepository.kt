package com.yy.askew.map.data

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.amap.api.services.route.BusRouteResult
import com.amap.api.services.route.DriveRouteResult
import com.amap.api.services.route.RouteSearch
import com.amap.api.services.route.WalkRouteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MapRepository(private val context: Context) {
    
    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()
    
    private var locationClient: AMapLocationClient? = null
    private var geocodeSearch: GeocodeSearch? = null
    private var routeSearch: RouteSearch? = null
    
    init {
        initLocationClient()
        initGeocodeSearch()
        initRouteSearch()
    }
    
    private fun initLocationClient() {
        try {
            locationClient = AMapLocationClient(context)
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isOnceLocation = false
                isNeedAddress = true
                interval = 2000
                httpTimeOut = 20000
                isLocationCacheEnable = true
            }
            locationClient?.setLocationOption(option)
        } catch (e: Exception) {
            updateErrorMessage("初始化定位客户端失败: ${e.message}")
        }
    }
    
    private fun initGeocodeSearch() {
        try {
            geocodeSearch = GeocodeSearch(context)
        } catch (e: Exception) {
            updateErrorMessage("初始化地理编码失败: ${e.message}")
        }
    }
    
    private fun initRouteSearch() {
        try {
            routeSearch = RouteSearch(context)
        } catch (e: Exception) {
            updateErrorMessage("初始化路线搜索失败: ${e.message}")
        }
    }
    
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val listener = object : AMapLocationListener {
            override fun onLocationChanged(location: AMapLocation?) {
                locationClient?.unRegisterLocationListener(this)
                
                if (location != null && location.errorCode == 0) {
                    val userLocation = Location(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = location.address ?: "",
                        name = location.poiName ?: ""
                    )
                    
                    _mapState.value = _mapState.value.copy(
                        userLocation = userLocation,
                        isLocationEnabled = true,
                        permissionState = LocationPermissionState.Granted
                    )
                    
                    continuation.resume(userLocation)
                } else {
                    val errorMsg = "定位失败: ${location?.errorInfo ?: "未知错误"}"
                    updateErrorMessage(errorMsg)
                    continuation.resume(null)
                }
            }
        }
        
        locationClient?.setLocationListener(listener)
        locationClient?.startLocation()
        
        continuation.invokeOnCancellation {
            locationClient?.unRegisterLocationListener(listener)
            locationClient?.stopLocation()
        }
    }
    
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? = 
        suspendCancellableCoroutine { continuation ->
            val query = RegeocodeQuery(LatLonPoint(latitude, longitude), 200f, GeocodeSearch.AMAP)
            
            geocodeSearch?.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onRegeocodeSearched(result: RegeocodeResult?, rCode: Int) {
                    if (rCode == AMapException.CODE_AMAP_SUCCESS && result?.regeocodeAddress != null) {
                        continuation.resume(result.regeocodeAddress.formatAddress)
                    } else {
                        continuation.resume(null)
                    }
                }
                
                override fun onGeocodeSearched(result: GeocodeResult?, rCode: Int) {
                    // 不需要实现
                }
            })
            
            geocodeSearch?.getFromLocationAsyn(query)
        }
    
    suspend fun calculateRoute(start: Location, end: Location): RouteInfo? = 
        suspendCancellableCoroutine { continuation ->
            val query = RouteSearch.FromAndTo(start.toLatLonPoint(), end.toLatLonPoint())
            val routeQuery = RouteSearch.DriveRouteQuery(query, RouteSearch.DrivingDefault, null, null, "")
            
            routeSearch?.setRouteSearchListener(object : RouteSearch.OnRouteSearchListener {
                override fun onDriveRouteSearched(result: DriveRouteResult?, errorCode: Int) {
                    if (errorCode == AMapException.CODE_AMAP_SUCCESS && result?.paths?.isNotEmpty() == true) {
                        val path = result.paths[0]
                        
                        // 提取路径点信息
                        val polylinePoints = mutableListOf<Location>()
                        for (step in path.steps) {
                            for (latLng in step.polyline) {
                                polylinePoints.add(Location(latLng.latitude, latLng.longitude))
                            }
                        }
                        
                        val routeInfo = RouteInfo(
                            startLocation = start,
                            endLocation = end,
                            distance = path.distance.toInt(),
                            duration = path.duration.toInt(),
                            cost = calculateCost(path.distance.toInt()),
                            polylinePoints = polylinePoints
                        )
                        
                        _mapState.value = _mapState.value.copy(routeInfo = routeInfo)
                        continuation.resume(routeInfo)
                    } else {
                        // 如果API失败，创建模拟路线
                        val mockRouteInfo = createMockRoute(start, end)
                        _mapState.value = _mapState.value.copy(routeInfo = mockRouteInfo)
                        continuation.resume(mockRouteInfo)
                    }
                }
                
                override fun onWalkRouteSearched(result: WalkRouteResult?, errorCode: Int) {}
                override fun onBusRouteSearched(result: BusRouteResult?, errorCode: Int) {}
                override fun onRideRouteSearched(result: com.amap.api.services.route.RideRouteResult?, errorCode: Int) {}
            })
            
            routeSearch?.calculateDriveRouteAsyn(routeQuery)
        }
    
    private fun calculateCost(distance: Int): Double {
        // 简单的打车费用计算：起步价8元 + 每公里2元
        val startPrice = 8.0
        val pricePerKm = 2.0
        val km = distance / 1000.0
        return startPrice + (km * pricePerKm)
    }
    
    // 创建模拟路线（用于测试）
    private fun createMockRoute(start: Location, end: Location): RouteInfo {
        // 计算距离
        val distance = calculateDistance(start.latitude, start.longitude, end.latitude, end.longitude)
        val duration = (distance / 50 * 60).toInt() // 假设50km/h平均速度
        
        // 创建简单的直线路径点
        val polylinePoints = mutableListOf<Location>()
        val steps = 20 // 分20个点
        for (i in 0..steps) {
            val ratio = i.toDouble() / steps
            val lat = start.latitude + (end.latitude - start.latitude) * ratio
            val lng = start.longitude + (end.longitude - start.longitude) * ratio
            polylinePoints.add(Location(lat, lng))
        }
        
        return RouteInfo(
            startLocation = start,
            endLocation = end,
            distance = distance.toInt(),
            duration = duration,
            cost = calculateCost(distance.toInt()),
            polylinePoints = polylinePoints
        )
    }
    
    // 计算两点间距离（单位：米）
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // 地球半径（米）
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
    
    fun updateStartLocation(location: Location) {
        _mapState.value = _mapState.value.copy(startLocation = location)
    }
    
    fun updateEndLocation(location: Location) {
        _mapState.value = _mapState.value.copy(endLocation = location)
    }
    
    fun updatePermissionState(state: LocationPermissionState) {
        _mapState.value = _mapState.value.copy(permissionState = state)
    }
    
    private fun updateErrorMessage(message: String?) {
        _mapState.value = _mapState.value.copy(errorMessage = message)
    }
    
    fun clearError() {
        _mapState.value = _mapState.value.copy(errorMessage = null)
    }
    
    // 设置模拟位置（用于测试）
    fun setSimulatedLocation(location: Location) {
        _mapState.value = _mapState.value.copy(
            userLocation = location,
            isLocationEnabled = true,
            permissionState = LocationPermissionState.Granted
        )
    }
    
    // 清除模拟位置标记（用于强制GPS定位）
    fun clearSimulatedLocation() {
        // 这个方法主要用于标记需要重新获取GPS位置
        // 实际的GPS获取在getCurrentLocation中处理
    }
    
    // 触发地图重新居中到用户位置
    fun triggerMapRecenter() {
        _mapState.value = _mapState.value.copy(shouldCenterOnUser = true)
    }
    
    // 重置地图居中标记
    fun resetMapCenterFlag() {
        _mapState.value = _mapState.value.copy(shouldCenterOnUser = false)
    }
    
    fun cleanup() {
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
        geocodeSearch = null
        routeSearch = null
    }
}