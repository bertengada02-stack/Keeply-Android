package com.keeply.app.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.keeply.app.ui.theme.KeeplyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsContentAndActionsAreAccessible() {
        val clicked = mutableListOf<String>()
        composeRule.setContent {
            KeeplyTheme {
                SettingsScreen(
                    versionName = "1.0",
                    onBack = { clicked += "back" },
                    onNotificationSettings = { clicked += "notifications" },
                    onExactReminderTiming = { clicked += "exact" },
                    onPrivacyPolicy = { clicked += "privacy" },
                    onContactSupport = { clicked += "contact" }
                )
            }
        }

        listOf(
            "Settings",
            "REMINDERS",
            "Notification settings",
            "Exact reminder timing",
            "PRIVACY & SUPPORT",
            "Privacy Policy",
            "Contact Oobert App Network",
            "ABOUT",
            "Keeply",
            "Version 1.0"
        ).forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }

        composeRule.onNodeWithContentDescription("Back").assertHasClickAction().performClick()
        composeRule.onNodeWithText("Notification settings").assertHasClickAction().performClick()
        composeRule.onNodeWithText("Exact reminder timing").assertHasClickAction().performClick()
        composeRule.onNodeWithText("Privacy Policy").assertHasClickAction().performClick()
        composeRule.onNodeWithText("Contact Oobert App Network").assertHasClickAction().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("back", "notifications", "exact", "privacy", "contact"), clicked)
        }
    }

    @Test
    fun settingsIntentsUseApprovedActionsAndUris() {
        val notification = notificationSettingsIntent("com.keeply.app")
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, notification.action)
        assertEquals(
            "com.keeply.app",
            notification.getStringExtra(Settings.EXTRA_APP_PACKAGE)
        )

        val exact = exactReminderTimingIntent("com.keeply.app", Build.VERSION_CODES.S)
        assertEquals(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, exact?.action)
        assertEquals("package:com.keeply.app", exact?.data.toString())
        assertNull(exactReminderTimingIntent("com.keeply.app", Build.VERSION_CODES.R))

        val privacy = privacyPolicyIntent()
        assertEquals(Intent.ACTION_VIEW, privacy.action)
        assertEquals(KEEPLY_PRIVACY_POLICY_URL, privacy.data.toString())

        val contact = supportEmailIntent()
        assertEquals(Intent.ACTION_SENDTO, contact.action)
        assertEquals("mailto:oobertappnetwork@gmail.com", contact.data.toString())

        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("1.0", installedVersionName(context))
    }

    @Test
    fun gearFromHomeOpensSettingsAndVisibleBackReturnsHome() {
        setKeeplyAppAndWaitForHome()

        composeRule.onNodeWithContentDescription("Open Settings")
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onAllNodesWithText("Home").assertCountEquals(0)

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Nothing to remember yet").assertIsDisplayed()
    }

    @Test
    fun gearFromMyThingsOpensSettingsAndSystemBackReturnsMyThings() {
        var dispatcher: OnBackPressedDispatcher? = null
        composeRule.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            KeeplyTheme { KeeplyApp() }
        }
        waitForGear()

        composeRule.onNodeWithText("My Things").performClick()
        composeRule.onNodeWithContentDescription("Clipboard checklist illustration").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open Settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()

        composeRule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        composeRule.onNodeWithContentDescription("Clipboard checklist illustration").assertIsDisplayed()
    }

    private fun setKeeplyAppAndWaitForHome() {
        composeRule.setContent { KeeplyTheme { KeeplyApp() } }
        waitForGear()
        composeRule.onNodeWithText("Nothing to remember yet").assertIsDisplayed()
    }

    private fun waitForGear() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithContentDescription("Open Settings")
                .fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            composeRule.onAllNodesWithContentDescription("Open Settings")
                .fetchSemanticsNodes().isNotEmpty()
        )
    }
}
