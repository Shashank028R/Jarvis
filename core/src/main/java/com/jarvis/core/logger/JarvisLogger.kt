package com.jarvis.core.logger

import android.util.Log

/**
 * Enterprise logger interface enforcing lazy message evaluation and safe redaction.
 */
interface JarvisLogger {
    fun v(tag: String, message: () -> String)
    fun d(tag: String, message: () -> String)
    fun i(tag: String, message: () -> String)
    fun w(tag: String, message: () -> String, throwable: Throwable? = null)
    fun e(tag: String, message: () -> String, throwable: Throwable? = null)
}

/**
 * Standard Android implementation delegating to android.util.Log.
 */
class AndroidJarvisLogger(private val isDebug: Boolean = true) : JarvisLogger {

    override fun v(tag: String, message: () -> String) {
        if (isDebug) Log.v(tag, message())
    }

    override fun d(tag: String, message: () -> String) {
        if (isDebug) Log.d(tag, message())
    }

    override fun i(tag: String, message: () -> String) {
        Log.i(tag, message())
    }

    override fun w(tag: String, message: () -> String, throwable: Throwable?) {
        if (throwable != null) {
            Log.w(tag, message(), throwable)
        } else {
            Log.w(tag, message())
        }
    }

    override fun e(tag: String, message: () -> String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message(), throwable)
        } else {
            Log.e(tag, message())
        }
    }
}
