package com.keeply.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeeplyNotificationsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun notificationChannelIsCreatedWithApprovedImportance() {
        createReminderChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(REMINDER_CHANNEL_ID)
            assertNotNull(channel)
            assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        }
    }

    @Test
    fun alarmAndDetailsIdentityAreStableAndThingSpecific() {
        assertEquals("keeply://alarm/id-1", alarmDataUri("id-1").toString())
        assertEquals("keeply://thing/id-1", detailsDataUri("id-1").toString())
        assertNotEquals(alarmDataUri("id-1"), alarmDataUri("id-2"))
        assertNotEquals(detailsDataUri("id-1"), detailsDataUri("id-2"))
    }

    @Test
    fun alarmCancellationLookupCannotOverwriteDeliveryExtras() {
        assertTrue(ALARM_CANCEL_LOOKUP_FLAGS and PendingIntent.FLAG_NO_CREATE != 0)
        assertEquals(0, ALARM_CANCEL_LOOKUP_FLAGS and PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @Test
    fun rebootReceiverIsExportedAndResolvableForPostBootBroadcasts() {
        val component = ComponentName(context, ReminderRescheduleReceiver::class.java)
        val receiverInfo = context.packageManager.getReceiverInfo(component, 0)
        assertTrue(receiverInfo.exported)

        listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_USER_UNLOCKED).forEach { action ->
            val matches = context.packageManager.queryBroadcastReceivers(
                Intent(action).setPackage(context.packageName),
                0
            )
            assertTrue(
                "$action must resolve to ReminderRescheduleReceiver",
                matches.any { it.activityInfo.name == ReminderRescheduleReceiver::class.java.name }
            )
        }
    }

    @Test
    fun missedSummaryUsesStableIdentityAndDeepLinksToMissedThings() {
        assertEquals("keeply_missed_reminders", MISSED_REMINDER_NOTIFICATION_TAG)
        assertEquals(2, MISSED_REMINDER_NOTIFICATION_ID)
        val intent = missedSummaryContentIntent(context)
        assertEquals(ACTION_OPEN_MISSED_THINGS, intent.action)
        assertEquals(com.keeply.app.MainActivity::class.java.name, intent.component?.className)
    }
}
