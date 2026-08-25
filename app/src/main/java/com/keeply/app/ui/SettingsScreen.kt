package com.keeply.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal const val KEEPLY_PRIVACY_POLICY_URL =
    "https://bertengada02-stack.github.io/keeply/privacy-policy/"
internal const val KEEPLY_SUPPORT_EMAIL = "oobertappnetwork@gmail.com"

@Composable
internal fun SettingsScreen(
    versionName: String,
    onBack: () -> Unit,
    onNotificationSettings: () -> Unit,
    onExactReminderTiming: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onContactSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(28.dp))
        SettingsSection("REMINDERS")
        SettingsRow("Notification settings", onNotificationSettings)
        SettingsRow("Exact reminder timing", onExactReminderTiming)
        Spacer(Modifier.height(24.dp))
        SettingsSection("PRIVACY & SUPPORT")
        SettingsRow("Privacy Policy", onPrivacyPolicy)
        SettingsRow("Contact Oobert App Network", onContactSupport)
        Spacer(Modifier.height(24.dp))
        SettingsSection("ABOUT")
        Text(
            text = "Keeply",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Version $versionName",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Back" }
            .padding(12.dp)
    ) {
        val stroke = 2.5.dp.toPx()
        drawLine(color, Offset(size.width * .72f, size.height * .18f), Offset(size.width * .28f, size.height * .5f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * .28f, size.height * .5f), Offset(size.width * .72f, size.height * .82f), stroke, StrokeCap.Round)
    }
}

internal fun notificationSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

internal fun exactReminderTimingIntent(packageName: String, sdkInt: Int): Intent? =
    if (sdkInt >= Build.VERSION_CODES.S) {
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    } else {
        null
    }

internal fun privacyPolicyIntent(): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(KEEPLY_PRIVACY_POLICY_URL))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

internal fun supportEmailIntent(): Intent =
    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$KEEPLY_SUPPORT_EMAIL"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

internal fun launchExternalIntent(context: Context, intent: Intent): Boolean =
    runCatching { context.startActivity(intent) }.isSuccess

internal fun installedVersionName(context: Context): String = runCatching {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    packageInfo.versionName
}.getOrNull().orEmpty().ifBlank { "Unknown" }
