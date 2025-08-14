package com.yy.askew.http.api

import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.model.LoginRequest
import com.yy.askew.http.model.LoginResponse
import com.yy.askew.http.model.LogoutResponse
import com.yy.askew.http.model.UserInfo
import com.yy.askew.http.model.UserProfileResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AuthApiService : ApiService() {
    
    private val baseUrl = "http://8.138.38.200/api"
    
    suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        return try {
            val loginRequest = LoginRequest(username, password)
            val json = gson.toJson(loginRequest)
            val requestBody = json.toRequestBody("application/json".toMediaType())
                
            val request = okhttp3.Request.Builder()
                .url("$baseUrl/auth/login/")
                .post(requestBody)
                .build()
                
            val response = client.newCall(request).execute()
            
            response.use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                    ApiResult.Success(loginResponse)
                } else {
                    ApiResult.Error(com.yy.askew.http.exception.NetworkException.AuthError("登录失败"))
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.toNetworkException())
        }
    }
    
    suspend fun logout(): ApiResult<LogoutResponse> {
        return try {
            val request = okhttp3.Request.Builder()
                .url("$baseUrl/auth/logout/")
                .post("".toRequestBody())
                .build()
                
            val response = client.newCall(request).execute()
            
            response.use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val logoutResponse = gson.fromJson(responseBody, LogoutResponse::class.java)
                    ApiResult.Success(logoutResponse)
                } else {
                    ApiResult.Error(com.yy.askew.http.exception.NetworkException.AuthError("登出失败"))
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.toNetworkException())
        }
    }
    
    suspend fun getUserProfile(): ApiResult<UserProfileResponse> {
        return try {
            val request = okhttp3.Request.Builder()
                .url("$baseUrl/auth/profile/")
                .get()
                .build()
                
            val response = client.newCall(request).execute()
            
            response.use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val profileResponse = gson.fromJson(responseBody, UserProfileResponse::class.java)
                    ApiResult.Success(profileResponse)
                } else {
                    ApiResult.Error(com.yy.askew.http.exception.NetworkException.RequestError("获取用户信息失败"))
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