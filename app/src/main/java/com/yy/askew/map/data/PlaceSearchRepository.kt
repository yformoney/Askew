package com.yy.askew.map.data

import android.content.Context
import com.amap.api.services.core.AMapException
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.amap.api.services.help.Tip
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class PlaceSuggestion(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val district: String = ""
) {
    companion object {
        fun fromTip(tip: Tip): PlaceSuggestion? {
            return if (tip.point != null) {
                PlaceSuggestion(
                    name = tip.name ?: "",
                    address = tip.address ?: "",
                    latitude = tip.point.latitude,
                    longitude = tip.point.longitude,
                    district = tip.district ?: ""
                )
            } else null
        }
    }
}

class PlaceSearchRepository(private val context: Context) {
    
    suspend fun searchPlaces(query: String, city: String = ""): List<PlaceSuggestion> = 
        suspendCancellableCoroutine { continuation ->
            if (query.isBlank()) {
                continuation.resume(emptyList())
                return@suspendCancellableCoroutine
            }
            
            val inputQuery = InputtipsQuery(query, city)
            inputQuery.cityLimit = city.isNotEmpty()
            
            val inputTips = Inputtips(context, inputQuery)
            
            inputTips.setInputtipsListener { tipList, rCode ->
                if (rCode == AMapException.CODE_AMAP_SUCCESS) {
                    val suggestions = tipList
                        ?.mapNotNull { tip -> PlaceSuggestion.fromTip(tip) }
                        ?.take(10) // 限制结果数量
                        ?: emptyList()
                    
                    continuation.resume(suggestions)
                } else {
                    continuation.resume(emptyList())
                }
            }
            
            inputTips.requestInputtipsAsyn()
        }
    
    // 获取热门地点建议
    fun getPopularPlaces(): List<PlaceSuggestion> {
        return listOf(
            PlaceSuggestion("家", "设置常用地址", 0.0, 0.0),
            PlaceSuggestion("公司", "设置常用地址", 0.0, 0.0),
            PlaceSuggestion("机场", "", 0.0, 0.0),
            PlaceSuggestion("火车站", "", 0.0, 0.0),
            PlaceSuggestion("医院", "", 0.0, 0.0),
            PlaceSuggestion("商场", "", 0.0, 0.0)
        )
    }
    
    // 获取历史搜索
    fun getSearchHistory(): List<PlaceSuggestion> {
        // TODO: 从本地存储获取历史搜索
        return emptyList()
    }
    
    // 保存搜索历史
    fun saveSearchHistory(suggestion: PlaceSuggestion) {
        // TODO: 保存到本地存储
    }
}