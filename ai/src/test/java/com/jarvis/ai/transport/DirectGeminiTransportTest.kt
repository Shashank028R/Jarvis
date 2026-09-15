package com.jarvis.ai.transport

import com.jarvis.core.coroutine.DispatcherProvider
import com.jarvis.core.error.JarvisError
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class DirectGeminiTransportTest {

    private val testDispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Unconfined
        override val io: CoroutineDispatcher = Dispatchers.Unconfined
        override val default: CoroutineDispatcher = Dispatchers.Unconfined
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    @Test
    fun `returns configuration error when API key is blank`() = runTest {
        val transport = DirectGeminiTransport(
            apiKey = "",
            dispatchers = testDispatchers
        )

        val request = TransportRequest(
            endpointUrl = "https://example.com/test",
            bodyJson = "{}"
        )

        val result = transport.execute(request)

        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertTrue(error is JarvisError.Configuration)
        assertTrue(error?.message?.contains("not configured") == true)
    }

    @Test
    fun `successful HTTP 200 response returns success with body`() = runTest {
        val mockConnection = mockk<HttpURLConnection>(relaxed = true)
        val outputStream = ByteArrayOutputStream()
        val responseBody = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello Sir.\"}]}}]}"

        every { mockConnection.outputStream } returns outputStream
        every { mockConnection.responseCode } returns 200
        every { mockConnection.inputStream } returns ByteArrayInputStream(responseBody.toByteArray(Charsets.UTF_8))
        every { mockConnection.headerFields } returns mapOf("Content-Type" to listOf("application/json"))

        val transport = DirectGeminiTransport(
            apiKey = "test-valid-api-key",
            dispatchers = testDispatchers,
            connectionFactory = { mockConnection }
        )

        val request = TransportRequest(
            endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent",
            bodyJson = "{\"test\":true}"
        )

        val result = transport.execute(request)

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertEquals(200, response.statusCode)
        assertEquals(responseBody, response.bodyJson)
        verify { mockConnection.disconnect() }
    }

    @Test
    fun `HTTP 429 returns RateLimited error`() = runTest {
        val mockConnection = mockk<HttpURLConnection>(relaxed = true)
        every { mockConnection.outputStream } returns ByteArrayOutputStream()
        every { mockConnection.responseCode } returns 429
        every { mockConnection.errorStream } returns ByteArrayInputStream("Resource exhausted".toByteArray())

        val transport = DirectGeminiTransport(
            apiKey = "test-key",
            dispatchers = testDispatchers,
            connectionFactory = { mockConnection }
        )

        val request = TransportRequest("https://example.com", bodyJson = "{}")
        val result = transport.execute(request)

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is JarvisError.RateLimited)
    }

    @Test
    fun `HTTP 400 error sanitizes API key from error message`() = runTest {
        val mockConnection = mockk<HttpURLConnection>(relaxed = true)
        val secretKey = "SECRET_AI_KEY_12345"
        val errorJson = "{\"error\":{\"code\":400,\"message\":\"Invalid API key $secretKey provided\"}}"

        every { mockConnection.outputStream } returns ByteArrayOutputStream()
        every { mockConnection.responseCode } returns 400
        every { mockConnection.errorStream } returns ByteArrayInputStream(errorJson.toByteArray())

        val transport = DirectGeminiTransport(
            apiKey = secretKey,
            dispatchers = testDispatchers,
            connectionFactory = { mockConnection }
        )

        val request = TransportRequest("https://example.com", bodyJson = "{}")
        val result = transport.execute(request)

        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertTrue(error is JarvisError.Network)
        assertFalse("API key must NOT appear in error message", error?.message?.contains(secretKey) == true)
        assertTrue("Expected [REDACTED] in error message", error?.message?.contains("[REDACTED]") == true)
    }

    @Test
    fun `HTTP 503 server error marked as transient`() = runTest {
        val mockConnection = mockk<HttpURLConnection>(relaxed = true)
        every { mockConnection.outputStream } returns ByteArrayOutputStream()
        every { mockConnection.responseCode } returns 503
        every { mockConnection.errorStream } returns ByteArrayInputStream("Service Unavailable".toByteArray())

        val transport = DirectGeminiTransport(
            apiKey = "test-key",
            dispatchers = testDispatchers,
            connectionFactory = { mockConnection }
        )

        val result = transport.execute(TransportRequest("https://example.com", bodyJson = "{}"))

        assertTrue(result.isFailure)
        val error = result.errorOrNull() as JarvisError.Network
        assertEquals(503, error.code)
        assertTrue("5xx errors must be marked transient", error.isTransient)
    }

    @Test
    fun `SocketTimeoutException maps to ExecutionTimeout error`() = runTest {
        val mockConnection = mockk<HttpURLConnection>(relaxed = true)
        every { mockConnection.outputStream } throws SocketTimeoutException("Read timed out")

        val transport = DirectGeminiTransport(
            apiKey = "test-key",
            dispatchers = testDispatchers,
            connectionFactory = { mockConnection }
        )

        val result = transport.execute(TransportRequest("https://example.com", bodyJson = "{}", readTimeoutMs = 5000L))

        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertTrue(error is JarvisError.ExecutionTimeout)
        assertEquals(5000L, (error as JarvisError.ExecutionTimeout).timeoutMs)
    }

    @Test
    fun `IOException maps to transient Network error`() = runTest {
        val mockConnection = mockk<HttpURLConnection>(relaxed = true)
        every { mockConnection.outputStream } throws IOException("Connection reset by peer")

        val transport = DirectGeminiTransport(
            apiKey = "test-key",
            dispatchers = testDispatchers,
            connectionFactory = { mockConnection }
        )

        val result = transport.execute(TransportRequest("https://example.com", bodyJson = "{}"))

        assertTrue(result.isFailure)
        val error = result.errorOrNull()
        assertTrue(error is JarvisError.Network)
        assertTrue((error as JarvisError.Network).isTransient)
    }

    @Test
    fun `CancellationException is rethrown and not swallowed`() = runTest {
        val mockConnection = mockk<HttpURLConnection>(relaxed = true)
        every { mockConnection.outputStream } throws CancellationException("Coroutine cancelled")

        val transport = DirectGeminiTransport(
            apiKey = "test-key",
            dispatchers = testDispatchers,
            connectionFactory = { mockConnection }
        )

        try {
            transport.execute(TransportRequest("https://example.com", bodyJson = "{}"))
            fail("CancellationException should have been thrown")
        } catch (e: CancellationException) {
            assertEquals("Coroutine cancelled", e.message)
        }
    }
}
