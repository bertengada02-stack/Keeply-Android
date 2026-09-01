package com.keeply.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keeply.app.model.ThingStatus
import java.util.Calendar
import java.util.TimeZone

@Composable
internal fun ItemDetailsScreen(
    state: ItemDetailsState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRemindAgain: (Long, String) -> Unit,
    onMarkDone: () -> Unit,
    onReopen: () -> Unit,
    onDelete: () -> Unit,
    isChangingLifecycle: Boolean,
    lifecycleError: String?,
    modifier: Modifier = Modifier,
    currentLocalDate: String = currentLocalDateIso()
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Back" }
            ) {
                BackArrowIcon()
            }
            Spacer(Modifier.weight(1f))
            if (state is ItemDetailsState.Content) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
            }
        }
        when (state) {
            ItemDetailsState.NotSelected,
            ItemDetailsState.Loading -> LoadingDetails()

            is ItemDetailsState.Content -> ItemDetailsContent(
                details = state.thing.toItemDetailsUiModel(currentLocalDate = currentLocalDate),
                status = state.thing.status,
                importantDateMillis = isoImportantDateToMillis(
                    state.thing.importantDate,
                    TimeZone.getDefault().id
                ),
                onEdit = onEdit,
                onRemindAgain = onRemindAgain,
                onMarkDone = onMarkDone,
                onReopen = onReopen,
                onDelete = onDelete,
                isChangingLifecycle = isChangingLifecycle,
                lifecycleError = lifecycleError
            )
            ItemDetailsState.NotFound -> UnavailableDetails(
                message = "This thing is no longer available.",
                onBack = onBack
            )
            ItemDetailsState.Error -> UnavailableDetails(
                message = "Keeply couldn't open this thing.",
                onBack = onBack
            )
        }
    }
}

@Composable
private fun LoadingDetails() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(
            text = "Loading…",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ItemDetailsContent(
    details: ItemDetailsUiModel,
    status: ThingStatus,
    importantDateMillis: Long?,
    onEdit: () -> Unit,
    onRemindAgain: (Long, String) -> Unit,
    onMarkDone: () -> Unit,
    onReopen: () -> Unit,
    onDelete: () -> Unit,
    isChangingLifecycle: Boolean,
    lifecycleError: String?
) {
    var showReminderChoices by remember { mutableStateOf(false) }
    var showDoneConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(details.category.toCategoryGlyph())
        Spacer(Modifier.width(12.dp))
        Text(
            text = details.categoryLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Text(
        text = details.name,
        modifier = Modifier.padding(top = 18.dp),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    details.importantDateContext?.let {
        UrgencyContextRow(it, Modifier.padding(top = 10.dp))
    }
    Spacer(Modifier.height(28.dp))
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DetailsValue("Status", details.statusText)
        DetailsValue(details.importantDateLabel, details.importantDateText)
        DetailsValue("Reminder", details.reminderText)
        details.notes?.let { DetailsValue("Notes", it) }
    }
    Spacer(Modifier.height(30.dp))
    if (status != ThingStatus.DONE) {
        Text("Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Button(
            onClick = { showReminderChoices = true },
            enabled = !isChangingLifecycle,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) { Text("Remind me again") }
        TextButton(
            onClick = { showDoneConfirmation = true },
            enabled = !isChangingLifecycle,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Mark as done") }
    } else {
        Button(
            onClick = onReopen,
            enabled = !isChangingLifecycle,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reopen") }
    }
    lifecycleError?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    }
    HorizontalDivider(modifier = Modifier.padding(top = 20.dp))
    TextButton(
        onClick = { showDeleteConfirmation = true },
        enabled = !isChangingLifecycle,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }

    if (showReminderChoices) {
        RemindAgainDialog(
            importantDateMillis = importantDateMillis,
            onDismiss = { showReminderChoices = false },
            onEdit = {
                showReminderChoices = false
                onEdit()
            },
            onSelected = { millis, zone ->
                showReminderChoices = false
                onRemindAgain(millis, zone)
            }
        )
    }
    if (showDoneConfirmation) {
        AlertDialog(
            onDismissRequest = { showDoneConfirmation = false },
            title = { Text("Mark as done?") },
            text = { Text("This will complete this Thing.") },
            confirmButton = { TextButton(onClick = { showDoneConfirmation = false; onMarkDone() }) { Text("Mark as done") } },
            dismissButton = { TextButton(onClick = { showDoneConfirmation = false }) { Text("Keep") } }
        )
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete this thing?") },
            text = { Text("This will permanently remove it from Keeply.") },
            confirmButton = { TextButton(onClick = { showDeleteConfirmation = false; onDelete() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Keep") } }
        )
    }
}

@Composable
private fun RemindAgainDialog(
    importantDateMillis: Long?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onSelected: (Long, String) -> Unit
) {
    val context = LocalContext.current
    val timeZone = remember { TimeZone.getDefault() }
    val nowMillis = System.currentTimeMillis()
    val availableChoices = availableReminderChoices(importantDateMillis, nowMillis, timeZone)
    fun selectPreset(choice: ReminderChoice) {
        val selectedImportantDate = importantDateMillis ?: return
        onSelected(presetReminderMillis(selectedImportantDate, choice, timeZone), timeZone.id)
    }
    fun selectCustom() {
        val initial = Calendar.getInstance(timeZone).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val dateDialog = DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                val selected = Calendar.getInstance(timeZone).apply {
                    set(year, month, day, hour, minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (importantDateMillis != null && validateReminderWindow(
                        reminderMillis = selected.timeInMillis,
                        nowMillis = System.currentTimeMillis(),
                        importantDateMillis = importantDateMillis,
                        timeZone = timeZone
                    ) == null
                ) {
                    onSelected(selected.timeInMillis, timeZone.id)
                }
            }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false).show()
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH))
        dateDialog.datePicker.minDate = Calendar.getInstance(timeZone).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        importantDateMillis?.let {
            dateDialog.datePicker.maxDate = importantDateCutoffMillis(it, timeZone)
        }
        dateDialog.show()
    }
    if (availableChoices.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Update the Important Date first") },
            text = {
                Text(
                    "This Thing’s Important Date has already passed. " +
                        "Set a new future Important Date before scheduling another reminder."
                )
            },
            confirmButton = { TextButton(onClick = onEdit) { Text("Edit Thing") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Remind me again") },
            text = {
                Column {
                    ReminderChoice.entries.filter { it in availableChoices }.forEach { choice ->
                        TextButton(
                            onClick = {
                                if (choice == ReminderChoice.CUSTOM) {
                                    onDismiss()
                                    selectCustom()
                                } else {
                                    selectPreset(choice)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(choice.label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
        )
    }
}

@Composable
private fun DetailsValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 5.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun UnavailableDetails(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Back to My Things")
        }
    }
}
