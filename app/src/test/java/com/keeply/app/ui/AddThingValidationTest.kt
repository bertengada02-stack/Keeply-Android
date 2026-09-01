package com.keeply.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AddThingValidationTest {
    private val timeZone = TimeZone.getTimeZone("Asia/Singapore")

    @Test
    fun blankNameShowsFriendlyError() {
        assertEquals(NAME_REQUIRED_ERROR, validateName("   "))
    }

    @Test
    fun missingImportantDateShowsFriendlyError() {
        assertEquals(IMPORTANT_DATE_REQUIRED_ERROR, validateImportantDate(null))
    }

    @Test
    fun onTheDayUsesNineAmLocalTime() {
        val importantDate = localMillis(2026, Calendar.AUGUST, 21, 17, 45)
        assertEquals(
            localMillis(2026, Calendar.AUGUST, 21, 9, 0),
            presetReminderMillis(importantDate, ReminderChoice.ON_DAY, timeZone)
        )
    }

    @Test
    fun oneDayBeforeUsesCalendarDayAtNineAm() = assertPresetOffset(ReminderChoice.ONE_DAY_BEFORE, 19)

    @Test
    fun threeDaysBeforeUsesCalendarDayAtNineAm() = assertPresetOffset(ReminderChoice.THREE_DAYS_BEFORE, 17)

    @Test
    fun oneWeekBeforeUsesCalendarDayAtNineAm() = assertPresetOffset(ReminderChoice.ONE_WEEK_BEFORE, 13)

    @Test
    fun thirtyDaysBeforeUsesCalendarDayAtNineAm() {
        val importantDate = localMillis(2026, Calendar.SEPTEMBER, 19, 0, 0)
        assertEquals(
            localMillis(2026, Calendar.AUGUST, 20, 9, 0),
            presetReminderMillis(importantDate, ReminderChoice.THIRTY_DAYS_BEFORE, timeZone)
        )
    }

    @Test
    fun presetReminderInPastIsRejected() {
        val importantDate = localMillis(2026, Calendar.AUGUST, 20, 0, 0)
        val now = localMillis(2026, Calendar.AUGUST, 20, 10, 0)
        val reminder = presetReminderMillis(importantDate, ReminderChoice.ON_DAY, timeZone)
        assertEquals(PAST_REMINDER_ERROR, validateReminderTime(reminder, now))
    }

    @Test
    fun customReminderInPastIsRejected() {
        val now = localMillis(2026, Calendar.AUGUST, 20, 10, 0)
        assertEquals(
            PAST_REMINDER_ERROR,
            validateReminderTime(localMillis(2026, Calendar.AUGUST, 20, 9, 59), now)
        )
    }

    @Test
    fun customReminderInFutureIsAccepted() {
        val now = localMillis(2026, Calendar.AUGUST, 20, 10, 0)
        assertNull(validateReminderTime(localMillis(2026, Calendar.AUGUST, 20, 10, 1), now))
    }

    @Test
    fun reminderAfterImportantDateIsInvalid() {
        val importantDate = localMillis(2026, Calendar.AUGUST, 25, 0, 0)
        assertEquals(
            REMINDER_AFTER_IMPORTANT_DATE_ERROR,
            validateReminderWindow(
                localMillis(2026, Calendar.AUGUST, 26, 0, 0),
                localMillis(2026, Calendar.AUGUST, 22, 12, 0),
                importantDate,
                timeZone
            )
        )
    }

    @Test
    fun reminderAtEndOfImportantDateIsValid() {
        val importantDate = localMillis(2026, Calendar.AUGUST, 25, 0, 0)
        assertNull(
            validateReminderWindow(
                importantDateCutoffMillis(importantDate, timeZone),
                localMillis(2026, Calendar.AUGUST, 22, 12, 0),
                importantDate,
                timeZone
            )
        )
    }

    @Test
    fun futureCustomReminderBeforeImportantDateIsValid() {
        val importantDate = localMillis(2026, Calendar.AUGUST, 25, 0, 0)
        assertNull(
            validateReminderWindow(
                localMillis(2026, Calendar.AUGUST, 24, 18, 30),
                localMillis(2026, Calendar.AUGUST, 22, 12, 0),
                importantDate,
                timeZone
            )
        )
    }

    @Test
    fun customReminderAfterImportantDateIsInvalid() {
        val importantDate = localMillis(2026, Calendar.AUGUST, 25, 0, 0)
        assertEquals(
            REMINDER_AFTER_IMPORTANT_DATE_ERROR,
            validateReminderWindow(
                localMillis(2026, Calendar.AUGUST, 26, 8, 0),
                localMillis(2026, Calendar.AUGUST, 22, 12, 0),
                importantDate,
                timeZone
            )
        )
    }

    @Test
    fun presetCalculatedIntoPastIsUnavailable() {
        val importantDate = localMillis(2026, Calendar.AUGUST, 25, 0, 0)
        val now = localMillis(2026, Calendar.AUGUST, 22, 10, 0)
        assertEquals(
            false,
            isPresetReminderAvailable(
                importantDate,
                ReminderChoice.THREE_DAYS_BEFORE,
                now,
                timeZone
            )
        )
    }

    @Test
    fun presetBeforeImportantDateIsAvailable() {
        val importantDate = localMillis(2026, Calendar.AUGUST, 25, 0, 0)
        val now = localMillis(2026, Calendar.AUGUST, 22, 10, 0)
        assertEquals(
            true,
            isPresetReminderAvailable(
                importantDate,
                ReminderChoice.ONE_DAY_BEFORE,
                now,
                timeZone
            )
        )
    }

    @Test
    fun reminderBecomesInvalidWhenImportantDateMovesEarlier() {
        val reminder = localMillis(2026, Calendar.AUGUST, 24, 18, 0)
        val now = localMillis(2026, Calendar.AUGUST, 22, 10, 0)
        val earlierImportantDate = localMillis(2026, Calendar.AUGUST, 23, 0, 0)
        assertEquals(
            REMINDER_AFTER_IMPORTANT_DATE_ERROR,
            validateReminderWindow(reminder, now, earlierImportantDate, timeZone)
        )
    }

    @Test
    fun customReminderTwoMinutesBeforeNowIsInvalid() {
        val now = localMillis(2026, Calendar.AUGUST, 22, 0, 54)
        val importantDate = localMillis(2026, Calendar.AUGUST, 25, 0, 0)
        assertEquals(
            PAST_REMINDER_ERROR,
            validateReminderWindow(
                localMillis(2026, Calendar.AUGUST, 22, 0, 52),
                now,
                importantDate,
                timeZone
            )
        )
    }

    @Test
    fun customReminderExactlyNowIsInvalid() {
        val now = localMillis(2026, Calendar.AUGUST, 22, 0, 54)
        val importantDate = localMillis(2026, Calendar.AUGUST, 25, 0, 0)
        assertEquals(
            PAST_REMINDER_ERROR,
            validateReminderWindow(now, now, importantDate, timeZone)
        )
    }

    @Test
    fun customReminderTwoMinutesAfterNowIsValid() {
        val now = localMillis(2026, Calendar.AUGUST, 22, 0, 54)
        val importantDate = localMillis(2026, Calendar.AUGUST, 25, 0, 0)
        assertNull(
            validateReminderWindow(
                localMillis(2026, Calendar.AUGUST, 22, 0, 56),
                now,
                importantDate,
                timeZone
            )
        )
    }

    @Test
    fun originallyFutureReminderIsInvalidBySubmissionTime() {
        val reminder = localMillis(2026, Calendar.AUGUST, 22, 0, 53)
        val importantDate = localMillis(2026, Calendar.AUGUST, 25, 0, 0)
        val selectionNow = localMillis(2026, Calendar.AUGUST, 22, 0, 52)
        val submissionNow = localMillis(2026, Calendar.AUGUST, 22, 0, 54)

        assertNull(validateReminderWindow(reminder, selectionNow, importantDate, timeZone))
        assertEquals(
            PAST_REMINDER_ERROR,
            validateReminderWindow(reminder, submissionNow, importantDate, timeZone)
        )
    }

    @Test
    fun customReminderInstantDoesNotShiftWhenImportantDateChangesAndRemainsValid() {
        val customReminder = localMillis(2026, Calendar.AUGUST, 24, 15, 30)
        val now = localMillis(2026, Calendar.AUGUST, 22, 10, 0)
        val changedImportantDate = localMillis(2026, Calendar.AUGUST, 26, 0, 0)

        val resolved = resolveReminderMillis(
            changedImportantDate,
            ReminderChoice.CUSTOM,
            customReminder,
            timeZone
        )

        assertEquals(customReminder, resolved)
        assertNull(validateReminderWindow(checkNotNull(resolved), now, changedImportantDate, timeZone))
    }

    @Test
    fun customReminderBecomesInvalidWhenImportantDateMovesBeforeIt() {
        val customReminder = localMillis(2026, Calendar.AUGUST, 24, 15, 30)
        val now = localMillis(2026, Calendar.AUGUST, 22, 10, 0)
        val changedImportantDate = localMillis(2026, Calendar.AUGUST, 23, 0, 0)

        assertEquals(
            REMINDER_AFTER_IMPORTANT_DATE_ERROR,
            validateReminderWindow(customReminder, now, changedImportantDate, timeZone)
        )
    }

    @Test
    fun presetReminderRecalculatesWhenImportantDateChanges() {
        val originalDate = localMillis(2026, Calendar.AUGUST, 25, 0, 0)
        val changedDate = localMillis(2026, Calendar.SEPTEMBER, 2, 0, 0)

        assertEquals(
            localMillis(2026, Calendar.AUGUST, 24, 9, 0),
            resolveReminderMillis(originalDate, ReminderChoice.ONE_DAY_BEFORE, null, timeZone)
        )
        assertEquals(
            localMillis(2026, Calendar.SEPTEMBER, 1, 9, 0),
            resolveReminderMillis(changedDate, ReminderChoice.ONE_DAY_BEFORE, null, timeZone)
        )
    }

    @Test
    fun newThingOffersValidChoicesForFutureImportantDate() {
        assertEquals(
            setOf(
                ReminderChoice.ON_DAY,
                ReminderChoice.ONE_DAY_BEFORE,
                ReminderChoice.THREE_DAYS_BEFORE,
                ReminderChoice.CUSTOM
            ),
            choicesForAugustTwentyFifth()
        )
    }

    @Test
    fun activeEditOffersSameChoicesAsNewThing() {
        val createChoices = choicesForAugustTwentyFifth()
        val activeEditChoices = availableReminderChoices(
            importantDateMillis = localMillis(2026, Calendar.AUGUST, 25, 0, 0),
            nowMillis = localMillis(2026, Calendar.AUGUST, 20, 10, 0),
            timeZone = timeZone
        )

        assertEquals(createChoices, activeEditChoices)
    }

    @Test
    fun reopenedDoneEditOffersSameChoicesAsNewThing() {
        val createChoices = choicesForAugustTwentyFifth()
        val reopenedEditChoices = choicesForAugustTwentyFifth()

        assertEquals(createChoices, reopenedEditChoices)
        assertEquals(true, ReminderChoice.ON_DAY in reopenedEditChoices)
        assertEquals(true, ReminderChoice.ONE_DAY_BEFORE in reopenedEditChoices)
    }

    @Test
    fun oneDayBeforeIsUnavailableAfterItsReminderTime() {
        val choices = availableReminderChoices(
            importantDateMillis = localMillis(2026, Calendar.AUGUST, 25, 0, 0),
            nowMillis = localMillis(2026, Calendar.AUGUST, 24, 9, 0),
            timeZone = timeZone
        )

        assertEquals(false, ReminderChoice.ONE_DAY_BEFORE in choices)
    }

    @Test
    fun onTheDayIsUnavailableAfterItsReminderTime() {
        val choices = availableReminderChoices(
            importantDateMillis = localMillis(2026, Calendar.AUGUST, 25, 0, 0),
            nowMillis = localMillis(2026, Calendar.AUGUST, 25, 9, 0),
            timeZone = timeZone
        )

        assertEquals(false, ReminderChoice.ON_DAY in choices)
    }

    @Test
    fun changingImportantDateRecalculatesAvailableChoices() {
        val now = localMillis(2026, Calendar.AUGUST, 24, 10, 0)
        val originalChoices = availableReminderChoices(
            localMillis(2026, Calendar.AUGUST, 25, 0, 0),
            now,
            timeZone
        )
        val changedChoices = availableReminderChoices(
            localMillis(2026, Calendar.AUGUST, 27, 0, 0),
            now,
            timeZone
        )

        assertEquals(false, ReminderChoice.ONE_DAY_BEFORE in originalChoices)
        assertEquals(true, ReminderChoice.ONE_DAY_BEFORE in changedChoices)
        assertEquals(true, ReminderChoice.ON_DAY in changedChoices)
    }

    private fun choicesForAugustTwentyFifth(): Set<ReminderChoice> =
        availableReminderChoices(
            importantDateMillis = localMillis(2026, Calendar.AUGUST, 25, 0, 0),
            nowMillis = localMillis(2026, Calendar.AUGUST, 20, 10, 0),
            timeZone = timeZone
        )

    private fun assertPresetOffset(choice: ReminderChoice, expectedDay: Int) {
        val importantDate = localMillis(2026, Calendar.AUGUST, 20, 23, 30)
        assertEquals(
            localMillis(2026, Calendar.AUGUST, expectedDay, 9, 0),
            presetReminderMillis(importantDate, choice, timeZone)
        )
    }

    private fun localMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
}
