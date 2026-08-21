package com.keeply.app.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThingLifecycleTest {
    private val singapore = TimeZone.getTimeZone("Asia/Singapore")
    private val fixedNow = Calendar.getInstance(singapore).apply {
        clear()
        set(2026, Calendar.AUGUST, 22, 12, 0, 0)
    }.timeInMillis

    @Test
    fun presetsUseCalendarDaysAtNineAmLocalTime() {
        assertPreset(RemindAgainPreset.TOMORROW, Calendar.AUGUST, 23)
        assertPreset(RemindAgainPreset.THREE_DAYS, Calendar.AUGUST, 25)
        assertPreset(RemindAgainPreset.ONE_WEEK, Calendar.AUGUST, 29)
    }

    @Test
    fun nextReminderMustBeStrictlyInFuture() {
        assertFalse(isValidNextReminder(fixedNow - 1, fixedNow))
        assertFalse(isValidNextReminder(fixedNow, fixedNow))
        assertTrue(isValidNextReminder(fixedNow + 1, fixedNow))
    }

    @Test
    fun statusCodesAreStableAndCancelledDoesNotExist() {
        assertEquals(listOf("ACTIVE", "IN_PROGRESS", "DONE"), ThingStatus.entries.map { it.code })
    }

    @Test
    fun actionableReminderResolutionReturnsOriginalOnlyWhenActiveAndActionable() {
        val original = thing(
            status = ThingStatus.ACTIVE,
            originalActionable = true,
            nextMillis = 9_000L
        ).actionableReminder()
        assertTrue(original is ActionableReminder.Original)
        assertEquals(null, thing(ThingStatus.ACTIVE, false, 9_000L).actionableReminder())
        assertEquals(null, thing(ThingStatus.DONE, true, 9_000L).actionableReminder())
    }

    @Test
    fun inProgressUsesExactlyOneFollowUpAndNeverOriginal() {
        val current = thing(
            status = ThingStatus.IN_PROGRESS,
            originalActionable = true,
            nextMillis = 9_000L
        ).actionableReminder()

        assertEquals(ActionableReminder.FollowUp(9_000L, "UTC"), current)
    }

    private fun thing(
        status: ThingStatus,
        originalActionable: Boolean,
        nextMillis: Long?
    ) = Thing(
        id = "id",
        name = "Passport",
        category = ThingCategory.DOCUMENT,
        importantDate = "2027-01-01",
        reminderType = ReminderType.ON_DAY,
        reminderAtEpochMillis = 8_000L,
        reminderTimeZoneId = "UTC",
        notes = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
        status = status,
        nextReminderAtEpochMillis = nextMillis,
        nextReminderTimeZoneId = "UTC",
        originalReminderActionable = originalActionable
    )

    private fun assertPreset(preset: RemindAgainPreset, month: Int, day: Int) {
        val result = Calendar.getInstance(singapore).apply {
            timeInMillis = calculateNextReminder(preset, fixedNow, singapore)
        }
        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(month, result.get(Calendar.MONTH))
        assertEquals(day, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, result.get(Calendar.MINUTE))
    }
}
