package com.yy.askew.http.api

import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.model.TokenResponse
import com.yy.askew.http.model.UserInfo
import okhttp3.FormBody

class AuthApiService : ApiService() {
    
    private val baseUrl = "https://your-django-server/o"
    
    suspend fun login(username: String, password: String, clientId: String): ApiResult<TokenResponse> {
        return try {
            val formBody = FormBody.Builder()
                .add("grant_type", "password")
                .add("username", username)
                .add("password", password)
                .add("client_id", clientId)
                .add("scope", "read trade")
                .build()
                
            val request = okhttp3.Request.Builder()
                .url("$baseUrl/token/")
                .post(formBody)
                .build()
                
            val response = client.newCall(request).execute()
            
            response.use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val tokenResponse = gson.fromJson(responseBody, TokenResponse::class.java)
                    ApiResult.Success(tokenResponse)
                } else {
                    ApiResult.Error(com.yy.askew.http.exception.NetworkException.AuthError("登录失败"))
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.toNetworkException())
        }
    }
    
    suspend fun refreshToken(refreshToken: String, clientId: String): ApiResult<TokenResponse> {
        return try {
            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", clientId)
                .build()
                
            val request = okhttp3.Request.Builder()
                .url("$baseUrl/token/")
                .post(formBody)
                .build()
                
            val response = client.newCall(request).execute()
            
            response.use { resp ->
                if (resp.isSuccessful) {
                    val responseBody = resp.body?.string() ?: ""
                    val tokenResponse = gson.fromJson(responseBody, TokenResponse::class.java)
                    ApiResult.Success(tokenResponse)
                } else {
                    ApiResult.Error(com.yy.askew.http.exception.NetworkException.AuthError("刷新token失败"))
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.toNetworkException())
        }
    }
    
    suspend fun getUserInfo(): ApiResult<UserInfo> {
        return get("$baseUrl/user/info/", type = object : com.google.gson.reflect.TypeToken<com.yy.askew.http.model.ApiResponse<UserInfo>>() {}.type)
    }
    
    private fun Exception.toNetworkException(): com.yy.askew.http.exception.NetworkException {
        return when (this) {
            is com.yy.askew.http.exception.NetworkException -> this
            else -> com.yy.askew.http.exception.NetworkException.UnknownError(this.message ?: "未知错误", this)
        }
    }
}