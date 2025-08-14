package com.yy.askew.http.repository

import com.yy.askew.http.api.OrderApiService
import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.model.CreateOrderRequest
import com.yy.askew.http.model.CreateOrderResponse
import com.yy.askew.http.model.OrderListResponse
import com.yy.askew.http.model.OrderDetailResponse

class OrderRepository {
    
    private val orderApiService = OrderApiService()
    
    suspend fun createOrder(createOrderRequest: CreateOrderRequest): ApiResult<CreateOrderResponse> {
        return orderApiService.createOrder(createOrderRequest)
    }
    
    suspend fun getOrderList(
        status: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        orderNumber: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): ApiResult<OrderListResponse> {
        return orderApiService.getOrderList(status, startDate, endDate, orderNumber, page, pageSize)
    }
    
    suspend fun getOrderDetail(orderId: String): ApiResult<OrderDetailResponse> {
        return orderApiService.getOrderDetail(orderId)
    }
    
    suspend fun cancelOrder(orderId: String): ApiResult<OrderDetailResponse> {
        return orderApiService.cancelOrder(orderId)
    }
}