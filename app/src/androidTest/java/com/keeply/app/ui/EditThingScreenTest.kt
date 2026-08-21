package com.keeply.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.ui.theme.KeeplyTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditThingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun failedUpdatePreservesEnteredFormValues() {
        var updateError by mutableStateOf<String?>(null)
        composeRule.setContent {
            KeeplyTheme {
                EditThingScreen(
                    thing = thing(),
                    onBack = {},
                    onSaveChanges = {
                        updateError = "Keeply couldn't update this yet. Please try again."
                    },
                    onUnchangedSave = {},
                    isUpdating = false,
                    updateError = updateError
                )
            }
        }

        composeRule.onNode(hasText("Passport") and hasSetTextAction())
            .performTextReplacement("Passport 2026")
        composeRule.onNodeWithText("Save changes").performClick()

        composeRule.onNode(hasText("Passport 2026") and hasSetTextAction())
            .fetchSemanticsNode()
        composeRule.onNodeWithText("Keeply couldn't update this yet. Please try again.")
            .assertTextEquals("Keeply couldn't update this yet. Please try again.")
    }

    @Test
    fun unchangedSaveUsesNoChangePathInsteadOfUpdate() {
        val updates = AtomicInteger(0)
        val unchangedSaves = AtomicInteger(0)
        composeRule.setContent {
            KeeplyTheme {
                EditThingScreen(
                    thing = thing(),
                    onBack = {},
                    onSaveChanges = { updates.incrementAndGet() },
                    onUnchangedSave = { unchangedSaves.incrementAndGet() },
                    isUpdating = false,
                    updateError = null
                )
            }
        }

        composeRule.onNodeWithText("Save changes").performClick()

        composeRule.runOnIdle {
            assertEquals(0, updates.get())
            assertEquals(1, unchangedSaves.get())
        }
    }

    @Test
    fun changeThenRestoreUsesNoChangePathInsteadOfUpdate() {
        val updates = AtomicInteger(0)
        val unchangedSaves = AtomicInteger(0)
        composeRule.setContent {
            KeeplyTheme {
                EditThingScreen(
                    thing = thing(),
                    onBack = {},
                    onSaveChanges = { updates.incrementAndGet() },
                    onUnchangedSave = { unchangedSaves.incrementAndGet() },
                    isUpdating = false,
                    updateError = null
                )
            }
        }

        val nameField = composeRule.onNode(hasText("Passport") and hasSetTextAction())
        nameField.performTextReplacement("Passport 2026")
        composeRule.onNode(hasText("Passport 2026") and hasSetTextAction())
            .performTextReplacement("Passport")
        composeRule.onNodeWithText("Save changes").performClick()

        composeRule.runOnIdle {
            assertEquals(0, updates.get())
            assertEquals(1, unchangedSaves.get())
        }
    }

    private fun thing() = Thing(
        id = "passport-id",
        name = "Passport",
        category = ThingCategory.DOCUMENT,
        importantDate = "2027-08-30",
        reminderType = null,
        reminderAtEpochMillis = null,
        reminderTimeZoneId = null,
        notes = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L
    )
}
