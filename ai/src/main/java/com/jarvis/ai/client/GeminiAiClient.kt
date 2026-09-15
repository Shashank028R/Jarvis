package com.jarvis.ai.client

import com.jarvis.ai.AiMessage
import com.jarvis.ai.AiProvider
import com.jarvis.ai.AiResponse
import com.jarvis.ai.AiRole
import com.jarvis.ai.model.ConversationTurn
import com.jarvis.ai.model.PlannerContext
import com.jarvis.ai.model.PlannerDecision
import com.jarvis.ai.model.TextResponse
import com.jarvis.ai.model.ToolObservation
import com.jarvis.ai.model.TurnRole
import com.jarvis.ai.model.VerificationResult
import com.jarvis.ai.prompt.JarvisPersonality
import com.jarvis.ai.transport.AiTransport
import com.jarvis.ai.transport.TransportRequest
import com.jarvis.core.error.JarvisError
import com.jarvis.core.result.Result
import org.json.JSONArray
import org.json.JSONObject

/**
 * Concrete Gemini implementation of [AiClient] and [AiProvider].
 * Connects the conversational reasoning loop to Google's Gemini API via pluggable [AiTransport].
 * Specified in 07_AI_ARCHITECTURE.md, ADR-002, and versions/V2_AI_CONVERSATION.md.
 */
