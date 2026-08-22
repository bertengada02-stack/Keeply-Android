package com.keeply.app.notifications

import android.util.Log

internal const val REMINDER_LOG_TAG = "KeeplyReminder"

internal fun reminderLog(message: String) {
    Log.i(REMINDER_LOG_TAG, message)
}

internal fun reminderLogError(message: String, error: Throwable) {
    Log.e(REMINDER_LOG_TAG, message, error)
}
