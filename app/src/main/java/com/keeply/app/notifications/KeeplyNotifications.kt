package com.keeply.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.keeply.app.MainActivity
import com.keeply.app.R
import com.keeply.app.model.Thing

internal const val REMINDER_CHANNEL_ID = "keeply_reminders"
internal const val ACTION_OPEN_THING = "com.keeply.app.action.OPEN_THING"
internal const val ACTION_OPEN_MISSED_THINGS = "com.keeply.app.action.OPEN_MISSED_THINGS"
private const val REMINDER_NOTIFICATION_ID = 1
internal const val MISSED_REMINDER_NOTIFICATION_ID = 2
internal const val MISSED_REMINDER_NOTIFICATION_TAG = "keeply_missed_reminders"

internal enum class NotificationPostResult { Posted, Blocked, Failed }

internal fun createReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        REMINDER_CHANNEL_ID,
        context.getString(R.string.reminder_channel_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.reminder_channel_description)
        enableVibration(true)
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

internal fun postThingReminder(context: Context, thing: Thing): NotificationPostResult {
    if (!notificationsAllowed(context)) {
        reminderLog("notification blocked by permission/settings thingId=${thing.id}")
        return NotificationPostResult.Blocked
    }
    val contentIntent = Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_THING
        data = detailsDataUri(thing.id)
        putExtra(EXTRA_THING_ID, thing.id)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_keeply)
        .setContentTitle(context.getString(R.string.reminder_notification_title))
        .setContentText(context.getString(R.string.reminder_notification_body, thing.name))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()
    return try {
        NotificationManagerCompat.from(context).notify(thing.id, REMINDER_NOTIFICATION_ID, notification)
        reminderLog("notification posted thingId=${thing.id}")
        NotificationPostResult.Posted
    } catch (error: Exception) {
        reminderLogError("notification post failed thingId=${thing.id}", error)
        NotificationPostResult.Failed
    }
}

internal fun postMissedReminderSummary(context: Context, missedCount: Int): NotificationPostResult {
    if (!notificationsAllowed(context)) {
        reminderLog("missed summary blocked by permission/settings count=$missedCount")
        return NotificationPostResult.Blocked
    }
    val contentIntent = missedSummaryContentIntent(context)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val singular = missedCount == 1
    val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_keeply)
        .setContentTitle(context.getString(if (singular) R.string.missed_reminder_title else R.string.missed_reminders_title))
        .setContentText(context.getString(if (singular) R.string.missed_reminder_body else R.string.missed_reminders_body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()
    return try {
        NotificationManagerCompat.from(context).notify(
            MISSED_REMINDER_NOTIFICATION_TAG,
            MISSED_REMINDER_NOTIFICATION_ID,
            notification
        )
        reminderLog("missed summary posted count=$missedCount")
        NotificationPostResult.Posted
    } catch (error: Exception) {
        reminderLogError("missed summary post failed count=$missedCount", error)
        NotificationPostResult.Failed
    }
}

internal fun missedSummaryContentIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_MISSED_THINGS
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
