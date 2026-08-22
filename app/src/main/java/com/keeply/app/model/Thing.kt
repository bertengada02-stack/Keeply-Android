package com.keeply.app.model

enum class ThingCategory(val code: String) {
    DOCUMENT("DOCUMENT"),
    OWNED_ITEM("OWNED_ITEM"),
    SUBSCRIPTION_PAYMENT("SUBSCRIPTION_PAYMENT"),
    LENT_BORROWED("LENT_BORROWED"),
    MONEY_OWED("MONEY_OWED"),
    VEHICLE("VEHICLE"),
    HOME_APPLIANCE("HOME_APPLIANCE"),
    MEDICINE("MEDICINE"),
    OTHER("OTHER");

    companion object {
        fun fromCode(code: String): ThingCategory =
            entries.firstOrNull { it.code == code }
                ?: error("Unknown Thing category code: $code")
    }
}

enum class ReminderType(val code: String) {
    ON_DAY("ON_DAY"),
    ONE_DAY_BEFORE("ONE_DAY_BEFORE"),
    THREE_DAYS_BEFORE("THREE_DAYS_BEFORE"),
    ONE_WEEK_BEFORE("ONE_WEEK_BEFORE"),
    THIRTY_DAYS_BEFORE("THIRTY_DAYS_BEFORE"),
    CUSTOM("CUSTOM");

    companion object {
        fun fromCode(code: String): ReminderType =
            entries.firstOrNull { it.code == code }
                ?: error("Unknown reminder type code: $code")
    }
}

enum class ThingStatus(val code: String) {
    ACTIVE("ACTIVE"),
    IN_PROGRESS("IN_PROGRESS"),
    DONE("DONE");

    companion object {
        fun fromCode(code: String): ThingStatus =
            entries.firstOrNull { it.code == code }
                ?: error("Unknown Thing status code: $code")
    }
}

data class Thing(
    val id: String,
    val name: String,
    val category: ThingCategory,
    val importantDate: String,
    val reminderType: ReminderType?,
    val reminderAtEpochMillis: Long?,
    val reminderTimeZoneId: String?,
    val notes: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val status: ThingStatus = ThingStatus.ACTIVE,
    val nextReminderAtEpochMillis: Long? = null,
    val nextReminderTimeZoneId: String? = null,
    val originalReminderActionable: Boolean = false,
    val reminderDeliveryState: ReminderDeliveryState = ReminderDeliveryState.NONE
)

data class NewThingDraft(
    val name: String,
    val category: ThingCategory,
    val importantDate: String,
    val reminderType: ReminderType?,
    val reminderAtEpochMillis: Long?,
    val reminderTimeZoneId: String?,
    val notes: String,
    val reminderExplicitlySelected: Boolean = false
)

data class PersistedThingValues(
    val name: String,
    val category: ThingCategory,
    val importantDate: String,
    val reminderType: ReminderType?,
    val reminderAtEpochMillis: Long?,
    val reminderTimeZoneId: String?,
    val notes: String?
)

fun NewThingDraft.toPersistedValues(): PersistedThingValues {
    val hasReminder = reminderType != null
    return PersistedThingValues(
        name = name.trim(),
        category = category,
        importantDate = importantDate,
        reminderType = reminderType,
        reminderAtEpochMillis = reminderAtEpochMillis.takeIf { hasReminder },
        reminderTimeZoneId = reminderTimeZoneId.takeIf { hasReminder },
        notes = notes.trim().ifBlank { null }
    )
}

fun Thing.toPersistedValues(): PersistedThingValues = PersistedThingValues(
    name = name,
    category = category,
    importantDate = importantDate,
    reminderType = reminderType,
    reminderAtEpochMillis = reminderAtEpochMillis,
    reminderTimeZoneId = reminderTimeZoneId,
    notes = notes
)

fun Thing.wouldPersistChanges(draft: NewThingDraft): Boolean {
    val desiredActionability = when (status) {
        ThingStatus.ACTIVE -> when {
            draft.reminderType == null -> false
            draft.reminderExplicitlySelected -> true
            else -> originalReminderActionable
        }
        ThingStatus.IN_PROGRESS, ThingStatus.DONE -> false
    }
    return toPersistedValues() != draft.toPersistedValues() ||
        originalReminderActionable != desiredActionability
}
