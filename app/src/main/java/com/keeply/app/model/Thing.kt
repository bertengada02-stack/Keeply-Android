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
    val updatedAtEpochMillis: Long
)

data class NewThingDraft(
    val name: String,
    val category: ThingCategory,
    val importantDate: String,
    val reminderType: ReminderType?,
    val reminderAtEpochMillis: Long?,
    val reminderTimeZoneId: String?,
    val notes: String
)
