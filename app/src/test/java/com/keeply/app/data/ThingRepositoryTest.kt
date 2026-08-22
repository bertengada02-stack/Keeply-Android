package com.keeply.app.data

import com.keeply.app.data.local.ThingDao
import com.keeply.app.data.local.ThingEntity
import com.keeply.app.model.NewThingDraft
import com.keeply.app.model.ReminderType
import com.keeply.app.model.ReminderDeliveryState
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.ThingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ThingRepositoryTest {
    @Test
    fun createThingMapsStableCodesAndNormalizesBlankNotes() = runBlocking {
        val dao = FakeThingDao()
        val repository = ThingRepository(dao, clock = { 1234L }, idFactory = { "fixed-id" })

        val created = repository.createThing(
            NewThingDraft(
                name = "  Passport  ",
                category = ThingCategory.DOCUMENT,
                importantDate = "2026-12-01",
                reminderType = ReminderType.ONE_WEEK_BEFORE,
                reminderAtEpochMillis = 1_796_042_400_000L,
                reminderTimeZoneId = "Asia/Singapore",
                notes = "   "
            )
        )

        assertEquals("fixed-id", created.id)
        assertEquals("Passport", created.name)
        assertEquals(ThingCategory.DOCUMENT, created.category)
        assertEquals(ReminderType.ONE_WEEK_BEFORE, created.reminderType)
        assertNull(created.notes)
        assertEquals("DOCUMENT", dao.inserted.single().categoryCode)
        assertEquals("ONE_WEEK_BEFORE", dao.inserted.single().reminderTypeCode)
        assertEquals(1234L, dao.inserted.single().createdAtEpochMillis)
        assertEquals(1234L, dao.inserted.single().updatedAtEpochMillis)
        assertTrue(created.originalReminderActionable)
        assertEquals(ReminderDeliveryState.PENDING, created.reminderDeliveryState)
    }

    @Test
    fun createThingWithoutReminderKeepsReminderFieldsNull() = runBlocking {
        val dao = FakeThingDao()
        val repository = ThingRepository(dao, idFactory = { "medicine-id" })

        repository.createThing(
            NewThingDraft(
                name = "Medicine",
                category = ThingCategory.MEDICINE,
                importantDate = "2027-01-31",
                reminderType = null,
                reminderAtEpochMillis = null,
                reminderTimeZoneId = null,
                notes = "Store in cabinet"
            )
        )

        val entity = dao.inserted.single()
        assertNull(entity.reminderTypeCode)
        assertNull(entity.reminderAtEpochMillis)
        assertNull(entity.reminderTimeZoneId)
        assertEquals(false, entity.originalReminderActionable)
        assertEquals("NONE", entity.reminderDeliveryStateCode)
        assertEquals("Store in cabinet", entity.notes)
    }

    @Test
    fun observedEntitiesAreMappedBackToDomainThings() = runBlocking {
        val dao = FakeThingDao()
        val repository = ThingRepository(dao)
        dao.insert(sampleEntity("first", "DOCUMENT"))
        dao.insert(sampleEntity("second", "VEHICLE"))

        val things = repository.things.first { it.size == 2 }

        assertEquals(listOf(ThingCategory.DOCUMENT, ThingCategory.VEHICLE), things.map { it.category })
        assertEquals(listOf("first", "second"), things.map { it.id })
    }

    @Test
    fun observeThingReturnsOnlyTheRequestedPersistedThing() = runBlocking {
        val dao = FakeThingDao()
        val repository = ThingRepository(dao)
        dao.insert(sampleEntity("passport", "DOCUMENT"))
        dao.insert(sampleEntity("vehicle", "VEHICLE"))

        val selected = repository.observeThing("vehicle").first()

        assertEquals("vehicle", selected?.id)
        assertEquals(ThingCategory.VEHICLE, selected?.category)
    }

    @Test
    fun observeThingReturnsNullForMissingId() = runBlocking {
        val repository = ThingRepository(FakeThingDao())

        assertNull(repository.observeThing("missing").first())
    }

    @Test
    fun updateThingChangesFieldsButPreservesIdentityAndCreatedTimestamp() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT"))
        val repository = ThingRepository(dao, clock = { 99L })

        val result = repository.updateThing(
            "passport",
            draft(
                name = "Passport 2026",
                category = ThingCategory.OWNED_ITEM,
                importantDate = "2027-03-12",
                reminderType = ReminderType.CUSTOM,
                reminderMillis = 500L,
                reminderZone = "Asia/Singapore",
                notes = "Renew soon"
            )
        )

        assertEquals(true, result.changed)
        assertEquals("passport", result.thing.id)
        assertEquals(1L, result.thing.createdAtEpochMillis)
        assertEquals(99L, result.thing.updatedAtEpochMillis)
        assertEquals("Passport 2026", result.thing.name)
        assertEquals(ThingCategory.OWNED_ITEM, result.thing.category)
        assertEquals("2027-03-12", result.thing.importantDate)
        assertEquals(ReminderType.CUSTOM, result.thing.reminderType)
        assertEquals("Renew soon", result.thing.notes)
        assertEquals(1, dao.updateCalls)
    }

    @Test
    fun unchangedUpdateDoesNotCallDaoOrChangeUpdatedTimestamp() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT"))
        val repository = ThingRepository(dao, clock = { 99L })

        val result = repository.updateThing("passport", draft(name = "passport"))

        assertEquals(false, result.changed)
        assertEquals(1L, result.thing.updatedAtEpochMillis)
        assertEquals(0, dao.updateCalls)
    }

    @Test
    fun removingReminderAndClearingNotesPersistsNulls() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(
            sampleEntity("passport", "DOCUMENT").copy(
                reminderTypeCode = "ON_DAY",
                reminderAtEpochMillis = 400L,
                reminderTimeZoneId = "Asia/Singapore",
                notes = "Old note"
            )
        )
        val repository = ThingRepository(dao, clock = { 99L })

        val result = repository.updateThing("passport", draft(name = "passport", notes = "   "))

        assertNull(result.thing.reminderType)
        assertNull(result.thing.reminderAtEpochMillis)
        assertNull(result.thing.reminderTimeZoneId)
        assertNull(result.thing.notes)
    }

    @Test
    fun updatingOneThingNeverChangesAnother() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT"))
        dao.insert(sampleEntity("vehicle", "VEHICLE"))
        val repository = ThingRepository(dao, clock = { 99L })

        repository.updateThing("vehicle", draft(name = "Registration", category = ThingCategory.VEHICLE))

        assertEquals("passport", dao.findById("passport")?.name)
        assertEquals("Registration", dao.findById("vehicle")?.name)
    }

    @Test
    fun missingThingUpdateFailsWithoutInserting() = runBlocking {
        val dao = FakeThingDao()
        val repository = ThingRepository(dao)

        assertThrows(ThingNotFoundException::class.java) {
            runBlocking { repository.updateThing("missing", draft()) }
        }
        assertEquals(0, dao.inserted.size)
        assertEquals(0, dao.updateCalls)
    }

    @Test
    fun activeCanBeRemindedAgainWithoutChangingImportantDateOrOriginalReminder() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT").copy(
            reminderTypeCode = "ON_DAY",
            reminderAtEpochMillis = 2_000L,
            reminderTimeZoneId = "Asia/Singapore"
        ))
        val repository = ThingRepository(dao, clock = { 100L })

        repository.remindAgain("passport", 5_000L, "Asia/Singapore")

        val updated = dao.findById("passport")!!
        assertEquals("IN_PROGRESS", updated.statusCode)
        assertEquals(5_000L, updated.nextReminderAtEpochMillis)
        assertEquals("2026-08-30", updated.importantDate)
        assertEquals("ON_DAY", updated.reminderTypeCode)
        assertEquals(2_000L, updated.reminderAtEpochMillis)
        assertEquals(false, updated.originalReminderActionable)
    }

    @Test
    fun inProgressReminderIsReplacedAndMayBeAfterImportantDate() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT").copy(
            statusCode = "IN_PROGRESS",
            nextReminderAtEpochMillis = 2_000L,
            nextReminderTimeZoneId = "UTC"
        ))
        val repository = ThingRepository(dao, clock = { 100L })

        repository.remindAgain("passport", 9_999_999_999_999L, "Asia/Singapore")

        assertEquals(9_999_999_999_999L, dao.findById("passport")?.nextReminderAtEpochMillis)
    }

    @Test
    fun activeAndInProgressCanBeMarkedDoneAndNextReminderIsCleared() = runBlocking {
        listOf("ACTIVE", "IN_PROGRESS").forEach { initialStatus ->
            val dao = FakeThingDao()
            dao.insert(sampleEntity("passport", "DOCUMENT").copy(
                statusCode = initialStatus,
                reminderTypeCode = "ON_DAY",
                reminderAtEpochMillis = 2_000L,
                nextReminderAtEpochMillis = 3_000L,
                nextReminderTimeZoneId = "UTC"
            ))
            ThingRepository(dao, clock = { 100L }).markDone("passport")

            val done = dao.findById("passport")!!
            assertEquals("DONE", done.statusCode)
            assertNull(done.nextReminderAtEpochMillis)
            assertNull(done.nextReminderTimeZoneId)
            assertEquals("ON_DAY", done.reminderTypeCode)
            assertEquals(2_000L, done.reminderAtEpochMillis)
            assertEquals(false, done.originalReminderActionable)
        }
    }

    @Test
    fun doneRejectsAllLifecycleTransitions() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT").copy(statusCode = "DONE"))
        val repository = ThingRepository(dao, clock = { 100L })

        assertThrows(InvalidLifecycleTransitionException::class.java) {
            runBlocking { repository.remindAgain("passport", 500L, "UTC") }
        }
        assertThrows(InvalidLifecycleTransitionException::class.java) {
            runBlocking { repository.markDone("passport") }
        }
        Unit
    }

    @Test
    fun pastOrEqualNextReminderIsRejected() {
        val repository = ThingRepository(FakeThingDao(), clock = { 500L })
        assertThrows(InvalidNextReminderException::class.java) {
            runBlocking { repository.remindAgain("passport", 500L, "UTC") }
        }
    }

    @Test
    fun deleteRemovesOnlyRequestedThingAndMissingDeleteFails() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT"))
        dao.insert(sampleEntity("vehicle", "VEHICLE"))
        val repository = ThingRepository(dao)

        repository.deleteThing("passport")

        assertNull(dao.findById("passport"))
        assertEquals("vehicle", dao.findById("vehicle")?.id)
        assertThrows(ThingNotFoundException::class.java) {
            runBlocking { repository.deleteThing("missing") }
        }
        Unit
    }

    @Test
    fun doneCanReopenWhilePreservingPersistedThingFields() = runBlocking {
        val dao = FakeThingDao()
        val original = sampleEntity("passport", "DOCUMENT").copy(
            name = "My Passport",
            importantDate = "2030-06-12",
            reminderTypeCode = "CUSTOM",
            reminderAtEpochMillis = 5_000L,
            reminderTimeZoneId = "Asia/Singapore",
            notes = "Keep safe",
            statusCode = "DONE",
            nextReminderAtEpochMillis = 7_000L,
            nextReminderTimeZoneId = "UTC"
        )
        dao.insert(original)
        val repository = ThingRepository(dao, clock = { 99L })

        repository.reopen("passport")

        val reopened = dao.findById("passport")!!
        assertEquals("ACTIVE", reopened.statusCode)
        assertEquals(original.id, reopened.id)
        assertEquals(original.createdAtEpochMillis, reopened.createdAtEpochMillis)
        assertEquals(original.name, reopened.name)
        assertEquals(original.categoryCode, reopened.categoryCode)
        assertEquals(original.importantDate, reopened.importantDate)
        assertEquals(original.notes, reopened.notes)
        assertEquals(original.reminderTypeCode, reopened.reminderTypeCode)
        assertEquals(original.reminderAtEpochMillis, reopened.reminderAtEpochMillis)
        assertEquals(original.reminderTimeZoneId, reopened.reminderTimeZoneId)
        assertNull(reopened.nextReminderAtEpochMillis)
        assertNull(reopened.nextReminderTimeZoneId)
        assertEquals(false, reopened.originalReminderActionable)
        assertEquals(99L, reopened.updatedAtEpochMillis)
    }

    @Test
    fun activeAndInProgressCannotReopenAndAreNotMutated() = runBlocking {
        listOf("ACTIVE", "IN_PROGRESS").forEach { status ->
            val dao = FakeThingDao()
            val original = sampleEntity("passport", "DOCUMENT").copy(statusCode = status)
            dao.insert(original)
            val repository = ThingRepository(dao, clock = { 99L })

            assertThrows(InvalidLifecycleTransitionException::class.java) {
                runBlocking { repository.reopen("passport") }
            }
            assertEquals(original, dao.findById("passport"))
        }
    }

    @Test
    fun reopeningOneThingNeverChangesAnother() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT").copy(statusCode = "DONE"))
        val vehicle = sampleEntity("vehicle", "VEHICLE").copy(statusCode = "DONE")
        dao.insert(vehicle)

        ThingRepository(dao, clock = { 99L }).reopen("passport")

        assertEquals("ACTIVE", dao.findById("passport")?.statusCode)
        assertEquals(vehicle, dao.findById("vehicle"))
    }

    @Test
    fun editingDoneThingDoesNotReopenIt() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT").copy(statusCode = "DONE"))

        ThingRepository(dao, clock = { 99L }).updateThing("passport", draft(name = "Updated passport"))

        assertEquals("DONE", dao.findById("passport")?.statusCode)
        assertEquals("Updated passport", dao.findById("passport")?.name)
        assertEquals(false, dao.findById("passport")?.originalReminderActionable)
    }

    @Test
    fun activeEditControlsOriginalReminderActionability() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT"))
        val repository = ThingRepository(dao, clock = { 99L })

        repository.updateThing("passport", draft(
            reminderType = ReminderType.ON_DAY,
            reminderMillis = 500L,
            reminderZone = "Asia/Singapore",
            reminderExplicitlySelected = true
        ))
        assertTrue(dao.findById("passport")!!.originalReminderActionable)

        repository.updateThing("passport", draft())
        assertEquals(false, dao.findById("passport")!!.originalReminderActionable)
    }

    @Test
    fun inProgressEditNeverReactivatesOriginalReminder() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT").copy(
            statusCode = "IN_PROGRESS",
            nextReminderAtEpochMillis = 1_000L,
            nextReminderTimeZoneId = "UTC"
        ))

        ThingRepository(dao, clock = { 99L }).updateThing("passport", draft(
            reminderType = ReminderType.ON_DAY,
            reminderMillis = 500L,
            reminderZone = "Asia/Singapore"
        ))

        val updated = dao.findById("passport")!!
        assertEquals(false, updated.originalReminderActionable)
        assertEquals(1_000L, updated.nextReminderAtEpochMillis)
    }

    @Test
    fun guardedDeliveryAndMissedUpdatesRequireCurrentPendingEpoch() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT").copy(
            reminderTypeCode = "CUSTOM",
            reminderAtEpochMillis = 5_000L,
            reminderTimeZoneId = "UTC",
            originalReminderActionable = true,
            reminderDeliveryStateCode = "PENDING"
        ))
        val repository = ThingRepository(dao)

        assertEquals(false, repository.markReminderDelivered("passport", 4_000L))
        assertEquals(true, repository.markReminderDelivered("passport", 5_000L))
        assertEquals(ReminderDeliveryState.DELIVERED, repository.findById("passport")?.reminderDeliveryState)
        assertEquals(false, repository.markReminderMissed("passport", 5_000L))
    }

    @Test
    fun unrelatedEditPreservesMissedWhileNewOrRemovedReminderClearsIt() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT").copy(
            reminderTypeCode = "CUSTOM",
            reminderAtEpochMillis = 5_000L,
            reminderTimeZoneId = "UTC",
            originalReminderActionable = true,
            reminderDeliveryStateCode = "MISSED_ANNOUNCED"
        ))
        val repository = ThingRepository(dao, clock = { 100L })

        repository.updateThing("passport", draft(
            name = "Updated passport",
            reminderType = ReminderType.CUSTOM,
            reminderMillis = 5_000L,
            reminderZone = "UTC"
        ))
        assertEquals("MISSED_ANNOUNCED", dao.findById("passport")?.reminderDeliveryStateCode)

        repository.updateThing("passport", draft(
            name = "Updated passport",
            reminderType = ReminderType.CUSTOM,
            reminderMillis = 6_000L,
            reminderZone = "UTC",
            reminderExplicitlySelected = true
        ))
        assertEquals("PENDING", dao.findById("passport")?.reminderDeliveryStateCode)

        repository.updateThing("passport", draft(name = "Updated passport"))
        assertEquals("NONE", dao.findById("passport")?.reminderDeliveryStateCode)
    }

    @Test
    fun meaningfulLifecycleActionsClearOrReplaceMissedState() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("passport", "DOCUMENT").copy(
            reminderTypeCode = "CUSTOM",
            reminderAtEpochMillis = 5_000L,
            reminderTimeZoneId = "UTC",
            originalReminderActionable = true,
            reminderDeliveryStateCode = "MISSED_ANNOUNCED"
        ))
        val repository = ThingRepository(dao, clock = { 100L })

        repository.remindAgain("passport", 8_000L, "UTC")
        assertEquals("PENDING", dao.findById("passport")?.reminderDeliveryStateCode)
        repository.markDone("passport")
        assertEquals("NONE", dao.findById("passport")?.reminderDeliveryStateCode)
        repository.reopen("passport")
        assertEquals("NONE", dao.findById("passport")?.reminderDeliveryStateCode)
    }

    @Test
    fun missedAnnouncementUpdatesOnlyUnannouncedCapturedRows() = runBlocking {
        val dao = FakeThingDao()
        dao.insert(sampleEntity("one", "DOCUMENT").copy(reminderDeliveryStateCode = "MISSED_UNANNOUNCED"))
        dao.insert(sampleEntity("two", "DOCUMENT").copy(reminderDeliveryStateCode = "MISSED_ANNOUNCED"))
        val repository = ThingRepository(dao)

        assertEquals(listOf("one"), repository.unannouncedMissedThings().map { it.id })
        assertEquals(setOf("one", "two"), repository.allMissedThings().map { it.id }.toSet())
        repository.markMissedAnnounced(listOf("one"))
        assertEquals("MISSED_ANNOUNCED", dao.findById("one")?.reminderDeliveryStateCode)
        assertEquals("MISSED_ANNOUNCED", dao.findById("two")?.reminderDeliveryStateCode)
    }

    private fun draft(
        name: String = "passport",
        category: ThingCategory = ThingCategory.DOCUMENT,
        importantDate: String = "2026-08-30",
        reminderType: ReminderType? = null,
        reminderMillis: Long? = null,
        reminderZone: String? = null,
        notes: String = "",
        reminderExplicitlySelected: Boolean = false
    ) = NewThingDraft(
        name = name,
        category = category,
        importantDate = importantDate,
        reminderType = reminderType,
        reminderAtEpochMillis = reminderMillis,
        reminderTimeZoneId = reminderZone,
        notes = notes,
        reminderExplicitlySelected = reminderExplicitlySelected
    )

    private fun sampleEntity(id: String, categoryCode: String) = ThingEntity(
        id = id,
        name = id,
        categoryCode = categoryCode,
        importantDate = "2026-08-30",
        reminderTypeCode = null,
        reminderAtEpochMillis = null,
        reminderTimeZoneId = null,
        notes = null,
        statusCode = ThingStatus.ACTIVE.code,
        nextReminderAtEpochMillis = null,
        nextReminderTimeZoneId = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L
    )
}

