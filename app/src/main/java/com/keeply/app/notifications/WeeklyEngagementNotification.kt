package com.keeply.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.keeply.app.MainActivity
import com.keeply.app.R

internal const val WEEKLY_ENGAGEMENT_CHANNEL_ID = "keeply_weekly_engagement"
private const val ACTION_WEEKLY_ENGAGEMENT = "com.keeply.app.action.WEEKLY_ENGAGEMENT"
private const val WEEKLY_ENGAGEMENT_NOTIFICATION_ID = 3
private const val WEEKLY_ENGAGEMENT_REQUEST_CODE = 3001
private const val WEEK_MILLIS = 7L * 24L * 60L * 60L * 1000L
private const val MIN_RESCHEDULE_DELAY_MILLIS = 60_000L
private const val PREFS_NAME = "keeply_weekly_engagement"
private const val PREF_NEXT_TRIGGER_AT = "next_trigger_at"

internal fun createWeeklyEngagementChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        WEEKLY_ENGAGEMENT_CHANNEL_ID,
        context.getString(R.string.weekly_engagement_channel_name),
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = context.getString(R.string.weekly_engagement_channel_description)
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

internal fun scheduleWeeklyEngagementNotification(
    context: Context,
    nowEpochMillis: Long = System.currentTimeMillis()
) {
    val appContext = context.applicationContext
    val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val savedTrigger = preferences.getLong(PREF_NEXT_TRIGGER_AT, 0L)
    val triggerAt = when {
        savedTrigger <= 0L -> nowEpochMillis + WEEK_MILLIS
        savedTrigger <= nowEpochMillis -> nowEpochMillis + MIN_RESCHEDULE_DELAY_MILLIS
        else -> savedTrigger
    }
    if (savedTrigger <= 0L) {
        preferences.edit().putLong(PREF_NEXT_TRIGGER_AT, triggerAt).apply()
    }

    val alarmManager = appContext.getSystemService(AlarmManager::class.java)
    alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAt,
        weeklyEngagementPendingIntent(appContext)
    )
}

private fun scheduleNextWeeklyEngagement(
    context: Context,
    nowEpochMillis: Long = System.currentTimeMillis()
) {
    val nextTrigger = nowEpochMillis + WEEK_MILLIS
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putLong(PREF_NEXT_TRIGGER_AT, nextTrigger)
        .apply()

    val alarmManager = context.getSystemService(AlarmManager::class.java)
    alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        nextTrigger,
        weeklyEngagementPendingIntent(context)
    )
}

private fun weeklyEngagementPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, WeeklyEngagementReceiver::class.java).apply {
        action = ACTION_WEEKLY_ENGAGEMENT
    }
    return PendingIntent.getBroadcast(
        context,
        WEEKLY_ENGAGEMENT_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun postWeeklyEngagementNotification(context: Context) {
    if (!notificationsAllowed(context)) return

    val contentIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        WEEKLY_ENGAGEMENT_REQUEST_CODE,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, WEEKLY_ENGAGEMENT_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_keeply)
        .setContentTitle(context.getString(R.string.weekly_engagement_notification_title))
        .setContentText(context.getString(R.string.weekly_engagement_notification_body))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    NotificationManagerCompat.from(context).notify(
        WEEKLY_ENGAGEMENT_NOTIFICATION_ID,
        notification
    )
}

class WeeklyEngagementReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WEEKLY_ENGAGEMENT) return
        createWeeklyEngagementChannel(context)
        postWeeklyEngagementNotification(context)
        scheduleNextWeeklyEngagement(context)
    }
}
