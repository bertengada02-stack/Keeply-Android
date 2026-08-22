package com.keeply.app.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XiaomiAutostartGuidanceTest {
    @Test
    fun xiaomiFamilyManufacturerAndBrandVariationsQualify() {
        assertTrue(isXiaomiFamilyDevice("Xiaomi", "Xiaomi"))
        assertTrue(isXiaomiFamilyDevice(" xIaOmI ", "unknown"))
        assertTrue(isXiaomiFamilyDevice("unknown", "Redmi"))
        assertTrue(isXiaomiFamilyDevice("unknown", " POCO "))
    }

    @Test
    fun unrelatedManufacturersDoNotQualify() {
        assertFalse(isXiaomiFamilyDevice("Google", "Pixel"))
        assertFalse(isXiaomiFamilyDevice("Samsung", "Galaxy"))
        assertFalse(isXiaomiFamilyDevice(null, null))
        assertFalse(isXiaomiFamilyDevice("NotXiaomi", "Pocophone"))
    }

    @Test
    fun offerRequiresActionableReminderAndNoAcknowledgement() {
        assertTrue(shouldOfferXiaomiAutostartGuidance("Xiaomi", "Redmi", true, false))
        assertFalse(shouldOfferXiaomiAutostartGuidance("Xiaomi", "Redmi", false, false))
        assertFalse(shouldOfferXiaomiAutostartGuidance("Xiaomi", "Redmi", true, true))
        assertFalse(shouldOfferXiaomiAutostartGuidance("Google", "Pixel", true, false))
    }

    @Test
    fun notificationPermissionFlowMustSettleBeforeGuidanceIsReady() {
        val waiting = xiaomiGuidanceStateAfterCreate(
            shouldOffer = true,
            notificationPermissionFlowStarted = true
        )

        assertTrue(waiting == XiaomiGuidanceFlowState.WAITING_FOR_NOTIFICATION_FLOW)
        assertTrue(
            waiting.afterNotificationFlowSettled() == XiaomiGuidanceFlowState.READY_TO_SHOW
        )
    }

    @Test
    fun guidanceIsReadyAfterCreateFeedbackWhenNoPermissionFlowStarted() {
        assertTrue(
            xiaomiGuidanceStateAfterCreate(
                shouldOffer = true,
                notificationPermissionFlowStarted = false
            ) == XiaomiGuidanceFlowState.READY_TO_SHOW
        )
        assertTrue(
            xiaomiGuidanceStateAfterCreate(
                shouldOffer = false,
                notificationPermissionFlowStarted = false
            ) == XiaomiGuidanceFlowState.IDLE
        )
    }
}
