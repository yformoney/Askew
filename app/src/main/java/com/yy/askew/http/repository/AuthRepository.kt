package com.yy.askew.http.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.yy.askew.http.api.AuthApiService
import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.model.TokenResponse
import com.yy.askew.http.model.UserInfo

class AuthRepository(private val context: Context) {
    
    private val authApiService = AuthApiService()
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    suspend fun login(username: String, password: String): ApiResult<TokenResponse> {
        val result = authApiService.login(username, password, CLIENT_ID)
        
        if (result is ApiResult.Success) {
            saveTokens(result.data)
        }
        
        return result
    }
    
    suspend fun refreshToken(): ApiResult<TokenResponse> {
        val refreshToken = getRefreshToken() ?: return ApiResult.Error(
            com.yy.askew.http.exception.NetworkException.AuthError("无有效的刷新token")
        )
        
        val result = authApiService.refreshToken(refreshToken, CLIENT_ID)
        
        if (result is ApiResult.Success) {
            saveTokens(result.data)
        } else {
            clearTokens()
        }
        
        return result
    }
    
    suspend fun getUserInfo(): ApiResult<UserInfo> {
        return authApiService.getUserInfo()
    }
    
    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }
    
    private fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }
    
    private fun saveTokens(tokenResponse: TokenResponse) {
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, tokenResponse.accessToken)
            .putString(KEY_REFRESH_TOKEN, tokenResponse.refreshToken)
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + tokenResponse.expiresIn * 1000)
            .apply()
    }
    
    fun clearTokens() {
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }
    
    fun isTokenExpired(): Boolean {
        val expiresAt = sharedPreferences.getLong(KEY_EXPIRES_AT, 0)
        return System.currentTimeMillis() > expiresAt
    }
    
    fun isLoggedIn(): Boolean {
        return getAccessToken() != null && !isTokenExpired()
    }
    
    companion object {
        private const val CLIENT_ID = "ANDROID_CLIENT_ID"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}