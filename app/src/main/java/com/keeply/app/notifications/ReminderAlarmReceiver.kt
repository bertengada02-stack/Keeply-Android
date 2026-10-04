package com.keeply.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.keeply.app.KeeplyApplication
import com.keeply.app.model.ActionableReminder
import com.keeply.app.model.Thing
import com.keeply.app.model.actionableReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.TimeZone

private const val SNOOZE_DURATION_MILLIS = 60L * 60L * 1000L

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SNOOZE_REMINDER) {
            handleSnooze(context, intent)
            return
        }
        if (intent.action == ACTION_MARK_DONE) {
            handleMarkDone(context, intent)
            return
        }
        if (intent.action != ACTION_DELIVER_REMINDER) return
        val thingId = intent.getStringExtra(EXTRA_THING_ID) ?: return
        val expectedEpoch = intent.getLongExtra(EXTRA_EXPECTED_EPOCH, -1L)
        reminderLog("receiver invoked thingId=$thingId expectedEpoch=$expectedEpoch")
        if (expectedEpoch <= 0L) {
            reminderLog("receiver rejected invalid epoch thingId=$thingId")
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = (context.applicationContext as KeeplyApplication).thingRepository
                val thing = repository.findById(thingId)
                if (thing == null) {
                    reminderLog("receiver missing Thing thingId=$thingId")
                    return@launch
                }
                if (thing.matchesExpectedReminder(expectedEpoch)) {
                    reminderLog("receiver validation passed thingId=$thingId epoch=$expectedEpoch")
                    val app = context.applicationContext as KeeplyApplication
                    app.missedReminderRecovery.handleDueReminder(thing, expectedEpoch)
                } else {
                    reminderLog("receiver validation rejected stale reminder thingId=$thingId epoch=$expectedEpoch")
                }
            } catch (error: Exception) {
                // Fail closed: a transient database/notification error must not post stale data.
                reminderLogError("receiver failed thingId=$thingId", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleMarkDone(context: Context, intent: Intent) {
        val thingId = intent.getStringExtra(EXTRA_THING_ID) ?: return
        val app = context.applicationContext as KeeplyApplication
        if (!app.proManager.isPro.value) {
            reminderLog("mark done from notification rejected non-Pro thingId=$thingId")
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val updated = app.thingRepository.markDone(thingId)
                app.reminderSyncCoordinator.sync(updated)
                NotificationManagerCompat.from(context).cancel(thingId, REMINDER_NOTIFICATION_ID)
                reminderLog("marked done from notification thingId=$thingId")
            } catch (error: Exception) {
                reminderLogError("mark done from notification failed thingId=$thingId", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val thingId = intent.getStringExtra(EXTRA_THING_ID) ?: return
        val app = context.applicationContext as KeeplyApplication
        if (!app.proManager.isPro.value) {
            reminderLog("snooze rejected non-Pro thingId=$thingId")
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val thing = app.thingRepository.findById(thingId)
                if (thing == null) {
                    reminderLog("snooze rejected missing Thing thingId=$thingId")
                    return@launch
                }
                val snoozeAt = System.currentTimeMillis() + SNOOZE_DURATION_MILLIS
                val updated = app.thingRepository.remindAgain(
                    thingId,
                    snoozeAt,
                    TimeZone.getDefault().id
                )
                val syncResult = app.reminderSyncCoordinator.sync(updated)
                NotificationManagerCompat.from(context).cancel(thingId, REMINDER_NOTIFICATION_ID)
                reminderLog("snoozed thingId=$thingId epoch=$snoozeAt sync=$syncResult")
            } catch (error: Exception) {
                reminderLogError("snooze failed thingId=$thingId", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

internal fun Thing.matchesExpectedReminder(expectedEpoch: Long): Boolean {
    val currentEpoch = when (val reminder = actionableReminder()) {
        is ActionableReminder.Original -> reminder.atEpochMillis
        is ActionableReminder.FollowUp -> reminder.atEpochMillis
        null -> null
    }
    return expectedEpoch > 0L &&
        reminderDeliveryState == com.keeply.app.model.ReminderDeliveryState.PENDING &&
        currentEpoch == expectedEpoch
}
