package com.jarvis.core.error

/**
 * Domain-safe error taxonomy for the JARVIS system.
 * Sealed class hierarchy guarantees exhaustive handling across all modules.
 */
sealed class JarvisError(open val message: String) {

    data class Network(
        override val message: String,
        val code: Int? = null,
        val isTransient: Boolean = false
    ) : JarvisError(message)

    data class Permission(
        val permission: String,
        val rationaleRequired: Boolean = false,
        override val message: String = "Required permission denied: $permission"
    ) : JarvisError(message)

    data class PolicyViolation(
        val reason: String,
        val riskTier: String = "HIGH",
        override val message: String = "Policy violation ($riskTier): $reason"
    ) : JarvisError(message)

    data class ExecutionTimeout(
        val timeoutMs: Long,
        val operation: String,
        override val message: String = "Operation '$operation' timed out after ${timeoutMs}ms"
    ) : JarvisError(message)

    data class ExecutionFailure(
        override val message: String,
        val details: String? = null
    ) : JarvisError(message)

    data class InvalidState(
        override val message: String
    ) : JarvisError(message)

    data class NotFound(
        val item: String,
        override val message: String = "Item not found: $item"
    ) : JarvisError(message)

    data class Configuration(
        val reason: String,
        override val message: String = "Configuration error: $reason"
    ) : JarvisError(message)

    data class RateLimited(
        val retryAfterSeconds: Long? = null,
        override val message: String = "Request rate limit exceeded"
    ) : JarvisError(message)

    data class Serialization(
        override val message: String,
        val rawPayload: String? = null
    ) : JarvisError(message)

    data class Unknown(
        override val message: String,
        val cause: Throwable? = null
    ) : JarvisError(message)
}

