package com.keeply.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.ThingStatus
import com.keeply.app.ui.theme.KeeplyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MyThingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun zeroDatabaseKeepsFullEmptyStateForEverySelectedFilter() {
        var filter by mutableStateOf(MyThingsFilter.ALL)
        composeRule.setContent {
            KeeplyTheme {
                MyThingsShell(emptyList(), filter, { filter = it }, {})
            }
        }

        composeRule.onNodeWithText("All").assert(selected())
        composeRule.onNodeWithText("My Things").assertIsDisplayed()
        composeRule.onNodeWithText("Things you ask Keeply to remember\nwill appear here.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Active").performClick()
        composeRule.onNodeWithText("Active").assert(selected())
        composeRule.onNodeWithText("Things you ask Keeply to remember\nwill appear here.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("No active things").assertCountEquals(0)

        composeRule.onNodeWithText("Completed").performClick()
        composeRule.onNodeWithText("Completed").assert(selected())
        composeRule.onNodeWithText("Things you ask Keeply to remember\nwill appear here.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("Nothing completed yet").assertCountEquals(0)
    }

    @Test
    fun nonEmptyDatabaseUsesCompactFilterSpecificEmptyStates() {
        var filter by mutableStateOf(MyThingsFilter.ACTIVE)
        val onlyDone = listOf(thing("done-id", "Finished passport", ThingStatus.DONE))
        var things by mutableStateOf(onlyDone)
        composeRule.setContent {
            KeeplyTheme {
                MyThingsShell(things, filter, { filter = it }, {})
            }
        }

        composeRule.onNodeWithText("No active things").assertIsDisplayed()
        composeRule.onNodeWithText("Things you're still keeping track of will appear here.")
            .assertIsDisplayed()

        val onlyActive = listOf(thing("active-id", "Active passport", ThingStatus.ACTIVE))
        composeRule.runOnIdle {
            filter = MyThingsFilter.COMPLETED
            things = onlyActive
        }
        composeRule.onNodeWithText("Nothing completed yet").assertIsDisplayed()
        composeRule.onNodeWithText("Things you mark as done will appear here.")
            .assertIsDisplayed()
    }

    @Test
    fun filteredRowsOpenTheExactThingId() {
        var filter by mutableStateOf(MyThingsFilter.COMPLETED)
        var selectedId: String? = null
        val things = listOf(
            thing("active-id", "Active passport", ThingStatus.ACTIVE),
            thing("done-id", "Finished passport", ThingStatus.DONE)
        )
        composeRule.setContent {
            KeeplyTheme {
                MyThingsShell(things, filter, { filter = it }, { selectedId = it })
            }
        }

        composeRule.onNodeWithText("Finished passport").performClick()
        composeRule.runOnIdle { assertEquals("done-id", selectedId) }
    }

    private fun selected() = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)

    private fun thing(id: String, name: String, status: ThingStatus) = Thing(
        id = id,
        name = name,
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
