package com.yy.askew.http.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.yy.askew.http.api.AuthApiService
import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.model.LoginResponse
import com.yy.askew.http.model.RegisterResponse
import com.yy.askew.http.model.UserInfo
import com.yy.askew.http.model.UserProfileResponse

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
    
    suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        val result = authApiService.login(username, password)
        
        if (result is ApiResult.Success) {
            saveToken(result.data.token)
            saveUser(result.data.user)
        }
        
        return result
    }
    
    suspend fun register(
        username: String, 
        password: String, 
        passwordConfirm: String, 
        email: String
    ): ApiResult<RegisterResponse> {
        val result = authApiService.register(username, password, passwordConfirm, email)
        
        if (result is ApiResult.Success && result.data.token != null && result.data.user != null) {
            saveToken(result.data.token)
            saveUser(result.data.user)
        }
        
        return result
    }
    
    suspend fun logout(): ApiResult<*> {
        val result = authApiService.logout()
        clearTokens()
        clearUser()
        return result
    }
    
    suspend fun getUserProfile(): ApiResult<UserProfileResponse> {
        return authApiService.getUserProfile()
    }
    
    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }
    
    fun getCurrentUser(): UserInfo? {
        val userJson = sharedPreferences.getString(KEY_USER_INFO, null)
        return userJson?.let { 
            try {
                com.google.gson.Gson().fromJson(it, UserInfo::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun saveToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }
    
    private fun saveUser(user: UserInfo) {
        val userJson = com.google.gson.Gson().toJson(user)
        sharedPreferences.edit()
            .putString(KEY_USER_INFO, userJson)
            .apply()
    }
    
    fun clearTokens() {
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .apply()
    }
    
    private fun clearUser() {
        sharedPreferences.edit()
            .remove(KEY_USER_INFO)
            .apply()
    }
    
    fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }
    
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_INFO = "user_info"
    }
}