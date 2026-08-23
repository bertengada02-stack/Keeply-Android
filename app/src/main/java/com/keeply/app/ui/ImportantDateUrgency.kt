package com.keeply.app.ui

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

internal enum class ImportantDateUrgency(val label: String) {
    OVERDUE("OVERDUE"),
    DUE_TODAY("DUE TODAY"),
    VERY_SOON("VERY SOON"),
    APPROACHING("APPROACHING"),
    LATER("LATER")
}

internal data class ImportantDateContext(
    val urgency: ImportantDateUrgency,
    val dayOffset: Int,
    val timingText: String
)

internal fun importantDateContext(
    importantDateIso: String,
    currentLocalDateIso: String
): ImportantDateContext? {
    val importantDay = isoDateDayIndex(importantDateIso) ?: return null
    val currentDay = isoDateDayIndex(currentLocalDateIso) ?: return null
    val dayOffset = (importantDay - currentDay).toInt()
    val urgency = when {
        dayOffset < 0 -> ImportantDateUrgency.OVERDUE
        dayOffset == 0 -> ImportantDateUrgency.DUE_TODAY
        dayOffset <= 3 -> ImportantDateUrgency.VERY_SOON
        dayOffset <= 14 -> ImportantDateUrgency.APPROACHING
        else -> ImportantDateUrgency.LATER
    }
    val timingText = when (dayOffset) {
        in Int.MIN_VALUE..-1 -> {
            val overdueDays = abs(dayOffset)
            if (overdueDays == 1) "1 day overdue" else "$overdueDays days overdue"
        }
        0 -> "Due today"
        1 -> "Due tomorrow"
        else -> "In $dayOffset days"
    }
    return ImportantDateContext(urgency, dayOffset, timingText)
}

internal fun currentLocalDateIso(
    nowEpochMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = nowEpochMillis }
    return String.format(
        Locale.US,
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

internal fun isoDateDayIndex(isoDate: String): Long? {
    val match = IsoDatePattern.matchEntire(isoDate) ?: return null
    val (year, month, day) = match.destructured
    return try {
        GregorianCalendar(Utc).apply {
            isLenient = false
            clear()
            set(year.toInt(), month.toInt() - 1, day.toInt(), 0, 0, 0)
        }.timeInMillis / MillisPerDay
    } catch (_: IllegalArgumentException) {
        null
    }
}

private val IsoDatePattern = Regex("(\\d{4})-(\\d{2})-(\\d{2})")
private val Utc = TimeZone.getTimeZone("UTC")
private const val MillisPerDay = 86_400_000L
