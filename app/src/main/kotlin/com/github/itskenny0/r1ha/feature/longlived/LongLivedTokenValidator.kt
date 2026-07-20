package com.github.itskenny0.r1ha.feature.longlived

import java.io.IOException
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal suspend fun validateLongLivedToken(
    http: OkHttpClient,
    baseUrl: String,
    token: String,
) {
    val request = Request.Builder()
        .url("${baseUrl.trimEnd('/')}/api/")
        .header("Authorization", "Bearer $token")
        .get()
        .build()
    val call = http.newCall(request)
    val response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, unconsumed, _ -> unconsumed.close() }
            }
        })
    }
    response.use {
        when {
            response.priorResponse != null ->
                throw IllegalStateException("Home Assistant przekierował żądanie API.")
            response.code == 401 || response.code == 403 ->
                throw IllegalArgumentException("Home Assistant odrzucił token.")
            !response.isSuccessful ->
                throw IllegalStateException("Home Assistant zwrócił HTTP ${response.code}.")
        }
        val message = runCatching {
            Json.parseToJsonElement(response.body?.string().orEmpty())
                .jsonObject["message"]
                ?.jsonPrimitive
                ?.content
        }.getOrNull()
        check(message == "API running.") { "Serwer nie zwrócił odpowiedzi API Home Assistant." }
    }
}
