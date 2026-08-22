package com.keeply.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.keeply.app.ui.theme.KeeplyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class XiaomiAutostartGuidanceDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun approvedGuidanceCopyAndActionsAreShown() {
        var action = ""
        composeRule.setContent {
            KeeplyTheme {
                XiaomiAutostartGuidanceDialog(
                    onOpenSettings = { action = "open" },
                    onNotNow = { action = "later" }
                )
            }
        }

        composeRule.onNodeWithText("Keep reminders working after restart").assertIsDisplayed()
        composeRule.onNodeWithText(
            "On Xiaomi, Redmi, and POCO phones, enable Autostart for Keeply so your reminders can be restored after your phone restarts."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Open Autostart settings").performClick()
        composeRule.runOnIdle { assertEquals("open", action) }
    }

    @Test
    fun notNowUsesSecondaryAction() {
        var dismissed = false
        composeRule.setContent {
            KeeplyTheme {
                XiaomiAutostartGuidanceDialog({}, { dismissed = true })
            }
        }

        composeRule.onNodeWithText("Not now").performClick()
        composeRule.runOnIdle { assertEquals(true, dismissed) }
    }
}
