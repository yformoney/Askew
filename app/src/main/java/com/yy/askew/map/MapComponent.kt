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
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        ) { view ->
            view.map?.let { map ->
                aMap = map
                
                // 设置地图属性
                map.uiSettings.isZoomControlsEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                map.isMyLocationEnabled = true
                
                // 设置自定义定位样式（2D地图简化版）
                map.isMyLocationEnabled = true
                
                // 地图点击事件
                map.setOnMapClickListener { latLng ->
                    onLocationSelected?.invoke(
                        latLng.latitude,
                        latLng.longitude,
                        "" // 地址将在ViewModel中获取
                    )
                }
                
                // 更新用户位置
                mapState.userLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition(latLng, 16f, 0f, 0f)
                        )
                    )
                }
                
                // 添加起点标记
                mapState.startLocation?.let { location ->
                    val marker = MarkerOptions()
                        .position(LatLng(location.latitude, location.longitude))
                        .title("起点")
                        .snippet(location.address)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    map.addMarker(marker)
                }
                
                // 添加终点标记
                mapState.endLocation?.let { location ->
                    val marker = MarkerOptions()
                        .position(LatLng(location.latitude, location.longitude))
                        .title("终点")
                        .snippet(location.address)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    map.addMarker(marker)
                }
            }
        }
        
        // 悬浮的当前位置按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp)
                .background(
                    Color.White,
                    CircleShape
                )
                .clickable { onCurrentLocationClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "当前位置",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // 显示路线信息
        mapState.routeInfo?.let { routeInfo ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row {
                        Text(
                            text = "距离: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${routeInfo.distance / 1000.0} 公里",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "时间: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${routeInfo.duration / 60} 分钟",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        Text(
                            text = "预估费用: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "¥${String.format("%.2f", routeInfo.cost)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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