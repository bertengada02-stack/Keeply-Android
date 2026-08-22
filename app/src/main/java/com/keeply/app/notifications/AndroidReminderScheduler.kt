package com.keeply.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.keeply.app.model.Thing

internal const val ACTION_DELIVER_REMINDER = "com.keeply.app.action.DELIVER_REMINDER"
internal const val EXTRA_THING_ID = "thing_id"
internal const val EXTRA_EXPECTED_EPOCH = "expected_reminder_epoch"
internal const val ALARM_CANCEL_LOOKUP_FLAGS = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE

internal fun alarmDataUri(thingId: String): Uri = Uri.parse("keeply://alarm/$thingId")
internal fun detailsDataUri(thingId: String): Uri = Uri.parse("keeply://thing/$thingId")

internal class AndroidReminderScheduler(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis
) : ReminderScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun sync(thing: Thing): ReminderSyncResult {
        cancel(thing.id)
        return when (val decision = thing.reminderScheduleDecision(clock())) {
            ReminderScheduleDecision.None -> {
                reminderLog("sync none thingId=${thing.id}")
                ReminderSyncResult.NoReminder
            }
            is ReminderScheduleDecision.Past -> {
                reminderLog("sync past thingId=${thing.id} epoch=${decision.triggerAtEpochMillis}")
                ReminderSyncResult.Past
            }
            is ReminderScheduleDecision.Future -> {
                if (!notificationsAllowed(appContext)) {
                    reminderLog("sync notifications-disabled thingId=${thing.id}")
                    return ReminderSyncResult.NotificationsDisabled
                }
                val pendingIntent = alarmPendingIntent(decision.thingId, decision.triggerAtEpochMillis)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            decision.triggerAtEpochMillis,
                            pendingIntent
                        )
                        reminderLog("scheduled exact thingId=${thing.id} epoch=${decision.triggerAtEpochMillis}")
                        ReminderSyncResult.ScheduledExact
                    } catch (_: SecurityException) {
                        scheduleInexact(decision.triggerAtEpochMillis, pendingIntent)
                    }
                } else {
                    scheduleInexact(decision.triggerAtEpochMillis, pendingIntent)
                }
            }
        }
    }

    private fun scheduleInexact(triggerAtEpochMillis: Long, pendingIntent: PendingIntent): ReminderSyncResult {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtEpochMillis,
            pendingIntent
        )
        reminderLog("scheduled inexact epoch=$triggerAtEpochMillis")
        return ReminderSyncResult.ScheduledInexact
    }

    override fun cancel(thingId: String) {
        val existing = existingAlarmPendingIntent(thingId)
        if (existing != null) {
            alarmManager.cancel(existing)
            reminderLog("cancelled existing thingId=$thingId")
        } else {
            reminderLog("cancel skipped; none registered thingId=$thingId")
        }
    }

    private fun alarmPendingIntent(thingId: String, expectedEpoch: Long): PendingIntent {
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_DELIVER_REMINDER
            data = alarmDataUri(thingId)
            putExtra(EXTRA_THING_ID, thingId)
            putExtra(EXTRA_EXPECTED_EPOCH, expectedEpoch)
        }
        return PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun existingAlarmPendingIntent(thingId: String): PendingIntent? =
        PendingIntent.getBroadcast(
            appContext,
            0,
            alarmIntent(thingId),
            ALARM_CANCEL_LOOKUP_FLAGS
        )

    private fun alarmIntent(thingId: String): Intent =
        Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_DELIVER_REMINDER
            data = alarmDataUri(thingId)
        }
}

internal fun notificationsAllowed(context: Context): Boolean {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) return false
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}
