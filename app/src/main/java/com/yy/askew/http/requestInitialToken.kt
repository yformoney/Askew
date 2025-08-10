package com.yy.askew.http

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// OAuthManager.kt
fun requestInitialToken(username: String, password: String) {
    val client = OkHttpClient.Builder().build()
    val formBody = FormBody.Builder()
        .add("grant_type", "password")
        .add("username", username)
        .add("password", password)
        .add("client_id", "ANDROID_CLIENT_ID") // Django 中注册的客户端 ID
        .add("scope", "read trade") // 请求所需 Scope
        .build()

    val request = Request.Builder()
        .url("https://your-django-server/o/token/")
        .post(formBody)
        .build()

    client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
            val json = JSONObject(response.body!!.string())
            val accessToken = json.getString("access_token")
            // TODO: 实现Token存储逻辑
            // saveToken(accessToken) // 存储 Token
        }
    }
}