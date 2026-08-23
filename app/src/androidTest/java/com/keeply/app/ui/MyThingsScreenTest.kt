package com.keeply.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.ThingStatus
import com.keeply.app.model.ReminderDeliveryState
import com.keeply.app.ui.theme.KeeplyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        composeRule.onNodeWithText("Missed").performClick()
        composeRule.onNodeWithText("Missed").assert(selected())
        composeRule.onNodeWithText("Things you ask Keeply to remember\nwill appear here.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("No missed reminders").assertCountEquals(0)
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

        composeRule.runOnIdle { filter = MyThingsFilter.MISSED }
        composeRule.onNodeWithText("No missed reminders").assertIsDisplayed()
        composeRule.onNodeWithText("You're all caught up.").assertIsDisplayed()
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

    @Test
    fun missedFilterShowsMissedThingAndOpensItsExactId() {
        var selectedId: String? = null
        val things = listOf(
            thing("normal", "Normal passport", ThingStatus.ACTIVE),
            thing(
                "missed",
                "Missed passport",
                ThingStatus.ACTIVE,
                ReminderDeliveryState.MISSED_ANNOUNCED
            )
        )
        composeRule.setContent {
            KeeplyTheme {
                MyThingsShell(things, MyThingsFilter.MISSED, {}, { selectedId = it })
            }
        }

        composeRule.onNodeWithText("Missed passport").performClick()
        composeRule.runOnIdle { assertEquals("missed", selectedId) }
        composeRule.onAllNodesWithText("Normal passport").assertCountEquals(0)
    }

    @Test
    fun categoryAndTitleSearchComposeWithoutMatchingNotesOrCategory() {
        var category by mutableStateOf<ThingCategory?>(null)
        var query by mutableStateOf("")
        var searchActive by mutableStateOf(false)
        val document = thing("document", "Family Passport", ThingStatus.ACTIVE)
        val vehicle = thing("vehicle", "Family Car", ThingStatus.ACTIVE).copy(
            category = ThingCategory.VEHICLE,
            notes = "Passport is mentioned only in notes"
        )
        composeRule.setContent {
            KeeplyTheme {
                MyThingsShell(
                    things = listOf(document, vehicle),
                    selectedFilter = MyThingsFilter.ALL,
                    onFilterSelected = {},
                    onThingSelected = {},
                    selectedCategory = category,
                    searchQuery = query,
                    searchActive = searchActive,
                    onCategorySelected = { category = it },
                    onSearchRequested = { searchActive = true },
                    onSearchQueryChanged = { query = it },
                    onSearchClosed = { searchActive = false; query = "" }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Category filter, All categories selected")
            .performClick()
        composeRule.onNodeWithContentDescription("Filter by Document").performClick()
        composeRule.onAllNodesWithText("Family Car").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Search My Things by title").performClick()
        composeRule.onNodeWithText("Search titles").performTextInput(" passport ")
        composeRule.onNodeWithText("Family Passport").assertIsDisplayed()

        composeRule.runOnIdle { category = ThingCategory.VEHICLE }
        composeRule.onNodeWithText("No matching things").assertIsDisplayed()
        composeRule.onNodeWithText("Try another title or adjust your filters.").assertIsDisplayed()
    }

    @Test
    fun categoryDropdownShowsEveryOptionAndSelectionCanReturnToAllCategories() {
        var category by mutableStateOf<ThingCategory?>(null)
        val document = thing("document", "Passport", ThingStatus.ACTIVE)
        val vehicle = thing("vehicle", "Family Car", ThingStatus.ACTIVE).copy(
            category = ThingCategory.VEHICLE
        )
        composeRule.setContent {
            KeeplyTheme {
                MyThingsShell(
                    things = listOf(document, vehicle),
                    selectedFilter = MyThingsFilter.ALL,
                    onFilterSelected = {},
                    onThingSelected = {},
                    selectedCategory = category,
                    onCategorySelected = { category = it }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Category filter, All categories selected")
            .assertIsDisplayed()
            .performClick()

        val labels = listOf(
            "All categories",
            "Document",
            "Something I own",
            "Subscription/payment",
            "Lent/borrowed",
            "Money owed",
            "Vehicle",
            "Home/appliance",
            "Medicine",
            "Something else"
        )
        labels.forEach { label ->
            composeRule.onAllNodesWithContentDescription("Filter by $label").assertCountEquals(1)
        }
        composeRule.onAllNodesWithContentDescription(
            "all categories icon",
            useUnmergedTree = true
        ).assertCountEquals(1)
        CategoryGlyph.entries.forEach { glyph ->
            assertTrue(
                composeRule.onAllNodesWithContentDescription(
                    "${glyph.name.lowercase()} category",
                    useUnmergedTree = true
                ).fetchSemanticsNodes().isNotEmpty()
            )
        }
        composeRule.onNodeWithContentDescription("Filter by All categories").assert(selected())

        composeRule.onNodeWithContentDescription("Filter by Vehicle").performClick()
        composeRule.onNodeWithContentDescription("Category filter, Vehicle selected")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Family Car").assertIsDisplayed()
        composeRule.onAllNodesWithText("Passport").assertCountEquals(0)

        composeRule.onNodeWithContentDescription("Category filter, Vehicle selected")
            .performClick()
        composeRule.onNodeWithContentDescription("Filter by All categories").performClick()
        composeRule.onNodeWithText("Passport").assertIsDisplayed()
        composeRule.onNodeWithText("Family Car").assertIsDisplayed()
    }

    @Test
    fun closingSearchClearsQueryAndRestoresCategoryFilteredRows() {
        var query by mutableStateOf("missing")
        var searchActive by mutableStateOf(true)
        composeRule.setContent {
            KeeplyTheme {
                MyThingsShell(
                    things = listOf(thing("document", "Passport", ThingStatus.ACTIVE)),
                    selectedFilter = MyThingsFilter.ALL,
                    onFilterSelected = {},
                    onThingSelected = {},
                    searchQuery = query,
                    searchActive = searchActive,
                    onSearchQueryChanged = { query = it },
                    onSearchClosed = { searchActive = false; query = "" }
                )
            }
        }

        composeRule.onNodeWithText("No matching things").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.onNodeWithText("Passport").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("", query)
            assertEquals(false, searchActive)
        }
    }

    @Test
    fun outsideTapDismissesFocusWithoutClosingSearchOrClearingResults() {
        var query by mutableStateOf("")
        var searchActive by mutableStateOf(false)
        var selectedId: String? = null
        composeRule.setContent {
            KeeplyTheme {
                MyThingsShell(
                    things = listOf(thing("missed-9", "Missed 9", ThingStatus.ACTIVE)),
                    selectedFilter = MyThingsFilter.ALL,
                    onFilterSelected = {},
                    onThingSelected = { selectedId = it },
                    searchQuery = query,
                    searchActive = searchActive,
                    onSearchRequested = { searchActive = true },
                    onSearchQueryChanged = { query = it },
                    onSearchClosed = { searchActive = false; query = "" }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Search My Things by title").performClick()
        composeRule.onNodeWithText("Search titles").performTextInput("missed 9")
        composeRule.onNodeWithText("missed 9").assertIsFocused()
        composeRule.onNodeWithText("Missed 9").assertIsDisplayed()

        composeRule.onNodeWithText("All").performClick()
        composeRule.onNodeWithText("missed 9").assertIsNotFocused()
        composeRule.onNodeWithText("missed 9").assertIsDisplayed()
        composeRule.onNodeWithText("Missed 9").assertIsDisplayed()
        composeRule.onNodeWithText("Close").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("missed 9", query)
            assertEquals(true, searchActive)
        }

        composeRule.onNodeWithText("missed 9").performClick()
        composeRule.onNodeWithText("missed 9").assertIsFocused()
        composeRule.onNodeWithText("Missed 9").performClick()
        composeRule.runOnIdle { assertEquals("missed-9", selectedId) }
    }

    @Test
    fun androidBackStillClosesAndClearsSearch() {
        var query by mutableStateOf("passport")
        var searchActive by mutableStateOf(true)
        lateinit var backDispatcher: OnBackPressedDispatcher
        composeRule.setContent {
            KeeplyTheme {
                backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current)
                    .onBackPressedDispatcher
                BackHandler(enabled = searchActive) {
                    searchActive = false
                    query = ""
                }
                MyThingsShell(
                    things = listOf(thing("document", "Passport", ThingStatus.ACTIVE)),
                    selectedFilter = MyThingsFilter.ALL,
                    onFilterSelected = {},
                    onThingSelected = {},
                    searchQuery = query,
                    searchActive = searchActive,
                    onSearchQueryChanged = { query = it },
                    onSearchClosed = { searchActive = false; query = "" }
                )
            }
        }

        composeRule.runOnIdle { backDispatcher.onBackPressed() }

        composeRule.onAllNodesWithText("passport").assertCountEquals(0)
        composeRule.onAllNodesWithText("Close").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Search My Things by title").assertIsDisplayed()
    }

    @Test
    fun importantDateUrgencyRendersAcrossStatusCategoryAndSearchViews() {
        val overdue = thing("overdue", "Overdue passport", ThingStatus.ACTIVE).copy(
            importantDate = "2026-08-22",
            reminderAtEpochMillis = 1_900_000_000_000L
        )
        val dueToday = thing("today", "License due today", ThingStatus.ACTIVE).copy(
            importantDate = "2026-08-23"
        )
        val completed = thing("completed", "Completed license", ThingStatus.DONE).copy(
            importantDate = "2026-08-25"
        )
        val tomorrow = thing("tomorrow", "Tomorrow payment", ThingStatus.ACTIVE).copy(
            importantDate = "2026-08-24",
            category = ThingCategory.SUBSCRIPTION_PAYMENT
        )
        val approachingMissed = thing(
            "approaching",
            "Approaching medicine",
            ThingStatus.ACTIVE,
            ReminderDeliveryState.MISSED_ANNOUNCED
        ).copy(
            importantDate = "2026-08-27",
            category = ThingCategory.MEDICINE
        )
        val later = thing("later", "Later vehicle", ThingStatus.IN_PROGRESS).copy(
            importantDate = "2026-09-07",
            category = ThingCategory.VEHICLE,
            nextReminderAtEpochMillis = 1L
        )
        val things = listOf(overdue, dueToday, tomorrow, approachingMissed, later, completed)
        var filter by mutableStateOf(MyThingsFilter.ALL)
        var category by mutableStateOf<ThingCategory?>(null)
        var query by mutableStateOf("")
        composeRule.setContent {
            KeeplyTheme {
                MyThingsShell(
                    things = things,
                    selectedFilter = filter,
                    onFilterSelected = { filter = it },
                    onThingSelected = {},
                    selectedCategory = category,
                    searchQuery = query,
                    onCategorySelected = { category = it },
                    onSearchQueryChanged = { query = it },
                    currentLocalDate = "2026-08-23"
                )
            }
        }

        composeRule.onNodeWithText("OVERDUE").assertIsDisplayed()
        composeRule.onNodeWithText(" · 1 day overdue").assertIsDisplayed()
        composeRule.onNodeWithText("DUE TODAY").assertIsDisplayed()
        composeRule.onNodeWithText(" · Due today").assertIsDisplayed()
        composeRule.onNodeWithText("VERY SOON").assertIsDisplayed()
        composeRule.onNodeWithText(" · Due tomorrow").assertIsDisplayed()
        composeRule.onNodeWithText("APPROACHING").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(" · In 4 days").assertIsDisplayed()
        composeRule.onNodeWithText("LATER").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(" · In 15 days").assertIsDisplayed()
        composeRule.onNodeWithText("Aug 22, 2026").assertIsDisplayed()

        composeRule.runOnIdle { filter = MyThingsFilter.ACTIVE }
        composeRule.onNodeWithText("OVERDUE").assertIsDisplayed()
        composeRule.onNodeWithText("DUE TODAY").assertIsDisplayed()

        composeRule.runOnIdle { filter = MyThingsFilter.COMPLETED }
        composeRule.onNodeWithText("✓ Done").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Completed").assertIsDisplayed()
        composeRule.onNodeWithText("Aug 25, 2026").assertIsDisplayed()
        composeRule.onAllNodesWithText("OVERDUE").assertCountEquals(0)
        composeRule.onAllNodesWithText("VERY SOON").assertCountEquals(0)

        composeRule.runOnIdle { filter = MyThingsFilter.MISSED }
        composeRule.onNodeWithText("APPROACHING").assertIsDisplayed()
        composeRule.onAllNodesWithText("VERY SOON").assertCountEquals(0)

        composeRule.runOnIdle {
            filter = MyThingsFilter.ALL
            category = ThingCategory.MEDICINE
        }
        composeRule.onNodeWithText("APPROACHING").assertIsDisplayed()
        composeRule.onAllNodesWithText("LATER").assertCountEquals(0)

        composeRule.runOnIdle {
            category = null
            query = "later"
        }
        composeRule.onNodeWithText("LATER").assertIsDisplayed()
        composeRule.onAllNodesWithText("APPROACHING").assertCountEquals(0)

        composeRule.runOnIdle {
            filter = MyThingsFilter.MISSED
            category = ThingCategory.MEDICINE
            query = "approaching"
        }
        composeRule.onNodeWithText("APPROACHING").assertIsDisplayed()
        composeRule.onNodeWithText("Approaching medicine").assertIsDisplayed()
    }

    @Test
    fun doneRowsShowDoneWithoutUrgencyAcrossDatesAndFilteredViews() {
        val donePast = thing("done-past", "Done passport", ThingStatus.DONE).copy(
            importantDate = "2026-08-20"
        )
        val doneToday = thing("done-today", "Done medicine", ThingStatus.DONE).copy(
            importantDate = "2026-08-23",
            category = ThingCategory.MEDICINE
        )
        val doneFuture = thing("done-future", "Done vehicle", ThingStatus.DONE).copy(
            importantDate = "2026-09-07",
            category = ThingCategory.VEHICLE
        )
        val activeToday = thing("active-today", "Active today", ThingStatus.ACTIVE).copy(
            importantDate = "2026-08-23"
        )
        val inProgressFuture = thing("progress-future", "Progress tomorrow", ThingStatus.IN_PROGRESS).copy(
            importantDate = "2026-08-24",
            nextReminderAtEpochMillis = 1L
        )
        val missedPast = thing(
            "missed-past",
            "Missed overdue",
            ThingStatus.ACTIVE,
            ReminderDeliveryState.MISSED_ANNOUNCED
        ).copy(importantDate = "2026-08-20")
        val things = listOf(
            donePast,
            doneToday,
            doneFuture,
            activeToday,
            inProgressFuture,
            missedPast
        )
        var filter by mutableStateOf(MyThingsFilter.ALL)
        var category by mutableStateOf<ThingCategory?>(null)
        var query by mutableStateOf("")
        composeRule.setContent {
            KeeplyTheme {
                MyThingsShell(
                    things = things,
                    selectedFilter = filter,
                    onFilterSelected = { filter = it },
                    onThingSelected = {},
                    selectedCategory = category,
                    searchQuery = query,
                    onCategorySelected = { category = it },
                    onSearchQueryChanged = { query = it },
                    currentLocalDate = "2026-08-23"
                )
            }
        }

        composeRule.onAllNodesWithText("✓ Done").assertCountEquals(3)
        composeRule.onAllNodesWithText("Aug 20, 2026").assertCountEquals(2)
        composeRule.onAllNodesWithText("Aug 23, 2026").assertCountEquals(2)
        composeRule.onNodeWithText("Sep 7, 2026").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("DUE TODAY").assertIsDisplayed()
        composeRule.onNodeWithText("VERY SOON").assertIsDisplayed()
        composeRule.onNodeWithText("OVERDUE").assertIsDisplayed()

        composeRule.runOnIdle { filter = MyThingsFilter.COMPLETED }
        composeRule.onAllNodesWithText("✓ Done").assertCountEquals(3)
        composeRule.onAllNodesWithText("OVERDUE").assertCountEquals(0)
        composeRule.onAllNodesWithText("DUE TODAY").assertCountEquals(0)
        composeRule.onAllNodesWithText("VERY SOON").assertCountEquals(0)
        composeRule.onAllNodesWithText("APPROACHING").assertCountEquals(0)
        composeRule.onAllNodesWithText("LATER").assertCountEquals(0)

        composeRule.runOnIdle {
            category = ThingCategory.MEDICINE
            query = "done medicine"
        }
        composeRule.onNodeWithText("✓ Done").assertIsDisplayed()
        composeRule.onNodeWithText("Aug 23, 2026").assertIsDisplayed()
        composeRule.onAllNodesWithText("DUE TODAY").assertCountEquals(0)

        composeRule.runOnIdle {
            filter = MyThingsFilter.MISSED
            category = null
            query = ""
        }
        composeRule.onNodeWithText("OVERDUE").assertIsDisplayed()
        composeRule.onAllNodesWithText("✓ Done").assertCountEquals(0)
    }

    private fun selected() = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)

    private fun thing(
        id: String,
        name: String,
        status: ThingStatus,
        deliveryState: ReminderDeliveryState = ReminderDeliveryState.NONE
    ) = Thing(
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
        status = status,
        reminderDeliveryState = deliveryState
    )
}
