package com.jarvis.accessibility

import com.jarvis.core.error.JarvisError
import com.jarvis.core.result.Result

/**
 * Interface abstracting Accessibility Service operations, isolating system privileges.
 */
interface AccessibilityBridge {
    fun isServiceEnabled(): Boolean
    fun dismissAppToHome(): Result<Unit>
}

/**
 * Stub implementation for V1 foundation (functional service activated in V7).
 */
class DefaultAccessibilityBridge : AccessibilityBridge {
    override fun isServiceEnabled(): Boolean = false

    override fun dismissAppToHome(): Result<Unit> {
        return Result.failure(
            JarvisError.InvalidState("Accessibility Service not activated in V1 Foundation")
        )
    }
}
