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

    @Query("SELECT * FROM things WHERE reminderDeliveryStateCode = 'MISSED_UNANNOUNCED'")
    suspend fun findUnannouncedMissed(): List<ThingEntity>

    @Query("SELECT * FROM things WHERE reminderDeliveryStateCode IN ('MISSED_UNANNOUNCED', 'MISSED_ANNOUNCED')")
    suspend fun findAllMissed(): List<ThingEntity>

    @Update
    suspend fun update(thing: ThingEntity): Int

    @Query("""
        UPDATE things SET statusCode = 'IN_PROGRESS',
            originalReminderActionable = 0,
            nextReminderAtEpochMillis = :nextReminderAtEpochMillis,
            nextReminderTimeZoneId = :nextReminderTimeZoneId,
            reminderDeliveryStateCode = 'PENDING',
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
            reminderDeliveryStateCode = 'NONE',
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :id AND statusCode IN ('ACTIVE', 'IN_PROGRESS')
    """)
    suspend fun markDone(id: String, updatedAtEpochMillis: Long): Int

    @Query("""
        UPDATE things SET statusCode = 'ACTIVE',
            originalReminderActionable = 0,
            nextReminderAtEpochMillis = NULL,
            nextReminderTimeZoneId = NULL,
            reminderDeliveryStateCode = 'NONE',
            updatedAtEpochMillis = :updatedAtEpochMillis
        WHERE id = :id AND statusCode = 'DONE'
    """)
    suspend fun reopen(id: String, updatedAtEpochMillis: Long): Int

    @Query("""
        UPDATE things SET reminderDeliveryStateCode = 'DELIVERED'
        WHERE id = :id AND reminderDeliveryStateCode = 'PENDING' AND (
            (statusCode = 'ACTIVE' AND originalReminderActionable = 1 AND reminderAtEpochMillis = :expectedEpoch)
            OR (statusCode = 'IN_PROGRESS' AND nextReminderAtEpochMillis = :expectedEpoch)
        )
    """)
    suspend fun markReminderDelivered(id: String, expectedEpoch: Long): Int

    @Query("""
        UPDATE things SET reminderDeliveryStateCode = 'MISSED_UNANNOUNCED'
        WHERE id = :id AND reminderDeliveryStateCode = 'PENDING' AND (
            (statusCode = 'ACTIVE' AND originalReminderActionable = 1 AND reminderAtEpochMillis = :expectedEpoch)
            OR (statusCode = 'IN_PROGRESS' AND nextReminderAtEpochMillis = :expectedEpoch)
        )
    """)
    suspend fun markReminderMissed(id: String, expectedEpoch: Long): Int

    @Query("""
        UPDATE things SET reminderDeliveryStateCode = 'MISSED_ANNOUNCED'
        WHERE id IN (:ids) AND reminderDeliveryStateCode = 'MISSED_UNANNOUNCED'
    """)
    suspend fun markMissedAnnounced(ids: List<String>): Int

    @Query("DELETE FROM things WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
