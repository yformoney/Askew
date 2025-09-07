package com.yy.askew.map.data

import com.amap.api.services.core.LatLonPoint

data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val name: String = ""
) {
    fun toLatLonPoint(): LatLonPoint {
        return LatLonPoint(latitude, longitude)
    }
    
    companion object {
        fun fromLatLonPoint(latLonPoint: LatLonPoint, address: String = "", name: String = ""): Location {
            return Location(
                latitude = latLonPoint.latitude,
                longitude = latLonPoint.longitude,
                address = address,
                name = name
            )
        }
    }
}

data class RouteInfo(
    val startLocation: Location,
    val endLocation: Location,
    val distance: Int = 0, // 距离（米）
    val duration: Int = 0, // 时间（秒）
    val cost: Double = 0.0, // 预估费用
    val polylinePoints: List<Location> = emptyList() // 路线轨迹点
)

sealed class LocationPermissionState {
    object Granted : LocationPermissionState()
    object Denied : LocationPermissionState()
    object Requesting : LocationPermissionState()
}

// 城市边界信息
data class CityBoundary(
    val cityName: String,
    val boundaryPoints: List<Location>, // 城市边界多边形的点
    val centerLocation: Location? = null // 城市中心点
)

data class MapState(
    val userLocation: Location? = null,
    val startLocation: Location? = null,
    val endLocation: Location? = null,
    val routeInfo: RouteInfo? = null,
    val isLocationEnabled: Boolean = false,
    val permissionState: LocationPermissionState = LocationPermissionState.Requesting,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val shouldCenterOnUser: Boolean = false, // 用于触发地图居中到用户位置
    val currentCity: String? = null, // 当前用户所在城市
    val cityBoundary: CityBoundary? = null, // 当前城市边界
    val showCityBoundary: Boolean = false // 是否显示城市边界和蒙层
)