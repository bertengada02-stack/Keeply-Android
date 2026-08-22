package com.keeply.app.notifications

import com.keeply.app.model.ReminderType
import com.keeply.app.model.ReminderDeliveryState
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.ThingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSchedulingTest {
    @Test
    fun activeOriginalReminderSchedulesOnlyWhenActionableAndFuture() {
        val future = thing(originalAt = 2_000L, originalActionable = true)
        assertEquals(
            ReminderScheduleDecision.Future("thing-1", 2_000L, "Asia/Singapore"),
            future.reminderScheduleDecision(nowEpochMillis = 1_000L)
        )
        assertEquals(
            ReminderScheduleDecision.None,
            future.copy(originalReminderActionable = false).reminderScheduleDecision(1_000L)
        )
        assertEquals(
            ReminderScheduleDecision.Past(2_000L),
            future.reminderScheduleDecision(2_000L)
        )
    }

    @Test
    fun inProgressUsesOnlyFollowUpReminder() {
        val decision = thing(
            status = ThingStatus.IN_PROGRESS,
            originalAt = 2_000L,
            originalActionable = false,
            nextAt = 3_000L
        ).reminderScheduleDecision(1_000L)

        assertEquals(
            ReminderScheduleDecision.Future("thing-1", 3_000L, "Asia/Singapore"),
            decision
        )
    }

    @Test
    fun doneAndReopenedWithoutNewReminderNeverScheduleHistoricalReminder() {
        val historical = thing(originalAt = 3_000L, originalActionable = false)
        assertEquals(ReminderScheduleDecision.None, historical.reminderScheduleDecision(1_000L))
        assertEquals(
            ReminderScheduleDecision.None,
            historical.copy(status = ThingStatus.DONE).reminderScheduleDecision(1_000L)
        )
    }

    @Test
    fun receiverValidationRejectsStaleAndHistoricalEpochs() {
        val current = thing(originalAt = 3_000L, originalActionable = true)
        assertEquals(true, current.matchesExpectedReminder(3_000L))
        assertEquals(false, current.matchesExpectedReminder(2_000L))
        assertEquals(
            false,
            current.copy(originalReminderActionable = false).matchesExpectedReminder(3_000L)
        )
    }

    @Test
    fun deliveredAndAlreadyMissedRemindersAreNeverRescheduledOrReclassifiedByTime() {
        val pending = thing(originalAt = 3_000L, originalActionable = true)
        assertEquals(ReminderScheduleDecision.Past(3_000L), pending.reminderScheduleDecision(4_000L))
        assertEquals(
            ReminderScheduleDecision.None,
            pending.copy(reminderDeliveryState = ReminderDeliveryState.DELIVERED)
                .reminderScheduleDecision(4_000L)
        )
        assertEquals(
            ReminderScheduleDecision.None,
            pending.copy(reminderDeliveryState = ReminderDeliveryState.MISSED_ANNOUNCED)
                .reminderScheduleDecision(4_000L)
        )
    }

    @Test
    fun replacedEpochIsTheOnlyReceiverIdentityThatCanValidate() {
        val replacement = thing(
            status = ThingStatus.IN_PROGRESS,
            originalAt = 2_000L,
            originalActionable = false,
            nextAt = 5_000L
        )
        assertEquals(false, replacement.matchesExpectedReminder(2_000L))
        assertEquals(true, replacement.matchesExpectedReminder(5_000L))
    }

    @Test
    fun coordinatorReplacesBySyncAndCancelsByStableThingId() {
        val scheduler = RecordingScheduler()
        val coordinator = ReminderSyncCoordinator(scheduler)
        val first = thing(nextAt = 3_000L, status = ThingStatus.IN_PROGRESS)
        val replacement = first.copy(nextReminderAtEpochMillis = 4_000L)

        coordinator.sync(first)
        coordinator.sync(replacement)
        coordinator.cancel(first.id)

        assertEquals(listOf(3_000L, 4_000L), scheduler.syncedEpochs)
        assertEquals(listOf("thing-1"), scheduler.cancelledIds)
    }

    @Test
    fun coordinatorContainsPlatformFailuresSoPersistenceCanSucceed() {
        val coordinator = ReminderSyncCoordinator(object : ReminderScheduler {
            override fun sync(thing: Thing): ReminderSyncResult = error("alarm unavailable")
            override fun cancel(thingId: String) = error("alarm unavailable")
        })

        assertEquals(ReminderSyncResult.Failed, coordinator.sync(thing(originalAt = 2_000L)))
        assertEquals(ReminderSyncResult.Failed, coordinator.cancel("thing-1"))
    }

    private class RecordingScheduler : ReminderScheduler {
        val syncedEpochs = mutableListOf<Long>()
        val cancelledIds = mutableListOf<String>()

        override fun sync(thing: Thing): ReminderSyncResult {
            syncedEpochs += thing.nextReminderAtEpochMillis ?: thing.reminderAtEpochMillis ?: -1L
            return ReminderSyncResult.ScheduledExact
        }

        override fun cancel(thingId: String) {
            cancelledIds += thingId
        }
    }

    private fun thing(
        status: ThingStatus = ThingStatus.ACTIVE,
        originalAt: Long? = null,
        originalActionable: Boolean = originalAt != null,
        nextAt: Long? = null
    ) = Thing(
        id = "thing-1",
        name = "Passport",
        category = ThingCategory.DOCUMENT,
        importantDate = "2026-08-25",
        reminderType = originalAt?.let { ReminderType.CUSTOM },
        reminderAtEpochMillis = originalAt,
        reminderTimeZoneId = originalAt?.let { "Asia/Singapore" },
        notes = null,
        createdAtEpochMillis = 10L,
        updatedAtEpochMillis = 10L,
        status = status,
        nextReminderAtEpochMillis = nextAt,
        nextReminderTimeZoneId = nextAt?.let { "Asia/Singapore" },
        originalReminderActionable = originalActionable,
        reminderDeliveryState = if (originalActionable || nextAt != null) {
            ReminderDeliveryState.PENDING
        } else {
            ReminderDeliveryState.NONE
        }
    )
}
