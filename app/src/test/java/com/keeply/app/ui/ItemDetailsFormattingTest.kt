package com.keeply.app.ui

import com.keeply.app.model.ReminderType
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
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

    private fun thing(
        reminderType: ReminderType? = null,
        reminderMillis: Long? = null,
        reminderTimeZoneId: String? = null,
        notes: String? = null,
        category: ThingCategory = ThingCategory.DOCUMENT
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
        updatedAtEpochMillis = 1L
    )

    private fun localMillis(timeZone: TimeZone): Long = Calendar.getInstance(timeZone).apply {
        clear()
        set(2026, Calendar.AUGUST, 25, 15, 30, 0)
    }.timeInMillis
}
