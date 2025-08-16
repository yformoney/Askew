package com.yy.askew

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yy.askew.http.example.AuthViewModel
import com.yy.askew.ui.TaxiOrderScreen
import com.yy.askew.ui.OrderListScreen
import com.yy.askew.http.example.LoginScreen
import com.yy.askew.http.example.RegisterScreen
import com.yy.askew.http.HttpManager
import com.yy.askew.map.MapComponent
import com.yy.askew.map.MapViewModel


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen() {
    val pagerState = rememberPagerState {
        3
    }
    HorizontalPager(state = pagerState) {
        Text(modifier = Modifier.fillMaxSize(), text = "当前页面是$it")
        when (it) {
            0 -> {}
            1 -> {}
            2 -> {}
        }
    }
}


// 3. 主屏幕组件
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        NavHostContainer(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// 4. 底部导航栏
@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(Screen.Home, Screen.Search, Screen.Profile)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    // 优化导航：避免重复点击重建页面
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

// 5. 导航容器和页面内容
@Composable
fun NavHostContainer(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) { 
            HomePage(navController)
        }
        composable(Screen.Search.route) { SearchPage(navController) }
        composable(Screen.Profile.route) { ProfilePage(navController) }
        composable("taxi_order") { 
            TaxiOrderScreen(
                onNavigateBack = { navController.popBackStack() },
                onOrderCreated = { 
                    navController.navigate(Screen.Search.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login")
                }
            )
        }
        composable("order_list") {
            OrderListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate("login")
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.popBackStack()
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.popBackStack()
                }
            )
        }
        composable("place_search") {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Screen.Home.route)
            }
            val parentMapViewModel: MapViewModel = viewModel(parentEntry)
            
            com.yy.askew.map.PlaceSearchScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onPlaceSelected = { suggestion ->
                    // 设置选中的地点为终点
                    parentMapViewModel.updateEndLocation(
                        suggestion.latitude,
                        suggestion.longitude,
                        suggestion.address.ifEmpty { suggestion.name }
                    )
                    navController.popBackStack()
                }
            )
        }
    }
}

// 6. 页面内容实现
@Composable
fun HomePage(navController: NavController? = null) {
    val isLoggedIn = remember { 
        try {
            HttpManager.getAuthRepository().isLoggedIn()
        } catch (e: Exception) {
            false
        }
    }
    
    val mapViewModel: MapViewModel = viewModel()
    val mapState by mapViewModel.mapState.collectAsState()
    
    // 添加模拟定位功能（用于测试）
    LaunchedEffect(Unit) {
        // 如果没有用户位置，使用广州的坐标作为模拟位置
        if (mapState.userLocation == null) {
            mapViewModel.setSimulatedLocation(
                latitude = 23.129110,
                longitude = 113.264385,
                address = "广州市天河区"
            )
        }
    }
    
    // 全屏地图布局
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 全屏地图作为背景
        MapComponent(
            modifier = Modifier.fillMaxSize(),
            mapViewModel = mapViewModel,
            onLocationSelected = { latitude, longitude, address ->
                mapViewModel.updateEndLocation(latitude, longitude, address)
            }
        )
        
        // 顶部搜索栏
        TopSearchBar(
            mapState = mapState,
            mapViewModel = mapViewModel,
            onSearchClick = {
                navController?.navigate("place_search")
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )
        
        // 底部打车操作栏
        BottomTaxiCard(
            mapState = mapState,
            isLoggedIn = isLoggedIn,
            onTaxiClick = {
                when {
                    !isLoggedIn -> navController?.navigate("login")
                    mapState.startLocation == null -> {
                        mapViewModel.setStartLocationAsCurrentLocation()
                    }
                    mapState.endLocation == null -> {
                        // 提示用户选择目的地
                    }
                    else -> navController?.navigate("taxi_order")
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
        
        // 右侧悬浮按钮
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 当前位置按钮
            FloatingActionButton(
                onClick = { mapViewModel.getCurrentLocation() },
                modifier = Modifier.size(48.dp),
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "当前位置"
                )
            }
        }
    }
}

@Composable
fun ServiceTypeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) 
                color.copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) color else color.copy(alpha = 0.6f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickServiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SearchPage(navController: NavController? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "我的订单",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "订单",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "暂无订单",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "您还没有创建任何打车订单\n立即去叫车吧！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                FilledTonalButton(
                    onClick = {
                        navController?.navigate(Screen.Home.route)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("去叫车")
                }
            }
        }
    }
}

