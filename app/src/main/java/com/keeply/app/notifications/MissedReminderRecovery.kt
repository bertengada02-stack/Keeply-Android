package com.keeply.app.notifications

import android.content.Context
import com.keeply.app.data.ThingRepository
import com.keeply.app.model.Thing

internal class MissedReminderRecovery(
    context: Context,
    private val repository: ThingRepository,
    private val reminderSyncCoordinator: ReminderSyncCoordinator,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val appContext = context.applicationContext

    suspend fun reconcile(things: List<Thing>) {
        val now = clock()
        things.forEach { thing ->
            when (val decision = thing.reminderScheduleDecision(now)) {
                is ReminderScheduleDecision.Future -> reminderSyncCoordinator.sync(thing)
                is ReminderScheduleDecision.Past -> {
                    if (repository.markReminderMissed(thing.id, decision.triggerAtEpochMillis)) {
                        reminderLog("marked missed thingId=${thing.id} epoch=${decision.triggerAtEpochMillis}")
                    }
                    reminderSyncCoordinator.cancel(thing.id)
                }
                ReminderScheduleDecision.None -> reminderSyncCoordinator.cancel(thing.id)
            }
        }
        announceMissedIfNeeded()
    }

    suspend fun handleDueReminder(thing: Thing, expectedEpoch: Long) {
        when (postThingReminder(appContext, thing)) {
            NotificationPostResult.Posted -> {
                if (repository.markReminderDelivered(thing.id, expectedEpoch)) {
                    reminderLog("delivery recorded thingId=${thing.id} epoch=$expectedEpoch")
                } else {
                    reminderLog("delivery record rejected stale thingId=${thing.id} epoch=$expectedEpoch")
                }
            }
            NotificationPostResult.Blocked,
            NotificationPostResult.Failed -> {
                if (repository.markReminderMissed(thing.id, expectedEpoch)) {
                    reminderLog("delivery failure marked missed thingId=${thing.id} epoch=$expectedEpoch")
                }
                announceMissedIfNeeded()
            }
        }
    }

    suspend fun announceMissedIfNeeded() {
        val unannounced = repository.unannouncedMissedThings()
        val totalMissed = repository.allMissedThings().size
        val decision = missedSummaryDecision(unannounced.size, totalMissed)
        if (decision !is MissedSummaryDecision.Post) return
        if (postMissedReminderSummary(appContext, decision.totalMissed) == NotificationPostResult.Posted) {
            repository.markMissedAnnounced(unannounced.map(Thing::id))
            reminderLog("missed summary recorded count=${decision.totalMissed} newlyAnnounced=${unannounced.size}")
        }
    }
}

internal sealed interface MissedSummaryDecision {
    data object None : MissedSummaryDecision
    data class Post(val totalMissed: Int) : MissedSummaryDecision
}

internal fun missedSummaryDecision(
    unannouncedCount: Int,
    totalMissedCount: Int
): MissedSummaryDecision = if (unannouncedCount > 0 && totalMissedCount > 0) {
    MissedSummaryDecision.Post(totalMissedCount)
} else {
    MissedSummaryDecision.None
}
