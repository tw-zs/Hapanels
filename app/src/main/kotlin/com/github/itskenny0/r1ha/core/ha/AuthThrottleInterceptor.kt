package com.github.itskenny0.r1ha.core.ha

import java.util.concurrent.Semaphore
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Gates Home Assistant REST API requests behind [AuthThrottle]. Auth and
 * WebSocket endpoints are exempt so refresh/reconnect paths can still recover.
 */
class AuthThrottleInterceptor(
    private val throttle: AuthThrottle,
    maxConcurrentGated: Int = 2,
) : Interceptor {
    private val gate = Semaphore(maxConcurrentGated.coerceAtLeast(1), true)

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath
        val gated = path.startsWith("/api/") && path != "/api/websocket"
        if (!gated) return chain.proceed(req)

        if (throttle.shouldShortCircuit()) return throttled(req)
        gate.acquire()
        try {
            if (throttle.isOpenNow()) return throttled(req)
            val resp = chain.proceed(req)
            when {
                resp.code == 401 -> throttle.recordAuthFailure()
                resp.isSuccessful -> throttle.recordSuccess()
            }
            return resp
        } finally {
            gate.release()
        }
    }

    private fun throttled(req: Request): Response =
        Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(503)
            .message("auth-throttled")
            .body("".toResponseBody(null))
            .build()
}
