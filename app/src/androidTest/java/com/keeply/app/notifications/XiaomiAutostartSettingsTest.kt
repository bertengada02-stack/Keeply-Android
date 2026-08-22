package com.keeply.app.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XiaomiAutostartSettingsTest {
    @Test
    fun candidatesPreferAutostartThenEditorsThenAppInfo() {
        val candidates = xiaomiAutostartSettingsCandidates("com.keeply.app")

        assertEquals(
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
            candidates[0].component?.className
        )
        assertEquals(
            "com.miui.permcenter.permissions.PermissionsEditorActivity",
            candidates[1].component?.className
        )
        assertEquals(
            "com.miui.permcenter.permissions.AppPermissionsEditorActivity",
            candidates[2].component?.className
        )
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, candidates[3].action)
        assertEquals("package:com.keeply.app", candidates[3].dataString)
    }

    @Test
    fun launcherUsesFirstResolvableCandidate() {
        val candidates = xiaomiAutostartSettingsCandidates("com.keeply.app")
        var launched: Intent? = null

        val result = launchFirstAvailableAutostartSettings(
            candidates,
            canResolve = { it === candidates[1] },
            launch = { launched = it }
        )

        assertTrue(result)
        assertEquals(candidates[1], launched)
    }

    @Test
    fun launchFailureContinuesToAppInfoFallback() {
        val candidates = xiaomiAutostartSettingsCandidates("com.keeply.app")
        val launched = mutableListOf<Intent>()

        val result = launchFirstAvailableAutostartSettings(
            candidates,
            canResolve = { true },
            launch = {
                launched += it
                if (it !== candidates.last()) throw SecurityException("blocked OEM target")
            }
        )

        assertTrue(result)
        assertEquals(candidates, launched)
    }

    @Test
    fun noResolvableTargetFailsSafely() {
        var launches = 0
        val result = launchFirstAvailableAutostartSettings(
            xiaomiAutostartSettingsCandidates("com.keeply.app"),
            canResolve = { false },
            launch = { launches++ }
        )

        assertFalse(result)
        assertEquals(0, launches)
    }

    @Test
    fun acknowledgementPersistsInExistingPrivatePreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("notification_preferences", Context.MODE_PRIVATE)
            .edit().clear().commit()

        assertFalse(xiaomiAutostartGuidanceAcknowledged(context))
        acknowledgeXiaomiAutostartGuidance(context)
        assertTrue(xiaomiAutostartGuidanceAcknowledged(context))
    }
}
