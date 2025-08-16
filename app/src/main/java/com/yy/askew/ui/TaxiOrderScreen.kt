package com.yy.askew.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yy.askew.viewmodel.OrderViewModel
import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.example.AuthViewModel
import com.yy.askew.http.HttpManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxiOrderScreen(
    onNavigateBack: () -> Unit = {},
    onOrderCreated: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    orderViewModel: OrderViewModel = viewModel()
) {
    var pickupLocation by remember { mutableStateOf("") }
    var destinationLocation by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    val createOrderState by orderViewModel.createOrderState.collectAsState()
    val isLoggedIn = remember { HttpManager.getAuthRepository().isLoggedIn() }
    
    LaunchedEffect(createOrderState) {
        val state = createOrderState
        if (state is ApiResult.Success && state.data.success) {
            onOrderCreated()
            orderViewModel.clearCreateOrderState()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("创建打车订单") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isLoggedIn) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "请先登录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "登录后即可享受便捷的打车服务",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "行程信息",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = pickupLocation,
                        onValueChange = { pickupLocation = it },
                        label = { Text("起点位置") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "起点",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    
                    OutlinedTextField(
                        value = destinationLocation,
                        onValueChange = { destinationLocation = it },
                        label = { Text("终点位置") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "终点",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                    
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("备注信息（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "温馨提示",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• 请准确填写起点和终点信息",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• 司机会在约定时间内到达",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• 如有特殊要求请在备注中说明",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    if (!isLoggedIn) {
                        onNavigateToLogin()
                    } else if (pickupLocation.isNotBlank() && destinationLocation.isNotBlank()) {
                        orderViewModel.createTaxiOrder(
                            pickupLocation = pickupLocation,
                            destinationLocation = destinationLocation,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = pickupLocation.isNotBlank() && 
                         destinationLocation.isNotBlank() && 
                         createOrderState !is ApiResult.Loading
            ) {
                if (createOrderState is ApiResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (!isLoggedIn) "登录后叫车" else "确认叫车")
            }
            
            createOrderState?.let { state ->
                when (state) {
                    is ApiResult.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = state.exception.message ?: "创建订单失败",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}