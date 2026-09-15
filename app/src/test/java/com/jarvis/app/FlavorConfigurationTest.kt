package com.jarvis.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates that build flavor configuration constants are properly defined and consistent with architectural specifications.
 */
class FlavorConfigurationTest {

    @Test
    fun `verify build config contains valid flavor metadata`() {
        val flavor = BuildConfig.FLAVOR_NAME
        assertNotNull("FLAVOR_NAME must be defined in BuildConfig", flavor)
        assertTrue(
            "FLAVOR_NAME must be either playStore or fullAssistant, but was: $flavor",
            flavor == "playStore" || flavor == "fullAssistant"
        )

        if (flavor == "fullAssistant") {
            assertTrue("IS_FULL_ASSISTANT must be true for fullAssistant flavor", BuildConfig.IS_FULL_ASSISTANT)
        } else {
            assertFalse("IS_FULL_ASSISTANT must be false for playStore flavor", BuildConfig.IS_FULL_ASSISTANT)
        }
    }

    @Test
    fun `verify application versioning metadata`() {
        assertTrue("versionCode must be positive", BuildConfig.VERSION_CODE > 0)
        assertNotNull("versionName must be defined", BuildConfig.VERSION_NAME)
        assertTrue("versionName must not be empty", BuildConfig.VERSION_NAME.isNotBlank())
    }
}
