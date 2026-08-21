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

    @Test
    fun migrationFromOneToTwoPreservesRowsAndInitializesLifecycle() {
        context.deleteDatabase(TEST_DATABASE)
        context.openOrCreateDatabase(TEST_DATABASE, Context.MODE_PRIVATE, null).use { legacy ->
            legacy.execSQL("""
                CREATE TABLE IF NOT EXISTS `things` (
                    `id` TEXT NOT NULL, `name` TEXT NOT NULL, `categoryCode` TEXT NOT NULL,
                    `importantDate` TEXT NOT NULL, `reminderTypeCode` TEXT,
                    `reminderAtEpochMillis` INTEGER, `reminderTimeZoneId` TEXT, `notes` TEXT,
                    `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            legacy.execSQL("""
                INSERT INTO things VALUES (
                    'passport', 'Passport', 'DOCUMENT', '2027-01-01', 'ON_DAY',
                    1798159200000, 'Asia/Singapore', 'Original note', 1, 1
                )
            """.trimIndent())
            legacy.version = 1
        }

        database = openPersistentDatabase()
        val migrated = runBlocking { database!!.thingDao().findById("passport") }

        assertEquals("Passport", migrated?.name)
        assertEquals("ON_DAY", migrated?.reminderTypeCode)
        assertEquals("Original note", migrated?.notes)
        assertEquals("ACTIVE", migrated?.statusCode)
        assertNull(migrated?.nextReminderAtEpochMillis)
        assertNull(migrated?.nextReminderTimeZoneId)
        assertEquals(true, migrated?.originalReminderActionable)
    }

    @Test
    fun migrationFromTwoToThreeInitializesActionabilityByLifecycle() {
        context.deleteDatabase(TEST_DATABASE)
        context.openOrCreateDatabase(TEST_DATABASE, Context.MODE_PRIVATE, null).use { legacy ->
            legacy.execSQL("""
                CREATE TABLE IF NOT EXISTS `things` (
                    `id` TEXT NOT NULL, `name` TEXT NOT NULL, `categoryCode` TEXT NOT NULL,
                    `importantDate` TEXT NOT NULL, `reminderTypeCode` TEXT,
                    `reminderAtEpochMillis` INTEGER, `reminderTimeZoneId` TEXT, `notes` TEXT,
                    `statusCode` TEXT NOT NULL DEFAULT 'ACTIVE',
                    `nextReminderAtEpochMillis` INTEGER, `nextReminderTimeZoneId` TEXT,
                    `createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            fun insert(id: String, status: String, reminderType: String?) {
                legacy.execSQL(
                    "INSERT INTO things VALUES (?, ?, 'DOCUMENT', '2027-01-01', ?, 5000, 'UTC', NULL, ?, NULL, NULL, 1, 1)",
                    arrayOf(id, id, reminderType, status)
                )
            }
            insert("active-reminder", "ACTIVE", "ON_DAY")
            insert("active-none", "ACTIVE", null)
            insert("progress", "IN_PROGRESS", "ON_DAY")
            insert("done", "DONE", "ON_DAY")
            legacy.version = 2
        }

        database = openPersistentDatabase()

        runBlocking {
            assertEquals(true, database!!.thingDao().findById("active-reminder")?.originalReminderActionable)
            assertEquals(false, database!!.thingDao().findById("active-none")?.originalReminderActionable)
            assertEquals(false, database!!.thingDao().findById("progress")?.originalReminderActionable)
            assertEquals(false, database!!.thingDao().findById("done")?.originalReminderActionable)
        }
    }

    @Test
    fun lifecycleAndDeleteAreDurableAndIsolated() = runBlocking {
        context.deleteDatabase(TEST_DATABASE)
        database = openPersistentDatabase()
        database!!.thingDao().insert(entity("passport", "DOCUMENT", reminder = true))
        database!!.thingDao().insert(entity("vehicle", "VEHICLE", reminder = false))
        assertEquals(1, database!!.thingDao().remindAgain("passport", 9_999L, "UTC", 10L))
        database!!.close()

        database = openPersistentDatabase()
        assertEquals("IN_PROGRESS", database!!.thingDao().findById("passport")?.statusCode)
        assertEquals(9_999L, database!!.thingDao().findById("passport")?.nextReminderAtEpochMillis)
        assertEquals(1, database!!.thingDao().markDone("passport", 11L))
        assertEquals("DONE", database!!.thingDao().findById("passport")?.statusCode)
        assertNull(database!!.thingDao().findById("passport")?.nextReminderAtEpochMillis)
        assertEquals(1, database!!.thingDao().reopen("passport", 12L))
        assertEquals("ACTIVE", database!!.thingDao().findById("passport")?.statusCode)
        assertEquals(0, database!!.thingDao().reopen("passport", 13L))
        assertEquals(false, database!!.thingDao().findById("passport")?.originalReminderActionable)
        database!!.close()
        database = openPersistentDatabase()
        assertEquals(false, database!!.thingDao().findById("passport")?.originalReminderActionable)
        assertEquals(1, database!!.thingDao().deleteById("passport"))
        database!!.close()

        database = openPersistentDatabase()
        assertNull(database!!.thingDao().findById("passport"))
        assertEquals("vehicle", database!!.thingDao().findById("vehicle")?.id)
    }

    private fun openPersistentDatabase(): KeeplyDatabase =
        Room.databaseBuilder(context, KeeplyDatabase::class.java, TEST_DATABASE)
            .addMigrations(KeeplyDatabase.MIGRATION_1_2, KeeplyDatabase.MIGRATION_2_3)
            .build()

    private fun entity(id: String, categoryCode: String, reminder: Boolean) = ThingEntity(
        id = id,
        name = id.replaceFirstChar(Char::uppercase),
        categoryCode = categoryCode,
        importantDate = "2027-01-01",
        reminderTypeCode = if (reminder) "ONE_WEEK_BEFORE" else null,
        reminderAtEpochMillis = if (reminder) 1_798_159_200_000L else null,
        reminderTimeZoneId = if (reminder) "Asia/Singapore" else null,
        notes = null,
        statusCode = "ACTIVE",
        nextReminderAtEpochMillis = null,
        nextReminderTimeZoneId = null,
        createdAtEpochMillis = if (id == "passport") 1L else 2L,
        updatedAtEpochMillis = if (id == "passport") 1L else 2L
    )

    private companion object {
        const val TEST_DATABASE = "keeply-room-test.db"
    }
}
