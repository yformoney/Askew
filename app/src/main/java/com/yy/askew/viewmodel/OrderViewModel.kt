package com.yy.askew.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.askew.http.HttpManager
import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.model.CreateOrderRequest
import com.yy.askew.http.model.CreateOrderResponse
import com.yy.askew.http.model.Order
import com.yy.askew.http.model.OrderItem
import com.yy.askew.http.model.OrderListResponse
import com.yy.askew.http.model.OrderDetailResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {
    
    private val orderRepository = HttpManager.getOrderRepository()
    
    private val _createOrderState = MutableStateFlow<ApiResult<CreateOrderResponse>?>(null)
    val createOrderState: StateFlow<ApiResult<CreateOrderResponse>?> = _createOrderState
    
    private val _orderListState = MutableStateFlow<ApiResult<OrderListResponse>?>(null)
    val orderListState: StateFlow<ApiResult<OrderListResponse>?> = _orderListState
    
    private val _orderDetailState = MutableStateFlow<ApiResult<OrderDetailResponse>?>(null)
    val orderDetailState: StateFlow<ApiResult<OrderDetailResponse>?> = _orderDetailState
    
    private val _cancelOrderState = MutableStateFlow<ApiResult<OrderDetailResponse>?>(null)
    val cancelOrderState: StateFlow<ApiResult<OrderDetailResponse>?> = _cancelOrderState
    
    fun createTaxiOrder(
        pickupLocation: String,
        destinationLocation: String,
        notes: String? = null
    ) {
        val orderItem = OrderItem(
            id = null,
            productName = "打车服务",
            productSku = "TAXI_SERVICE",
            productImage = null,
            quantity = 1,
            unitPrice = "0.00",
            totalPrice = "0.00"
        )
        
        val createOrderRequest = CreateOrderRequest(
            receiverName = "乘客",
            receiverPhone = "待确定",
            receiverAddress = destinationLocation,
            notes = "起点：$pickupLocation\n终点：$destinationLocation${if (notes != null) "\n备注：$notes" else ""}",
            items = listOf(orderItem)
        )
        
        viewModelScope.launch {
            _createOrderState.value = ApiResult.Loading()
            val result = orderRepository.createOrder(createOrderRequest)
            _createOrderState.value = result
        }
    }
    
    fun getOrderList(
        status: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ) {
        viewModelScope.launch {
            _orderListState.value = ApiResult.Loading()
            val result = orderRepository.getOrderList(
                status = status,
                page = page,
                pageSize = pageSize
            )
            _orderListState.value = result
        }
    }
    
    fun getOrderDetail(orderId: String) {
        viewModelScope.launch {
            _orderDetailState.value = ApiResult.Loading()
            val result = orderRepository.getOrderDetail(orderId)
            _orderDetailState.value = result
        }
    }
    
    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            _cancelOrderState.value = ApiResult.Loading()
            val result = orderRepository.cancelOrder(orderId)
            _cancelOrderState.value = result
        }
    }
    
    fun clearCreateOrderState() {
        _createOrderState.value = null
    }
    
    fun clearCancelOrderState() {
        _cancelOrderState.value = null
    }
}