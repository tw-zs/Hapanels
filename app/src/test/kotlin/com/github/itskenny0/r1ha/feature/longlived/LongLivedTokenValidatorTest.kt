package com.github.itskenny0.r1ha.feature.longlived

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class LongLivedTokenValidatorTest {
    @Test fun acceptsAuthenticatedApiResponse() = runTest {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(200).setBody("{\"message\":\"API running.\"}"))
            start()
        }
        try {
            validateLongLivedToken(OkHttpClient(), server.url("/").toString(), "secret-token")
            val request = server.takeRequest()
            assertThat(request.path).isEqualTo("/api/")
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer secret-token")
        } finally {
            server.shutdown()
        }
    }

    @Test fun rejectsUnauthorizedToken() = runTest {
        val server = MockWebServer().apply { enqueue(MockResponse().setResponseCode(401)); start() }
        try {
            val result = runCatching {
                validateLongLivedToken(OkHttpClient(), server.url("/").toString(), "wrong-token")
            }
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        } finally {
            server.shutdown()
        }
    }

    @Test fun reportsNetworkFailure() = runTest {
        val server = MockWebServer().apply { start() }
        val url = server.url("/").toString()
        server.shutdown()

        val result = runCatching {
            validateLongLivedToken(OkHttpClient(), url, "secret-token")
        }

        assertThat(result.isFailure).isTrue()
    }

    @Test fun rejectsNonHomeAssistantResponse() = runTest {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(200).setBody("<html>login</html>"))
            start()
        }
        try {
            val result = runCatching {
                validateLongLivedToken(OkHttpClient(), server.url("/").toString(), "secret-token")
            }
            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        } finally {
            server.shutdown()
        }
    }
}
