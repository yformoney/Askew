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
                        val routeInfo = RouteInfo(
                            startLocation = start,
                            endLocation = end,
                            distance = path.distance.toInt(),
                            duration = path.duration.toInt(),
                            cost = calculateCost(path.distance.toInt())
                        )
                        
                        _mapState.value = _mapState.value.copy(routeInfo = routeInfo)
                        continuation.resume(routeInfo)
                    } else {
                        continuation.resume(null)
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
    
    fun cleanup() {
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        locationClient = null
        geocodeSearch = null
        routeSearch = null
    }
}