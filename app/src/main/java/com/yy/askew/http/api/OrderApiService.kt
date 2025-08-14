package com.yy.askew.http.api

import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.model.CreateOrderRequest
import com.yy.askew.http.model.CreateOrderResponse
import com.yy.askew.http.model.OrderListResponse
import com.yy.askew.http.model.OrderDetailResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class OrderApiService : ApiService() {
    
    private val baseUrl = "http://8.138.38.200/api"
    
    suspend fun createOrder(createOrderRequest: CreateOrderRequest): ApiResult<CreateOrderResponse> {
        return try {
            val json = gson.toJson(createOrderRequest)
            val requestBody = json.toRequestBody("application/json".toMediaType())
                
            val request = okhttp3.Request.Builder()
                .url("$baseUrl/orders/")
                .post(requestBody)
                .build()
                
            val response = client.newCall(request).execute()
            
            response.use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val createOrderResponse = gson.fromJson(responseBody, CreateOrderResponse::class.java)
                    ApiResult.Success(createOrderResponse)
                } else {
                    ApiResult.Error(com.yy.askew.http.exception.NetworkException.RequestError("创建订单失败"))
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.toNetworkException())
        }
    }
    
    suspend fun getOrderList(
        status: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        orderNumber: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): ApiResult<OrderListResponse> {
        return try {
            val urlBuilder = StringBuilder("$baseUrl/orders/")
            val queryParams = mutableListOf<String>()
            
            status?.let { queryParams.add("status=$it") }
            startDate?.let { queryParams.add("start_date=$it") }
            endDate?.let { queryParams.add("end_date=$it") }
            orderNumber?.let { queryParams.add("order_number=$it") }
            page?.let { queryParams.add("page=$it") }
            pageSize?.let { queryParams.add("page_size=$it") }
            
            if (queryParams.isNotEmpty()) {
                urlBuilder.append("?").append(queryParams.joinToString("&"))
            }
            
            val request = okhttp3.Request.Builder()
                .url(urlBuilder.toString())
                .get()
                .build()
                
            val response = client.newCall(request).execute()
            
            response.use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val orderListResponse = gson.fromJson(responseBody, OrderListResponse::class.java)
                    ApiResult.Success(orderListResponse)
                } else {
                    ApiResult.Error(com.yy.askew.http.exception.NetworkException.RequestError("获取订单列表失败"))
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.toNetworkException())
        }
    }
    
    suspend fun getOrderDetail(orderId: String): ApiResult<OrderDetailResponse> {
        return try {
            val request = okhttp3.Request.Builder()
                .url("$baseUrl/orders/$orderId/")
                .get()
                .build()
                
            val response = client.newCall(request).execute()
            
            response.use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val orderDetailResponse = gson.fromJson(responseBody, OrderDetailResponse::class.java)
                    ApiResult.Success(orderDetailResponse)
                } else {
                    ApiResult.Error(com.yy.askew.http.exception.NetworkException.RequestError("获取订单详情失败"))
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.toNetworkException())
        }
    }
    
    suspend fun cancelOrder(orderId: String): ApiResult<OrderDetailResponse> {
        return try {
            val request = okhttp3.Request.Builder()
                .url("$baseUrl/orders/$orderId/cancel/")
                .post("".toRequestBody())
                .build()
                
            val response = client.newCall(request).execute()
            
            response.use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val orderDetailResponse = gson.fromJson(responseBody, OrderDetailResponse::class.java)
                    ApiResult.Success(orderDetailResponse)
                } else {
                    ApiResult.Error(com.yy.askew.http.exception.NetworkException.RequestError("取消订单失败"))
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.toNetworkException())
        }
    }
    
    private fun Exception.toNetworkException(): com.yy.askew.http.exception.NetworkException {
        return when (this) {
            is com.yy.askew.http.exception.NetworkException -> this
            else -> com.yy.askew.http.exception.NetworkException.UnknownError(this.message ?: "未知错误", this)
        }
    }
}