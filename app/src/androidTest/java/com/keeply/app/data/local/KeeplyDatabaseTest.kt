package com.keeply.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeeplyDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var database: KeeplyDatabase? = null

    @After
    fun closeDatabase() {
        database?.close()
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun insertAndObserveMultipleThingsIncludingNoReminder() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, KeeplyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database = db

        db.thingDao().insert(entity("passport", "DOCUMENT", reminder = true))
        db.thingDao().insert(entity("medicine", "MEDICINE", reminder = false))

        val saved = db.thingDao().observeAll().first { it.size == 2 }
        assertEquals(setOf("passport", "medicine"), saved.map { it.id }.toSet())
        val withoutReminder = saved.single { it.id == "medicine" }
        assertNull(withoutReminder.reminderTypeCode)
        assertNull(withoutReminder.reminderAtEpochMillis)
        assertNull(withoutReminder.reminderTimeZoneId)
    }

    @Test
    fun thingSurvivesDatabaseCloseAndReopen() = runBlocking {
        context.deleteDatabase(TEST_DATABASE)
        database = openPersistentDatabase()
        database!!.thingDao().insert(entity("passport", "DOCUMENT", reminder = true))
        database!!.close()

        database = openPersistentDatabase()
        val restored = database!!.thingDao().findById("passport")

        assertEquals("Passport", restored?.name)
        assertEquals("2027-01-01", restored?.importantDate)
        assertEquals("Asia/Singapore", restored?.reminderTimeZoneId)
    }

    @Test
    fun observeByIdReturnsOnlyRequestedThingAndNullForMissingId() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, KeeplyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database = db
        db.thingDao().insert(entity("passport", "DOCUMENT", reminder = true))
        db.thingDao().insert(entity("vehicle", "VEHICLE", reminder = false))

        assertEquals("vehicle", db.thingDao().observeById("vehicle").first()?.id)
        assertNull(db.thingDao().observeById("missing").first())
    }

    @Test
    fun updateChangesOnlyRequestedRowAndPreservesIdentityFields() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, KeeplyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database = db
        val passport = entity("passport", "DOCUMENT", reminder = true)
        val vehicle = entity("vehicle", "VEHICLE", reminder = false)
        db.thingDao().insert(passport)
        db.thingDao().insert(vehicle)

        val affected = db.thingDao().update(
            passport.copy(
                name = "Passport 2026",
                categoryCode = "OWNED_ITEM",
                importantDate = "2027-04-02",
                reminderTypeCode = null,
                reminderAtEpochMillis = null,
                reminderTimeZoneId = null,
                notes = null,
                updatedAtEpochMillis = 99L
            )
        )

        assertEquals(1, affected)
        val updated = db.thingDao().observeById("passport").first()
        assertEquals("passport", updated?.id)
        assertEquals(passport.createdAtEpochMillis, updated?.createdAtEpochMillis)
        assertEquals(99L, updated?.updatedAtEpochMillis)
        assertEquals("Passport 2026", updated?.name)
        assertEquals("vehicle", db.thingDao().findById("vehicle")?.name?.lowercase())
        assertEquals(0, db.thingDao().update(passport.copy(id = "missing")))
    }

    private fun openPersistentDatabase(): KeeplyDatabase =
        Room.databaseBuilder(context, KeeplyDatabase::class.java, TEST_DATABASE).build()

    private fun entity(id: String, categoryCode: String, reminder: Boolean) = ThingEntity(
        id = id,
        name = id.replaceFirstChar(Char::uppercase),
        categoryCode = categoryCode,
        importantDate = "2027-01-01",
        reminderTypeCode = if (reminder) "ONE_WEEK_BEFORE" else null,
        reminderAtEpochMillis = if (reminder) 1_798_159_200_000L else null,
        reminderTimeZoneId = if (reminder) "Asia/Singapore" else null,
        notes = null,
        createdAtEpochMillis = if (id == "passport") 1L else 2L,
        updatedAtEpochMillis = if (id == "passport") 1L else 2L
    )

    private companion object {
        const val TEST_DATABASE = "keeply-room-test.db"
    }
}
