package com.keeply.app.model

enum class ReminderDeliveryState(val code: String) {
    NONE("NONE"),
    PENDING("PENDING"),
    DELIVERED("DELIVERED"),
    MISSED_UNANNOUNCED("MISSED_UNANNOUNCED"),
    MISSED_ANNOUNCED("MISSED_ANNOUNCED");

    val isMissed: Boolean
        get() = this == MISSED_UNANNOUNCED || this == MISSED_ANNOUNCED

    companion object {
        fun fromCode(code: String): ReminderDeliveryState =
            entries.firstOrNull { it.code == code }
                ?: error("Unknown reminder delivery state code: $code")
    }
}
