package com.keeply.app.ui

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.DatePicker
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.keeply.app.data.ThingRepository
import com.keeply.app.data.local.KeeplyDatabase
import com.keeply.app.data.local.ThingEntity
import com.keeply.app.model.ReminderDeliveryState
import com.keeply.app.model.ReminderType
import com.keeply.app.model.ThingCategory
import com.keeply.app.model.ThingStatus
import com.keeply.app.notifications.AndroidReminderScheduler
import com.keeply.app.notifications.MissedReminderRecovery
import com.keeply.app.notifications.ReminderSyncCoordinator
import com.keeply.app.ui.theme.KeeplyTheme
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout

class ThingCreationPersistenceFlowTest {
    private val composeRule = createAndroidComposeRule<ThingCreationTestHostActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.seconds(TEST_TIMEOUT_SECONDS))
        .around(composeRule)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notificationPreferences by lazy {
        context.getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE)
    }
    private lateinit var database: KeeplyDatabase
    private lateinit var repository: ThingRepository
    private lateinit var reminderScheduler: AndroidReminderScheduler
    private lateinit var reminderSyncCoordinator: ReminderSyncCoordinator
    private lateinit var missedReminderRecovery: MissedReminderRecovery
    private var permissionRequestedWasPresent = false
    private var permissionRequestedOriginal = false
    private var xiaomiGuidanceWasPresent = false
    private var xiaomiGuidanceOriginal = false
    private val createdThingIds = mutableSetOf<String>()

    @Before
    fun setUp() {
        checkpoint("setUp entered")
        context.deleteDatabase(TEST_DATABASE)
        preserveAndSuppressNotificationPrompts()
        openProductionStack()
        composeRule.setContent {
            KeeplyTheme {
                val keeplyViewModel: KeeplyViewModel = viewModel(
                    factory = KeeplyViewModel.factory(
                        repository,
                        reminderSyncCoordinator,
                        missedReminderRecovery
                    )
                )
                KeeplyApp(viewModel = keeplyViewModel)
            }
        }
        composeRule.waitForIdle()
        checkpoint("setContent completed")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Nothing to remember yet")
                .fetchSemanticsNodes().isNotEmpty()
        }
        checkpoint("Home visible")
    }

    @After
    fun tearDown() {
        checkpoint("teardown start")
        try {
            runCatching { detachAppComposition() }
            val idsToCancel = createdThingIds.toMutableSet()
            if (::database.isInitialized && database.isOpen) {
                runCatching {
                    runBlocking {
                        idsToCancel += database.thingDao().observeAll().first().map(ThingEntity::id)
                    }
                }
            }
            if (::reminderScheduler.isInitialized) {
                idsToCancel.forEach { id -> runCatching { reminderScheduler.cancel(id) } }
            }
        } finally {
            if (::database.isInitialized && database.isOpen) runCatching { database.close() }
            runCatching { context.deleteDatabase(TEST_DATABASE) }
            runCatching { restoreNotificationPreferences() }
            checkpoint("teardown end")
        }
    }

    @Test
    fun minimumThingSavesDisplaysAndSurvivesDatabaseReopen() {
        checkpoint("test start: minimum Thing")
        val name = "E2E Minimum Passport"
        val date = futureDate(daysFromNow = 60)
        val expectedIsoDate = importantDateToIso(date.timeInMillis, TimeZone.getDefault().id)

        openCreateFlow(category = "Document")
        composeRule.onNodeWithText("Name").performTextInput(name)
        chooseImportantDate(date)
        composeRule.onNodeWithText("No reminder").performClick()
        composeRule.onNodeWithText("✓  No reminder").performClick()
        checkpoint("Save tapped")
        composeRule.onNodeWithText("Remember this").performClick()

        waitForThingInMyThings(name)
        checkpoint("My Things reached")
        composeRule.onNodeWithText("Document").assertIsDisplayed()
        composeRule.onNodeWithText(formatIsoImportantDate(expectedIsoDate)).assertIsDisplayed()

        val saved = awaitThing(name)
        createdThingIds += saved.id
        assertEquals(name, saved.name)
        assertEquals(ThingCategory.DOCUMENT.code, saved.categoryCode)
        assertEquals(expectedIsoDate, saved.importantDate)
        assertNull(saved.reminderTypeCode)
        assertNull(saved.reminderAtEpochMillis)
        assertNull(saved.reminderTimeZoneId)
        assertNull(saved.notes)
        assertEquals(ThingStatus.ACTIVE.code, saved.statusCode)
        assertEquals(false, saved.originalReminderActionable)
        assertEquals(ReminderDeliveryState.NONE.code, saved.reminderDeliveryStateCode)

        val restored = closeAndReopenDatabase(saved.id)
        assertEquals(saved.id, restored.id)
        assertEquals(saved.name, restored.name)
        assertEquals(saved.categoryCode, restored.categoryCode)
        assertEquals(saved.importantDate, restored.importantDate)
        assertNull(restored.reminderTypeCode)
        assertNull(restored.reminderAtEpochMillis)
        assertNull(restored.reminderTimeZoneId)
        assertNull(restored.notes)
        checkpoint("persistence assertion complete")
    }

    @Test
    fun thingWithReminderAndNotesSavesDisplaysAndSurvivesDatabaseReopen() {
        checkpoint("test start: Thing with optional fields")
        val name = "E2E Vehicle Registration"
        val notes = "Renew registration documents before the due date."
        val timeZone = TimeZone.getDefault()
        val date = futureDate(daysFromNow = 60)
        val expectedIsoDate = importantDateToIso(date.timeInMillis, timeZone.id)
        val expectedReminder = presetReminderMillis(
            importantDateMillis = date.timeInMillis,
            choice = ReminderChoice.ONE_DAY_BEFORE,
            timeZone = timeZone
        )

        openCreateFlow(category = "Vehicle")
        composeRule.onNodeWithText("Name").performTextInput(name)
        chooseImportantDate(date)
        composeRule.onNodeWithText("No reminder").performClick()
        composeRule.onNodeWithText("1 day before").performClick()
        composeRule.onNodeWithText("Notes (optional)").performTextInput(notes)
        checkpoint("Save tapped")
        composeRule.onNodeWithText("Remember this").performClick()

        waitForThingInMyThings(name)
        checkpoint("My Things reached")
        composeRule.onNodeWithText("Vehicle").assertIsDisplayed()
        composeRule.onNodeWithText(formatIsoImportantDate(expectedIsoDate)).assertIsDisplayed()

        val saved = awaitThing(name)
        createdThingIds += saved.id
        assertEquals(name, saved.name)
        assertEquals(ThingCategory.VEHICLE.code, saved.categoryCode)
        assertEquals(expectedIsoDate, saved.importantDate)
        assertEquals(ReminderType.ONE_DAY_BEFORE.code, saved.reminderTypeCode)
        assertEquals(expectedReminder, saved.reminderAtEpochMillis)
        assertEquals(timeZone.id, saved.reminderTimeZoneId)
        assertEquals(notes, saved.notes)
        assertEquals(ThingStatus.ACTIVE.code, saved.statusCode)
        assertEquals(true, saved.originalReminderActionable)
        assertEquals(ReminderDeliveryState.PENDING.code, saved.reminderDeliveryStateCode)

        val restored = closeAndReopenDatabase(saved.id)
        assertEquals(saved.id, restored.id)
        assertEquals(saved.name, restored.name)
        assertEquals(saved.categoryCode, restored.categoryCode)
        assertEquals(saved.importantDate, restored.importantDate)
        assertEquals(saved.reminderTypeCode, restored.reminderTypeCode)
        assertEquals(saved.reminderAtEpochMillis, restored.reminderAtEpochMillis)
        assertEquals(saved.reminderTimeZoneId, restored.reminderTimeZoneId)
        assertEquals(saved.notes, restored.notes)
        assertEquals(saved.originalReminderActionable, restored.originalReminderActionable)
        assertEquals(saved.reminderDeliveryStateCode, restored.reminderDeliveryStateCode)
        checkpoint("persistence assertion complete")
    }

    private fun openCreateFlow(category: String) {
        composeRule.onNodeWithContentDescription("Remember something").performClick()
        composeRule.onNodeWithText("What do you want to remember?").assertIsDisplayed()
        checkpoint("Category Selection reached")
        composeRule.onNodeWithText(category).performClick()
        composeRule.onNodeWithText("Remember something").assertIsDisplayed()
        composeRule.onNodeWithText(category).assertIsDisplayed()
        checkpoint("Add form reached")
    }

    private fun chooseImportantDate(date: Calendar) {
        composeRule.onNodeWithText("Choose a date").performClick()
        onView(isAssignableFrom(DatePicker::class.java)).perform(
            SetDateAction(
                year = date.get(Calendar.YEAR),
                month = date.get(Calendar.MONTH),
                day = date.get(Calendar.DAY_OF_MONTH)
            )
        )
        onView(withText(android.R.string.ok)).perform(click())
        composeRule.waitForIdle()
    }

    private fun waitForThingInMyThings(name: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(name).assertIsDisplayed()
        composeRule.onNodeWithText("All").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Category filter, All categories selected")
            .assertIsDisplayed()
    }

    private fun awaitThing(name: String): ThingEntity = runBlocking {
        withTimeout(10_000) {
            database.thingDao().observeAll().first { things -> things.any { it.name == name } }
                .single { it.name == name }
        }
    }

    private fun closeAndReopenDatabase(id: String): ThingEntity {
        detachAppComposition()
        checkpoint("database close")
        database.close()
        openProductionStack()
        checkpoint("database reopen")
        return runBlocking {
            assertNotNull(database.thingDao().findById(id))
            checkNotNull(database.thingDao().findById(id))
        }
    }

    private fun detachAppComposition() {
        composeRule.runOnIdle {
            composeRule.activity.detachAppComposition()
        }
        composeRule.waitForIdle()
    }

    private fun checkpoint(message: String) {
        Log.i(LOG_TAG, "checkpoint: $message")
    }

    private fun openProductionStack() {
        database = Room.databaseBuilder(
            context,
            KeeplyDatabase::class.java,
            TEST_DATABASE
        ).addMigrations(
            KeeplyDatabase.MIGRATION_1_2,
            KeeplyDatabase.MIGRATION_2_3,
            KeeplyDatabase.MIGRATION_3_4
        ).build()
        repository = ThingRepository(database.thingDao())
        reminderScheduler = AndroidReminderScheduler(context)
        reminderSyncCoordinator = ReminderSyncCoordinator(reminderScheduler)
        missedReminderRecovery = MissedReminderRecovery(
            context,
            repository,
            reminderSyncCoordinator
        )
    }

    private fun futureDate(daysFromNow: Int): Calendar =
        Calendar.getInstance(TimeZone.getDefault()).apply {
            add(Calendar.DAY_OF_YEAR, daysFromNow)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun preserveAndSuppressNotificationPrompts() {
        permissionRequestedWasPresent = notificationPreferences.contains(PERMISSION_REQUESTED)
        permissionRequestedOriginal = notificationPreferences.getBoolean(PERMISSION_REQUESTED, false)
        xiaomiGuidanceWasPresent = notificationPreferences.contains(XIAOMI_GUIDANCE_ACKNOWLEDGED)
        xiaomiGuidanceOriginal = notificationPreferences.getBoolean(XIAOMI_GUIDANCE_ACKNOWLEDGED, false)
        notificationPreferences.edit()
            .putBoolean(PERMISSION_REQUESTED, true)
            .putBoolean(XIAOMI_GUIDANCE_ACKNOWLEDGED, true)
            .commit()
    }

    private fun restoreNotificationPreferences() {
        notificationPreferences.edit().apply {
            if (permissionRequestedWasPresent) {
                putBoolean(PERMISSION_REQUESTED, permissionRequestedOriginal)
            } else {
                remove(PERMISSION_REQUESTED)
            }
            if (xiaomiGuidanceWasPresent) {
                putBoolean(XIAOMI_GUIDANCE_ACKNOWLEDGED, xiaomiGuidanceOriginal)
            } else {
                remove(XIAOMI_GUIDANCE_ACKNOWLEDGED)
            }
        }.commit()
    }

    private class SetDateAction(
        private val year: Int,
        private val month: Int,
        private val day: Int
    ) : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()

        override fun getDescription(): String = "set date to $year-${month + 1}-$day"

        override fun perform(uiController: UiController, view: View) {
            (view as DatePicker).updateDate(year, month, day)
            uiController.loopMainThreadUntilIdle()
        }
    }

    private companion object {
        const val TEST_DATABASE = "keeply-v1-creation-flow-test.db"
        const val NOTIFICATION_PREFERENCES = "notification_preferences"
        const val PERMISSION_REQUESTED = "post_notifications_requested"
        const val XIAOMI_GUIDANCE_ACKNOWLEDGED = "xiaomi_autostart_guidance_acknowledged"
        const val LOG_TAG = "ThingCreationE2E"
        const val TEST_TIMEOUT_SECONDS = 120L
    }
}
