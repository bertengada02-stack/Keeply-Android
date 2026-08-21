package com.keeply.app.data

import com.keeply.app.data.local.ThingDao
import com.keeply.app.data.local.ThingEntity
import com.keeply.app.model.NewThingDraft
import com.keeply.app.model.ReminderType
import com.keeply.app.model.ThingCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun sampleEntity(id: String, categoryCode: String) = ThingEntity(
        id = id,
        name = id,
        categoryCode = categoryCode,
        importantDate = "2026-08-30",
        reminderTypeCode = null,
        reminderAtEpochMillis = null,
        reminderTimeZoneId = null,
        notes = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L
    )
}

private class FakeThingDao : ThingDao {
    val inserted = mutableListOf<ThingEntity>()
    private val values = MutableStateFlow<List<ThingEntity>>(emptyList())

    override suspend fun insert(thing: ThingEntity) {
        inserted += thing
        values.value = inserted.toList()
    }

    override fun observeAll(): Flow<List<ThingEntity>> = values

    override suspend fun findById(id: String): ThingEntity? = inserted.firstOrNull { it.id == id }

    override fun observeById(id: String): Flow<ThingEntity?> =
        values.map { entities -> entities.firstOrNull { it.id == id } }
}
