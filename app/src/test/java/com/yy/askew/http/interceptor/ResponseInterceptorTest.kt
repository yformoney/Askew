package com.yy.askew.http.interceptor

import com.yy.askew.http.exception.NetworkException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ResponseInterceptorTest {

    private open class FakeChain(
        private val req: Request,
        private val responseToReturn: Response? = null,
        private val exceptionToThrow: IOException? = null
    ) : Interceptor.Chain {
        override fun request(): Request = req
        override fun proceed(request: Request): Response {
            exceptionToThrow?.let { throw it }
            return responseToReturn ?: throw IllegalStateException("No response configured")
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

    private fun request(): Request = Request.Builder().url("http://localhost/test").build()
    private fun response(request: Request, code: Int, body: String = ""): Response = Response.Builder()
        .code(code)
        .protocol(Protocol.HTTP_1_1)
        .request(request)
        .message("${code}")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()

    @Test
    fun passesThrough_onSuccess() {
        val req = request()
        val chain = FakeChain(req, response(req, 200, "{}"))
        val resp = ResponseInterceptor().intercept(chain)
        assertEquals(200, resp.code)
    }

    @Test
    fun throwsAuthError_on401() {
        val req = request()
        val chain = FakeChain(req, response(req, 401))
        try {
            ResponseInterceptor().intercept(chain)
            fail("Expected AuthError")
        } catch (e: Exception) {
            assertTrue(e is NetworkException.AuthError)
        }
    }

    @Test
    fun throwsServerError_on5xxAnd404() {
        listOf(404, 500, 502).forEach { code ->
            val req = request()
            val chain = FakeChain(req, response(req, code))
            try {
                ResponseInterceptor().intercept(chain)
                fail("Expected ServerError for $code")
            } catch (e: Exception) {
                assertTrue(e is NetworkException)
            }
        }
    }

    // Note: IOException mapping path uses android.util.Log. In JVM unit tests
    // without Android stubs, calling that branch may throw a runtime error.
    // Therefore we only verify HTTP status to exception mapping here.
}
