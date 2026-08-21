package com.keeply.app.ui

import com.keeply.app.model.ReminderType
import com.keeply.app.model.Thing
import com.keeply.app.model.ThingCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

internal data class ItemDetailsUiModel(
    val name: String,
    val category: ThingCategory,
    val categoryLabel: String,
    val importantDateLabel: String,
    val importantDateText: String,
    val reminderText: String,
    val notes: String?
)

internal fun Thing.toItemDetailsUiModel(
    deviceTimeZone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault()
): ItemDetailsUiModel = ItemDetailsUiModel(
    name = name,
    category = category,
    categoryLabel = category.displayName(),
    importantDateLabel = category.importantDateLabel(),
    importantDateText = formatIsoImportantDate(importantDate),
    reminderText = reminderDisplayText(deviceTimeZone, locale),
    notes = notes?.takeIf { it.isNotBlank() }
)

internal fun ThingCategory.importantDateLabel(): String = when (this) {
    ThingCategory.DOCUMENT, ThingCategory.MEDICINE -> "Expiration date"
    ThingCategory.SUBSCRIPTION_PAYMENT -> "Renewal/payment date"
    ThingCategory.LENT_BORROWED -> "Return date"
    ThingCategory.MONEY_OWED -> "Due date"
    else -> "Important date"
}

internal fun Thing.reminderDisplayText(
    deviceTimeZone: TimeZone = TimeZone.getDefault(),
    locale: Locale = Locale.getDefault()
): String = when (reminderType) {
    null -> "No reminder"
    ReminderType.ON_DAY -> "On the day"
    ReminderType.ONE_DAY_BEFORE -> "1 day before"
    ReminderType.THREE_DAYS_BEFORE -> "3 days before"
    ReminderType.ONE_WEEK_BEFORE -> "1 week before"
    ReminderType.THIRTY_DAYS_BEFORE -> "30 days before"
    ReminderType.CUSTOM -> formatCustomReminder(deviceTimeZone, locale)
}

private fun Thing.formatCustomReminder(deviceTimeZone: TimeZone, locale: Locale): String {
    val reminderMillis = reminderAtEpochMillis ?: return "Reminder unavailable"
    val storedTimeZoneId = reminderTimeZoneId ?: return "Reminder unavailable"
    val storedTimeZone = TimeZone.getTimeZone(storedTimeZoneId)
    val formatted = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", locale).apply {
        timeZone = storedTimeZone
    }.format(Date(reminderMillis))

    return if (deviceTimeZone.id == storedTimeZone.id) {
        formatted
    } else {
        "$formatted ${readableTimeZoneIndicator(storedTimeZone, reminderMillis, locale)}"
    }
}

private fun readableTimeZoneIndicator(
    timeZone: TimeZone,
    instantMillis: Long,
    locale: Locale
): String {
    val abbreviation = SimpleDateFormat("z", locale).apply {
        this.timeZone = timeZone
    }.format(Date(instantMillis))
    if (
        !abbreviation.startsWith("GMT") &&
        !abbreviation.contains('/') &&
        abbreviation != timeZone.id
    ) return abbreviation

    val offsetMinutes = timeZone.getOffset(instantMillis) / 60_000
    val sign = if (offsetMinutes >= 0) "+" else "-"
    val absoluteMinutes = abs(offsetMinutes)
    val hours = absoluteMinutes / 60
    val minutes = absoluteMinutes % 60
    return if (minutes == 0) {
        "GMT$sign$hours"
    } else {
        "GMT$sign$hours:${minutes.toString().padStart(2, '0')}"
    }
}
