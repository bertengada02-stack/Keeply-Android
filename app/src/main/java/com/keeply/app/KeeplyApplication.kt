package com.keeply.app

import android.app.Application
import com.keeply.app.data.ThingRepository
import com.keeply.app.data.local.KeeplyDatabase
import com.keeply.app.notifications.AndroidReminderScheduler
import com.keeply.app.notifications.ReminderSyncCoordinator
import com.keeply.app.notifications.MissedReminderRecovery
import com.keeply.app.notifications.createReminderChannel
import com.keeply.app.notifications.reminderLog
import com.keeply.app.ads.KeeplyConsentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KeeplyApplication : Application() {
    val consentManager: KeeplyConsentManager by lazy { KeeplyConsentManager(this) }
    val database: KeeplyDatabase by lazy { KeeplyDatabase.create(this) }
    val thingRepository: ThingRepository by lazy { ThingRepository(database.thingDao()) }
    internal val reminderSyncCoordinator: ReminderSyncCoordinator by lazy {
        ReminderSyncCoordinator(AndroidReminderScheduler(this))
    }
    internal val missedReminderRecovery: MissedReminderRecovery by lazy {
        MissedReminderRecovery(this, thingRepository, reminderSyncCoordinator)
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createReminderChannel(this)
        reconcileReminders()
    }

    fun reconcileReminders() {
        applicationScope.launch {
            reconcileRemindersNow()
        }
    }

    internal suspend fun reconcileRemindersNow() {
        val things = thingRepository.things.first()
        reminderLog("reconcile start thingCount=${things.size}")
        missedReminderRecovery.reconcile(things)
        reminderLog("reconcile complete thingCount=${things.size}")
    }
}
