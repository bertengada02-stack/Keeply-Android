package com.keeply.app.ui

import com.keeply.app.model.Thing
import com.keeply.app.model.ThingStatus
import com.keeply.app.model.ThingCategory

internal enum class MyThingsFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    OVERDUE("Overdue"),
    MISSED("Missed"),
    COMPLETED("Done");

    fun includes(status: ThingStatus): Boolean = when (this) {
        ALL -> true
        ACTIVE -> status == ThingStatus.ACTIVE || status == ThingStatus.IN_PROGRESS
        OVERDUE -> false
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
    titleQuery: String,
    currentLocalDate: String = currentLocalDateIso()
): List<Thing> {
    val query = titleQuery.trim()
    val isGlobalSearch = query.isNotEmpty() && statusFilter == MyThingsFilter.ALL && category == null
    val filtered = filter { thing ->
        if (!isGlobalSearch && thing.status == ThingStatus.DONE && statusFilter != MyThingsFilter.COMPLETED) {
            return@filter false
        }
        val statusMatches = isGlobalSearch || when (statusFilter) {
            MyThingsFilter.ALL -> thing.status != ThingStatus.DONE
            MyThingsFilter.ACTIVE ->
                statusFilter.includes(thing.status) &&
                    !thing.reminderDeliveryState.isMissed &&
                    importantDateContext(thing.importantDate, currentLocalDate)
                        ?.urgency != ImportantDateUrgency.OVERDUE
            MyThingsFilter.MISSED -> thing.reminderDeliveryState.isMissed
            MyThingsFilter.OVERDUE ->
                (thing.status == ThingStatus.ACTIVE || thing.status == ThingStatus.IN_PROGRESS) &&
                    importantDateContext(thing.importantDate, currentLocalDate)
                        ?.urgency == ImportantDateUrgency.OVERDUE
            else -> statusFilter.includes(thing.status)
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
