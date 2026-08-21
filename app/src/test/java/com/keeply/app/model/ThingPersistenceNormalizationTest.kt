package com.keeply.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThingPersistenceNormalizationTest {
    @Test
    fun dirtyComparisonUsesExactlyTheRepositoryPersistenceNormalization() {
        val original = thing()
        val equivalentDraft = draft(name = "  Passport  ", notes = "   ")

        assertEquals(original.toPersistedValues(), equivalentDraft.toPersistedValues())
        assertFalse(original.wouldPersistChanges(equivalentDraft))
    }

    @Test
    fun changingThenRestoringValuesReturnsToCleanSnapshot() {
        val original = thing()

        assertTrue(original.wouldPersistChanges(draft(name = "Passport 2026")))
        assertFalse(original.wouldPersistChanges(draft(name = "Passport")))
    }

    @Test
    fun noReminderAlwaysNormalizesReminderFieldsToNull() {
        val normalized = draft(
            reminderType = null,
            reminderMillis = 500L,
            reminderZone = "Asia/Singapore"
        ).toPersistedValues()

        assertNull(normalized.reminderAtEpochMillis)
        assertNull(normalized.reminderTimeZoneId)
    }

    @Test
    fun allPersistedFieldsParticipateInDirtyComparison() {
        val original = thing()

        assertTrue(original.wouldPersistChanges(draft(category = ThingCategory.VEHICLE)))
        assertTrue(original.wouldPersistChanges(draft(importantDate = "2027-01-01")))
        assertTrue(original.wouldPersistChanges(draft(notes = "New note")))
    }

    private fun thing() = Thing(
        id = "id",
        name = "Passport",
        category = ThingCategory.DOCUMENT,
        importantDate = "2026-08-30",
        reminderType = null,
        reminderAtEpochMillis = null,
        reminderTimeZoneId = null,
        notes = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L
    )

    private fun draft(
        name: String = "Passport",
        category: ThingCategory = ThingCategory.DOCUMENT,
        importantDate: String = "2026-08-30",
        reminderType: ReminderType? = null,
        reminderMillis: Long? = null,
        reminderZone: String? = null,
        notes: String = ""
    ) = NewThingDraft(
        name = name,
        category = category,
        importantDate = importantDate,
        reminderType = reminderType,
        reminderAtEpochMillis = reminderMillis,
        reminderTimeZoneId = reminderZone,
        notes = notes
    )
}
