package com.keeply.app.ui

import com.keeply.app.model.Thing
import com.keeply.app.model.ThingStatus

internal enum class MyThingsFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    MISSED("Missed");

    fun includes(status: ThingStatus): Boolean = when (this) {
        ALL -> true
        ACTIVE -> status == ThingStatus.ACTIVE || status == ThingStatus.IN_PROGRESS
        COMPLETED -> status == ThingStatus.DONE
        MISSED -> false
    }
}

internal fun List<Thing>.filteredBy(filter: MyThingsFilter): List<Thing> =
    filter { thing ->
        if (filter == MyThingsFilter.MISSED) thing.reminderDeliveryState.isMissed
        else filter.includes(thing.status)
    }
