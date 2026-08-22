package com.keeply.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.keeply.app.R
import com.keeply.app.model.Thing
import com.keeply.app.notifications.ReminderSyncResult
import com.keeply.app.ui.theme.KeeplyTheme
import kotlinx.coroutines.delay

private enum class AppDestination {
    HOME,
    MY_THINGS,
    CATEGORY_SELECTION,
    ADD_THING,
    ITEM_DETAILS,
    EDIT_THING
}

@Composable
fun KeeplyApp(
    viewModel: KeeplyViewModel? = null,
    requestedThingId: String? = null,
    onRequestedThingConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    var showStartup by remember { mutableStateOf(true) }
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var previousPrimaryDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    var selectedCategory by rememberSaveable { mutableStateOf<CategoryGlyph?>(null) }
    var addThingBackDestination by rememberSaveable {
        mutableStateOf(AppDestination.CATEGORY_SELECTION)
    }
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedThingId by rememberSaveable { mutableStateOf<String?>(null) }
    var updateError by rememberSaveable { mutableStateOf<String?>(null) }
    var lifecycleError by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val things = viewModel?.things?.collectAsStateWithLifecycle()?.value.orEmpty()
    val myThingsFilter = viewModel?.myThingsFilter?.collectAsStateWithLifecycle()?.value
        ?: MyThingsFilter.ALL
    val isSaving = viewModel?.isSaving?.collectAsStateWithLifecycle()?.value ?: false
    val itemDetailsState = viewModel?.itemDetailsState?.collectAsStateWithLifecycle()?.value
        ?: ItemDetailsState.NotSelected
    val isUpdating = viewModel?.isUpdating?.collectAsStateWithLifecycle()?.value ?: false
    val isChangingLifecycle = viewModel?.isChangingLifecycle?.collectAsStateWithLifecycle()?.value ?: false
    var pendingPermissionSuccess by remember { mutableStateOf<String?>(null) }
    var permissionOutcome by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val successMessage = pendingPermissionSuccess
        pendingPermissionSuccess = null
        if (successMessage != null) permissionOutcome = granted to successMessage
    }

    LaunchedEffect(Unit) {
        delay(900)
        showStartup = false
    }

    LaunchedEffect(showStartup, requestedThingId, viewModel) {
        if (!showStartup && requestedThingId != null) {
            selectedThingId = requestedThingId
            destination = AppDestination.ITEM_DETAILS
            viewModel?.selectThing(requestedThingId)
            onRequestedThingConsumed()
        }
    }

    suspend fun showReminderFeedback(
        result: ReminderSyncResult,
        successMessage: String
    ) {
        when (result) {
            ReminderSyncResult.NotificationsDisabled -> {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED &&
                    !notificationPermissionWasRequested(context)
                ) {
                    markNotificationPermissionRequested(context)
                    pendingPermissionSuccess = successMessage
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = "Saved to Keeply\nNotifications are off, so Keeply can't notify you yet.",
                        actionLabel = "Settings"
                    )
                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                        openNotificationSettings(context)
                    }
                }
            }
            ReminderSyncResult.ScheduledInexact -> {
                val snackbarResult = snackbarHostState.showSnackbar(
                    message = successMessage,
                    actionLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Exact timing" else null
                )
                if (snackbarResult == SnackbarResult.ActionPerformed) openExactAlarmSettings(context)
            }
            ReminderSyncResult.Failed -> snackbarHostState.showSnackbar(
                "Saved to Keeply\nKeeply couldn't activate notifications yet. Open Keeply to try again."
            )
            ReminderSyncResult.NoReminder,
            ReminderSyncResult.Past,
            ReminderSyncResult.ScheduledExact -> snackbarHostState.showSnackbar(successMessage)
        }
    }

    LaunchedEffect(permissionOutcome) {
        val (granted, successMessage) = permissionOutcome ?: return@LaunchedEffect
        permissionOutcome = null
        if (granted) {
            viewModel?.reconcileReminders()
            val result = snackbarHostState.showSnackbar(
                message = successMessage,
                actionLabel = if (exactAlarmAccessUnavailable(context)) "Exact timing" else null
            )
            if (result == SnackbarResult.ActionPerformed) openExactAlarmSettings(context)
        } else {
            val result = snackbarHostState.showSnackbar(
                message = "Saved to Keeply\nNotifications are off, so Keeply can't notify you yet.",
                actionLabel = "Settings"
            )
            if (result == SnackbarResult.ActionPerformed) openNotificationSettings(context)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel?.saveEvents?.collect { event ->
            when (event) {
                is SaveThingEvent.Saved -> {
                    saveError = null
                    destination = AppDestination.MY_THINGS
                    showReminderFeedback(
                        event.reminderSyncResult,
                        event.thing.persistenceSuccessMessage()
                    )
                }
                SaveThingEvent.Failed -> {
                    saveError = "Keeply couldn't save this yet. Please try again."
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel?.updateEvents?.collect { event ->
            when (event) {
                is UpdateThingEvent.Updated -> {
                    updateError = null
                    destination = AppDestination.ITEM_DETAILS
                    showReminderFeedback(
                        event.reminderSyncResult,
                        event.thing.persistenceSuccessMessage()
                    )
                }
                UpdateThingEvent.Unchanged -> {
                    updateError = null
                    destination = AppDestination.ITEM_DETAILS
                }
                UpdateThingEvent.Failed -> {
                    updateError = "Keeply couldn't update this yet. Please try again."
                }
                UpdateThingEvent.Missing -> updateError = null
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel?.lifecycleEvents?.collect { event ->
            when (event) {
                is LifecycleEvent.ReminderUpdated -> {
                    lifecycleError = null
                    showReminderFeedback(
                        event.reminderSyncResult,
                        "New reminder set\nKeeply will remind you on ${formatFollowUpReminder(event.reminderAtEpochMillis, event.timeZoneId)}."
                    )
                }
                LifecycleEvent.MarkedDone -> {
                    lifecycleError = null
                    snackbarHostState.showSnackbar("Marked as done")
                }
                LifecycleEvent.Reopened -> {
                    lifecycleError = null
                    snackbarHostState.showSnackbar("Reopened")
                }
                LifecycleEvent.Deleted -> {
                    lifecycleError = null
                    destination = AppDestination.MY_THINGS
                    selectedThingId = null
                    viewModel?.clearSelectedThing()
                    snackbarHostState.showSnackbar("Deleted")
                }
                LifecycleEvent.Failed -> lifecycleError = "Keeply couldn't make that change yet. Please try again."
                LifecycleEvent.Missing -> {
                    lifecycleError = null
                    destination = AppDestination.MY_THINGS
                    selectedThingId = null
                    viewModel?.clearSelectedThing()
                    snackbarHostState.showSnackbar("This thing is no longer available.")
                }
            }
        }
    }

    LaunchedEffect(destination, selectedThingId, viewModel) {
        if (
            destination == AppDestination.ITEM_DETAILS ||
            destination == AppDestination.EDIT_THING
        ) {
            selectedThingId?.let { viewModel?.selectThing(it) }
        }
    }

    if (showStartup) {
        KeeplyStartupScreen()
        return
    }

    val openCategorySelection = {
        if (destination != AppDestination.CATEGORY_SELECTION) {
            previousPrimaryDestination = destination
        }
        destination = AppDestination.CATEGORY_SELECTION
    }

    BackHandler(enabled = destination == AppDestination.CATEGORY_SELECTION) {
        destination = previousPrimaryDestination
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (destination == AppDestination.HOME || destination == AppDestination.MY_THINGS) {
                KeeplyNavigationBar(
                    destination = destination,
                    onDestinationSelected = { destination = it },
                    onRememberSomething = openCategorySelection
                )
            }
        }
    ) { innerPadding ->
        when (destination) {
            AppDestination.HOME -> EmptyHomeScreen(
                onRememberSomething = openCategorySelection,
                onCategoryShortcut = { category ->
                    saveError = null
                    selectedCategory = category
                    addThingBackDestination = AppDestination.HOME
                    destination = AppDestination.ADD_THING
                },
                modifier = Modifier.padding(innerPadding)
            )

            AppDestination.MY_THINGS -> MyThingsShell(
                things = things,
                selectedFilter = myThingsFilter,
                onFilterSelected = { viewModel?.selectMyThingsFilter(it) },
                onThingSelected = { thingId ->
                    selectedThingId = thingId
                    destination = AppDestination.ITEM_DETAILS
                },
                modifier = Modifier.padding(innerPadding)
            )

            AppDestination.CATEGORY_SELECTION -> CategorySelectionScreen(
                onBack = { destination = previousPrimaryDestination },
                onCategorySelected = { category ->
                    saveError = null
                    selectedCategory = category
                    addThingBackDestination = AppDestination.CATEGORY_SELECTION
                    destination = AppDestination.ADD_THING
                },
                modifier = Modifier.padding(innerPadding)
            )

            AppDestination.ADD_THING -> AddThingScreen(
                category = checkNotNull(selectedCategory),
                onBack = { destination = addThingBackDestination },
                onRememberThing = { draft ->
                    saveError = null
                    viewModel?.createThing(draft)
                },
                isSaving = isSaving,
                saveError = saveError,
                modifier = Modifier.padding(innerPadding)
            )

            AppDestination.ITEM_DETAILS -> ItemDetailsScreen(
                state = itemDetailsState,
                onBack = {
                    destination = AppDestination.MY_THINGS
                    selectedThingId = null
                    viewModel?.clearSelectedThing()
                },
                onEdit = {
                    updateError = null
                    destination = AppDestination.EDIT_THING
                },
                onRemindAgain = { millis, zone ->
                    lifecycleError = null
                    selectedThingId?.let { viewModel?.remindAgain(it, millis, zone) }
                },
                onMarkDone = {
                    lifecycleError = null
                    selectedThingId?.let { viewModel?.markDone(it) }
                },
                onReopen = {
                    lifecycleError = null
                    selectedThingId?.let { viewModel?.reopen(it) }
                },
                onDelete = {
                    lifecycleError = null
                    selectedThingId?.let { viewModel?.deleteThing(it) }
                },
                isChangingLifecycle = isChangingLifecycle,
                lifecycleError = lifecycleError,
                modifier = Modifier.padding(innerPadding)
            )

            AppDestination.EDIT_THING -> when (val details = itemDetailsState) {
                is ItemDetailsState.Content -> EditThingScreen(
                    thing = details.thing,
                    onBack = { destination = AppDestination.ITEM_DETAILS },
                    onSaveChanges = { draft ->
                        updateError = null
                        selectedThingId?.let { viewModel?.updateThing(it, draft) }
                    },
                    onUnchangedSave = { destination = AppDestination.ITEM_DETAILS },
                    isUpdating = isUpdating,
                    updateError = updateError,
                    modifier = Modifier.padding(innerPadding)
                )
                else -> EditThingUnavailableScreen(
                    state = details,
                    onBackToMyThings = {
                        destination = AppDestination.MY_THINGS
                        selectedThingId = null
                        viewModel?.clearSelectedThing()
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun KeeplyNavigationBar(
    destination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit,
    onRememberSomething: () -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .navigationBarsPadding()
            .height(72.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationBarItem(
                modifier = Modifier.weight(1f),
                selected = destination == AppDestination.HOME,
                onClick = { onDestinationSelected(AppDestination.HOME) },
                icon = { HomeIcon(destination == AppDestination.HOME) },
                label = { Text("Home") },
                colors = keeplyNavigationColors()
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FloatingActionButton(
                    onClick = onRememberSomething,
                    modifier = Modifier
                        .size(52.dp)
                        .semantics { contentDescription = "Remember something" },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(text = "+", fontSize = 32.sp)
                }
            }
            NavigationBarItem(
                modifier = Modifier.weight(1f),
                selected = destination == AppDestination.MY_THINGS,
                onClick = { onDestinationSelected(AppDestination.MY_THINGS) },
                icon = { ThingsIcon(destination == AppDestination.MY_THINGS) },
                label = { Text("My Things") },
                colors = keeplyNavigationColors()
            )
        }
    }
}

@Composable
private fun keeplyNavigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = Color.Transparent,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
private fun EmptyHomeScreen(
    onRememberSomething: () -> Unit,
    onCategoryShortcut: (CategoryGlyph) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KeeplyHeader()
        Spacer(Modifier.height(22.dp))
        HomeHeroImage()
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Nothing to remember yet",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Add the things you don't want to forget,\nand Keeply will remind you when they matter.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        ExampleThings(onCategoryShortcut = onCategoryShortcut)
        Spacer(Modifier.height(12.dp))
        ReassurancePanel()
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onRememberSomething,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("+  Remember something")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun KeeplyHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Image(
                painter = painterResource(R.drawable.keeply_header),
                contentDescription = "Keeply",
                modifier = Modifier.height(36.dp),
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart
            )
        }
        SearchIcon()
        Spacer(Modifier.width(20.dp))
        SettingsIcon()
    }
}

@Composable
private fun SearchIcon() {
    val teal = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(28.dp).semantics { contentDescription = "Search, unavailable in Milestone 1" }) {
        drawCircle(teal, size.width * .28f, Offset(size.width * .42f, size.height * .40f), style = Stroke(2.5.dp.toPx()))
        drawLine(teal, Offset(size.width * .62f, size.height * .61f), Offset(size.width * .85f, size.height * .84f), 2.5.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun SettingsIcon() {
    val teal = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(28.dp).semantics { contentDescription = "Settings, unavailable in Milestone 1" }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            val inner = size.width * .31f
            val outer = size.width * .45f
            drawLine(
                teal,
                Offset(center.x + kotlin.math.cos(angle).toFloat() * inner, center.y + kotlin.math.sin(angle).toFloat() * inner),
                Offset(center.x + kotlin.math.cos(angle).toFloat() * outer, center.y + kotlin.math.sin(angle).toFloat() * outer),
                4.dp.toPx(),
                StrokeCap.Round
            )
        }
        drawCircle(teal, size.width * .28f, center, style = Stroke(3.dp.toPx()))
        drawCircle(teal, size.width * .08f, center)
    }
}

@Composable
private fun HomeHeroImage() {
    Image(
        painter = painterResource(R.drawable.keeply_calendar_reminder_hero),
        contentDescription = "Calendar and reminder illustration",
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
    )
}

@Composable
private fun ReassurancePanel() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniBellIcon()
        Spacer(Modifier.width(12.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Keeply will notify you on time")
                }
                append("\nso you'll never miss what matters.")
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class ExampleThing(
    val title: String,
    val example: String,
    val glyph: CategoryGlyph
)

internal enum class CategoryGlyph {
    DOCUMENT, OWNED, PAYMENT, EXCHANGE, MONEY, VEHICLE, HOME, MEDICINE, OTHER
}

@Composable
private fun ExampleThings(onCategoryShortcut: (CategoryGlyph) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Examples of things to remember",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        val examples = listOf(
            ExampleThing("Documents", "Passport, license", CategoryGlyph.DOCUMENT),
            ExampleThing("Things I own", "Warranty, receipts", CategoryGlyph.OWNED),
            ExampleThing("Payments", "Bills, subscriptions", CategoryGlyph.PAYMENT),
            ExampleThing("Lent / Borrowed", "Lend or borrow", CategoryGlyph.EXCHANGE),
            ExampleThing("Money owed", "You owe / owed", CategoryGlyph.MONEY),
            ExampleThing("Vehicle", "Registration, insurance", CategoryGlyph.VEHICLE),
            ExampleThing("Home / Appliance", "Maintenance, warranty", CategoryGlyph.HOME),
            ExampleThing("Medicine", "Expiration dates", CategoryGlyph.MEDICINE)
        )
        examples.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { example ->
                    ExampleCard(
                        example = example,
                        onClick = { onCategoryShortcut(example.glyph) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExampleCard(
    example: ExampleThing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(116.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(13.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
                RoundedCornerShape(14.dp)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CategoryIcon(example.glyph)
        Text(
            text = example.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Text(
            text = example.example,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun ClipboardIllustration() {
    Image(
        painter = painterResource(R.drawable.clipboard_checklist),
        contentDescription = "Clipboard checklist illustration",
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
    )
}

@Composable
internal fun MyThingsShell(
    things: List<Thing>,
    selectedFilter: MyThingsFilter,
    onFilterSelected: (MyThingsFilter) -> Unit,
    onThingSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredThings = remember(things, selectedFilter) { things.filteredBy(selectedFilter) }
    if (things.isEmpty()) {
        EmptyMyThingsShell(selectedFilter, onFilterSelected, modifier)
    } else {
        PopulatedMyThingsShell(
            things = filteredThings,
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected,
            onThingSelected = onThingSelected,
            modifier = modifier
        )
    }
}

@Composable
private fun EmptyMyThingsShell(
    selectedFilter: MyThingsFilter,
    onFilterSelected: (MyThingsFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeeplyHeader()
        Spacer(Modifier.height(20.dp))
        MyThingsFilterControl(selectedFilter, onFilterSelected)
        Spacer(Modifier.weight(0.8f))
        ClipboardIllustration()
        Spacer(Modifier.height(26.dp))
        Text(
            text = "My Things",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Things you ask Keeply to remember\nwill appear here.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1.2f))
    }
}

@Composable
private fun PopulatedMyThingsShell(
    things: List<Thing>,
    selectedFilter: MyThingsFilter,
    onFilterSelected: (MyThingsFilter) -> Unit,
    onThingSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        KeeplyHeader()
        Spacer(Modifier.height(28.dp))
        Text(
            text = "My Things",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(14.dp))
        MyThingsFilterControl(selectedFilter, onFilterSelected)
        Spacer(Modifier.height(16.dp))
        if (things.isEmpty()) {
            FilterEmptyState(selectedFilter, Modifier.weight(1f))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items = things, key = Thing::id) { thing ->
                    ThingSummaryRow(
                        thing = thing,
                        onClick = { onThingSelected(thing.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyThingsFilterControl(
    selectedFilter: MyThingsFilter,
    onFilterSelected: (MyThingsFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MyThingsFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onFilterSelected(filter) }
                    )
                    .semantics {
                        stateDescription = if (selected) "Selected" else "Not selected"
                    }
                    .padding(horizontal = 6.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FilterEmptyState(filter: MyThingsFilter, modifier: Modifier = Modifier) {
    val (title, description) = when (filter) {
        MyThingsFilter.ACTIVE -> "No active things" to
            "Things you're still keeping track of will appear here."
        MyThingsFilter.COMPLETED -> "Nothing completed yet" to
            "Things you mark as done will appear here."
        MyThingsFilter.ALL -> "No things to show" to "Your saved things will appear here."
    }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text = description,
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ThingSummaryRow(thing: Thing, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Open ${thing.name} details" }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(thing.category.toCategoryGlyph())
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = thing.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = thing.category.displayName(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatIsoImportantDate(thing.importantDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KeeplyStartupScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.keeply_splash),
            contentDescription = "Keeply splash screen",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )
    }
}

@Composable
internal fun CategoryIcon(glyph: CategoryGlyph) {
    val teal = when (glyph) {
        CategoryGlyph.DOCUMENT -> Color(0xFF7451B9)
        CategoryGlyph.OWNED, CategoryGlyph.EXCHANGE -> Color(0xFFE06B17)
        CategoryGlyph.PAYMENT -> Color(0xFF176A9A)
        CategoryGlyph.MEDICINE -> Color(0xFFC83C3C)
        else -> MaterialTheme.colorScheme.primary
    }
    val iconBackground = when (glyph) {
        CategoryGlyph.DOCUMENT -> Color(0xFFECE5F8)
        CategoryGlyph.OWNED, CategoryGlyph.EXCHANGE -> Color(0xFFFFEBD9)
        CategoryGlyph.PAYMENT -> Color(0xFFDDEEF8)
        CategoryGlyph.MEDICINE -> Color(0xFFFBE2E2)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    Canvas(
        modifier = Modifier
            .size(34.dp)
            .semantics { contentDescription = "${glyph.name.lowercase()} category" }
    ) {
        drawCircle(iconBackground, size.minDimension / 2f)
        val stroke = 1.8.dp.toPx()
        when (glyph) {
            CategoryGlyph.DOCUMENT -> {
                drawRoundRect(teal, Offset(size.width * .30f, size.height * .19f), Size(size.width * .40f, size.height * .62f), CornerRadius(2.dp.toPx()), Stroke(stroke))
                drawLine(teal, Offset(size.width * .38f, size.height * .42f), Offset(size.width * .62f, size.height * .42f), stroke)
                drawLine(teal, Offset(size.width * .38f, size.height * .56f), Offset(size.width * .58f, size.height * .56f), stroke)
            }
            CategoryGlyph.OWNED -> {
                val box = Path().apply {
                    moveTo(size.width * .20f, size.height * .36f); lineTo(size.width * .50f, size.height * .20f)
                    lineTo(size.width * .80f, size.height * .36f); lineTo(size.width * .50f, size.height * .52f); close()
                }
                drawPath(box, teal, style = Stroke(stroke, cap = StrokeCap.Round))
                drawLine(teal, Offset(size.width * .20f, size.height * .36f), Offset(size.width * .20f, size.height * .69f), stroke)
                drawLine(teal, Offset(size.width * .80f, size.height * .36f), Offset(size.width * .80f, size.height * .69f), stroke)
                drawLine(teal, Offset(size.width * .20f, size.height * .69f), Offset(size.width * .50f, size.height * .84f), stroke)
                drawLine(teal, Offset(size.width * .80f, size.height * .69f), Offset(size.width * .50f, size.height * .84f), stroke)
                drawLine(teal, Offset(size.width * .50f, size.height * .52f), Offset(size.width * .50f, size.height * .84f), stroke)
            }
            CategoryGlyph.PAYMENT -> {
                drawRoundRect(teal, Offset(size.width * .20f, size.height * .31f), Size(size.width * .60f, size.height * .42f), CornerRadius(3.dp.toPx()), Stroke(stroke))
                drawLine(teal, Offset(size.width * .20f, size.height * .43f), Offset(size.width * .80f, size.height * .43f), stroke)
                drawLine(teal, Offset(size.width * .56f, size.height * .60f), Offset(size.width * .69f, size.height * .60f), stroke, StrokeCap.Round)
            }
            CategoryGlyph.MONEY -> {
                val bag = Path().apply {
                    moveTo(size.width * .38f, size.height * .28f); lineTo(size.width * .62f, size.height * .28f)
                    lineTo(size.width * .58f, size.height * .38f)
                    quadraticTo(size.width * .76f, size.height * .48f, size.width * .72f, size.height * .69f)
                    quadraticTo(size.width * .68f, size.height * .82f, size.width * .50f, size.height * .82f)
                    quadraticTo(size.width * .32f, size.height * .82f, size.width * .28f, size.height * .69f)
                    quadraticTo(size.width * .24f, size.height * .48f, size.width * .42f, size.height * .38f)
                    close()
                }
                drawPath(bag, teal, style = Stroke(stroke, cap = StrokeCap.Round))
                drawLine(teal, Offset(size.width * .43f, size.height * .55f), Offset(size.width * .57f, size.height * .55f), stroke)
                drawLine(teal, Offset(size.width * .50f, size.height * .48f), Offset(size.width * .50f, size.height * .69f), stroke)
            }
            CategoryGlyph.EXCHANGE -> {
                val hands = Path().apply {
                    moveTo(size.width * .16f, size.height * .43f); lineTo(size.width * .34f, size.height * .32f)
                    lineTo(size.width * .49f, size.height * .45f); lineTo(size.width * .63f, size.height * .33f)
                    lineTo(size.width * .84f, size.height * .45f); lineTo(size.width * .64f, size.height * .68f)
                    quadraticTo(size.width * .57f, size.height * .75f, size.width * .49f, size.height * .67f)
                    lineTo(size.width * .39f, size.height * .58f); lineTo(size.width * .29f, size.height * .68f); close()
                }
                drawPath(hands, teal, style = Stroke(stroke, cap = StrokeCap.Round))
                drawLine(teal, Offset(size.width * .42f, size.height * .51f), Offset(size.width * .58f, size.height * .64f), stroke, StrokeCap.Round)
            }
            CategoryGlyph.VEHICLE -> {
                val car = Path().apply {
                    moveTo(size.width * .18f, size.height * .62f); lineTo(size.width * .25f, size.height * .40f)
                    quadraticTo(size.width * .28f, size.height * .31f, size.width * .38f, size.height * .31f)
                    lineTo(size.width * .62f, size.height * .31f)
                    quadraticTo(size.width * .72f, size.height * .31f, size.width * .75f, size.height * .40f)
                    lineTo(size.width * .82f, size.height * .62f); lineTo(size.width * .78f, size.height * .73f)
                    lineTo(size.width * .22f, size.height * .73f); close()
                }
                drawPath(car, teal, style = Stroke(stroke, cap = StrokeCap.Round))
                drawLine(teal, Offset(size.width * .28f, size.height * .50f), Offset(size.width * .72f, size.height * .50f), stroke)
                drawCircle(teal, size.width * .07f, Offset(size.width * .32f, size.height * .75f))
                drawCircle(teal, size.width * .07f, Offset(size.width * .68f, size.height * .75f))
            }
            CategoryGlyph.HOME -> {
                val roof = Path().apply { moveTo(size.width * .22f, size.height * .46f); lineTo(size.width * .50f, size.height * .22f); lineTo(size.width * .78f, size.height * .46f) }
                drawPath(roof, teal, style = Stroke(stroke, cap = StrokeCap.Round))
                drawRect(teal, Offset(size.width * .30f, size.height * .45f), Size(size.width * .40f, size.height * .34f), style = Stroke(stroke))
            }
            CategoryGlyph.MEDICINE -> {
                drawRoundRect(teal, Offset(size.width * .34f, size.height * .18f), Size(size.width * .32f, size.height * .14f), CornerRadius(2.dp.toPx()), Stroke(stroke))
                drawRoundRect(teal, Offset(size.width * .27f, size.height * .34f), Size(size.width * .46f, size.height * .50f), CornerRadius(5.dp.toPx()), Stroke(stroke))
                drawRect(teal, Offset(size.width * .32f, size.height * .50f), Size(size.width * .36f, size.height * .20f), style = Stroke(stroke))
                drawLine(teal, Offset(size.width * .50f, size.height * .53f), Offset(size.width * .50f, size.height * .67f), stroke)
                drawLine(teal, Offset(size.width * .43f, size.height * .60f), Offset(size.width * .57f, size.height * .60f), stroke)
            }
            CategoryGlyph.OTHER -> {
                repeat(3) { index ->
                    drawCircle(
                        color = teal,
                        radius = 2.5.dp.toPx(),
                        center = Offset(size.width * (.34f + index * .16f), size.height * .52f)
                    )
                }
            }
        }
    }
}

private val MaterialThemeColorFallback = Color(0xFFD8EFEC)

@Composable
private fun MiniBellIcon() {
    Image(
        painter = painterResource(R.drawable.keeply_bell),
        contentDescription = "Reminder",
        modifier = Modifier.size(28.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun HomeIcon(selected: Boolean) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val cutoutColor = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .semantics { contentDescription = "Home" }
    ) {
        val house = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.48f)
            lineTo(size.width * 0.50f, size.height * 0.16f)
            lineTo(size.width * 0.86f, size.height * 0.48f)
            lineTo(size.width * 0.76f, size.height * 0.48f)
            lineTo(size.width * 0.76f, size.height * 0.84f)
            lineTo(size.width * 0.24f, size.height * 0.84f)
            lineTo(size.width * 0.24f, size.height * 0.48f)
            close()
        }
        drawPath(house, color, style = androidx.compose.ui.graphics.drawscope.Fill)
        drawRect(
            cutoutColor,
            Offset(size.width * .44f, size.height * .60f),
            Size(size.width * .12f, size.height * .24f)
        )
    }
}

@Composable
private fun ThingsIcon(selected: Boolean) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .semantics { contentDescription = "My Things" }
    ) {
        val stroke = 2.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.18f, size.height * 0.25f),
            size = Size(size.width * 0.64f, size.height * 0.58f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = if (selected) androidx.compose.ui.graphics.drawscope.Fill else Stroke(stroke)
        )
        val detailColor = if (selected) MaterialThemeColorFallback else color
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * .36f, size.height * .16f),
            size = Size(size.width * .28f, size.height * .16f),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        drawLine(
            color = detailColor,
            start = Offset(size.width * 0.18f, size.height * 0.43f),
            end = Offset(size.width * 0.82f, size.height * 0.43f),
            strokeWidth = stroke
        )
        drawLine(detailColor, Offset(size.width * .33f, size.height * .58f), Offset(size.width * .67f, size.height * .58f), stroke, StrokeCap.Round)
        drawLine(detailColor, Offset(size.width * .33f, size.height * .70f), Offset(size.width * .60f, size.height * .70f), stroke, StrokeCap.Round)
    }
}

private const val NotificationPreferences = "notification_preferences"
private const val NotificationPermissionRequested = "post_notifications_requested"

private fun notificationPermissionWasRequested(context: Context): Boolean =
    context.getSharedPreferences(NotificationPreferences, Context.MODE_PRIVATE)
        .getBoolean(NotificationPermissionRequested, false)

private fun markNotificationPermissionRequested(context: Context) {
    context.getSharedPreferences(NotificationPreferences, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(NotificationPermissionRequested, true)
        .apply()
}

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    context.startActivity(
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun exactAlarmAccessUnavailable(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

@Preview(showBackground = true)
@Composable
private fun KeeplyAppPreview() {
    KeeplyTheme {
        KeeplyApp()
    }
}
