package com.keeply.app.ads

/** Main-thread state: repeated UMP callbacks must not initialize the SDK twice. */
internal class AdConsentGate {
    private var initializationStarted = false
    private var initialized = false
    var eligible = false
        private set
    val mayLoadAds: Boolean get() = eligible && initialized

    fun updateEligibility(canRequestAds: Boolean): Boolean {
        eligible = canRequestAds
        if (!eligible || initializationStarted) return false
        initializationStarted = true
        return true
    }

    fun initializationComplete() { initialized = true }
}
