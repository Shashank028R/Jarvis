package com.jarvis.app

import android.app.Application
import com.jarvis.app.di.AppContainer
import com.jarvis.app.di.DefaultAppContainer

/**
 * Root Application class for JARVIS Assistant.
 * Initializes foundational logging, dependency graph, and early diagnostics.
 */
class JarvisApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        container = DefaultAppContainer(this)
        container.logger.i(TAG) {
            "JARVIS Application starting. Flavor: ${BuildConfig.FLAVOR_NAME}, FullAssistant: ${BuildConfig.IS_FULL_ASSISTANT}, Version: ${BuildConfig.VERSION_NAME}"
        }

        // Initialize orchestrator for V1 Foundation
        container.orchestrator.initialize()
    }

    companion object {
        lateinit var instance: JarvisApplication
            private set
        private const val TAG = "JarvisApplication"
    }
}
