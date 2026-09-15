package com.jarvis.core.result

import com.jarvis.core.error.JarvisError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit test suite providing complete semantic coverage of the [Result] contract:
 * - Success contract: creation, value extraction, predicates, callbacks.
 * - Failure contract: creation, error extraction, predicates, callbacks.
 * - Transformation contract: map, flatMap, and exception boundary wrapping.
 * - Fold contract: exhaustive branching for success and failure cases.
 * - Recovery contract: recover, recoverWith, fallback handling, and recovery chain failure.
 * - Error propagation contract: ensuring domain-specific errors traverse multi-stage pipelines unmodified.
 */
class ResultTest {

    // =========================================================================
    // 1. SUCCESS CONTRACT
    // =========================================================================

    @Test
    fun `success contract - creation, predicates, and value access`() {
        val result: Result<String> = Result.success("JARVIS_READY")

        assertTrue("isSuccess should be true for Success", result.isSuccess)
        assertFalse("isFailure should be false for Success", result.isFailure)
        assertEquals("JARVIS_READY", result.getOrNull())
        assertNull("errorOrNull should be null for Success", result.errorOrNull())
        assertEquals("JARVIS_READY", result.getOrThrow())
        assertEquals("JARVIS_READY", result.getOrDefault("FALLBACK"))

        var fallbackEvaluated = false
        val extracted = result.getOrElse {
            fallbackEvaluated = true
            "FALLBACK"
        }
        assertEquals("JARVIS_READY", extracted)
        assertFalse("getOrElse should not evaluate fallback on Success", fallbackEvaluated)
    }

    @Test
    fun `success contract - callbacks execution`() {
        val result = Result.success(42)
        var successReceived = 0
        var failureInvoked = false

        result
            .onSuccess { successReceived = it }
            .onFailure { failureInvoked = true }

        assertEquals(42, successReceived)
        assertFalse("onFailure must not be called on Success", failureInvoked)
    }

    // =========================================================================
    // 2. FAILURE CONTRACT
    // =========================================================================

    @Test
    fun `failure contract - creation, predicates, and error access`() {
        val rootCause = IllegalArgumentException("Root cause details")
        val domainError = JarvisError.PolicyViolation("Restricted action", riskTier = "CRITICAL")
        val result: Result<String> = Result.failure(domainError, rootCause)

        assertFalse("isSuccess should be false for Failure", result.isSuccess)
        assertTrue("isFailure should be true for Failure", result.isFailure)
        assertNull("getOrNull should be null for Failure", result.getOrNull())
        assertEquals(domainError, result.errorOrNull())
        assertEquals("FALLBACK", result.getOrDefault("FALLBACK"))

        var fallbackErrorReceived: JarvisError? = null
        val fallbackValue = result.getOrElse { err ->
            fallbackErrorReceived = err
            "RECOVERED_VALUE"
        }
        assertEquals("RECOVERED_VALUE", fallbackValue)
        assertEquals(domainError, fallbackErrorReceived)

        try {
            result.getOrThrow()
            fail("getOrThrow must throw on Failure")
        } catch (e: IllegalArgumentException) {
            assertEquals("Root cause details", e.message)
        }
    }

    @Test
    fun `failure contract - callbacks execution`() {
        val domainError = JarvisError.Network("DNS resolution failed", code = 503)
        val result: Result<Int> = Result.failure(domainError)
        var successInvoked = false
        var capturedError: JarvisError? = null

        result
            .onSuccess { successInvoked = true }
            .onFailure { capturedError = it }

        assertFalse("onSuccess must not be called on Failure", successInvoked)
        assertEquals(domainError, capturedError)
    }

    // =========================================================================
    // 3. MAP & FLATMAP CONTRACT
    // =========================================================================

    @Test
    fun `map contract - transforms value on success`() {
        val initial = Result.success(10)
        val transformed = initial.map { it * 2 }

        assertTrue(transformed.isSuccess)
        assertEquals(20, transformed.getOrNull())
    }

    @Test
    fun `map contract - skips transformation and retains error on failure`() {
        val error = JarvisError.InvalidState("Uninitialized")
        val initial: Result<Int> = Result.failure(error)

        var mapExecuted = false
        val transformed = initial.map {
            mapExecuted = true
            it * 2
        }

        assertFalse("map must not execute closure on failure", mapExecuted)
        assertTrue(transformed.isFailure)
        assertEquals(error, transformed.errorOrNull())
    }

    @Test
    fun `map contract - wraps thrown exception into execution failure`() {
        val initial = Result.success("valid")
        val mapped = initial.map {
            throw RuntimeException("Calculation overflow")
        }

        assertTrue("Throwing in map must produce a Failure", mapped.isFailure)
        val error = mapped.errorOrNull()
        assertTrue(error is JarvisError.ExecutionFailure)
        assertEquals("Calculation overflow", (error as JarvisError.ExecutionFailure).message)
    }

