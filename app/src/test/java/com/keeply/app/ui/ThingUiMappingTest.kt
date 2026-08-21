package com.keeply.app.ui

import com.keeply.app.model.ReminderType
import com.keeply.app.model.ThingCategory
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class ThingUiMappingTest {
    @Test
    fun everyCategoryGlyphMapsToItsStableCategory() {
        assertEquals(
            listOf(
                ThingCategory.DOCUMENT,
                ThingCategory.OWNED_ITEM,
                ThingCategory.SUBSCRIPTION_PAYMENT,
                ThingCategory.LENT_BORROWED,
                ThingCategory.MONEY_OWED,
                ThingCategory.VEHICLE,
                ThingCategory.HOME_APPLIANCE,
                ThingCategory.MEDICINE,
                ThingCategory.OTHER
            ),
            CategoryGlyph.entries.map(CategoryGlyph::toThingCategory)
        )
    }

    @Test
    fun reminderChoicesMapToStableReminderTypes() {
        assertEquals(ReminderType.ON_DAY, ReminderChoice.ON_DAY.toReminderType())
        assertEquals(ReminderType.ONE_DAY_BEFORE, ReminderChoice.ONE_DAY_BEFORE.toReminderType())
        assertEquals(ReminderType.THREE_DAYS_BEFORE, ReminderChoice.THREE_DAYS_BEFORE.toReminderType())
        assertEquals(ReminderType.ONE_WEEK_BEFORE, ReminderChoice.ONE_WEEK_BEFORE.toReminderType())
        assertEquals(ReminderType.THIRTY_DAYS_BEFORE, ReminderChoice.THIRTY_DAYS_BEFORE.toReminderType())
        assertEquals(ReminderType.CUSTOM, ReminderChoice.CUSTOM.toReminderType())
    }

    @Test
    fun importantDateUsesIsoLocalDateInControlledTimeZone() {
        val timeZone = TimeZone.getTimeZone("Asia/Singapore")
        val date = Calendar.getInstance(timeZone).apply {
            clear()
            set(2026, Calendar.AUGUST, 22, 23, 45)
        }.timeInMillis

        assertEquals("2026-08-22", importantDateToIso(date, timeZone.id))
    }
}