@Composable
fun ProfilePage(navController: NavController? = null) {
    val authViewModel: AuthViewModel = viewModel()
    var isLoggedIn by remember { mutableStateOf(authViewModel.isLoggedIn()) }
    var currentUser by remember { mutableStateOf(authViewModel.getCurrentUser()) }
    
    // 监听登录状态变化
    val loginState by authViewModel.loginState.collectAsState()
    val registerState by authViewModel.registerState.collectAsState()
    val logoutState by authViewModel.logoutState.collectAsState()
    
    LaunchedEffect(loginState, registerState, logoutState) {
        isLoggedIn = authViewModel.isLoggedIn()
        currentUser = authViewModel.getCurrentUser()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        // 顶部用户信息区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "用户头像",
                        modifier = Modifier.size(60.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isLoggedIn) {
                        currentUser?.username ?: "用户"
                    } else {
                        "未登录"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                if (isLoggedIn) {
                    val user = currentUser
                    if (user != null) {
                        Text(
                            text = user.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    
                    // 用户统计信息
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        UserStatCard(
                            title = "总行程",
                            value = "0",
                            icon = Icons.Default.List
                        )
                        UserStatCard(
                            title = "积分",
                            value = "0",
                            icon = Icons.Default.Star
                        )
                        UserStatCard(
                            title = "优惠券",
                            value = "0",
                            icon = Icons.Default.AccountCircle
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        // 登录/登出操作区域
        if (!isLoggedIn) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "登录后享受更多服务",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "查看订单历史、管理常用地址、获取专属优惠",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Button(
                        onClick = { 
                            navController?.navigate("login")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "立即登录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // 已登录用户的操作区域
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    FilledTonalButton(
                        onClick = { 
                            authViewModel.logout()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("退出登录")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        // 功能菜单区域
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                ProfileMenuItem(
                    icon = Icons.Default.List,
                    text = "我的订单",
                    subtitle = "查看历史出行记录",
                    onClick = { 
                        navController?.navigate("order_list")
                    }
                )
                
                ProfileMenuItem(
                    icon = Icons.Default.Star,
                    text = "我的收藏",
                    subtitle = "常用地址和路线",
                    onClick = { /* TODO */ }
                )
                
                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    text = "设置",
                    subtitle = "个性化偏好设置",
                    onClick = { /* TODO */ }
                )
                
                ProfileMenuItem(
                    icon = Icons.Default.Info,
                    text = "帮助与反馈",
                    subtitle = "常见问题和意见反馈",
                    onClick = { /* TODO */ }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // 底部间距
    }
}

@Composable
fun UserStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    Color.White.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "进入",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 顶部搜索栏组件
@Composable
fun TopSearchBar(
    mapState: com.yy.askew.map.data.MapState,
    mapViewModel: MapViewModel,
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 起点输入框
            OutlinedTextField(
                value = mapState.startLocation?.address ?: mapState.userLocation?.address ?: "",
                onValueChange = { },
                label = { Text("您在哪里？", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("点击获取当前位置") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        mapViewModel.setStartLocationAsCurrentLocation()
                    },
                enabled = false,
                leadingIcon = { 
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "当前位置",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                mapViewModel.setStartLocationAsCurrentLocation()
                            }
                    )
                },
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 终点输入框
            OutlinedTextField(
                value = mapState.endLocation?.address ?: "",
                onValueChange = { },
                label = { Text("要去哪里？", style = MaterialTheme.typography.bodySmall) },
                placeholder = { Text("搜索目的地") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSearchClick() },
                enabled = false,
                leadingIcon = { 
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.error,
                                CircleShape
                            )
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            // 路线信息（如果有的话）
            mapState.routeInfo?.let { routeInfo ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${String.format("%.1f", routeInfo.distance / 1000.0)}公里",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${routeInfo.duration / 60}分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "¥${String.format("%.2f", routeInfo.cost)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 底部打车操作栏
@Composable
fun BottomTaxiCard(
    mapState: com.yy.askew.map.data.MapState,
    isLoggedIn: Boolean,
    onTaxiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canOrderTaxi = isLoggedIn && 
        mapState.startLocation != null && 
        mapState.endLocation != null
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 服务类型选择
            if (mapState.routeInfo != null) {
                Text(
                    text = "选择车型",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ServiceTypeCard(
                        icon = Icons.Default.Add,
                        title = "快车",
                        subtitle = "¥${String.format("%.2f", mapState.routeInfo.cost)}",
                        color = MaterialTheme.colorScheme.primary,
                        isSelected = true,
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    )
                    ServiceTypeCard(
                        icon = Icons.Default.Person,
                        title = "专车",
                        subtitle = "¥${String.format("%.2f", mapState.routeInfo.cost * 1.5)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        isSelected = false,
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    )
                    ServiceTypeCard(
                        icon = Icons.Default.Star,
                        title = "豪华车",
                        subtitle = "¥${String.format("%.2f", mapState.routeInfo.cost * 2.0)}",
                        color = MaterialTheme.colorScheme.secondary,
                        isSelected = false,
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 叫车按钮
            Button(
                onClick = onTaxiClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = canOrderTaxi,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        !isLoggedIn -> MaterialTheme.colorScheme.secondary
                        canOrderTaxi -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "叫车",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            !isLoggedIn -> "登录后叫车"
                            mapState.startLocation == null -> "获取当前位置"
                            mapState.endLocation == null -> "选择目的地"
                            else -> "立即叫车"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