    @Test
    fun `flatMap contract - chains successive operations cleanly`() {
        fun divide(numerator: Int, denominator: Int): Result<Int> =
            if (denominator == 0) {
                Result.failure(JarvisError.ExecutionFailure("Division by zero"))
            } else {
                Result.success(numerator / denominator)
            }

        val successChain = Result.success(100)
            .flatMap { divide(it, 5) }
            .flatMap { divide(it, 2) }

        assertTrue(successChain.isSuccess)
        assertEquals(10, successChain.getOrNull())

        val failureChain = Result.success(100)
            .flatMap { divide(it, 0) }
            .flatMap { divide(it, 2) }

        assertTrue(failureChain.isFailure)
        val err = failureChain.errorOrNull()
        assertTrue(err is JarvisError.ExecutionFailure)
        assertEquals("Division by zero", err?.message)
    }

    // =========================================================================
    // 4. FOLD CONTRACT
    // =========================================================================

    @Test
    fun `fold contract - executes success branch for success`() {
        val result = Result.success("OK")
        val folded = result.fold(
            onSuccess = { "STATUS: $it" },
            onFailure = { "ERROR: ${it.message}" }
        )
        assertEquals("STATUS: OK", folded)
    }

    @Test
    fun `fold contract - executes failure branch for failure`() {
        val result: Result<String> = Result.failure(JarvisError.NotFound("ToolId:Weather"))
        val folded = result.fold(
            onSuccess = { "STATUS: $it" },
            onFailure = { "ERROR: ${it.message}" }
        )
        assertEquals("ERROR: Item not found: ToolId:Weather", folded)
    }

    // =========================================================================
    // 5. RECOVERY CONTRACT
    // =========================================================================

    @Test
    fun `recover contract - leaves success untouched and recovers failure`() {
        val success = Result.success("INITIAL")
        val recoveredSuccess = success.recover { "FALLBACK" }
        assertEquals("INITIAL", recoveredSuccess.getOrNull())

        val failure: Result<String> = Result.failure(JarvisError.Network("Offline", isTransient = true))
        val recoveredFailure = failure.recover { "FALLBACK_CACHED_DATA" }
        assertTrue(recoveredFailure.isSuccess)
        assertEquals("FALLBACK_CACHED_DATA", recoveredFailure.getOrNull())
    }

    @Test
    fun `recoverWith contract - allows conditional recovery chain`() {
        val transientError: Result<String> = Result.failure(JarvisError.Network("Gateway 504", isTransient = true))
        val recovered = transientError.recoverWith { err ->
            if (err is JarvisError.Network && err.isTransient) {
                Result.success("TRANSIENT_RETRY_OK")
            } else {
                Result.failure(err)
            }
        }
        assertTrue(recovered.isSuccess)
        assertEquals("TRANSIENT_RETRY_OK", recovered.getOrNull())

        val permanentError: Result<String> = Result.failure(JarvisError.Permission("android.permission.RECORD_AUDIO"))
        val unrecovered = permanentError.recoverWith { err ->
            if (err is JarvisError.Network && err.isTransient) {
                Result.success("RETRY_OK")
            } else {
                Result.failure(err)
            }
        }
        assertTrue(unrecovered.isFailure)
        assertEquals(permanentError.errorOrNull(), unrecovered.errorOrNull())
    }

    // =========================================================================
    // 6. ERROR PROPAGATION CONTRACT
    // =========================================================================

    @Test
    fun `error propagation contract - preserves error hierarchy and fields across pipeline`() {
        val originalError = JarvisError.ExecutionTimeout(timeoutMs = 5000L, operation = "ToolDispatch")
        val initial: Result<Int> = Result.failure(originalError)
        val pipelineResult: Result<Int> = initial
            .map { it + 1 }
            .flatMap { Result.success(it * 2) }
            .map { it - 5 }

        assertTrue(pipelineResult.isFailure)
        val propagatedError = pipelineResult.errorOrNull()
        assertNotNull(propagatedError)
        assertTrue("Error type must remain ExecutionTimeout", propagatedError is JarvisError.ExecutionTimeout)
        val timeoutError = propagatedError as JarvisError.ExecutionTimeout
        assertEquals(5000L, timeoutError.timeoutMs)
        assertEquals("ToolDispatch", timeoutError.operation)
    }

    // =========================================================================
    // 7. RUN CATCHING CONTRACT
    // =========================================================================

    @Test
    fun `runCatching contract - wraps success and failure cleanly`() {
        val success = Result.runCatching { 2 + 2 }
        assertTrue(success.isSuccess)
        assertEquals(4, success.getOrNull())

        val failure = Result.runCatching {
            check(false) { "Invariant violated" }
        }
        assertTrue(failure.isFailure)
        val error = failure.errorOrNull()
        assertTrue(error is JarvisError.Unknown)
        assertEquals("Invariant violated", error?.message)
    }
}
