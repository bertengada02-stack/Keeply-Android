package com.keeply.app.ads

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class KeeplyConsentManager(context: Context) {
    private val appContext = context.applicationContext
    private val information = UserMessagingPlatform.getConsentInformation(appContext)
    private val gate = AdConsentGate()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutableAdsReady = MutableStateFlow(false)
    val adsReady = mutableAdsReady.asStateFlow()
    private val mutablePrivacyRequired = MutableStateFlow(false)
    val privacyRequired = mutablePrivacyRequired.asStateFlow()
    private var generation = 0
    private var formOpen = false

    /** Called by the foreground Activity, never by boot/reminder startup. */
    fun refresh(activity: Activity) {
        val request = ++generation
        formOpen = false
        information.requestConsentInfoUpdate(activity, requestParameters(activity), {
            if (request != generation || activity.isDestroyed || activity.isFinishing) return@requestConsentInfoUpdate
            formOpen = true
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (request == generation) {
                    formOpen = false
                    if (error != null) Log.w(TAG, "Consent form unavailable: ${error.errorCode}")
                    publishEligibility()
                }
            }
            publishEligibility()
        }, { error ->
            if (request == generation) {
                Log.w(TAG, "Consent refresh unavailable: ${error.errorCode}")
                publishEligibility()
            }
        })
        // UMP can authorize returning users even while the refresh is in flight.
        publishEligibility()
    }

    fun showPrivacyOptions(activity: Activity, onError: () -> Unit) {
        if (formOpen || activity.isDestroyed || activity.isFinishing) return
        if (!mutablePrivacyRequired.value) return
        formOpen = true
        // Dispose existing banners while choices are being changed.
        mutableAdsReady.value = false
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            formOpen = false
            publishEligibility()
            if (error != null) {
                Log.w(TAG, "Privacy options unavailable: ${error.errorCode}")
                onError()
            }
        }
    }

    private fun publishEligibility() {
        mutablePrivacyRequired.value = information.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (gate.updateEligibility(information.canRequestAds())) {
            MobileAds.initialize(appContext) {
                mainHandler.post {
                    gate.initializationComplete()
                    publishEligibility()
                }
            }
        }
        mutableAdsReady.value = gate.mayLoadAds && !formOpen
    }

    private fun requestParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        // Opt-in QA only; release ignores all extras and never resets consent.
        if (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            val geography = activity.intent.getStringExtra("keeply.ump.geography")
            if (geography == "EEA" || geography == "OTHER") {
                val debug = ConsentDebugSettings.Builder(appContext)
                    .setDebugGeography(if (geography == "EEA")
                        ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
                    else ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_OTHER)
                activity.intent.getStringExtra("keeply.ump.testDevice")
                    ?.takeIf { it.isNotBlank() }?.let { debug.addTestDeviceHashedId(it) }
                builder.setConsentDebugSettings(debug.build())
            }
        }
        return builder.build()
    }

    private companion object { const val TAG = "KeeplyConsent" }
}
