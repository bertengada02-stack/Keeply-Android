package com.keeply.app.ui

import com.keeply.app.model.Thing
import com.keeply.app.model.ThingStatus
import com.keeply.app.model.ThingCategory

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

internal data class MyThingsUiSelection(
    val filter: MyThingsFilter,
    val category: ThingCategory?,
    val searchActive: Boolean,
    val searchQuery: String
)

internal fun myThingsStateAfterSuccessfulCreate() = MyThingsUiSelection(
    filter = MyThingsFilter.ALL,
    category = null,
    searchActive = false,
    searchQuery = ""
)

internal fun List<Thing>.filteredBy(filter: MyThingsFilter): List<Thing> =
    filterMyThings(filter, null, "")

internal fun List<Thing>.filterMyThings(
    statusFilter: MyThingsFilter,
    category: ThingCategory?,
    titleQuery: String
): List<Thing> {
    val query = titleQuery.trim()
    val filtered = filter { thing ->
        val statusMatches = if (statusFilter == MyThingsFilter.MISSED) {
            thing.reminderDeliveryState.isMissed
        } else {
            statusFilter.includes(thing.status)
        }
        statusMatches &&
            (category == null || thing.category == category) &&
            (query.isEmpty() || thing.name.contains(query, ignoreCase = true))
    }
    if (statusFilter == MyThingsFilter.COMPLETED) return filtered

    val (actionable, done) = filtered.partition {
        it.status == ThingStatus.ACTIVE || it.status == ThingStatus.IN_PROGRESS
    }
    return actionable.sortedWith(importantDateThingComparator) + done
}
