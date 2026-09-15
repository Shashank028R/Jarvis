package com.jarvis.androidintegration

import com.jarvis.core.result.Result

/**
 * System hardware and platform capabilities abstraction.
 */
data class SystemCapabilities(
    val hasTelephony: Boolean = true,
    val isNetworkAvailable: Boolean = true,
    val sdkVersion: Int = android.os.Build.VERSION.SDK_INT
)

/**
 * Adapter interface mediating interactions with the Android operating system.
 */
interface SystemAdapter {
    fun getCapabilities(): SystemCapabilities
}

/**
 * Default implementation for V1 foundation.
 */
class DefaultSystemAdapter : SystemAdapter {
    override fun getCapabilities(): SystemCapabilities = SystemCapabilities()
}
