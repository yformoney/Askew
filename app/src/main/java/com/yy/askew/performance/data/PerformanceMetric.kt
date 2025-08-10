package com.yy.askew.performance.data

import androidx.compose.ui.graphics.Color


data class PerformanceMetric(
    val name: String,      // 指标名称（如CPU、内存）
    val value: String,      // 指标值（如"25%"）
    val unit: String = "",  // 单位（如"MB"）
    val color: Color = Color.Unspecified // 根据数值动态设置颜色
)