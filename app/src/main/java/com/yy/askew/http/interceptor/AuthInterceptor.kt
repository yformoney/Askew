package com.yy.askew.http.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val token = tokenProvider()
        
        return if (token != null) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}