package com.keeply.app.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.keeply.app.KeeplyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in SUPPORTED_ACTIONS) {
            reminderLog("reschedule broadcast rejected unsupported action=$action")
            return
        }
        reminderLog("reschedule broadcast received action=$action")
        scheduleWeeklyEngagementNotification(context)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as KeeplyApplication
                reminderLog("reschedule reconciliation starting action=$action")
                app.reconcileRemindersNow()
                reminderLog("reschedule reconciliation finished action=$action")
            } catch (error: Exception) {
                reminderLogError("reschedule reconciliation failed action=$action", error)
            } finally {
                pendingResult.finish()
                reminderLog("reschedule broadcast finished action=$action")
            }
        }
    }

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }
}
