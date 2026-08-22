package com.keeply.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class MissedReminderRecoveryTest {
    @Test
    fun multipleNewMissedThingsProduceOneSummaryForTotalUnresolvedCount() {
        assertEquals(
            MissedSummaryDecision.Post(totalMissed = 3),
            missedSummaryDecision(unannouncedCount = 2, totalMissedCount = 3)
        )
    }

    @Test
    fun repeatedReconciliationDoesNotRepostAnAlreadyAnnouncedSet() {
        assertEquals(
            MissedSummaryDecision.None,
            missedSummaryDecision(unannouncedCount = 0, totalMissedCount = 3)
        )
    }

    @Test
    fun newlyMissedThingUpdatesStableSummaryUsingNewTotal() {
        assertEquals(
            MissedSummaryDecision.Post(totalMissed = 4),
            missedSummaryDecision(unannouncedCount = 1, totalMissedCount = 4)
        )
    }

    @Test
    fun emptyMissedSetNeverPostsSummary() {
        assertEquals(
            MissedSummaryDecision.None,
            missedSummaryDecision(unannouncedCount = 0, totalMissedCount = 0)
        )
    }
}
