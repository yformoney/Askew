package com.yy.askew.http.api

import com.google.gson.reflect.TypeToken
import com.yy.askew.http.model.ApiResult
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Type

class ApiServiceTest {

    data class TestData(val name: String, val value: Int)

    private class TestApi : ApiService() {
        suspend fun <T> doGet(url: String, type: Type) = get<T>(url, emptyMap(), type)
        suspend fun <T> doPost(url: String, body: Any?, type: Type) = post<T>(url, body, emptyMap(), type)
    }

    private lateinit var server: MockWebServer
    private lateinit var baseUrl: HttpUrl
    private val api = TestApi()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        baseUrl = server.url("/")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun parsesWrappedApiResponse_success() = runBlocking {
        val json = """
            {"code":200,"message":"ok","data":{"name":"foo","value":42},"success":true}
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val type: Type = object : TypeToken<com.yy.askew.http.model.ApiResponse<TestData>>() {}.type
        val result = api.doGet<TestData>(baseUrl.toString(), type)
        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals("foo", data.name)
        assertEquals(42, data.value)
    }

    @Test
    fun fallsBackToRawType_whenNotWrapped() = runBlocking {
        val json = """
            {"name":"bar","value":7}
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        // Pass T's type so the first parse as ApiResponse<T> fails and fallback to T succeeds
        val type: Type = object : TypeToken<TestData>() {}.type
        val result = api.doGet<TestData>(baseUrl.toString(), type)
        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals("bar", data.name)
        assertEquals(7, data.value)
    }

    @Test
    fun returnsError_whenWrappedIndicatesFailure() = runBlocking {
        val json = """
            {"code":400,"message":"bad","data":null,"success":false}
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(400).setBody(json))

        val type: Type = object : TypeToken<com.yy.askew.http.model.ApiResponse<TestData>>() {}.type
        val result = api.doGet<TestData>(baseUrl.toString(), type)
        assertTrue(result is ApiResult.Error)
    }
}

