package com.yy.askew.http

import android.content.Context
import com.yy.askew.http.client.NetworkClient
import com.yy.askew.http.repository.AuthRepository
import com.yy.askew.http.repository.OrderRepository

object HttpManager {
    
    private lateinit var authRepository: AuthRepository
    private lateinit var orderRepository: OrderRepository
    private var isInitialized = false
    
    fun initialize(context: Context) {
        if (!isInitialized) {
            authRepository = AuthRepository(context)
            orderRepository = OrderRepository()
            
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
    
    fun getOrderRepository(): OrderRepository {
        checkInitialized()
        return orderRepository
    }
    
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("HttpManager must be initialized before use. Call HttpManager.initialize(context) first.")
        }
    }
}