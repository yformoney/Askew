package com.yy.askew.http.model

import com.google.gson.annotations.SerializedName

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Throwable) : ApiResult<Nothing>()
    data class Loading(val isLoading: Boolean = true) : ApiResult<Nothing>()
}

data class ApiResponse<T>(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: T?,
    @SerializedName("success")
    val success: Boolean = code == 200
)

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("scope")
    val scope: String?
)

data class UserInfo(
    @SerializedName("id")
    val id: Long,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("avatar")
    val avatar: String?,
    @SerializedName("date_joined")
    val dateJoined: String?
)

data class LoginRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("user")
    val user: UserInfo
)

data class LogoutResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)

data class UserProfileResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("user")
    val user: UserInfo
)

data class OrderItem(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("product_name")
    val productName: String,
    @SerializedName("product_sku")
    val productSku: String?,
    @SerializedName("product_image")
    val productImage: String?,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("unit_price")
    val unitPrice: String,
    @SerializedName("total_price")
    val totalPrice: String?
)

data class OrderLog(
    @SerializedName("id")
    val id: Long,
    @SerializedName("action")
    val action: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("operator_name")
    val operatorName: String,
    @SerializedName("created_at")
    val createdAt: String
)

data class Order(
    @SerializedName("id")
    val id: String,
    @SerializedName("order_number")
    val orderNumber: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("status_display")
    val statusDisplay: String,
    @SerializedName("user")
    val userId: Long,
    @SerializedName("user_name")
    val userName: String,
    @SerializedName("receiver_name")
    val receiverName: String,
    @SerializedName("receiver_phone")
    val receiverPhone: String,
    @SerializedName("receiver_address")
    val receiverAddress: String,
    @SerializedName("total_amount")
    val totalAmount: String,
    @SerializedName("discount_amount")
    val discountAmount: String?,
    @SerializedName("shipping_fee")
    val shippingFee: String?,
    @SerializedName("final_amount")
    val finalAmount: String,
    @SerializedName("notes")
    val notes: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("paid_at")
    val paidAt: String?,
    @SerializedName("shipped_at")
    val shippedAt: String?,
    @SerializedName("delivered_at")
    val deliveredAt: String?,
    @SerializedName("items")
    val items: List<OrderItem>?,
    @SerializedName("logs")
    val logs: List<OrderLog>?
)

data class CreateOrderRequest(
    @SerializedName("receiver_name")
    val receiverName: String,
    @SerializedName("receiver_phone")
    val receiverPhone: String,
    @SerializedName("receiver_address")
    val receiverAddress: String,
    @SerializedName("notes")
    val notes: String?,
    @SerializedName("items")
    val items: List<OrderItem>
)

data class CreateOrderResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: Order?
)

data class OrderListResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: List<Order>
)

data class OrderDetailResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: Order?
)