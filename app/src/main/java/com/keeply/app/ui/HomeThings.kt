package com.keeply.app.ui

import com.keeply.app.model.Thing
import com.keeply.app.model.ThingStatus

internal fun List<Thing>.homeEligibleAndOrdered(
    currentLocalDate: String = currentLocalDateIso()
): List<Thing> =
    asSequence()
        .filter { it.status == ThingStatus.ACTIVE || it.status == ThingStatus.IN_PROGRESS }
        .filter { it.isWithinHomeOverdueGracePeriod(currentLocalDate) }
        .sortedWith(importantDateThingComparator)
        .toList()

internal fun Thing.isWithinHomeOverdueGracePeriod(currentLocalDate: String): Boolean =
    importantDateContext(importantDate, currentLocalDate)
        ?.dayOffset
        ?.let { it >= -1 }
        ?: true

internal val importantDateThingComparator: Comparator<Thing> =
    compareBy<Thing> { isoDateDayIndex(it.importantDate) ?: Long.MAX_VALUE }
        .thenByDescending(Thing::createdAtEpochMillis)
        .thenBy(Thing::id)
