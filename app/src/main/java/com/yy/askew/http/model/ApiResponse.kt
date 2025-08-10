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
    @SerializedName("avatar")
    val avatar: String?
)