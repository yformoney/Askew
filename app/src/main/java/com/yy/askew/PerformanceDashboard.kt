package com.yy.askew

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PerformanceDashboard(viewModel: PerformanceViewModel = viewModel()) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题栏
            Text(
                text = "系统性能监控",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 数据表格
            PerformanceTable(metrics = viewModel.metrics)

            // 刷新按钮
            FilledTonalButton(
                onClick = { viewModel.refreshData() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                Spacer(Modifier.width(8.dp))
                Text("刷新数据")
            }
        }
    }
}