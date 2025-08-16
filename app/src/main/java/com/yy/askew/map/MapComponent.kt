package com.yy.askew.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.BitmapDescriptorFactory
import com.amap.api.maps2d.model.CameraPosition
import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps2d.model.MarkerOptions
import com.amap.api.maps2d.model.MyLocationStyle
import com.amap.api.maps2d.model.Polyline
import com.amap.api.maps2d.model.PolylineOptions
import com.yy.askew.map.data.LocationPermissionState

@Composable
fun MapComponent(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = viewModel(),
    onLocationSelected: ((latitude: Double, longitude: Double, address: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val mapState by mapViewModel.mapState.collectAsState()
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (granted) {
            mapViewModel.onPermissionGranted()
        } else {
            mapViewModel.onPermissionDenied()
        }
    }
    
    LaunchedEffect(Unit) {
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasCoarseLocation && !hasFineLocation) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    when (mapState.permissionState) {
        is LocationPermissionState.Requesting -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        is LocationPermissionState.Denied -> {
            PermissionDeniedContent(
                modifier = modifier,
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }
        
        is LocationPermissionState.Granted -> {
            ActualMapComponent(
                modifier = modifier,
                mapState = mapState,
                onLocationSelected = onLocationSelected,
                onCurrentLocationClick = { mapViewModel.getCurrentLocation() }
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "位置权限",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "需要位置权限",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "为了提供准确的打车服务，需要获取您的位置信息",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("授权位置权限")
            }
        }
    }
}

@Composable
private fun ActualMapComponent(
    modifier: Modifier = Modifier,
    mapState: com.yy.askew.map.data.MapState,
    onLocationSelected: ((latitude: Double, longitude: Double, address: String) -> Unit)?,
    onCurrentLocationClick: () -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    var aMap: AMap? = null
    
    AndroidView(
        factory = { mapView },
        modifier = modifier
    ) { view ->
        view.map?.let { map ->
            aMap = map
            
            // 设置地图属性
            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false
            map.uiSettings.isCompassEnabled = false
            map.uiSettings.isScaleControlsEnabled = false
            map.isMyLocationEnabled = true
            
            // 地图点击事件
            map.setOnMapClickListener { latLng ->
                onLocationSelected?.invoke(
                    latLng.latitude,
                    latLng.longitude,
                    "" // 地址将在ViewModel中获取
                )
            }
            
            // 清除之前的标记
            map.clear()
            
            // 更新用户位置
            mapState.userLocation?.let { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                
                // 如果没有起点和终点，聚焦到当前位置
                if (mapState.startLocation == null && mapState.endLocation == null) {
                    map.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition(latLng, 16f, 0f, 0f)
                        )
                    )
                }
            }
            
            // 添加起点标记
            mapState.startLocation?.let { location ->
                val marker = MarkerOptions()
                    .position(LatLng(location.latitude, location.longitude))
                    .title("起点")
                    .snippet(location.address.ifEmpty { "当前位置" })
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                map.addMarker(marker)
            }
            
            // 添加终点标记
            mapState.endLocation?.let { location ->
                val marker = MarkerOptions()
                    .position(LatLng(location.latitude, location.longitude))
                    .title("终点")
                    .snippet(location.address.ifEmpty { "目的地" })
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                map.addMarker(marker)
            }
            
            // 绘制路线
            mapState.routeInfo?.let { routeInfo ->
                if (routeInfo.polylinePoints.isNotEmpty()) {
                    val latLngs = routeInfo.polylinePoints.map { 
                        LatLng(it.latitude, it.longitude) 
                    }
                    
                    val polylineOptions = PolylineOptions()
                        .addAll(latLngs)
                        .color(android.graphics.Color.parseColor("#FF4081")) // 粉红色路线
                        .width(8f)
                    
                    map.addPolyline(polylineOptions)
                }
            }
            
            // 如果有起点和终点，调整视野以包含两点
            if (mapState.startLocation != null && mapState.endLocation != null) {
                val startLatLng = LatLng(mapState.startLocation.latitude, mapState.startLocation.longitude)
                val endLatLng = LatLng(mapState.endLocation.latitude, mapState.endLocation.longitude)
                
                // 计算边界
                val boundsBuilder = com.amap.api.maps2d.model.LatLngBounds.Builder()
                boundsBuilder.include(startLatLng)
                boundsBuilder.include(endLatLng)
                
                // 如果有路线点，也包含在边界内
                mapState.routeInfo?.polylinePoints?.forEach { point ->
                    boundsBuilder.include(LatLng(point.latitude, point.longitude))
                }
                
                val bounds = boundsBuilder.build()
                
                // 调整视野
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, 100)
                )
            }
        }
    }
    
    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onResume()
        
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
}