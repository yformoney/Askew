package com.yy.askew.http.client

import com.yy.askew.http.interceptor.AuthInterceptor
import com.yy.askew.http.interceptor.ResponseInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object NetworkClient {
    
    private var tokenProvider: (() -> String?)? = null
    
    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (com.yy.askew.BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(ResponseInterceptor())
            .addInterceptor(AuthInterceptor { tokenProvider?.invoke() })
            .build()
    }
}