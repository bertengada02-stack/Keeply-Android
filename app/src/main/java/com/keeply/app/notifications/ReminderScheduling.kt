package com.keeply.app.notifications

import com.keeply.app.model.ActionableReminder
import com.keeply.app.model.Thing
import com.keeply.app.model.ReminderDeliveryState
import com.keeply.app.model.actionableReminder

internal sealed interface ReminderScheduleDecision {
    data object None : ReminderScheduleDecision
    data class Past(val triggerAtEpochMillis: Long) : ReminderScheduleDecision
    data class Future(
        val thingId: String,
        val triggerAtEpochMillis: Long,
        val timeZoneId: String?
    ) : ReminderScheduleDecision
}

internal fun Thing.reminderScheduleDecision(nowEpochMillis: Long): ReminderScheduleDecision {
    if (reminderDeliveryState != ReminderDeliveryState.PENDING) {
        return ReminderScheduleDecision.None
    }
    val actionable = actionableReminder() ?: return ReminderScheduleDecision.None
    val (trigger, zone) = when (actionable) {
        is ActionableReminder.Original -> actionable.atEpochMillis to actionable.timeZoneId
        is ActionableReminder.FollowUp -> actionable.atEpochMillis to actionable.timeZoneId
    }
    val epoch = trigger ?: return ReminderScheduleDecision.None
    return if (epoch <= nowEpochMillis) ReminderScheduleDecision.Past(epoch)
    else ReminderScheduleDecision.Future(id, epoch, zone)
}

internal sealed interface ReminderSyncResult {
    data object NoReminder : ReminderSyncResult
    data object Past : ReminderSyncResult
    data object ScheduledExact : ReminderSyncResult
    data object ScheduledInexact : ReminderSyncResult
    data object NotificationsDisabled : ReminderSyncResult
    data object Failed : ReminderSyncResult
}

internal interface ReminderScheduler {
    fun sync(thing: Thing): ReminderSyncResult
    fun cancel(thingId: String)
}

internal class ReminderSyncCoordinator(private val scheduler: ReminderScheduler) {
    fun sync(thing: Thing): ReminderSyncResult = try {
        scheduler.sync(thing)
    } catch (_: Exception) {
        ReminderSyncResult.Failed
    }

    fun cancel(thingId: String): ReminderSyncResult = try {
        scheduler.cancel(thingId)
        ReminderSyncResult.NoReminder
    } catch (_: Exception) {
        ReminderSyncResult.Failed
    }

    fun syncAll(things: List<Thing>) {
        things.forEach(::sync)
    }
}
