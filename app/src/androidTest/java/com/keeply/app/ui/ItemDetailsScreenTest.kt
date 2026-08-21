package com.keeply.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.ThingStatus
import com.keeply.app.ui.theme.KeeplyTheme
import org.junit.Rule
import org.junit.Test

class ItemDetailsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reopenIsShownOnlyForDoneThings() {
        composeRule.setContent {
            KeeplyTheme {
                ItemDetailsScreen(
                    state = ItemDetailsState.Content(thing(ThingStatus.DONE)),
                    onBack = {}, onEdit = {}, onRemindAgain = { _, _ -> },
                    onMarkDone = {}, onReopen = {}, onDelete = {},
                    isChangingLifecycle = false, lifecycleError = null
                )
            }
        }

        composeRule.onNodeWithText("Reopen").assertIsDisplayed()
        composeRule.onAllNodesWithText("Remind me again").assertCountEquals(0)
        composeRule.onAllNodesWithText("Mark as done").assertCountEquals(0)
    }

    @Test
    fun successfulReopenShowsActiveActionsAgain() {
        var status by mutableStateOf(ThingStatus.DONE)
        composeRule.setContent {
            KeeplyTheme {
                ItemDetailsScreen(
                    state = ItemDetailsState.Content(thing(status)),
                    onBack = {}, onEdit = {}, onRemindAgain = { _, _ -> },
                    onMarkDone = {}, onReopen = { status = ThingStatus.ACTIVE }, onDelete = {},
                    isChangingLifecycle = false, lifecycleError = null
                )
            }
        }

        composeRule.onNodeWithText("Reopen").performClick()
        composeRule.onNodeWithText("Active").assertIsDisplayed()
        composeRule.onNodeWithText("Remind me again").assertIsDisplayed()
        composeRule.onNodeWithText("Mark as done").assertIsDisplayed()
        composeRule.onAllNodesWithText("Reopen").assertCountEquals(0)
    }

    private fun thing(status: ThingStatus) = Thing(
        id = "passport",
        name = "Passport",
        category = ThingCategory.DOCUMENT,
        importantDate = "2027-01-01",
        reminderType = null,
        reminderAtEpochMillis = null,
        reminderTimeZoneId = null,
        notes = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
        status = status
    )
}
