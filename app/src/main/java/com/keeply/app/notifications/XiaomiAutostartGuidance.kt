package com.keeply.app.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.util.Locale

private const val PreferencesName = "notification_preferences"
private const val GuidanceAcknowledged = "xiaomi_autostart_guidance_acknowledged"
private const val XiaomiSecurityCenter = "com.miui.securitycenter"

internal enum class XiaomiGuidanceFlowState {
    IDLE,
    WAITING_FOR_NOTIFICATION_FLOW,
    READY_TO_SHOW
}

internal fun xiaomiGuidanceStateAfterCreate(
    shouldOffer: Boolean,
    notificationPermissionFlowStarted: Boolean
): XiaomiGuidanceFlowState = when {
    !shouldOffer -> XiaomiGuidanceFlowState.IDLE
    notificationPermissionFlowStarted -> XiaomiGuidanceFlowState.WAITING_FOR_NOTIFICATION_FLOW
    else -> XiaomiGuidanceFlowState.READY_TO_SHOW
}

internal fun XiaomiGuidanceFlowState.afterNotificationFlowSettled(): XiaomiGuidanceFlowState =
    if (this == XiaomiGuidanceFlowState.WAITING_FOR_NOTIFICATION_FLOW) {
        XiaomiGuidanceFlowState.READY_TO_SHOW
    } else {
        this
    }

internal fun isXiaomiFamilyDevice(manufacturer: String?, brand: String?): Boolean {
    val identifiers = listOf(manufacturer, brand).mapNotNull {
        it?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty)
    }
    return identifiers.any { it == "xiaomi" || it == "redmi" || it == "poco" }
}

internal fun shouldOfferXiaomiAutostartGuidance(
    manufacturer: String?,
    brand: String?,
    hasActionableReminder: Boolean,
    acknowledged: Boolean
): Boolean = hasActionableReminder &&
    !acknowledged &&
    isXiaomiFamilyDevice(manufacturer, brand)

internal fun xiaomiAutostartGuidanceAcknowledged(context: Context): Boolean =
    context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .getBoolean(GuidanceAcknowledged, false)

internal fun acknowledgeXiaomiAutostartGuidance(context: Context) {
    context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(GuidanceAcknowledged, true)
        .apply()
}

internal fun xiaomiAutostartSettingsCandidates(packageName: String): List<Intent> {
    fun xiaomiIntent(activityName: String) = Intent().apply {
        component = ComponentName(XiaomiSecurityCenter, activityName)
        putExtra("extra_pkgname", packageName)
        putExtra("package_name", packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return listOf(
        xiaomiIntent("com.miui.permcenter.autostart.AutoStartManagementActivity"),
        xiaomiIntent("com.miui.permcenter.permissions.PermissionsEditorActivity"),
        xiaomiIntent("com.miui.permcenter.permissions.AppPermissionsEditorActivity"),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

internal fun launchFirstAvailableAutostartSettings(
    candidates: List<Intent>,
    canResolve: (Intent) -> Boolean,
    launch: (Intent) -> Unit
): Boolean {
    for (candidate in candidates) {
        if (!canResolve(candidate)) continue
        try {
            launch(candidate)
            return true
        } catch (_: Exception) {
            // OEM settings components are private and can disappear between releases.
        }
    }
    return false
}

internal fun openXiaomiAutostartSettings(context: Context): Boolean =
    launchFirstAvailableAutostartSettings(
        candidates = xiaomiAutostartSettingsCandidates(context.packageName),
        canResolve = { it.resolveActivity(context.packageManager) != null },
        launch = context::startActivity
    )
