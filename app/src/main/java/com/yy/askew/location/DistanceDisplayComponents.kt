package com.yy.askew.location

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 距离显示卡片组件
 */
@Composable
fun DistanceCard(
    distanceResult: DistanceResult?,
    targetDeviceName: String? = null,
    isCalculating: Boolean = false,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                distanceResult?.isValid() == true -> Color(0xFFE8F5E8)
                isCalculating -> Color(0xFFFFF3E0)
                else -> Color(0xFFFFF0F0)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "距离",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF00BCD4)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "设备距离",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                if (targetDeviceName != null) {
                    Text(
                        text = " · $targetDeviceName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when {
                isCalculating -> {
                    // 计算中状态
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp,
                        color = Color(0xFF00BCD4)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "正在计算距离...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF757575)
                    )
                }
                
                distanceResult?.isValid() == true -> {
                    // 显示距离结果
                    DistanceDisplay(distanceResult)
                }
                
                else -> {
                    // 无法计算状态
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "提示",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF757575)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "暂无距离数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = "请确保GPS和蓝牙已开启",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 距离结果显示组件
 */
@Composable
private fun DistanceDisplay(distanceResult: DistanceResult) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 主要距离显示
        Text(
            text = distanceResult.getFormattedDistance(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32),
            fontSize = 32.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 精度指示器
        AccuracyIndicator(distanceResult.accuracy)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 计算方法和描述
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.7f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "计算方式",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = distanceResult.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF424242)
                )
            }
        }
    }
}

/**
 * 精度指示器组件
 */
@Composable
private fun AccuracyIndicator(accuracy: EstimatedAccuracy) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 精度点指示器
        repeat(3) { index ->
            val isActive = when (accuracy) {
                EstimatedAccuracy.HIGH -> index < 3
                EstimatedAccuracy.MEDIUM -> index < 2
                EstimatedAccuracy.LOW -> index < 1
                EstimatedAccuracy.UNKNOWN -> false
            }
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
                    )
            )
            
            if (index < 2) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = accuracy.description,
            style = MaterialTheme.typography.bodySmall,
            color = when (accuracy) {
                EstimatedAccuracy.HIGH -> Color(0xFF4CAF50)
                EstimatedAccuracy.MEDIUM -> Color(0xFFFF9800)
                EstimatedAccuracy.LOW -> Color(0xFFF44336)
                EstimatedAccuracy.UNKNOWN -> Color(0xFF757575)
            }
        )
    }
}

/**
 * 紧凑版距离显示组件（用于列表项）
 */
@Composable
fun CompactDistanceDisplay(
    distanceResult: DistanceResult?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = when {
            distanceResult?.isValid() == true -> Color(0xFFE8F5E8)
            else -> Color(0xFFF5F5F5)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "距离",
                modifier = Modifier.size(16.dp),
                tint = if (distanceResult?.isValid() == true) Color(0xFF4CAF50) else Color(0xFF757575)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Text(
                text = distanceResult?.getFormattedDistance() ?: "未知",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (distanceResult?.isValid() == true) Color(0xFF2E7D32) else Color(0xFF757575)
            )
        }
    }
}

/**
 * 距离历史记录组件
 */
@Composable
fun DistanceHistory(
    distances: List<Pair<Long, DistanceResult>>, // timestamp, distance
    modifier: Modifier = Modifier
) {
    if (distances.isEmpty()) return
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "距离变化",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            distances.takeLast(5).forEach { (timestamp, distance) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )
                    
                    Text(
                        text = distance.getFormattedDistance(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "刚才"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        else -> "${diff / (60 * 60 * 1000)}小时前"
    }
}