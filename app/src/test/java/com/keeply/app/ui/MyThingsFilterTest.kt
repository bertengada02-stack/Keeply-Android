package com.keeply.app.ui

import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.ThingStatus
import com.keeply.app.model.ReminderDeliveryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyThingsFilterTest {
    private val mixed = listOf(
        thing("newest-done", ThingStatus.DONE, 4L),
        thing("progress", ThingStatus.IN_PROGRESS, 3L),
        thing("active", ThingStatus.ACTIVE, 2L),
        thing("oldest-done", ThingStatus.DONE, 1L)
    )

    @Test
    fun allContainsEveryLifecycleStateOnceInSourceOrder() {
        assertEquals(
            listOf("progress", "active", "newest-done", "oldest-done"),
            mixed.filteredBy(MyThingsFilter.ALL).map(Thing::id)
        )
    }

    @Test
    fun activeContainsActiveAndInProgressOnlyInSourceOrder() {
        assertEquals(
            listOf("progress", "active"),
            mixed.filteredBy(MyThingsFilter.ACTIVE).map(Thing::id)
        )
    }

    @Test
    fun completedContainsDoneOnlyInSourceOrder() {
        assertEquals(
            listOf("newest-done", "oldest-done"),
            mixed.filteredBy(MyThingsFilter.COMPLETED).map(Thing::id)
        )
    }

    @Test
    fun missedIncludesActiveAndInProgressMissedThingsWithoutChangingLifecycleFilters() {
        val withMissed = listOf(
            thing("active-missed", ThingStatus.ACTIVE, 3L, ReminderDeliveryState.MISSED_UNANNOUNCED),
            thing("progress-missed", ThingStatus.IN_PROGRESS, 2L, ReminderDeliveryState.MISSED_ANNOUNCED),
            thing("active-normal", ThingStatus.ACTIVE, 1L)
        )

        assertEquals(
            listOf("active-missed", "progress-missed"),
            withMissed.filteredBy(MyThingsFilter.MISSED).map(Thing::id)
        )
        assertEquals(3, withMissed.filteredBy(MyThingsFilter.ALL).size)
        assertEquals(3, withMissed.filteredBy(MyThingsFilter.ACTIVE).size)
    }

    @Test
    fun filteringDoesNotMutateOrDuplicateSourceThings() {
        val original = mixed.toList()
        MyThingsFilter.entries.forEach { filter ->
            val result = mixed.filteredBy(filter)
            assertEquals(result.size, result.distinctBy(Thing::id).size)
        }
        assertEquals(original, mixed)
    }

    @Test
    fun emptyAndSingleStatusListsFilterDeterministically() {
        assertTrue(emptyList<Thing>().filteredBy(MyThingsFilter.ALL).isEmpty())
        assertTrue(listOf(thing("done", ThingStatus.DONE, 1L))
            .filteredBy(MyThingsFilter.ACTIVE).isEmpty())
        assertTrue(listOf(thing("active", ThingStatus.ACTIVE, 1L))
            .filteredBy(MyThingsFilter.COMPLETED).isEmpty())
    }

    @Test
    fun allNineCategoriesAndAllCategoriesFilterCorrectly() {
        val allCategories = ThingCategory.entries.mapIndexed { index, category ->
            thing("thing-$index", ThingStatus.ACTIVE, index.toLong()).copy(category = category)
        }

        assertEquals(
            allCategories.sortedWith(importantDateThingComparator),
            allCategories.filterMyThings(MyThingsFilter.ALL, null, "")
        )
        ThingCategory.entries.forEach { category ->
            assertEquals(
                listOf(category),
                allCategories.filterMyThings(MyThingsFilter.ALL, category, "").map(Thing::category)
            )
        }
    }

    @Test
    fun titleSearchIsTrimmedPartialCaseInsensitiveAndTitleOnly() {
        val passport = thing("passport", ThingStatus.ACTIVE, 2).copy(
            name = "Family Passport",
            category = ThingCategory.VEHICLE,
            notes = "secret medicine"
        )
        val license = thing("license", ThingStatus.ACTIVE, 1).copy(name = "Driver License")
        val things = listOf(passport, license)

        assertEquals(listOf("passport"), things.filterMyThings(MyThingsFilter.ALL, null, " passport ").map(Thing::id))
        assertEquals(listOf("passport"), things.filterMyThings(MyThingsFilter.ALL, null, "FAMILY").map(Thing::id))
        assertEquals(listOf("license"), things.filterMyThings(MyThingsFilter.ALL, null, "lic").map(Thing::id))
        assertTrue(things.filterMyThings(MyThingsFilter.ALL, null, "vehicle").isEmpty())
        assertTrue(things.filterMyThings(MyThingsFilter.ALL, null, "medicine").isEmpty())
        assertEquals(things, things.filterMyThings(MyThingsFilter.ALL, null, "   "))
    }

    @Test
    fun statusCategoryAndSearchComposeIncludingMissed() {
        val target = thing(
            "target",
            ThingStatus.ACTIVE,
            3,
            ReminderDeliveryState.MISSED_ANNOUNCED
        ).copy(name = "Passport renewal", category = ThingCategory.DOCUMENT)
        val wrongCategory = thing(
            "vehicle",
            ThingStatus.ACTIVE,
            2,
            ReminderDeliveryState.MISSED_UNANNOUNCED
        ).copy(name = "Passport vehicle", category = ThingCategory.VEHICLE)
        val notMissed = thing("normal", ThingStatus.ACTIVE, 1).copy(
            name = "Passport normal",
            category = ThingCategory.DOCUMENT
        )

        assertEquals(
            listOf("target"),
            listOf(target, wrongCategory, notMissed)
                .filterMyThings(MyThingsFilter.MISSED, ThingCategory.DOCUMENT, "PASS")
                .map(Thing::id)
        )
    }

    @Test
    fun successfulCreateFromAnyEntryPointResetsMyThingsAndShowsCreatedThing() {
        val staleState = MyThingsUiSelection(
            filter = MyThingsFilter.MISSED,
            category = ThingCategory.MEDICINE,
            searchActive = true,
            searchQuery = "hidden"
        )
        val reset = myThingsStateAfterSuccessfulCreate()
        val created = thing("new-passport", ThingStatus.ACTIVE, 5L)

        assertTrue(staleState != reset)
        assertEquals(MyThingsFilter.ALL, reset.filter)
        assertEquals(null, reset.category)
        assertEquals(false, reset.searchActive)
        assertEquals("", reset.searchQuery)
        assertEquals(
            listOf("new-passport"),
            listOf(created)
                .filterMyThings(reset.filter, reset.category, reset.searchQuery)
                .map(Thing::id)
        )
    }

    @Test
    fun actionableOrderingUsesHomeDatePriorityAfterEveryFilterAndKeepsCompletedOrder() {
        val overdue = thing("overdue", ThingStatus.ACTIVE, 1).copy(
            name = "Renew overdue",
            importantDate = "2026-08-20",
            category = ThingCategory.MEDICINE,
            reminderDeliveryState = ReminderDeliveryState.MISSED_ANNOUNCED
        )
        val today = thing("today", ThingStatus.ACTIVE, 1).copy(
            name = "Renew today",
            importantDate = "2026-08-23",
            category = ThingCategory.MEDICINE,
            reminderDeliveryState = ReminderDeliveryState.MISSED_ANNOUNCED
        )
        val tomorrow = thing("tomorrow", ThingStatus.IN_PROGRESS, 1).copy(
            name = "Renew tomorrow",
            importantDate = "2026-08-24",
            category = ThingCategory.MEDICINE,
            reminderDeliveryState = ReminderDeliveryState.MISSED_UNANNOUNCED
        )
        val day14 = thing("day-14", ThingStatus.ACTIVE, 1).copy(
            name = "Renew day fourteen",
            importantDate = "2026-09-06",
            category = ThingCategory.MEDICINE,
            reminderDeliveryState = ReminderDeliveryState.MISSED_ANNOUNCED
        )
        val day15 = thing("day-15", ThingStatus.ACTIVE, 1).copy(
            name = "Renew day fifteen",
            importantDate = "2026-09-07",
            category = ThingCategory.MEDICINE,
            reminderDeliveryState = ReminderDeliveryState.MISSED_ANNOUNCED
        )
        val sameOld = thing("same-old", ThingStatus.ACTIVE, 2).copy(importantDate = "2026-09-08")
        val zSameNew = thing("z-same-new", ThingStatus.ACTIVE, 4).copy(importantDate = "2026-09-08")
        val aSameNew = thing("a-same-new", ThingStatus.ACTIVE, 4).copy(importantDate = "2026-09-08")
        val doneOlderInSource = thing("done-first", ThingStatus.DONE, 1).copy(importantDate = "2030-01-01")
        val doneNewerInSource = thing("done-second", ThingStatus.DONE, 9).copy(importantDate = "2020-01-01")
        val source = listOf(
            day15,
            doneOlderInSource,
            tomorrow,
            sameOld,
            today,
            zSameNew,
            overdue,
            day14,
            aSameNew,
            doneNewerInSource
        )
        val chronological = listOf(
            "overdue", "today", "tomorrow", "day-14", "day-15",
            "a-same-new", "z-same-new", "same-old"
        )

        assertEquals(
            chronological + listOf("done-first", "done-second"),
            source.filterMyThings(MyThingsFilter.ALL, null, "").map(Thing::id)
        )
        assertEquals(
            chronological.take(5),
            source.filterMyThings(MyThingsFilter.ACTIVE, ThingCategory.MEDICINE, "renew")
                .map(Thing::id)
        )
        assertEquals(
            chronological.take(5),
            source.filterMyThings(MyThingsFilter.MISSED, ThingCategory.MEDICINE, "renew")
                .map(Thing::id)
        )
        assertEquals(
            listOf("done-first", "done-second"),
            source.filterMyThings(MyThingsFilter.COMPLETED, null, "").map(Thing::id)
        )
    }

    private fun thing(
        id: String,
        status: ThingStatus,
        createdAt: Long,
        deliveryState: ReminderDeliveryState = ReminderDeliveryState.NONE
    ) = Thing(
        id = id,
        name = id,
        category = ThingCategory.DOCUMENT,
        importantDate = "2027-01-01",
        reminderType = null,
        reminderAtEpochMillis = null,
        reminderTimeZoneId = null,
        notes = null,
        createdAtEpochMillis = createdAt,
        updatedAtEpochMillis = createdAt,
        status = status,
        reminderDeliveryState = deliveryState
    )
}
