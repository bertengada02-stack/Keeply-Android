package com.keeply.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.ThingStatus
import com.keeply.app.ui.theme.KeeplyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedHomeShowsUrgencyAndOpensExactThingWithoutSearchControl() {
        var selectedId: String? = null
        composeRule.setContent {
            KeeplyTheme {
                PopulatedHomeScreen(
                    things = listOf(thing("passport", "Passport", "2026-08-30")),
                    onCategoryShortcut = {},
                    onThingSelected = { selectedId = it },
                    currentLocalDate = "2026-08-22"
                )
            }
        }

        composeRule.onNodeWithText("Coming up").assertIsDisplayed()
        composeRule.onNodeWithText("APPROACHING").assertIsDisplayed()
        composeRule.onNodeWithText(" · In 8 days").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Search My Things by title").assertCountEquals(0)
        composeRule.onNodeWithText("Passport").performClick()
        composeRule.runOnIdle { assertEquals("passport", selectedId) }
    }

    @Test
    fun inProgressCardKeepsImportantDateUrgencyAndShowsNextReminderSecondarily() {
        composeRule.setContent {
            KeeplyTheme {
                PopulatedHomeScreen(
                    things = listOf(
                        thing("progress", "Registration", "2026-08-21").copy(
                            status = ThingStatus.IN_PROGRESS,
                            nextReminderAtEpochMillis = 1_798_159_200_000L,
                            nextReminderTimeZoneId = "UTC"
                        )
                    ),
                    onCategoryShortcut = {},
                    onThingSelected = {},
                    currentLocalDate = "2026-08-22"
                )
            }
        }

        composeRule.onNodeWithText("OVERDUE").assertIsDisplayed()
        composeRule.onNodeWithText(" · 1 day overdue").assertIsDisplayed()
        composeRule.onNodeWithText("Next reminder", substring = true).assertIsDisplayed()
    }

    @Test
    fun populatedHomeShowsEightCategoryShortcutsAndRoutesEachSelectedCategory() {
        var selectedCategory: CategoryGlyph? = null
        composeRule.setContent {
            KeeplyTheme {
                PopulatedHomeScreen(
                    things = listOf(thing("passport", "Passport", "2026-08-30")),
                    onCategoryShortcut = { selectedCategory = it },
                    onThingSelected = {},
                    currentLocalDate = "2026-08-22"
                )
            }
        }

        composeRule.onNodeWithText("Remember something").assertIsDisplayed()
        composeRule.onNodeWithText("Coming up").assertIsDisplayed()

        val shortcuts = listOf(
            "Documents" to CategoryGlyph.DOCUMENT,
            "Things I own" to CategoryGlyph.OWNED,
            "Payments" to CategoryGlyph.PAYMENT,
            "Lent / Borrowed" to CategoryGlyph.EXCHANGE,
            "Money owed" to CategoryGlyph.MONEY,
            "Vehicle" to CategoryGlyph.VEHICLE,
            "Home / Appliance" to CategoryGlyph.HOME,
            "Medicine" to CategoryGlyph.MEDICINE
        )
        shortcuts.forEach { (label, expectedCategory) ->
            composeRule.onNodeWithText(label).performClick()
            composeRule.runOnIdle { assertEquals(expectedCategory, selectedCategory) }
        }
        composeRule.onAllNodesWithText("Something else").assertCountEquals(0)
    }

    @Test
    fun populatedHomeSectionHeadingsUseTheSameTypography() {
        composeRule.setContent {
            KeeplyTheme {
                PopulatedHomeScreen(
                    things = listOf(thing("passport", "Passport", "2026-08-30")),
                    onCategoryShortcut = {},
                    onThingSelected = {},
                    currentLocalDate = "2026-08-22"
                )
            }
        }

        val rememberLayouts = mutableListOf<TextLayoutResult>()
        val comingUpLayouts = mutableListOf<TextLayoutResult>()
        val thingTitleLayouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("Remember something").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult
        ) { action -> action(rememberLayouts) }
        composeRule.onNodeWithText("Coming up").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult
        ) { action -> action(comingUpLayouts) }
        composeRule.onNodeWithText("Passport").performSemanticsAction(
            SemanticsActions.GetTextLayoutResult
        ) { action -> action(thingTitleLayouts) }

        assertEquals(
            rememberLayouts.single().layoutInput.style,
            comingUpLayouts.single().layoutInput.style
        )
        assertTrue(
            rememberLayouts.single().layoutInput.style.fontSize >
                thingTitleLayouts.single().layoutInput.style.fontSize
        )
    }

    @Test
    fun emptyHomeAndCenterCreateActionStillOpenTheFullCategoryFlow() {
        composeRule.setContent {
            KeeplyTheme { KeeplyApp() }
        }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Nothing to remember yet")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Nothing to remember yet").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remember something").performClick()

        composeRule.onNodeWithText("What do you want to remember?").assertIsDisplayed()
        listOf(
            "Document",
            "Something I own",
            "Subscription/payment",
            "Lent/borrowed",
            "Money owed",
            "Vehicle",
            "Home/appliance",
            "Medicine",
            "Something else"
        ).forEach { label ->
            composeRule.onAllNodesWithText(label).assertCountEquals(1)
        }
    }

    private fun thing(id: String, name: String, date: String) = Thing(
        id = id,
        name = name,
        category = ThingCategory.DOCUMENT,
        importantDate = date,
        reminderType = null,
        reminderAtEpochMillis = null,
        reminderTimeZoneId = null,
        notes = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
        status = ThingStatus.ACTIVE
    )
}
