package com.yy.askew.http

import android.content.Context
import com.yy.askew.http.client.NetworkClient
import com.yy.askew.http.repository.AuthRepository

object HttpManager {
    
    private lateinit var authRepository: AuthRepository
    private var isInitialized = false
    
    fun initialize(context: Context) {
        if (!isInitialized) {
            authRepository = AuthRepository(context)
            
            NetworkClient.setTokenProvider {
                authRepository.getAccessToken()
            }
            
            isInitialized = true
        }
    }
    
    fun getAuthRepository(): AuthRepository {
        checkInitialized()
        return authRepository
    }
    
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("HttpManager must be initialized before use. Call HttpManager.initialize(context) first.")
        }
    }
}