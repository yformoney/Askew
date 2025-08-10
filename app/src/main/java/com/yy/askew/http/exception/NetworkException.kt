package com.yy.askew.http.exception

sealed class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    class NetworkError(message: String = "网络连接失败", cause: Throwable? = null) : NetworkException(message, cause)
    
    class TimeoutError(message: String = "请求超时", cause: Throwable? = null) : NetworkException(message, cause)
    
    class ServerError(val code: Int, message: String = "服务器错误", cause: Throwable? = null) : NetworkException("[$code] $message", cause)
    
    class AuthError(message: String = "认证失败", cause: Throwable? = null) : NetworkException(message, cause)
    
    class ParseError(message: String = "数据解析失败", cause: Throwable? = null) : NetworkException(message, cause)
    
    class UnknownError(message: String = "未知错误", cause: Throwable? = null) : NetworkException(message, cause)
}

fun Throwable.toNetworkException(): NetworkException {
    return when (this) {
        is NetworkException -> this
        is java.net.SocketTimeoutException -> NetworkException.TimeoutError(cause = this)
        is java.net.UnknownHostException -> NetworkException.NetworkError("无法连接到服务器", this)
        is java.net.ConnectException -> NetworkException.NetworkError("连接失败", this)
        else -> NetworkException.UnknownError(this.message ?: "未知错误", this)
    }
}