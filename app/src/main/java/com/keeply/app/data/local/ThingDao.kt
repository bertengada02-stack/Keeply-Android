package com.keeply.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(thing: ThingEntity)

    @Query("SELECT * FROM things ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<ThingEntity>>

    @Query("SELECT * FROM things WHERE id = :id")
    suspend fun findById(id: String): ThingEntity?

    @Query("SELECT * FROM things WHERE id = :id")
    fun observeById(id: String): Flow<ThingEntity?>
}
