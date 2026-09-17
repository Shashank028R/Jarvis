package com.jarvis.assistant.util

import android.content.Context
import com.jarvis.assistant.JarvisApplication
import java.io.InputStream
import java.util.Properties

/**
 * Utility to load variables from SharedPreferences and assets/env.properties file.
 */
object EnvLoader {
    private var cachedApiKey: String? = null
    private var cachedYoutubeKey: String? = null
    fun resetCache() {
        cachedApiKey = null
        cachedYoutubeKey = null
    }

    fun getApiKey(context: Context): String {
        cachedApiKey?.takeIf { it.isNotBlank() }?.let { return it }

        // 1. Check local env.properties first (direct developer/user key)
        val envKey = try {
            val properties = Properties()
            properties.load(context.assets.open("env.properties"))
            val key = properties.getProperty("GEMINI_API_KEY")?.trim() ?: ""
            if (key.isNotEmpty() && !key.contains("YOUR_GEMINI_API_KEY")) key else ""
        } catch (e: Exception) {
            ""
        }

        if (envKey.isNotEmpty()) {
            cachedApiKey = envKey
            try {
                context.getSharedPreferences(JarvisApplication.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString("api_key", envKey).apply()
            } catch (e: Exception) {}
            return envKey
        }

        // 2. Fallback to user-saved SharedPreferences key
        val prefsKey = context.getSharedPreferences(JarvisApplication.PREFS_NAME, Context.MODE_PRIVATE)
            .getString("api_key", "")?.trim() ?: ""
        if (prefsKey.isNotEmpty()) {
            cachedApiKey = prefsKey
            return prefsKey
        }

        return ""
    }

    fun getYoutubeApiKey(context: Context): String {
        cachedYoutubeKey?.let { return it }

        // 1. Check Firebase Remote Config backend first for secure key
        val firebaseKey = com.jarvis.assistant.firebase.FirebaseManager.getYoutubeApiKey()
        if (firebaseKey.isNotBlank()) {
            cachedYoutubeKey = firebaseKey
            return firebaseKey
        }

        // 2. Fallback to local env.properties if Firebase key is not configured
        return try {
            val properties = Properties()
            properties.load(context.assets.open("env.properties"))
            val key = properties.getProperty("YOUTUBE_API_KEY")?.trim() ?: ""
            if (key.isNotEmpty() && !key.contains("YOUR_YOUTUBE_DATA_API_KEY")) {
                cachedYoutubeKey = key
                key
            } else {
                ""
            }
        } catch (e: Exception) {
            android.util.Log.e("EnvLoader", "Failed to load YouTube API key from env.properties", e)
            ""
        }
    }

    fun getOpenRouterApiKey(context: Context): String {
        return try {
            val properties = Properties()
            properties.load(context.assets.open("env.properties"))
            val key = properties.getProperty("OPENROUTER_API_KEY")?.trim() ?: ""
            if (key.isNotEmpty() && !key.contains("YOUR_OPENROUTER_API_KEY")) {
                key
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}