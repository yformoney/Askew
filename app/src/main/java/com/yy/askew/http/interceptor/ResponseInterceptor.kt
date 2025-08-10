package com.yy.askew.http.interceptor

import android.util.Log
import com.yy.askew.http.exception.NetworkException
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class ResponseInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        return try {
            val response = chain.proceed(request)
            
            if (!response.isSuccessful) {
                when (response.code) {
                    401 -> throw NetworkException.AuthError("认证失败，请重新登录")
                    403 -> throw NetworkException.AuthError("权限不足")
                    404 -> throw NetworkException.ServerError(404, "请求的资源不存在")
                    500 -> throw NetworkException.ServerError(500, "服务器内部错误")
                    in 500..599 -> throw NetworkException.ServerError(response.code, "服务器错误")
                    else -> throw NetworkException.ServerError(response.code, "请求失败")
                }
            }
            
            response
        } catch (e: IOException) {
            Log.e("NetworkInterceptor", "网络请求异常", e)
            throw e.toNetworkException()
        }
    }
    
    private fun IOException.toNetworkException(): NetworkException {
        return when (this) {
            is java.net.SocketTimeoutException -> NetworkException.TimeoutError()
            is java.net.UnknownHostException -> NetworkException.NetworkError("无法连接到服务器")
            is java.net.ConnectException -> NetworkException.NetworkError("连接失败")
            else -> NetworkException.NetworkError("网络请求失败", this)
        }
    }
}