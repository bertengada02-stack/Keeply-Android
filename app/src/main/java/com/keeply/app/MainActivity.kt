package com.keeply.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keeply.app.ui.KeeplyApp
import com.keeply.app.ui.KeeplyViewModel
import com.keeply.app.ui.theme.KeeplyTheme
import com.keeply.app.notifications.ACTION_OPEN_THING
import com.keeply.app.notifications.ACTION_OPEN_MISSED_THINGS
import com.keeply.app.notifications.EXTRA_THING_ID
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val requestedThingId = MutableStateFlow<String?>(null)
    private val requestedMissedThings = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            KeeplyTheme {
                val keeplyViewModel: KeeplyViewModel = viewModel(
                    factory = KeeplyViewModel.factory(
                        (application as KeeplyApplication).thingRepository,
                        (application as KeeplyApplication).reminderSyncCoordinator,
                        (application as KeeplyApplication).missedReminderRecovery
                    )
                )
                val requestedId = requestedThingId.collectAsStateWithLifecycle().value
                val openMissed = requestedMissedThings.collectAsStateWithLifecycle().value
                KeeplyApp(
                    viewModel = keeplyViewModel,
                    requestedThingId = requestedId,
                    onRequestedThingConsumed = { requestedThingId.value = null },
                    requestedMissedThings = openMissed,
                    onRequestedMissedThingsConsumed = { requestedMissedThings.value = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        (application as KeeplyApplication).reconcileReminders()
    }

    private fun acceptNotificationIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_THING -> requestedThingId.value = intent.getStringExtra(EXTRA_THING_ID)
            ACTION_OPEN_MISSED_THINGS -> requestedMissedThings.value = true
        }
    }
}
