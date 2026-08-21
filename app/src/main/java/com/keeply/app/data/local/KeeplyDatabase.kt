package com.keeply.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ThingEntity::class],
    version = 1,
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
            ).build()
    }
}
