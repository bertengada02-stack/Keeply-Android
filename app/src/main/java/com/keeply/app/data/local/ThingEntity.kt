package com.keeply.app.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "'ACTIVE'") val statusCode: String,
    val nextReminderAtEpochMillis: Long?,
    val nextReminderTimeZoneId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    @ColumnInfo(defaultValue = "0") val originalReminderActionable: Boolean = false,
    @ColumnInfo(defaultValue = "'NONE'") val reminderDeliveryStateCode: String = "NONE"
)
