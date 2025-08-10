package com.yy.askew.http.api

import com.google.gson.Gson
import com.yy.askew.http.client.NetworkClient
import com.yy.askew.http.exception.NetworkException
import com.yy.askew.http.model.ApiResponse
import com.yy.askew.http.model.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.reflect.Type

abstract class ApiService {
    
    protected val client = NetworkClient.okHttpClient
    protected val gson = Gson()
    
    protected suspend inline fun <reified T> get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ApiResult<T> {
        return executeRequest<T> {
            Request.Builder()
                .url(url)
                .apply {
                    headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .build()
        }
    }
    
    protected suspend inline fun <reified T> post(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): ApiResult<T> {
        return executeRequest<T> {
            val jsonBody = if (body != null) {
                gson.toJson(body).toRequestBody(MEDIA_TYPE_JSON)
            } else {
                "".toRequestBody(MEDIA_TYPE_JSON)
            }
            
            Request.Builder()
                .url(url)
                .post(jsonBody)
                .apply {
                    headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .build()
        }
    }
    
    protected suspend inline fun <reified T> put(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap()
    ): ApiResult<T> {
        return executeRequest<T> {
            val jsonBody = if (body != null) {
                gson.toJson(body).toRequestBody(MEDIA_TYPE_JSON)
            } else {
                "".toRequestBody(MEDIA_TYPE_JSON)
            }
            
            Request.Builder()
                .url(url)
                .put(jsonBody)
                .apply {
                    headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .build()
        }
    }
    
    protected suspend inline fun <reified T> delete(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ApiResult<T> {
        return executeRequest<T> {
            Request.Builder()
                .url(url)
                .delete()
                .apply {
                    headers.forEach { (key, value) ->
                        addHeader(key, value)
                    }
                }
                .build()
        }
    }
    
    protected suspend inline fun <reified T> executeRequest(
        crossinline requestBuilder: () -> Request
    ): ApiResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                val request = requestBuilder()
                val response = client.newCall(request).execute()
                
                response.use { resp ->
                    val responseBody = resp.body?.string() ?: ""
                    
                    try {
                        val apiResponse: ApiResponse<T> = gson.fromJson(responseBody, getType<T>())
                        if (apiResponse.success && apiResponse.data != null) {
                            ApiResult.Success(apiResponse.data)
                        } else {
                            ApiResult.Error(NetworkException.ServerError(apiResponse.code, apiResponse.message))
                        }
                    } catch (e: Exception) {
                        val data: T = gson.fromJson(responseBody, getType<T>())
                        ApiResult.Success(data)
                    }
                }
            } catch (e: Exception) {
                ApiResult.Error(e.toNetworkException())
            }
        }
    }
    
    protected inline fun <reified T> getType(): Type {
        return object : com.google.gson.reflect.TypeToken<ApiResponse<T>>() {}.type
    }
    
    companion object {
        private val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toRequestBody().contentType()
    }
    
    private fun Exception.toNetworkException(): NetworkException {
        return when (this) {
            is NetworkException -> this
            else -> NetworkException.UnknownError(this.message ?: "未知错误", this)
        }
    }
}