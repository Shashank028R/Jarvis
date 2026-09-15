package com.jarvis.security

/**
 * Risk tier taxonomy matching constitutional security requirements:
 * - NORMAL: Read-only operations, zero state change (auto-approved).
 * - SENSITIVE: Modifying local state, launching standard apps (auto-approved with audit log).
 * - CRITICAL: SMS, Calling, System settings, accessibility automation (requires explicit confirmation).
 */
enum class RiskTier {
    NORMAL,
    SENSITIVE,
    CRITICAL
}

/**
 * Result of evaluating an action against the security policy engine.
 */
sealed class SecurityPolicyDecision {
    data object Allowed : SecurityPolicyDecision()

    data class Denied(
        val reason: String
    ) : SecurityPolicyDecision()

    data class ConfirmationRequired(
        val prompt: String,
        val riskTier: RiskTier
    ) : SecurityPolicyDecision()
}

/**
 * Contract for the security policy engine guarding all tool and system invocations.
 */
interface SecurityPolicyEngine {
    fun evaluate(toolId: String, riskTier: RiskTier, params: Map<String, Any?> = emptyMap()): SecurityPolicyDecision
}

/**
 * Default implementation for V1 foundation.
 */
class DefaultSecurityPolicyEngine : SecurityPolicyEngine {
    override fun evaluate(
        toolId: String,
        riskTier: RiskTier,
        params: Map<String, Any?>
    ): SecurityPolicyDecision {
        return when (riskTier) {
            RiskTier.NORMAL, RiskTier.SENSITIVE -> SecurityPolicyDecision.Allowed
            RiskTier.CRITICAL -> SecurityPolicyDecision.ConfirmationRequired(
                prompt = "Confirm execution of critical action: $toolId",
                riskTier = riskTier
            )
        }
    }
}
