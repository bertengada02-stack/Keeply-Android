package com.keeply.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeply.app.model.NewThingDraft
import com.keeply.app.model.Thing
import com.keeply.app.model.wouldPersistChanges
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal enum class ReminderChoice(val label: String, val daysBefore: Int?) {
    ON_DAY("On the day", 0),
    ONE_DAY_BEFORE("1 day before", 1),
    THREE_DAYS_BEFORE("3 days before", 3),
    ONE_WEEK_BEFORE("1 week before", 7),
    THIRTY_DAYS_BEFORE("30 days before", 30),
    CUSTOM("Custom", null)
}

internal const val NAME_REQUIRED_ERROR = "Tell Keeply what you want to remember."
internal const val IMPORTANT_DATE_REQUIRED_ERROR = "Choose an important date."
internal const val PAST_REMINDER_ERROR = "That reminder time has already passed. Choose another reminder."
internal const val REMINDER_AFTER_IMPORTANT_DATE_ERROR = "Choose a reminder on or before the important date."

internal fun validateName(name: String): String? =
    NAME_REQUIRED_ERROR.takeIf { name.isBlank() }

internal fun validateImportantDate(importantDateMillis: Long?): String? =
    IMPORTANT_DATE_REQUIRED_ERROR.takeIf { importantDateMillis == null }

