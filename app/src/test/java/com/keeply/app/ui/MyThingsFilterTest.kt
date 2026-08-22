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
            listOf("newest-done", "progress", "active", "oldest-done"),
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
