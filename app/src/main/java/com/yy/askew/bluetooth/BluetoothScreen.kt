package com.yy.askew.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yy.askew.bluetooth.data.BluetoothConnectionState
import com.yy.askew.bluetooth.data.BluetoothDeviceInfo
import com.yy.askew.bluetooth.data.BluetoothDeviceType
import com.yy.askew.bluetooth.data.BluetoothScanState
import com.yy.askew.location.DistanceCard
import com.yy.askew.location.CompactDistanceDisplay

/**
 * 蓝牙功能主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    onBackClick: () -> Unit = {},
    viewModel: BluetoothViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val hasPermissions by viewModel.hasPermissions.collectAsState()
    val hasLocationPermissions by viewModel.hasLocationPermissions.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val distanceResult by viewModel.distanceResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 蓝牙权限请求启动器
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.updatePermissionStatus(allGranted)
    }
    
    // 位置权限请求启动器
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.updateLocationPermissionStatus(allGranted)
        if (allGranted) {
            viewModel.startLocationUpdates()
        }
    }
    
    // 蓝牙启用请求启动器
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // 蓝牙启用后重新检查权限
        val hasPerms = BluetoothPermissionHelper.hasAllPermissions(context)
        viewModel.updatePermissionStatus(hasPerms)
    }
    
    // 初始化蓝牙管理器
    DisposableEffect(context) {
        val bluetoothManager = BluetoothManager.getInstance(context)
        viewModel.initBluetooth(bluetoothManager)
        
        // 检查初始权限状态
        val hasPerms = BluetoothPermissionHelper.hasAllPermissions(context)
        viewModel.updatePermissionStatus(hasPerms)
        
        val hasLocationPerms = viewModel.hasLocationPermissions()
        viewModel.updateLocationPermissionStatus(hasLocationPerms)
        
        // 如果有位置权限，开始位置更新
        if (hasLocationPerms) {
            viewModel.startLocationUpdates()
        }
        
        onDispose { }
    }
    
    // 显示错误消息
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("蓝牙设备") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00BCD4),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (hasPermissions && BluetoothPermissionHelper.isBluetoothEnabled()) {
                FloatingActionButton(
                    onClick = {
                        when (uiState.scanState) {
                            BluetoothScanState.SCANNING -> viewModel.stopScan()
                            else -> viewModel.startScan()
                        }
                    },
                    containerColor = Color(0xFF00BCD4)
                ) {
                    Icon(
                        imageVector = when (uiState.scanState) {
                            BluetoothScanState.SCANNING -> Icons.Default.Settings
                            else -> Icons.Default.Refresh
                        },
                        contentDescription = when (uiState.scanState) {
                            BluetoothScanState.SCANNING -> "停止扫描"
                            else -> "开始扫描"
                        },
                        tint = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasPermissions) {
                // 权限请求界面
                PermissionRequestCard(
                    onRequestBluetoothPermissions = {
                        val permissions = BluetoothPermissionHelper.getRequiredPermissions()
                        bluetoothPermissionLauncher.launch(permissions)
                    },
                    onRequestLocationPermissions = {
                        val permissions = viewModel.getLocationPermissions()
                        locationPermissionLauncher.launch(permissions)
                    },
                    hasBluetoothPermissions = hasPermissions,
                    hasLocationPermissions = hasLocationPermissions
                )
            } else if (!BluetoothPermissionHelper.isBluetoothEnabled()) {
                // 蓝牙未启用界面
                BluetoothDisabledCard(
                    onEnableBluetooth = {
                        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        enableBluetoothLauncher.launch(intent)
                    }
                )
            } else {
                // 主功能界面
                BluetoothMainContent(
                    uiState = uiState,
                    distanceResult = distanceResult,
                    hasLocationPermissions = hasLocationPermissions,
                    onDeviceClick = viewModel::connectToDevice,
                    onDisconnect = viewModel::disconnect,
                    onSendMessage = viewModel::sendMessage,
                    onClearMessages = viewModel::clearMessages,
                    onRequestLocationPermissions = {
                        val permissions = viewModel.getLocationPermissions()
                        locationPermissionLauncher.launch(permissions)
                    },
                    onCalculateDistance = { viewModel.calculateDistance() }
                )
            }
        }
    }
}

@Composable
private fun PermissionRequestCard(
    onRequestBluetoothPermissions: () -> Unit,
    onRequestLocationPermissions: () -> Unit,
    hasBluetoothPermissions: Boolean,
    hasLocationPermissions: Boolean
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF00BCD4)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "需要权限",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "为了实现完整的蓝牙定位功能，需要获取以下权限：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // 蓝牙权限
            if (!hasBluetoothPermissions) {
                Button(
                    onClick = onRequestBluetoothPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00BCD4)
                    )
                ) {
                    Text("授予蓝牙权限", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // 位置权限
            if (!hasLocationPermissions) {
                Button(
                    onClick = onRequestLocationPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Text("授予位置权限（用于距离计算）", color = Color.White)
                }
            }
            
            if (hasBluetoothPermissions && hasLocationPermissions) {
                Text(
                    text = "所有权限已获取✓",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun BluetoothDisabledCard(onEnableBluetooth: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "蓝牙已关闭",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请开启蓝牙以使用此功能",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onEnableBluetooth,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00BCD4)
                )
            ) {
                Text("开启蓝牙", color = Color.White)
            }
        }
    }
}

@Composable
private fun BluetoothMainContent(
    uiState: BluetoothUiState,
    distanceResult: com.yy.askew.location.DistanceResult?,
    hasLocationPermissions: Boolean,
    onDeviceClick: (BluetoothDeviceInfo) -> Unit,
    onDisconnect: () -> Unit,
    onSendMessage: (String) -> Unit,
    onClearMessages: () -> Unit,
    onRequestLocationPermissions: () -> Unit,
    onCalculateDistance: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        // 扫描状态显示
        item {
            ScanStatusCard(uiState.scanState)
        }
        
        // 已连接设备
        if (uiState.connectedDevice != null) {
            item {
                ConnectedDeviceCard(
                    device = uiState.connectedDevice,
                    connectionState = uiState.connectionState,
                    onDisconnect = onDisconnect
                )
            }
            
            // 距离显示卡片
            item {
                DistanceCard(
                    distanceResult = distanceResult,
                    targetDeviceName = uiState.connectedDevice.name,
                    isCalculating = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 位置权限提示和距离计算按钮
            item {
                LocationPermissionCard(
                    hasLocationPermissions = hasLocationPermissions,
                    onRequestPermissions = onRequestLocationPermissions,
                    onCalculateDistance = onCalculateDistance,
                    isConnected = true
                )
            }
        }
        
        // 设备列表
        if (uiState.discoveredDevices.isNotEmpty()) {
            item {
                Text(
                    text = "发现的设备 (${uiState.discoveredDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(uiState.discoveredDevices) { device ->
                DeviceListItem(
                    device = device,
                    isConnected = device.address == uiState.connectedDevice?.address,
                    distanceResult = if (device.address == uiState.connectedDevice?.address) distanceResult else null,
                    onClick = { onDeviceClick(device) }
                )
            }
        }
        
        // 消息传输区域
        if (uiState.connectedDevice != null) {
            item {
                MessageSection(
                    messages = uiState.messages,
                    onSendMessage = onSendMessage,
                    onClearMessages = onClearMessages
                )
            }
        }
    }
}

@Composable
private fun ScanStatusCard(scanState: BluetoothScanState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (scanState) {
                BluetoothScanState.SCANNING -> Color(0xFFE3F2FD)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (scanState == BluetoothScanState.SCANNING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF00BCD4)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF00BCD4)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = when (scanState) {
                    BluetoothScanState.SCANNING -> "正在扫描蓝牙设备..."
                    BluetoothScanState.STOPPED -> "扫描已停止"
                    BluetoothScanState.IDLE -> "点击右下角按钮开始扫描"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ConnectedDeviceCard(
    device: BluetoothDeviceInfo,
    connectionState: BluetoothConnectionState,
    onDisconnect: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFFE8F5E8)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color(0xFF4CAF50)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "已连接: ${device.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = when (connectionState) {
                        BluetoothConnectionState.CONNECTING -> "连接中..."
                        BluetoothConnectionState.CONNECTED -> "已连接"
                        BluetoothConnectionState.DISCONNECTING -> "断开中..."
                        BluetoothConnectionState.DISCONNECTED -> "已断开"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            }
            
            TextButton(onClick = onDisconnect) {
                Text("断开", color = Color(0xFFD32F2F))
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: BluetoothDeviceInfo,
    isConnected: Boolean,
    distanceResult: com.yy.askew.location.DistanceResult? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) 
                Color(0xFFE8F5E8) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getDeviceIcon(device.deviceType),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFF00BCD4)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "未知设备",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (device.rssi != 0) {
                        Text(
                            text = "信号强度: ${device.rssi} dBm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // 显示距离信息（仅对已连接设备）
                    if (isConnected && distanceResult != null) {
                        CompactDistanceDisplay(
                            distanceResult = distanceResult
                        )
                    }
                }
            }
            
            if (device.isPaired) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF4CAF50), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "已配对",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageSection(
    messages: List<String>,
    onSendMessage: (String) -> Unit,
    onClearMessages: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "消息传输",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (messages.isNotEmpty()) {
                TextButton(onClick = onClearMessages) {
                    Text("清除")
                }
            }
        }
        
        // 消息显示区域
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(messages) { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 发送消息区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("输入消息") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            FloatingActionButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = Color(0xFF00BCD4)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "发送",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun LocationPermissionCard(
    hasLocationPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onCalculateDistance: () -> Unit,
    isConnected: Boolean
) {
    if (!hasLocationPermissions) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFFFFF3E0)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "位置权限",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "启用精确距离测量",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "授予位置权限以获取GPS精确距离",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Text("授予位置权限", color = Color.White)
                }
            }
        }
    } else if (isConnected) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFFF3E5F5)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "距离测量",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "计算与连接设备的精确距离",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Button(
                    onClick = onCalculateDistance,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    )
                ) {
                    Text("计算距离", color = Color.White)
                }
            }
        }
    }
}

private fun getDeviceIcon(deviceType: BluetoothDeviceType): ImageVector {
    return when (deviceType) {
        BluetoothDeviceType.PHONE -> Icons.Default.Phone
        BluetoothDeviceType.HEADSET -> Icons.Default.Phone
        BluetoothDeviceType.COMPUTER -> Icons.Default.Person
        BluetoothDeviceType.SPEAKER -> Icons.Default.Info
        BluetoothDeviceType.UNKNOWN -> Icons.Default.Info
    }
}