package com.keeply.app.ui

import com.keeply.app.model.ReminderDeliveryState
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.ThingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeThingsTest {
    @Test
    fun homeIncludesActiveInProgressAndMissedButExcludesDone() {
        val result = listOf(
            thing("done", "2026-08-01", ThingStatus.DONE),
            thing("active", "2026-08-03", ThingStatus.ACTIVE),
            thing("progress", "2026-08-04", ThingStatus.IN_PROGRESS),
            thing(
                "missed-active",
                "2026-08-02",
                ThingStatus.ACTIVE,
                delivery = ReminderDeliveryState.MISSED_ANNOUNCED
            )
        ).homeEligibleAndOrdered("2026-08-02")

        assertEquals(listOf("missed-active", "active", "progress"), result.map(Thing::id))
    }

    @Test
    fun orderingUsesImportantDateThenNewestCreationThenId() {
        val result = listOf(
            thing("future", "2026-08-30", ThingStatus.ACTIVE, createdAt = 1),
            thing("today", "2026-08-22", ThingStatus.ACTIVE, createdAt = 1),
            thing("old-overdue", "2026-08-01", ThingStatus.ACTIVE, createdAt = 1),
            thing("same-old", "2026-08-10", ThingStatus.ACTIVE, createdAt = 2),
            thing("z-same-new", "2026-08-10", ThingStatus.ACTIVE, createdAt = 4),
            thing("a-same-new", "2026-08-10", ThingStatus.ACTIVE, createdAt = 4)
        ).homeEligibleAndOrdered("2026-08-01")

        assertEquals(
            listOf("old-overdue", "a-same-new", "z-same-new", "same-old", "today", "future"),
            result.map(Thing::id)
        )
    }

    @Test
    fun inProgressUrgencyStillUsesImportantDateNotNextReminder() {
        val thing = thing("progress", "2026-08-17", ThingStatus.IN_PROGRESS)
            .copy(nextReminderAtEpochMillis = Long.MAX_VALUE, nextReminderTimeZoneId = "UTC")

        val context = importantDateContext(thing.importantDate, "2026-08-22")

        assertEquals(ImportantDateUrgency.OVERDUE, context?.urgency)
        assertEquals("5 days overdue", context?.timingText)
    }

    @Test
    fun unresolvedThingRemainsForOneOverdueDayThenLeavesHomeWithoutMutation() {
        val active = thing("active", "2026-08-30", ThingStatus.ACTIVE)
        val inProgress = thing("progress", "2026-08-30", ThingStatus.IN_PROGRESS)
        val original = listOf(active, inProgress)

        assertEquals(
            listOf("active", "progress"),
            original.homeEligibleAndOrdered("2026-08-30").map(Thing::id)
        )
        assertEquals(
            listOf("active", "progress"),
            original.homeEligibleAndOrdered("2026-08-31").map(Thing::id)
        )
        assertEquals(
            emptyList<String>(),
            original.homeEligibleAndOrdered("2026-09-01").map(Thing::id)
        )
        assertEquals(false, active.isWithinHomeOverdueGracePeriod("2026-09-01"))
        assertEquals(listOf(active, inProgress), original)
    }

    private fun thing(
        id: String,
        date: String,
        status: ThingStatus,
        createdAt: Long = 1,
        delivery: ReminderDeliveryState = ReminderDeliveryState.NONE
    ) = Thing(
        id = id,
        name = id,
        category = ThingCategory.DOCUMENT,
        importantDate = date,
        reminderType = null,
        reminderAtEpochMillis = null,
        reminderTimeZoneId = null,
        notes = null,
        createdAtEpochMillis = createdAt,
        updatedAtEpochMillis = createdAt,
        status = status,
        reminderDeliveryState = delivery
    )
}
