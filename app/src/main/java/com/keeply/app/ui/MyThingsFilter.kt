package com.keeply.app.ui

import com.keeply.app.model.Thing
import com.keeply.app.model.ThingStatus

internal enum class MyThingsFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed");

    fun includes(status: ThingStatus): Boolean = when (this) {
        ALL -> true
        ACTIVE -> status == ThingStatus.ACTIVE || status == ThingStatus.IN_PROGRESS
        COMPLETED -> status == ThingStatus.DONE
    }
}

internal fun List<Thing>.filteredBy(filter: MyThingsFilter): List<Thing> =
    filter { filter.includes(it.status) }
