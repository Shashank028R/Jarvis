package com.jarvis.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jarvis.app.ui.theme.JarvisBlack
import com.jarvis.app.ui.theme.JarvisTheme

/**
 * Main activity hosting the JARVIS edge-to-edge Compose UI.
 * Configured for true AMOLED edge-to-edge without system bar scrims.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce true edge-to-edge without default navigation/status bar scrims
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        setContent {
            JarvisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = JarvisBlack
                ) {
                    JarvisFoundationScreen()
                }
            }
        }
    }
}
