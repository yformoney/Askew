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
            
            // 先尝试从模拟数据中匹配（用于测试）
            val mockResults = getMockSearchResults(query)
            if (mockResults.isNotEmpty()) {
                continuation.resume(mockResults)
                return@suspendCancellableCoroutine
            }
            
            val inputQuery = InputtipsQuery(query, city.ifEmpty { "广州市" })
            inputQuery.cityLimit = true
            
            val inputTips = Inputtips(context, inputQuery)
            
            inputTips.setInputtipsListener { tipList, rCode ->
                if (rCode == AMapException.CODE_AMAP_SUCCESS) {
                    val suggestions = tipList
                        ?.mapNotNull { tip -> PlaceSuggestion.fromTip(tip) }
                        ?.take(10) // 限制结果数量
                        ?: emptyList()
                    
                    continuation.resume(suggestions)
                } else {
                    // 如果API搜索失败，返回模拟数据
                    continuation.resume(getMockSearchResults(query))
                }
            }
            
            inputTips.requestInputtipsAsyn()
        }
    
    // 模拟搜索数据（用于测试和演示）
    private fun getMockSearchResults(query: String): List<PlaceSuggestion> {
        val mockData = mapOf(
            "广州南" to listOf(
                PlaceSuggestion("广州南站", "广州市番禺区石壁街道南站北路", 23.026772, 113.272530, "番禺区"),
                PlaceSuggestion("广州南站地铁站", "广州地铁2号线/7号线", 23.026772, 113.272530, "番禺区"),
                PlaceSuggestion("广州南站西广场", "广州市番禺区", 23.025550, 113.271200, "番禺区")
            ),
            "天河" to listOf(
                PlaceSuggestion("天河城", "广州市天河区天河路208号", 23.135542, 113.324520, "天河区"),
                PlaceSuggestion("天河体育中心", "广州市天河区体育西路", 23.137133, 113.324684, "天河区"),
                PlaceSuggestion("天河客运站", "广州市天河区燕岭路633号", 23.157028, 113.366837, "天河区")
            ),
            "珠江新城" to listOf(
                PlaceSuggestion("珠江新城", "广州市天河区", 23.119495, 113.321509, "天河区"),
                PlaceSuggestion("花城广场", "广州市天河区华夏路", 23.119495, 113.324520, "天河区"),
                PlaceSuggestion("广州塔", "广州市海珠区阅江西路222号", 23.105472, 113.324520, "海珠区")
            ),
            "白云机场" to listOf(
                PlaceSuggestion("广州白云国际机场", "广州市白云区机场路", 23.392436, 113.298786, "白云区"),
                PlaceSuggestion("白云机场T1航站楼", "广州白云国际机场1号航站楼", 23.392436, 113.298786, "白云区"),
                PlaceSuggestion("白云机场T2航站楼", "广州白云国际机场2号航站楼", 23.392436, 113.298786, "白云区")
            ),
            "北京路" to listOf(
                PlaceSuggestion("北京路步行街", "广州市越秀区北京路", 23.129110, 113.264385, "越秀区"),
                PlaceSuggestion("北京路地铁站", "广州地铁6号线", 23.129110, 113.264385, "越秀区")
            )
        )
        
        return mockData.entries
            .filter { it.key.contains(query, ignoreCase = true) || query.contains(it.key, ignoreCase = true) }
            .flatMap { it.value }
            .take(10)
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