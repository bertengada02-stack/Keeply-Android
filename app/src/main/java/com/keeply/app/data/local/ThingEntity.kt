package com.keeply.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "things")
data class ThingEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryCode: String,
    val importantDate: String,
    val reminderTypeCode: String?,
    val reminderAtEpochMillis: Long?,
    val reminderTimeZoneId: String?,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
