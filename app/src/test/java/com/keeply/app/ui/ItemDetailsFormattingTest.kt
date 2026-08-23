package com.keeply.app.ui

import com.keeply.app.model.ReminderType
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.ThingStatus
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemDetailsFormattingTest {
    @Test
    fun categoriesUseTheApprovedImportantDateLabels() {
        assertEquals("Expiration date", ThingCategory.DOCUMENT.importantDateLabel())
        assertEquals("Renewal/payment date", ThingCategory.SUBSCRIPTION_PAYMENT.importantDateLabel())
        assertEquals("Return date", ThingCategory.LENT_BORROWED.importantDateLabel())
        assertEquals("Due date", ThingCategory.MONEY_OWED.importantDateLabel())
        assertEquals("Expiration date", ThingCategory.MEDICINE.importantDateLabel())
        listOf(
            ThingCategory.OWNED_ITEM,
            ThingCategory.VEHICLE,
            ThingCategory.HOME_APPLIANCE,
            ThingCategory.OTHER
        ).forEach { category ->
            assertEquals("Important date", category.importantDateLabel())
        }
    }

    @Test
    fun presetAndMissingRemindersUseHumanReadableText() {
        assertEquals("No reminder", thing(reminderType = null).reminderDisplayText())
        assertEquals("On the day", thing(ReminderType.ON_DAY).reminderDisplayText())
        assertEquals("1 day before", thing(ReminderType.ONE_DAY_BEFORE).reminderDisplayText())
        assertEquals("3 days before", thing(ReminderType.THREE_DAYS_BEFORE).reminderDisplayText())
        assertEquals("1 week before", thing(ReminderType.ONE_WEEK_BEFORE).reminderDisplayText())
        assertEquals("30 days before", thing(ReminderType.THIRTY_DAYS_BEFORE).reminderDisplayText())
    }

    @Test
    fun customReminderOmitsTimezoneWhenDeviceUsesStoredTimezone() {
        val singapore = TimeZone.getTimeZone("Asia/Singapore")
        val reminder = thing(
            reminderType = ReminderType.CUSTOM,
            reminderMillis = localMillis(singapore),
            reminderTimeZoneId = singapore.id
        )

        assertEquals(
            "Aug 25, 2026 at 3:30 PM",
            reminder.reminderDisplayText(singapore, Locale.US)
        )
    }

    @Test
    fun customReminderUsesStoredTimezoneAndAddsIndicatorWhenDeviceDiffers() {
        val singapore = TimeZone.getTimeZone("Asia/Singapore")
        val reminder = thing(
            reminderType = ReminderType.CUSTOM,
            reminderMillis = localMillis(singapore),
            reminderTimeZoneId = singapore.id
        )

        assertEquals(
            "Aug 25, 2026 at 3:30 PM GMT+8",
            reminder.reminderDisplayText(TimeZone.getTimeZone("UTC"), Locale.US)
        )
    }

    @Test
    fun customReminderWithIncompletePersistedDataFailsSafely() {
        assertEquals(
            "Reminder unavailable",
            thing(ReminderType.CUSTOM).reminderDisplayText(TimeZone.getTimeZone("UTC"), Locale.US)
        )
    }

    @Test
    fun notesArePresentOnlyWhenTheyContainText() {
        assertEquals("Keep it current", thing(notes = "Keep it current").toItemDetailsUiModel().notes)
        assertNull(thing(notes = null).toItemDetailsUiModel().notes)
        assertNull(thing(notes = "   ").toItemDetailsUiModel().notes)
    }

    @Test
    fun categoryMappingIsPreservedInDetailsModel() {
        val details = thing(category = ThingCategory.VEHICLE).toItemDetailsUiModel()

        assertEquals(ThingCategory.VEHICLE, details.category)
        assertEquals("Vehicle", details.categoryLabel)
        assertEquals("Important date", details.importantDateLabel)
    }

    @Test
    fun detailsIncludeImportantDateUrgencyWithoutUsingReminderTime() {
        val futureReminder = localMillis(TimeZone.getTimeZone("UTC"))
        val overdue = thing(
            reminderType = ReminderType.CUSTOM,
            reminderMillis = futureReminder,
            reminderTimeZoneId = "UTC",
            originalReminderActionable = true
        ).copy(importantDate = "2026-08-17")

        val overdueDetails = overdue.toItemDetailsUiModel(
            deviceTimeZone = TimeZone.getTimeZone("UTC"),
            locale = Locale.US,
            currentLocalDate = "2026-08-22"
        )
        assertEquals(ImportantDateUrgency.OVERDUE, overdueDetails.importantDateContext?.urgency)
        assertEquals("5 days overdue", overdueDetails.importantDateContext?.timingText)

        val approaching = overdue.copy(importantDate = "2026-08-30").toItemDetailsUiModel(
            deviceTimeZone = TimeZone.getTimeZone("UTC"),
            locale = Locale.US,
            currentLocalDate = "2026-08-22"
        )
        assertEquals(ImportantDateUrgency.APPROACHING, approaching.importantDateContext?.urgency)
        assertEquals("In 8 days", approaching.importantDateContext?.timingText)
    }

    @Test
    fun detailsSelectsReminderSourceFromLifecycleState() {
        val active = thing(
            reminderType = ReminderType.ON_DAY,
            originalReminderActionable = true
        )
        assertEquals("On the day", active.toItemDetailsUiModel().reminderText)

        val reopened = thing(
            reminderType = ReminderType.ON_DAY,
            originalReminderActionable = false
        )
        assertEquals("No active reminder", reopened.toItemDetailsUiModel().reminderText)

        val inProgress = thing(
            reminderType = ReminderType.ON_DAY,
            originalReminderActionable = false,
            status = ThingStatus.IN_PROGRESS,
            nextReminderMillis = localMillis(TimeZone.getTimeZone("UTC")),
            nextReminderZone = "UTC"
        )
        assertEquals(
            "Aug 25, 2026 at 3:30 PM",
            inProgress.toItemDetailsUiModel(TimeZone.getTimeZone("UTC"), Locale.US).reminderText
        )

        val done = active.copy(status = ThingStatus.DONE)
        assertEquals("No active reminder", done.toItemDetailsUiModel().reminderText)
    }

    @Test
    fun saveFeedbackUsesOnlyTheCurrentActionableReminder() {
        val utc = TimeZone.getTimeZone("UTC")
        val historicalOnly = thing(
            reminderType = ReminderType.CUSTOM,
            reminderMillis = localMillis(utc),
            reminderTimeZoneId = "UTC",
            originalReminderActionable = false
        )
        assertEquals(
            "Saved to Keeply\nNo reminder is set, so Keeply won't notify you about this thing.",
            historicalOnly.persistenceSuccessMessage(utc, Locale.US)
        )

        val activeOriginal = historicalOnly.copy(originalReminderActionable = true)
        assertEquals(
            "Reminder set\nKeeply will remind you on Aug 25, 2026 at 3:30 PM.",
            activeOriginal.persistenceSuccessMessage(utc, Locale.US)
        )

        val followUp = historicalOnly.copy(
            status = ThingStatus.IN_PROGRESS,
            nextReminderAtEpochMillis = localMillis(utc),
            nextReminderTimeZoneId = "UTC"
        )
        assertEquals(
            "Reminder set\nKeeply will remind you on Aug 25, 2026 at 3:30 PM.",
            followUp.persistenceSuccessMessage(utc, Locale.US)
        )
    }

    private fun thing(
        reminderType: ReminderType? = null,
        reminderMillis: Long? = null,
        reminderTimeZoneId: String? = null,
        notes: String? = null,
        category: ThingCategory = ThingCategory.DOCUMENT,
        originalReminderActionable: Boolean = false,
        status: ThingStatus = ThingStatus.ACTIVE,
        nextReminderMillis: Long? = null,
        nextReminderZone: String? = null
    ) = Thing(
        id = "thing-id",
        name = "Passport",
        category = category,
        importantDate = "2026-08-25",
        reminderType = reminderType,
        reminderAtEpochMillis = reminderMillis,
        reminderTimeZoneId = reminderTimeZoneId,
        notes = notes,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
        status = status,
        nextReminderAtEpochMillis = nextReminderMillis,
        nextReminderTimeZoneId = nextReminderZone,
        originalReminderActionable = originalReminderActionable
    )

    private fun localMillis(timeZone: TimeZone): Long = Calendar.getInstance(timeZone).apply {
        clear()
        set(2026, Calendar.AUGUST, 25, 15, 30, 0)
    }.timeInMillis
}
