package com.jarvis.voice.aec

import android.media.audiofx.AcousticEchoCanceler
import com.jarvis.core.error.JarvisError
import com.jarvis.core.logger.JarvisLogger
import com.jarvis.core.result.Result

/**
 * Interface controlling Acoustic Echo Cancellation (AEC) lifecycle and audio session binding.
 */
interface AecManager {
    val isHardwareAecAvailable: Boolean
    val isAttached: Boolean
    fun attachToAudioSession(audioSessionId: Int): Result<Unit>
    fun release()
}

/**
 * Android implementation of [AecManager] interfacing with [android.media.audiofx.AcousticEchoCanceler].
 * Degrades gracefully when hardware AEC is unsupported on the host device.
 */
class DefaultAecManager(
    private val logger: JarvisLogger? = null
) : AecManager {

    private var echoCanceler: AcousticEchoCanceler? = null
    private var attachedSessionId: Int? = null

    override val isHardwareAecAvailable: Boolean
        get() = try {
            AcousticEchoCanceler.isAvailable()
        } catch (e: Throwable) {
            logger?.w("AecManager", { "Failed to query AcousticEchoCanceler.isAvailable(): ${e.message}" })
            false
        }

    override val isAttached: Boolean
        get() = echoCanceler != null

    override fun attachToAudioSession(audioSessionId: Int): Result<Unit> {
        if (!isHardwareAecAvailable) {
            logger?.i("AecManager") { "Hardware AEC not available on this device; degrading to software suppression" }
            return Result.success(Unit)
        }

        return try {
            release()
            val aec = AcousticEchoCanceler.create(audioSessionId)
            if (aec != null) {
                aec.enabled = true
                echoCanceler = aec
                attachedSessionId = audioSessionId
                logger?.i("AecManager") { "AcousticEchoCanceler attached and enabled for session $audioSessionId" }
                Result.success(Unit)
            } else {
                logger?.w("AecManager", { "AcousticEchoCanceler.create returned null for session $audioSessionId" })
                Result.failure(JarvisError.ExecutionFailure("Failed to initialize AcousticEchoCanceler for session $audioSessionId"))
            }
        } catch (e: Throwable) {
            logger?.e("AecManager", { "Exception attaching AcousticEchoCanceler: ${e.message}" }, e)
            Result.failure(JarvisError.Unknown("Exception attaching AEC: ${e.message}", cause = e))
        }
    }

    override fun release() {
        try {
            echoCanceler?.enabled = false
            echoCanceler?.release()
        } catch (e: Throwable) {
            logger?.w("AecManager", { "Error releasing AcousticEchoCanceler: ${e.message}" })
        } finally {
            echoCanceler = null
            attachedSessionId = null
        }
    }
}
