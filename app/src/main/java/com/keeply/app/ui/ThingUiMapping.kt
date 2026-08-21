package com.keeply.app.ui

import com.keeply.app.model.ReminderType
import com.keeply.app.model.ThingCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

internal fun CategoryGlyph.toThingCategory(): ThingCategory = when (this) {
    CategoryGlyph.DOCUMENT -> ThingCategory.DOCUMENT
    CategoryGlyph.OWNED -> ThingCategory.OWNED_ITEM
    CategoryGlyph.PAYMENT -> ThingCategory.SUBSCRIPTION_PAYMENT
    CategoryGlyph.EXCHANGE -> ThingCategory.LENT_BORROWED
    CategoryGlyph.MONEY -> ThingCategory.MONEY_OWED
    CategoryGlyph.VEHICLE -> ThingCategory.VEHICLE
    CategoryGlyph.HOME -> ThingCategory.HOME_APPLIANCE
    CategoryGlyph.MEDICINE -> ThingCategory.MEDICINE
    CategoryGlyph.OTHER -> ThingCategory.OTHER
}

internal fun ThingCategory.toCategoryGlyph(): CategoryGlyph = when (this) {
    ThingCategory.DOCUMENT -> CategoryGlyph.DOCUMENT
    ThingCategory.OWNED_ITEM -> CategoryGlyph.OWNED
    ThingCategory.SUBSCRIPTION_PAYMENT -> CategoryGlyph.PAYMENT
    ThingCategory.LENT_BORROWED -> CategoryGlyph.EXCHANGE
    ThingCategory.MONEY_OWED -> CategoryGlyph.MONEY
    ThingCategory.VEHICLE -> CategoryGlyph.VEHICLE
    ThingCategory.HOME_APPLIANCE -> CategoryGlyph.HOME
    ThingCategory.MEDICINE -> CategoryGlyph.MEDICINE
    ThingCategory.OTHER -> CategoryGlyph.OTHER
}

internal fun ThingCategory.displayName(): String = when (this) {
    ThingCategory.DOCUMENT -> "Document"
    ThingCategory.OWNED_ITEM -> "Something I own"
    ThingCategory.SUBSCRIPTION_PAYMENT -> "Subscription/payment"
    ThingCategory.LENT_BORROWED -> "Lent/borrowed"
    ThingCategory.MONEY_OWED -> "Money owed"
    ThingCategory.VEHICLE -> "Vehicle"
    ThingCategory.HOME_APPLIANCE -> "Home/appliance"
    ThingCategory.MEDICINE -> "Medicine"
    ThingCategory.OTHER -> "Something else"
}

internal fun ReminderChoice.toReminderType(): ReminderType = when (this) {
    ReminderChoice.ON_DAY -> ReminderType.ON_DAY
    ReminderChoice.ONE_DAY_BEFORE -> ReminderType.ONE_DAY_BEFORE
    ReminderChoice.THREE_DAYS_BEFORE -> ReminderType.THREE_DAYS_BEFORE
    ReminderChoice.ONE_WEEK_BEFORE -> ReminderType.ONE_WEEK_BEFORE
    ReminderChoice.THIRTY_DAYS_BEFORE -> ReminderType.THIRTY_DAYS_BEFORE
    ReminderChoice.CUSTOM -> ReminderType.CUSTOM
}

internal fun ReminderType.toReminderChoice(): ReminderChoice = when (this) {
    ReminderType.ON_DAY -> ReminderChoice.ON_DAY
    ReminderType.ONE_DAY_BEFORE -> ReminderChoice.ONE_DAY_BEFORE
    ReminderType.THREE_DAYS_BEFORE -> ReminderChoice.THREE_DAYS_BEFORE
    ReminderType.ONE_WEEK_BEFORE -> ReminderChoice.ONE_WEEK_BEFORE
    ReminderType.THIRTY_DAYS_BEFORE -> ReminderChoice.THIRTY_DAYS_BEFORE
    ReminderType.CUSTOM -> ReminderChoice.CUSTOM
}

internal fun importantDateToIso(millis: Long, timeZoneId: String): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).apply {
        timeInMillis = millis
    }
    return String.format(
        Locale.US,
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

internal fun isoImportantDateToMillis(isoDate: String, timeZoneId: String): Long? {
    val parts = isoDate.split('-')
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)).apply {
        isLenient = false
        clear()
        set(year, month - 1, day, 0, 0, 0)
        try {
            timeInMillis
        } catch (_: IllegalArgumentException) {
            return null
        }
    }.timeInMillis
}

internal fun formatIsoImportantDate(isoDate: String): String {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
    }.parse(isoDate) ?: return isoDate
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsed)
}
