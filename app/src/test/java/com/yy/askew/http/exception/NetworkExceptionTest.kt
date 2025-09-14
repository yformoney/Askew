package com.yy.askew.http.exception

import org.junit.Assert.*
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkExceptionTest {

    @Test
    fun toNetworkException_mappings() {
        assertTrue(UnknownHostException().toNetworkException() is NetworkException.NetworkError)
        assertTrue(SocketTimeoutException().toNetworkException() is NetworkException.TimeoutError)
        assertTrue(ConnectException().toNetworkException() is NetworkException.NetworkError)

        val ex = IllegalStateException("boom").toNetworkException()
        assertTrue(ex is NetworkException.UnknownError)
        assertTrue(ex.message?.contains("boom") == true)
    }

    @Test
    fun serverError_messageIncludesCode() {
        val se = NetworkException.ServerError(503, "Service Unavailable")
        assertTrue(se.message!!.contains("503"))
        assertTrue(se.message!!.contains("Service Unavailable"))
    }
}

