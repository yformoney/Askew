package com.yy.askew.http.api

import android.util.Log
import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.model.LoginRequest
import com.yy.askew.http.model.LoginResponse
import com.yy.askew.http.model.LogoutResponse
import com.yy.askew.http.model.UserInfo
import com.yy.askew.http.model.UserProfileResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AuthApiService : ApiService() {
    
    private val baseUrl = "http://8.138.38.200/api"
    
    suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthApiService", "开始登录请求: username=$username")
                
                val loginRequest = LoginRequest(username, password)
                val json = gson.toJson(loginRequest)
                Log.d("AuthApiService", "登录请求JSON: $json")
                
                val requestBody = json.toRequestBody("application/json".toMediaType())
                
                val request = okhttp3.Request.Builder()
                    .url("$baseUrl/auth/login/")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .build()
                
                Log.d("AuthApiService", "发送请求到: ${request.url}")
                
                val response = client.newCall(request).execute()
                
                response.use { resp ->
                    val responseBody = resp.body?.string() ?: ""
                    Log.d("AuthApiService", "响应状态码: ${resp.code}")
                    Log.d("AuthApiService", "响应内容: $responseBody")
                    
                    if (resp.isSuccessful) {
                        try {
                            val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                            if (loginResponse.success) {
                                ApiResult.Success(loginResponse)
                            } else {
                                ApiResult.Error(com.yy.askew.http.exception.NetworkException.AuthError(loginResponse.message))
                            }
                        } catch (e: Exception) {
                            Log.e("AuthApiService", "解析登录响应失败", e)
                            ApiResult.Error(com.yy.askew.http.exception.NetworkException.UnknownError("响应解析失败: ${e.message}", e))
                        }
                    } else {
                        // 尝试解析错误响应
                        try {
                            val errorResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                            ApiResult.Error(com.yy.askew.http.exception.NetworkException.AuthError(errorResponse.message ?: "登录失败"))
                        } catch (e: Exception) {
                            ApiResult.Error(com.yy.askew.http.exception.NetworkException.AuthError("登录失败: HTTP ${resp.code}"))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthApiService", "登录请求异常", e)
                ApiResult.Error(e.toNetworkException())
            }
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
            is UnknownHostException -> com.yy.askew.http.exception.NetworkException.NetworkError("网络连接失败，请检查网络设置")
            is SocketTimeoutException -> com.yy.askew.http.exception.NetworkException.NetworkError("请求超时，请稍后重试")
            is IOException -> com.yy.askew.http.exception.NetworkException.NetworkError("网络请求失败: ${this.message}")
            else -> {
                Log.e("AuthApiService", "未知异常", this)
                com.yy.askew.http.exception.NetworkException.UnknownError(this.message ?: "未知错误: ${this.javaClass.simpleName}", this)
            }
        }
    }
}