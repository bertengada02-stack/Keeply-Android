package com.keeply.app.ui

import com.keeply.app.model.Thing
import com.keeply.app.model.ThingStatus

internal fun List<Thing>.homeEligibleAndOrdered(): List<Thing> =
    asSequence()
        .filter { it.status == ThingStatus.ACTIVE || it.status == ThingStatus.IN_PROGRESS }
        .sortedWith(importantDateThingComparator)
        .toList()

internal val importantDateThingComparator: Comparator<Thing> =
    compareBy<Thing> { isoDateDayIndex(it.importantDate) ?: Long.MAX_VALUE }
        .thenByDescending(Thing::createdAtEpochMillis)
        .thenBy(Thing::id)