internal fun presetReminderMillis(
    importantDateMillis: Long,
    choice: ReminderChoice,
    timeZone: TimeZone
): Long {
    require(choice.daysBefore != null)
    return Calendar.getInstance(timeZone).apply {
        timeInMillis = importantDateMillis
        add(Calendar.DAY_OF_MONTH, -choice.daysBefore)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

internal fun validateReminderTime(reminderMillis: Long, nowMillis: Long): String? =
    PAST_REMINDER_ERROR.takeIf { reminderMillis <= nowMillis }

internal fun importantDateCutoffMillis(importantDateMillis: Long, timeZone: TimeZone): Long =
    Calendar.getInstance(timeZone).apply {
        timeInMillis = importantDateMillis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

internal fun validateReminderWindow(
    reminderMillis: Long,
    nowMillis: Long,
    importantDateMillis: Long,
    timeZone: TimeZone
): String? = when {
    reminderMillis <= nowMillis -> PAST_REMINDER_ERROR
    reminderMillis > importantDateCutoffMillis(importantDateMillis, timeZone) ->
        REMINDER_AFTER_IMPORTANT_DATE_ERROR
    else -> null
}

internal fun isPresetReminderAvailable(
    importantDateMillis: Long,
    choice: ReminderChoice,
    nowMillis: Long,
    timeZone: TimeZone
): Boolean = validateReminderWindow(
    reminderMillis = presetReminderMillis(importantDateMillis, choice, timeZone),
    nowMillis = nowMillis,
    importantDateMillis = importantDateMillis,
    timeZone = timeZone
) == null

internal fun resolveReminderMillis(
    importantDateMillis: Long?,
    choice: ReminderChoice?,
    customReminderMillis: Long?,
    timeZone: TimeZone
): Long? = when (choice) {
    null -> null
    ReminderChoice.CUSTOM -> customReminderMillis
    else -> importantDateMillis?.let {
        presetReminderMillis(it, choice, timeZone)
    }
}

@Composable
internal fun AddThingScreen(
    category: CategoryGlyph,
    onBack: () -> Unit,
    onRememberThing: (NewThingDraft) -> Unit,
    isSaving: Boolean,
    saveError: String?,
    modifier: Modifier = Modifier,
    nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    ThingFormScreen(
        initialThing = null,
        initialCategory = category,
        heading = "Remember something",
        primaryAction = "Remember this",
        categoryEditable = false,
        onBack = onBack,
        onSubmit = { draft, _ -> onRememberThing(draft) },
        onUnchangedSave = {},
        isSubmitting = isSaving,
        submitError = saveError,
        modifier = modifier,
        nowMillis = nowMillis
    )
}

@Composable
internal fun EditThingScreen(
    thing: Thing,
    onBack: () -> Unit,
    onSaveChanges: (NewThingDraft) -> Unit,
    onUnchangedSave: () -> Unit,
    isUpdating: Boolean,
    updateError: String?,
    modifier: Modifier = Modifier,
    nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    ThingFormScreen(
        initialThing = thing,
        initialCategory = thing.category.toCategoryGlyph(),
        heading = "Edit thing",
        primaryAction = "Save changes",
        categoryEditable = true,
        onBack = onBack,
        onSubmit = { draft, _ -> onSaveChanges(draft) },
        onUnchangedSave = onUnchangedSave,
        isSubmitting = isUpdating,
        submitError = updateError,
        modifier = modifier,
        nowMillis = nowMillis
    )
}

@Composable
internal fun EditThingUnavailableScreen(
    state: ItemDetailsState,
    onBackToMyThings: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToMyThings)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBackToMyThings,
                modifier = Modifier.semantics { contentDescription = "Back" }
            ) {
                BackArrowIcon()
            }
        }
        Spacer(Modifier.height(72.dp))
        if (state == ItemDetailsState.Loading || state == ItemDetailsState.NotSelected) {
            CircularProgressIndicator()
        } else {
            Text(
                text = if (state == ItemDetailsState.NotFound) {
                    "This thing is no longer available."
                } else {
                    "Keeply couldn't open this thing."
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(
                onClick = onBackToMyThings,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Back to My Things")
            }
        }
    }
}

@Composable
private fun ThingFormScreen(
    initialThing: Thing?,
    initialCategory: CategoryGlyph,
    heading: String,
    primaryAction: String,
    categoryEditable: Boolean,
    onBack: () -> Unit,
    onSubmit: (NewThingDraft, Boolean) -> Unit,
    onUnchangedSave: () -> Unit,
    isSubmitting: Boolean,
    submitError: String?,
    modifier: Modifier = Modifier,
    nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    val context = LocalContext.current
    var timeZone by rememberSaveable(initialThing?.id) {
        mutableStateOf(initialThing?.reminderTimeZoneId ?: TimeZone.getDefault().id)
    }
    var category by rememberSaveable(initialThing?.id) { mutableStateOf(initialCategory) }
    var name by rememberSaveable(initialThing?.id) { mutableStateOf(initialThing?.name.orEmpty()) }
    var importantDateMillis by rememberSaveable(initialThing?.id) {
        mutableStateOf(initialThing?.importantDate?.let { isoImportantDateToMillis(it, timeZone) })
    }
    var reminderChoice by rememberSaveable(initialThing?.id) {
        mutableStateOf(initialThing?.reminderType?.toReminderChoice())
    }
    var reminderExplicitlySelected by rememberSaveable(initialThing?.id) { mutableStateOf(false) }
    var customReminderMillis by rememberSaveable(initialThing?.id) {
        mutableStateOf(initialThing?.reminderAtEpochMillis.takeIf {
            initialThing?.reminderType == com.keeply.app.model.ReminderType.CUSTOM
        })
    }
    var notes by rememberSaveable(initialThing?.id) { mutableStateOf(initialThing?.notes.orEmpty()) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var importantDateError by rememberSaveable { mutableStateOf<String?>(null) }
    var reminderError by rememberSaveable { mutableStateOf<String?>(null) }
    var showReminderChoices by rememberSaveable { mutableStateOf(false) }
    var showCategoryChoices by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    fun currentReminderMillis(): Long? = resolveReminderMillis(
        importantDateMillis = importantDateMillis,
        choice = reminderChoice,
        customReminderMillis = customReminderMillis,
        timeZone = TimeZone.getTimeZone(timeZone)
    )

    fun revalidateReminder() {
        val selectedImportantDate = importantDateMillis
        val selectedReminder = currentReminderMillis()
        reminderError = if (selectedReminder != null && selectedImportantDate != null) {
            validateReminderWindow(
                reminderMillis = selectedReminder,
                nowMillis = nowMillis(),
                importantDateMillis = selectedImportantDate,
                timeZone = TimeZone.getTimeZone(timeZone)
            )
        } else {
            null
        }
    }

    fun currentDraft(): NewThingDraft? {
        val selectedImportantDate = importantDateMillis ?: return null
        val resolvedReminder = currentReminderMillis()
        return NewThingDraft(
            name = name,
            category = category.toThingCategory(),
            importantDate = importantDateToIso(selectedImportantDate, timeZone),
            reminderType = reminderChoice?.toReminderType(),
            reminderAtEpochMillis = resolvedReminder,
            reminderTimeZoneId = resolvedReminder?.let { timeZone },
            notes = notes,
            reminderExplicitlySelected = reminderExplicitlySelected
        )
    }

    val isDirty = if (initialThing == null) {
        name.isNotEmpty() || importantDateMillis != null || reminderChoice != null ||
            customReminderMillis != null || notes.isNotEmpty()
    } else {
        currentDraft()?.let(initialThing::wouldPersistChanges) ?: true
    }
    val requestBack = {
        if (isDirty) showDiscardDialog = true else onBack()
    }

    BackHandler(onBack = requestBack)

    fun chooseCustomReminder() {
        val selectedImportantDate = importantDateMillis ?: return
        val localTimeZone = TimeZone.getTimeZone(timeZone)
        val initial = Calendar.getInstance(localTimeZone).apply {
            customReminderMillis?.let { timeInMillis = it }
        }
        val dateDialog = DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        customReminderMillis = Calendar.getInstance(localTimeZone).apply {
                            clear()
                            set(year, month, day, hour, minute, 0)
                        }.timeInMillis
                        reminderChoice = ReminderChoice.CUSTOM
                        reminderExplicitlySelected = true
                        revalidateReminder()
                    },
                    initial.get(Calendar.HOUR_OF_DAY),
                    initial.get(Calendar.MINUTE),
                    false
                ).show()
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH)
        )
        dateDialog.datePicker.minDate = Calendar.getInstance(localTimeZone).apply {
            timeInMillis = nowMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        dateDialog.datePicker.maxDate = importantDateCutoffMillis(selectedImportantDate, localTimeZone)
        dateDialog.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        IconButton(
            onClick = requestBack,
            modifier = Modifier.semantics { contentDescription = "Back" }
        ) {
            BackArrowIcon()
        }
        Text(
            text = heading,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 22.dp)
                .then(
                    if (categoryEditable) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClick = { showCategoryChoices = true }
                        )
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(category)
            Spacer(Modifier.width(12.dp))
            Text(
                text = categoryDisplayName(category),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (categoryEditable) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = null
                revalidateReminder()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") },
            supportingText = nameError?.let { error -> ({ Text(error) }) },
            isError = nameError != null,
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = importantDateLabel(category),
            style = MaterialTheme.typography.labelLarge,
            color = if (importantDateError == null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        OutlinedButton(
            onClick = {
                val formTimeZone = TimeZone.getTimeZone(timeZone)
                val initial = Calendar.getInstance(formTimeZone).apply {
                    importantDateMillis?.let { timeInMillis = it }
                }
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        importantDateMillis = Calendar.getInstance(formTimeZone).apply {
                            clear()
                            set(year, month, day, 0, 0, 0)
                        }.timeInMillis
                        importantDateError = null
                        revalidateReminder()
                    },
                    initial.get(Calendar.YEAR),
                    initial.get(Calendar.MONTH),
                    initial.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text(importantDateMillis?.let(::formatDate) ?: "Choose a date")
        }
        importantDateError?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Reminder (optional)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        OutlinedButton(
            onClick = {
                revalidateReminder()
                showReminderChoices = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text(reminderChoice?.label ?: "No reminder")
        }
        if (reminderChoice == ReminderChoice.CUSTOM && customReminderMillis != null) {
            Text(
                text = formatDateTime(
                    checkNotNull(customReminderMillis),
                    TimeZone.getTimeZone(timeZone)
                ),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        reminderError?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                revalidateReminder()
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            label = { Text("Notes (optional)") },
            minLines = 3
        )
        submitError?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = {
                if (initialThing != null && !isDirty) {
                    onUnchangedSave()
                    return@Button
                }
                val submittedNameError = validateName(name)
                val submittedDateError = validateImportantDate(importantDateMillis)
                nameError = submittedNameError
                importantDateError = submittedDateError
                revalidateReminder()
                val submittedReminderError = reminderError
                val submittedImportantDate = importantDateMillis
                if (
                    submittedNameError == null &&
                    submittedDateError == null &&
                    submittedReminderError == null &&
                    submittedImportantDate != null
                ) {
                    currentDraft()?.let { onSubmit(it, isDirty) }
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .height(48.dp)
        ) {
            Text(primaryAction)
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showReminderChoices) {
        ReminderChoiceDialog(
            selected = reminderChoice,
            importantDateMillis = importantDateMillis,
            nowMillis = nowMillis(),
            timeZone = TimeZone.getTimeZone(timeZone),
            onDismiss = { showReminderChoices = false },
            onSelected = { choice ->
                showReminderChoices = false
                timeZone = TimeZone.getDefault().id
                if (choice == ReminderChoice.CUSTOM) {
                    chooseCustomReminder()
                } else {
                    reminderChoice = choice
                    reminderExplicitlySelected = true
                    customReminderMillis = null
                    revalidateReminder()
                }
            },
            onNoReminder = {
                showReminderChoices = false
                reminderChoice = null
                reminderExplicitlySelected = true
                customReminderMillis = null
                reminderError = null
            }
        )
    }


    if (showCategoryChoices) {
        CategoryChoiceDialog(
            selected = category,
            onDismiss = { showCategoryChoices = false },
            onSelected = { selected ->
                category = selected
                showCategoryChoices = false
                revalidateReminder()
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your changes haven't been saved.") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    }
                ) {
                    Text("Discard")
                }
            }
        )
    }
}

@Composable
private fun ReminderChoiceDialog(
    selected: ReminderChoice?,
    importantDateMillis: Long?,
    nowMillis: Long,
    timeZone: TimeZone,
    onDismiss: () -> Unit,
    onSelected: (ReminderChoice) -> Unit,
    onNoReminder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remind me") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ReminderDialogRow(
                    label = "No reminder",
                    selected = selected == null,
                    onClick = onNoReminder
                )
                ReminderChoice.entries.forEach { choice ->
                    val enabled = when (choice) {
                        ReminderChoice.CUSTOM -> importantDateMillis != null &&
                            importantDateCutoffMillis(importantDateMillis, timeZone) > nowMillis
                        else -> importantDateMillis != null && isPresetReminderAvailable(
                            importantDateMillis = importantDateMillis,
                            choice = choice,
                            nowMillis = nowMillis,
                            timeZone = timeZone
                        )
                    }
                    ReminderDialogRow(
                        label = choice.label,
                        selected = selected == choice,
                        enabled = enabled
                    ) { onSelected(choice) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReminderDialogRow(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (selected) "✓  $label" else label,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    selected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun CategoryChoiceDialog(
    selected: CategoryGlyph,
    onDismiss: () -> Unit,
    onSelected: (CategoryGlyph) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a category") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CategoryGlyph.entries.forEach { category ->
                    TextButton(
                        onClick = { onSelected(category) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIcon(category)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = categoryDisplayName(category),
                                modifier = Modifier.weight(1f),
                                color = if (selected == category) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (selected == category) {
                                Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun categoryDisplayName(category: CategoryGlyph): String = when (category) {
    CategoryGlyph.DOCUMENT -> "Document"
    CategoryGlyph.OWNED -> "Something I own"
    CategoryGlyph.PAYMENT -> "Subscription/payment"
    CategoryGlyph.EXCHANGE -> "Lent/borrowed"
    CategoryGlyph.MONEY -> "Money owed"
    CategoryGlyph.VEHICLE -> "Vehicle"
    CategoryGlyph.HOME -> "Home/appliance"
    CategoryGlyph.MEDICINE -> "Medicine"
    CategoryGlyph.OTHER -> "Something else"
}

private fun importantDateLabel(category: CategoryGlyph): String = when (category) {
    CategoryGlyph.DOCUMENT, CategoryGlyph.MEDICINE -> "Expiration date"
    CategoryGlyph.PAYMENT -> "Renewal/payment date"
    CategoryGlyph.EXCHANGE -> "Return date"
    CategoryGlyph.MONEY -> "Due date"
    else -> "Important date"
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

private fun formatDateTime(millis: Long, timeZone: TimeZone): String =
    SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).apply {
        this.timeZone = timeZone
    }.format(Date(millis))
