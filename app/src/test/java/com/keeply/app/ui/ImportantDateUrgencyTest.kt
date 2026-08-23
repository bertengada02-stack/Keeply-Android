package com.keeply.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportantDateUrgencyTest {
    private val today = "2026-08-22"

    @Test
    fun boundariesUseApprovedUrgencyAndTimingCopy() {
        assertContext("2026-08-21", ImportantDateUrgency.OVERDUE, "1 day overdue")
        assertContext("2026-08-17", ImportantDateUrgency.OVERDUE, "5 days overdue")
        assertContext("2026-08-22", ImportantDateUrgency.DUE_TODAY, "Due today")
        assertContext("2026-08-23", ImportantDateUrgency.VERY_SOON, "Due tomorrow")
        assertContext("2026-08-24", ImportantDateUrgency.VERY_SOON, "In 2 days")
        assertContext("2026-08-25", ImportantDateUrgency.VERY_SOON, "In 3 days")
        assertContext("2026-08-26", ImportantDateUrgency.APPROACHING, "In 4 days")
        assertContext("2026-09-05", ImportantDateUrgency.APPROACHING, "In 14 days")
        assertContext("2026-09-06", ImportantDateUrgency.LATER, "In 15 days")
        assertContext("2026-09-21", ImportantDateUrgency.LATER, "In 30 days")
    }

    @Test
    fun calculationHandlesMonthYearAndLeapBoundaries() {
        assertEquals(1, importantDateContext("2027-01-01", "2026-12-31")?.dayOffset)
        assertEquals(1, importantDateContext("2028-02-29", "2028-02-28")?.dayOffset)
    }

    @Test
    fun invalidIsoDatesFailSafely() {
        assertNull(importantDateContext("not-a-date", today))
        assertNull(importantDateContext("2026-02-30", today))
        assertNull(importantDateContext("2026-08-23", "bad-today"))
    }

    private fun assertContext(
        importantDate: String,
        urgency: ImportantDateUrgency,
        timing: String
    ) {
        val context = importantDateContext(importantDate, today)
        assertEquals(urgency, context?.urgency)
        assertEquals(timing, context?.timingText)
    }
}
