package com.keeply.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.keeply.app.KeeplyApplication
import com.keeply.app.model.ActionableReminder
import com.keeply.app.model.Thing
import com.keeply.app.model.actionableReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
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
