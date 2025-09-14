package com.yy.askew.http.interceptor

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AuthInterceptorTest {

    private class FakeChain(
        private var req: Request,
        private val responseToReturn: Response
    ) : Interceptor.Chain {
        var proceededRequest: Request? = null

        override fun request(): Request = req
        override fun proceed(request: Request): Response {
            proceededRequest = request
            return responseToReturn
        }
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun connection(): Connection? = null
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
    }

    private fun baseRequest(): Request = Request.Builder().url("http://localhost/test").build()
    private fun okResponse(request: Request): Response = Response.Builder()
        .code(200)
        .protocol(Protocol.HTTP_1_1)
        .request(request)
        .message("OK")
        .body("{}".toResponseBody("application/json".toMediaType()))
        .build()

    @Test
    fun addsAuthorizationHeader_whenTokenPresent() {
        val interceptor = AuthInterceptor { "abc123" }
        val req = baseRequest()
        val fake = FakeChain(req, okResponse(req))

        val resp = interceptor.intercept(fake)
        assertEquals(200, resp.code)
        val proceeded = fake.proceededRequest!!
        assertEquals("Bearer abc123", proceeded.header("Authorization"))
    }

    @Test
    fun doesNotAddHeader_whenTokenNull() {
        val interceptor = AuthInterceptor { null }
        val req = baseRequest()
        val fake = FakeChain(req, okResponse(req))

        interceptor.intercept(fake)
        val proceeded = fake.proceededRequest!!
        assertNull(proceeded.header("Authorization"))
    }
}

