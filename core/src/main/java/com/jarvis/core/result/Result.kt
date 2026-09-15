package com.jarvis.core.result

import com.jarvis.core.error.JarvisError
import kotlin.coroutines.cancellation.CancellationException

/**
 * Functional Result monad encapsulating either a successful outcome [Success] with a value of type [T],
 * or a failure outcome [Failure] carrying a domain [JarvisError] and optional root cause [Throwable].
 */
sealed class Result<out T> {

    data class Success<out T>(val value: T) : Result<T>()

    data class Failure(
        val error: JarvisError,
        val cause: Throwable? = null
    ) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun errorOrNull(): JarvisError? = when (this) {
        is Success -> null
        is Failure -> error
    }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw cause ?: IllegalStateException(error.message)
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> defaultValue
    }

    inline fun getOrElse(onFailure: (JarvisError) -> @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> onFailure(error)
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> try {
            Success(transform(value))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Failure(
                error = JarvisError.ExecutionFailure(e.message ?: "Transformation failed"),
                cause = e
            )
        }
        is Failure -> this
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> try {
            transform(value)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Failure(
                error = JarvisError.ExecutionFailure(e.message ?: "Transformation failed"),
                cause = e
            )
        }
        is Failure -> this
    }

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (JarvisError) -> R
    ): R = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(error)
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(value)
        return this
    }

    inline fun onFailure(action: (JarvisError) -> Unit): Result<T> {
        if (this is Failure) action(error)
        return this
    }

    inline fun recover(transform: (JarvisError) -> @UnsafeVariance T): Result<T> = when (this) {
        is Success -> this
        is Failure -> try {
            Success(transform(error))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Failure(
                error = JarvisError.ExecutionFailure(e.message ?: "Recovery failed"),
                cause = e
            )
        }
    }

    inline fun recoverWith(transform: (JarvisError) -> Result<@UnsafeVariance T>): Result<T> = when (this) {
        is Success -> this
        is Failure -> try {
            transform(error)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Failure(
                error = JarvisError.ExecutionFailure(e.message ?: "Recovery failed"),
                cause = e
            )
        }
    }

    companion object {
        fun <T> success(value: T): Result<T> = Success(value)

        fun <T> failure(error: JarvisError, cause: Throwable? = null): Result<T> =
            Failure(error, cause)

        inline fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Failure(
                error = JarvisError.Unknown(e.message ?: "Unexpected error", cause = e),
                cause = e
            )
        }
    }
}
