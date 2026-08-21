package com.keeply.app.model

import java.util.Calendar
import java.util.TimeZone

enum class RemindAgainPreset(val daysFromToday: Int) {
    TOMORROW(1),
    THREE_DAYS(3),
    ONE_WEEK(7)
}

fun calculateNextReminder(
    preset: RemindAgainPreset,
    nowEpochMillis: Long,
    timeZone: TimeZone
): Long = Calendar.getInstance(timeZone).apply {
    timeInMillis = nowEpochMillis
    add(Calendar.DAY_OF_MONTH, preset.daysFromToday)
    set(Calendar.HOUR_OF_DAY, 9)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun isValidNextReminder(reminderEpochMillis: Long, nowEpochMillis: Long): Boolean =
    reminderEpochMillis > nowEpochMillis

sealed interface ActionableReminder {
    data class Original(
        val type: ReminderType,
        val atEpochMillis: Long?,
        val timeZoneId: String?
    ) : ActionableReminder

    data class FollowUp(val atEpochMillis: Long, val timeZoneId: String?) : ActionableReminder
}

fun Thing.actionableReminder(): ActionableReminder? = when (status) {
    ThingStatus.ACTIVE -> if (originalReminderActionable) {
        reminderType?.let { ActionableReminder.Original(it, reminderAtEpochMillis, reminderTimeZoneId) }
    } else null
    ThingStatus.IN_PROGRESS -> nextReminderAtEpochMillis?.let {
        ActionableReminder.FollowUp(it, nextReminderTimeZoneId)
    }
    ThingStatus.DONE -> null
}
