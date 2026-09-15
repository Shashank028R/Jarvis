package com.jarvis.ai.transport

import com.jarvis.core.coroutine.DefaultDispatcherProvider
import com.jarvis.core.coroutine.DispatcherProvider
import com.jarvis.core.error.JarvisError
import com.jarvis.core.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Direct HTTP transport for development mode (V2-V14) communicating with Google's Gemini API.
 * Reads API key securely; never exposes secrets in logs, exceptions, or error messages.
 * Specified in 04_TECH_STACK.md, 13_SECURITY.md, and versions/V2_AI_CONVERSATION.md.
 */
class DirectGeminiTransport(
    private val apiKeyProvider: () -> String?,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    private val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) : AiTransport {

    constructor(
        apiKey: String,
        dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
        connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
    ) : this(apiKeyProvider = { apiKey }, dispatchers = dispatchers, connectionFactory = connectionFactory)

    override val transportName: String = "DirectGeminiTransport"

    override suspend fun execute(request: TransportRequest): Result<TransportResponse> = withContext(dispatchers.io) {
        val apiKey = apiKeyProvider()?.trim().orEmpty()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                JarvisError.Configuration(
                    "Gemini API key is not configured. Please set GEMINI_API_KEY in local.properties."
                )
            )
        }

        var connection: HttpURLConnection? = null
        try {
            // Append API key query param safely
            val separator = if (request.endpointUrl.contains("?")) "&" else "?"
            val fullUrl = "${request.endpointUrl}${separator}key=$apiKey"
            val url = URL(fullUrl)

            connection = connectionFactory(url).apply {
                requestMethod = "POST"
                connectTimeout = request.connectTimeoutMs.toInt()
                readTimeout = request.readTimeoutMs.toInt()
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                request.headers.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }
            }

            // Write request body
            connection.outputStream.use { os ->
                val input = request.bodyJson.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
                os.flush()
            }

            val statusCode = connection.responseCode
            val responseHeaders = connection.headerFields ?: emptyMap()

            if (statusCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                Result.success(TransportResponse(statusCode, responseBody, responseHeaders))
            } else if (statusCode == 429) {
                val errorBody = readStreamSafely(connection.errorStream)
                Result.failure(
                    JarvisError.RateLimited(
                        message = "Gemini API rate limit exceeded (HTTP 429). Please wait before retrying."
                    )
                )
            } else {
                val errorBody = readStreamSafely(connection.errorStream)
                val sanitizedMessage = extractErrorMessage(errorBody, apiKey) ?: "HTTP $statusCode"
                val isTransient = statusCode in 500..599
                Result.failure(
                    JarvisError.Network(
                        message = "Gemini API error ($statusCode): $sanitizedMessage",
                        code = statusCode,
                        isTransient = isTransient
                    )
                )
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(
                JarvisError.ExecutionTimeout(
                    timeoutMs = request.readTimeoutMs,
                    operation = "Gemini API Request",
                    message = "Gemini API request timed out after ${request.readTimeoutMs}ms"
                )
            )
        } catch (e: IOException) {
            Result.failure(
                JarvisError.Network(
                    message = "Network connection to Gemini API failed: ${e.message ?: "Unknown IO error"}",
                    isTransient = true
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(
                JarvisError.ExecutionFailure(
                    message = "Unexpected failure executing Gemini transport",
                    details = e.message
                )
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun readStreamSafely(stream: java.io.InputStream?): String {
        if (stream == null) return ""
        return try {
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractErrorMessage(errorJson: String, secretToSanitize: String): String? {
        if (errorJson.isBlank()) return null
        return try {
            val root = JSONObject(errorJson)
            val errorObj = root.optJSONObject("error")
            val rawMsg = errorObj?.optString("message") ?: errorJson
            // Sanitize against secret leaks
            if (secretToSanitize.isNotBlank()) {
                rawMsg.replace(secretToSanitize, "[REDACTED]")
            } else {
                rawMsg
            }
        } catch (_: Exception) {
            if (secretToSanitize.isNotBlank()) {
                errorJson.replace(secretToSanitize, "[REDACTED]")
            } else {
                errorJson
            }
        }
    }
}
