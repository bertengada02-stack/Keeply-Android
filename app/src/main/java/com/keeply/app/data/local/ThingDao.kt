package com.keeply.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Update
    suspend fun update(thing: ThingEntity): Int

    @Query("""
        UPDATE things SET statusCode = 'IN_PROGRESS',
            originalReminderActionable = 0,
            nextReminderAtEpochMillis = :nextReminderAtEpochMillis,
            nextReminderTimeZoneId = :nextReminderTimeZoneId,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :id AND statusCode IN ('ACTIVE', 'IN_PROGRESS')
    """)
    suspend fun remindAgain(
        id: String,
        nextReminderAtEpochMillis: Long,
        nextReminderTimeZoneId: String,
        updatedAtEpochMillis: Long
    ): Int

    @Query("""
        UPDATE things SET statusCode = 'DONE',
            originalReminderActionable = 0,
            nextReminderAtEpochMillis = NULL,
            nextReminderTimeZoneId = NULL,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :id AND statusCode IN ('ACTIVE', 'IN_PROGRESS')
    """)
    suspend fun markDone(id: String, updatedAtEpochMillis: Long): Int

    @Query("""
        UPDATE things SET statusCode = 'ACTIVE',
            originalReminderActionable = 0,
            nextReminderAtEpochMillis = NULL,
            nextReminderTimeZoneId = NULL,
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :id AND statusCode = 'DONE'
    """)
    suspend fun reopen(id: String, updatedAtEpochMillis: Long): Int

    @Query("DELETE FROM things WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
