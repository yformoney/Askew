package com.yy.askew.http.api

import com.google.gson.Gson
import com.yy.askew.http.client.NetworkClient
import com.yy.askew.http.exception.NetworkException
import com.yy.askew.http.model.ApiResponse
import com.yy.askew.http.model.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.reflect.Type

abstract class ApiService {
    
    protected val client = NetworkClient.okHttpClient
    protected val gson = Gson()
    
    protected suspend fun <T> get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        type: Type
    ): ApiResult<T> {
        return executeRequest(type) {
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
    
    protected suspend fun <T> post(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap(),
        type: Type
    ): ApiResult<T> {
        return executeRequest(type) {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val jsonBody = if (body != null) {
                gson.toJson(body).toRequestBody(mediaType)
            } else {
                "".toRequestBody(mediaType)
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
    
    protected suspend fun <T> put(
        url: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap(),
        type: Type
    ): ApiResult<T> {
        return executeRequest(type) {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val jsonBody = if (body != null) {
                gson.toJson(body).toRequestBody(mediaType)
            } else {
                "".toRequestBody(mediaType)
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
    
    protected suspend fun <T> delete(
        url: String,
        headers: Map<String, String> = emptyMap(),
        type: Type
    ): ApiResult<T> {
        return executeRequest(type) {
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
    
    private suspend fun <T> executeRequest(
        type: Type,
        requestBuilder: () -> Request
    ): ApiResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                val request = requestBuilder()
                val response = client.newCall(request).execute()
                
                response.use { resp ->
                    val responseBody = resp.body?.string() ?: ""
                    
                    try {
                        val apiResponse: ApiResponse<T> = gson.fromJson(responseBody, type)
                        if (apiResponse.success && apiResponse.data != null) {
                            ApiResult.Success(apiResponse.data)
                        } else {
                            ApiResult.Error(NetworkException.ServerError(apiResponse.code, apiResponse.message))
                        }
                    } catch (e: Exception) {
                        val data: T = gson.fromJson(responseBody, type)
                        ApiResult.Success(data)
                    }
                }
            } catch (e: Exception) {
                ApiResult.Error(convertToNetworkException(e))
            }
        }
    }
    
    private fun convertToNetworkException(e: Exception): NetworkException {
        return when (e) {
            is NetworkException -> e
            else -> NetworkException.UnknownError(e.message ?: "未知错误", e)
        }
    }
}