package com.keeply.app.ads

import org.junit.Assert.*
import org.junit.Test

class AdConsentGateTest {
    @Test fun freshOrFailedConsentDoesNotInitializeOrLoad() {
        val gate = AdConsentGate()
        assertFalse(gate.mayLoadAds)
        assertFalse(gate.updateEligibility(false))
        assertFalse(gate.mayLoadAds)
    }

    @Test fun returningOrGrantedConsentInitializesOnlyOnceAndWaitsForCompletion() {
        val gate = AdConsentGate()
        assertTrue(gate.updateEligibility(true))
        assertFalse(gate.mayLoadAds)
        assertFalse(gate.updateEligibility(true))
        gate.initializationComplete()
        assertTrue(gate.mayLoadAds)
        assertFalse(gate.updateEligibility(true))
    }

    @Test fun revocationDuringInitializationCannotEnableAds() {
        val gate = AdConsentGate()
        gate.updateEligibility(true)
        gate.updateEligibility(false)
        gate.initializationComplete()
        assertFalse(gate.mayLoadAds)
    }

    @Test fun privacyChangesRecheckEligibilityWithoutReinitialization() {
        val gate = AdConsentGate()
        gate.updateEligibility(true)
        gate.initializationComplete()
        gate.updateEligibility(false)
        assertFalse(gate.mayLoadAds)
        assertFalse(gate.updateEligibility(true))
        assertTrue(gate.mayLoadAds)
    }
}
