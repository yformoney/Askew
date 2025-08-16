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
    val cost: Double = 0.0 // 预估费用
)

sealed class LocationPermissionState {
    object Granted : LocationPermissionState()
    object Denied : LocationPermissionState()
    object Requesting : LocationPermissionState()
}

data class MapState(
    val userLocation: Location? = null,
    val startLocation: Location? = null,
    val endLocation: Location? = null,
    val routeInfo: RouteInfo? = null,
    val isLocationEnabled: Boolean = false,
    val permissionState: LocationPermissionState = LocationPermissionState.Requesting,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)