private class FakeThingDao : ThingDao {
    val inserted = mutableListOf<ThingEntity>()
    private val values = MutableStateFlow<List<ThingEntity>>(emptyList())
    var updateCalls = 0

    override suspend fun insert(thing: ThingEntity) {
        inserted += thing
        values.value = inserted.toList()
    }

    override fun observeAll(): Flow<List<ThingEntity>> = values

    override suspend fun findById(id: String): ThingEntity? = inserted.firstOrNull { it.id == id }

    override fun observeById(id: String): Flow<ThingEntity?> =
        values.map { entities -> entities.firstOrNull { it.id == id } }

    override suspend fun findUnannouncedMissed(): List<ThingEntity> =
        inserted.filter { it.reminderDeliveryStateCode == "MISSED_UNANNOUNCED" }

    override suspend fun findAllMissed(): List<ThingEntity> =
        inserted.filter { it.reminderDeliveryStateCode in setOf("MISSED_UNANNOUNCED", "MISSED_ANNOUNCED") }

    override suspend fun update(thing: ThingEntity): Int {
        val index = inserted.indexOfFirst { it.id == thing.id }
        if (index < 0) return 0
        updateCalls += 1
        inserted[index] = thing
        values.value = inserted.toList()
        return 1
    }

    override suspend fun remindAgain(id: String, nextReminderAtEpochMillis: Long, nextReminderTimeZoneId: String, updatedAtEpochMillis: Long): Int {
        val current = findById(id) ?: return 0
        if (current.statusCode == ThingStatus.DONE.code) return 0
        return update(current.copy(statusCode = ThingStatus.IN_PROGRESS.code, originalReminderActionable = false, nextReminderAtEpochMillis = nextReminderAtEpochMillis, nextReminderTimeZoneId = nextReminderTimeZoneId, reminderDeliveryStateCode = "PENDING", updatedAtEpochMillis = updatedAtEpochMillis))
    }

