package com.keeply.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
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
}
