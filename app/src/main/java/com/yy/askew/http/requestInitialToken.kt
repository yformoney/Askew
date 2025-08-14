package com.yy.askew.http

import com.yy.askew.http.model.ApiResult
import com.yy.askew.http.model.LoginResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 使用新HTTP模块的示例函数
suspend fun loginUser(username: String, password: String): ApiResult<LoginResponse> {
    val authRepository = HttpManager.getAuthRepository()
    return authRepository.login(username, password)
}

// 在协程中使用的示例
fun loginUserAsync(
    username: String, 
    password: String,
    onResult: (ApiResult<LoginResponse>) -> Unit
) {
    CoroutineScope(Dispatchers.Main).launch {
        val result = loginUser(username, password)
        onResult(result)
    }
}