    override suspend fun markDone(id: String, updatedAtEpochMillis: Long): Int {
        val current = findById(id) ?: return 0
        if (current.statusCode == ThingStatus.DONE.code) return 0
        return update(current.copy(statusCode = ThingStatus.DONE.code, originalReminderActionable = false, nextReminderAtEpochMillis = null, nextReminderTimeZoneId = null, reminderDeliveryStateCode = "NONE", updatedAtEpochMillis = updatedAtEpochMillis))
    }

    override suspend fun reopen(id: String, updatedAtEpochMillis: Long): Int {
        val current = findById(id) ?: return 0
        if (current.statusCode != ThingStatus.DONE.code) return 0
        return update(current.copy(statusCode = ThingStatus.ACTIVE.code, originalReminderActionable = false, nextReminderAtEpochMillis = null, nextReminderTimeZoneId = null, reminderDeliveryStateCode = "NONE", updatedAtEpochMillis = updatedAtEpochMillis))
    }


    override suspend fun markReminderDelivered(id: String, expectedEpoch: Long): Int =
        updateDeliveryState(id, expectedEpoch, "DELIVERED")

    override suspend fun markReminderMissed(id: String, expectedEpoch: Long): Int =
        updateDeliveryState(id, expectedEpoch, "MISSED_UNANNOUNCED")

    override suspend fun markMissedAnnounced(ids: List<String>): Int {
        var changed = 0
        ids.forEach { id ->
            val current = findById(id)
            if (current?.reminderDeliveryStateCode == "MISSED_UNANNOUNCED") {
                update(current.copy(reminderDeliveryStateCode = "MISSED_ANNOUNCED"))
                changed += 1
            }
        }
        return changed
    }

    private suspend fun updateDeliveryState(id: String, expectedEpoch: Long, state: String): Int {
        val current = findById(id) ?: return 0
        val matches = current.reminderDeliveryStateCode == "PENDING" && when (current.statusCode) {
            ThingStatus.ACTIVE.code -> current.originalReminderActionable && current.reminderAtEpochMillis == expectedEpoch
            ThingStatus.IN_PROGRESS.code -> current.nextReminderAtEpochMillis == expectedEpoch
            else -> false
        }
        return if (matches) update(current.copy(reminderDeliveryStateCode = state)) else 0
    }

    override suspend fun deleteById(id: String): Int {
        val removed = inserted.removeAll { it.id == id }
        values.value = inserted.toList()
        return if (removed) 1 else 0
    }
}