class GeminiAiClient(
    private val transport: AiTransport,
    val model: String = DEFAULT_MODEL,
    val deepThinkModel: String = DEEP_THINK_MODEL,
    val fallbackModel: String = FALLBACK_MODEL
) : AiClient, AiProvider {

    companion object {
        const val DEFAULT_MODEL = "gemini-3.6-flash"
        const val FALLBACK_MODEL = "gemini-3.5-flash"
        const val DEEP_THINK_MODEL = "gemini-3.6-flash"
        private const val API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"

        /**
         * Checks if the user explicitly requested deep thinking, high reasoning, or maximum accuracy.
         */
        fun isDeepThinkingRequested(text: String): Boolean {
            val lower = text.lowercase()
            return lower.contains("deep think") ||
                   lower.contains("deepthink") ||
                   lower.contains("think deeply") ||
                   lower.contains("deep reasoning") ||
                   lower.contains("reason deeply") ||
                   lower.contains("accurately") ||
                   lower.contains("accurate answer") ||
                   lower.contains("answer accurately") ||
                   lower.contains("give me the answer accurately") ||
                   lower.contains("higher model") ||
                   lower.contains("high model")
        }
    }

    override val providerName: String = "Gemini ($model)"

    override suspend fun converse(
        input: ConversationTurn,
        context: PlannerContext
    ): Result<TextResponse> {
        val isDeepThink = isDeepThinkingRequested(input.text)
        val activeModel = if (isDeepThink) deepThinkModel else model
        val payloadResult = buildGenerateContentPayload(input, context, isDeepThink)
        val payloadJson = payloadResult.fold(
            onSuccess = { it },
            onFailure = { return Result.failure(it) }
        )

        val endpoint = "$API_BASE/$activeModel:generateContent"
        val request = TransportRequest(
            endpointUrl = endpoint,
            bodyJson = payloadJson
        )

        val primaryResult = transport.execute(request)
        val responseToParse = if (primaryResult.isFailure) {
            val error = primaryResult.errorOrNull()
            val isTransient = (error is JarvisError.Network && error.isTransient) ||
                             (error is JarvisError.RateLimited)
            if (isTransient && activeModel != fallbackModel) {
                val fallbackEndpoint = "$API_BASE/$fallbackModel:generateContent"
                transport.execute(request.copy(endpointUrl = fallbackEndpoint))
            } else {
                primaryResult
            }
        } else {
            primaryResult
        }

        return responseToParse.flatMap { response ->
            parseGeminiResponse(response.bodyJson)
        }
    }

    override suspend fun interpret(
        input: ConversationTurn,
        context: PlannerContext
    ): Result<PlannerDecision> {
        // V2 Version boundary: conversational reply only. Tool planning arrives in V9.
        return converse(input, context).map { textResponse ->
            PlannerDecision.ConversationalReply(textResponse.text)
        }
    }

    override suspend fun verify(
        observation: ToolObservation,
        context: PlannerContext
    ): Result<VerificationResult> {
        // V2 Version boundary: stubbed verification.
        return Result.success(VerificationResult(isSuccess = true))
    }

    override suspend fun generateResponse(messages: List<AiMessage>): Result<AiResponse> {
        val userMessage = messages.lastOrNull { it.role == AiRole.USER }
            ?: return Result.failure(JarvisError.InvalidState("No user message provided to generateResponse"))

        val recentTurns = messages.dropLast(1).mapNotNull { msg ->
            when (msg.role) {
                AiRole.USER -> ConversationTurn(role = TurnRole.USER, text = msg.content)
                AiRole.ASSISTANT -> ConversationTurn(role = TurnRole.ASSISTANT, text = msg.content)
                AiRole.SYSTEM -> null
                AiRole.TOOL -> null
            }
        }

        val systemInstruction = messages.firstOrNull { it.role == AiRole.SYSTEM }?.content

        val currentTurn = ConversationTurn(role = TurnRole.USER, text = userMessage.content)
        val context = PlannerContext(
            recentTurns = recentTurns,
            systemInstruction = systemInstruction ?: JarvisPersonality.DEFAULT_SYSTEM_INSTRUCTION
        )

        return converse(currentTurn, context).map { textResponse ->
            AiResponse(content = textResponse.text)
        }
    }

    private fun buildGenerateContentPayload(
        currentInput: ConversationTurn,
        context: PlannerContext,
        isDeepThink: Boolean = false
    ): Result<String> {
        return try {
            val root = JSONObject()

            // 1. System Instruction
            val systemText = context.systemInstruction ?: JarvisPersonality.DEFAULT_SYSTEM_INSTRUCTION
            val systemParts = JSONArray().apply {
                put(JSONObject().put("text", systemText))
            }
            root.put("systemInstruction", JSONObject().put("parts", systemParts))

            // 2. Contents (Multi-turn conversation history + current input)
            val contentsArray = JSONArray()

            val allTurns = mutableListOf<ConversationTurn>()
            allTurns.addAll(context.recentTurns)
            if (allTurns.isEmpty() || allTurns.last().id != currentInput.id) {
                allTurns.add(currentInput)
            }

            for (turn in allTurns) {
                if (turn.role == TurnRole.SYSTEM) continue

                val geminiRole = when (turn.role) {
                    TurnRole.USER -> "user"
                    TurnRole.ASSISTANT -> "model"
                    TurnRole.SYSTEM -> continue
                }

                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", turn.text))
                }

                val contentObj = JSONObject().apply {
                    put("role", geminiRole)
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
            }

            root.put("contents", contentsArray)

            // 3. Generation Config
            val genConfig = JSONObject().apply {
                if (isDeepThink) {
                    // Deep Thinking mode: Lower temperature for precision and enable thinking budget
                    put("temperature", 0.4)
                    put("maxOutputTokens", 2048)
                    val thinkingConfig = JSONObject().apply {
                        put("thinkingBudget", 2048)
                    }
                    put("thinkingConfig", thinkingConfig)
                } else {
                    // Standard fast mode: default temperature and zero thinking budget for instantaneous response
                    put("temperature", 0.7)
                    put("maxOutputTokens", 1024)
                    val thinkingConfig = JSONObject().apply {
                        put("thinkingBudget", 0)
                    }
                    put("thinkingConfig", thinkingConfig)
                }
            }
            root.put("generationConfig", genConfig)

            Result.success(root.toString())
        } catch (e: Exception) {
            Result.failure(
                JarvisError.Serialization(
                    message = "Failed to serialize Gemini request payload: ${e.message}"
                )
            )
        }
    }

    private fun parseGeminiResponse(bodyJson: String): Result<TextResponse> {
        return try {
            val root = JSONObject(bodyJson)

            if (root.has("error")) {
                val errorObj = root.getJSONObject("error")
                val code = errorObj.optInt("code", 500)
                val message = errorObj.optString("message", "Unknown Gemini API error")
                return Result.failure(
                    JarvisError.Network(
                        message = "Gemini error ($code): $message",
                        code = code
                    )
                )
            }

            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                // Check if blocked by safety ratings
                val promptFeedback = root.optJSONObject("promptFeedback")
                val blockReason = promptFeedback?.optString("blockReason")
                val reasonMsg = if (!blockReason.isNullOrBlank()) {
                    "Response generation blocked by policy: $blockReason"
                } else {
                    "Gemini returned empty candidates list"
                }
                return Result.failure(JarvisError.Serialization(reasonMsg, bodyJson))
            }

            val firstCandidate = candidates.getJSONObject(0)
            val contentObj = firstCandidate.optJSONObject("content")
            val partsArray = contentObj?.optJSONArray("parts")

            if (partsArray == null || partsArray.length() == 0) {
                val finishReason = firstCandidate.optString("finishReason", "UNKNOWN")
                return Result.failure(
                    JarvisError.Serialization(
                        message = "Gemini candidate had no text parts (finishReason: $finishReason)",
                        rawPayload = bodyJson
                    )
                )
            }

            val textBuilder = StringBuilder()
            for (i in 0 until partsArray.length()) {
                val part = partsArray.getJSONObject(i)
                if (part.has("text")) {
                    textBuilder.append(part.getString("text"))
                }
            }

            val extractedText = textBuilder.toString().trim()
            if (extractedText.isEmpty()) {
                Result.failure(
                    JarvisError.Serialization(
                        message = "Gemini returned empty text content",
                        rawPayload = bodyJson
                    )
                )
            } else {
                Result.success(TextResponse(extractedText))
            }
        } catch (e: Exception) {
            Result.failure(
                JarvisError.Serialization(
                    message = "Failed to parse Gemini response JSON: ${e.message}",
                    rawPayload = bodyJson
                )
            )
        }
    }
}
