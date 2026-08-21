package com.keeply.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ThingEntity::class],
    version = 3,
    exportSchema = true
)
abstract class KeeplyDatabase : RoomDatabase() {
    abstract fun thingDao(): ThingDao

    companion object {
        fun create(context: Context): KeeplyDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                KeeplyDatabase::class.java,
                "keeply.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE things ADD COLUMN statusCode TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("ALTER TABLE things ADD COLUMN nextReminderAtEpochMillis INTEGER")
                db.execSQL("ALTER TABLE things ADD COLUMN nextReminderTimeZoneId TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE things ADD COLUMN originalReminderActionable INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    UPDATE things SET originalReminderActionable = 1
                    WHERE statusCode = 'ACTIVE' AND reminderTypeCode IS NOT NULL
                """.trimIndent())
            }
        }
    }
}
