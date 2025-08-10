package com.yy.askew.performance

import android.os.Debug
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.askew.performance.data.PerformanceMetric
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PerformanceViewModel : ViewModel() {
    private val _metrics = mutableStateListOf(
        PerformanceMetric("CPU使用率", "--%", color = Color(0xFF4CAF50)),
        PerformanceMetric("内存占用", "--", "KB", Color(0xFF2196F3)),
        PerformanceMetric("电池温度", "--", "°C", Color(0xFFFF9800))
    )
    val metrics: List<PerformanceMetric> = _metrics

    fun refreshData() {
        viewModelScope.launch {
            while (true) {

                _metrics.replaceAll { metric ->
                    metric.copy(
                        value = when (metric.name) {
                            "CPU使用率" -> "%"
                            "内存占用" -> "${getCurrentProcessPss()}"
                            else -> metric.value
                        },
                        color = when ((0..2).random()) {
                            0 -> Color(0xFF4CAF50)
                            1 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
                delay(5000)
            }
        }
    }

    private fun getCurrentProcessPss(): Int {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo) // 填充当前进程的内存数据
        return memoryInfo.totalPss // 单位：KB
    }
}