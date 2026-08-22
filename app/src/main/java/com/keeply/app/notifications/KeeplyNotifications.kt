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
private const val REMINDER_NOTIFICATION_ID = 1

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

internal fun postThingReminder(context: Context, thing: Thing) {
    if (!notificationsAllowed(context)) {
        reminderLog("notification blocked by permission/settings thingId=${thing.id}")
        return
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
    NotificationManagerCompat.from(context).notify(thing.id, REMINDER_NOTIFICATION_ID, notification)
    reminderLog("notification posted thingId=${thing.id}")
}
