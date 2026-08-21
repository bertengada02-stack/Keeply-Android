package com.keeply.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ItemDetailsScreen(
    state: ItemDetailsState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
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

            is ItemDetailsState.Content -> ItemDetailsContent(state.thing.toItemDetailsUiModel())
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
private fun ItemDetailsContent(details: ItemDetailsUiModel) {
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
        modifier = Modifier.padding(top = 18.dp, bottom = 28.dp),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DetailsValue(details.importantDateLabel, details.importantDateText)
        DetailsValue("Reminder", details.reminderText)
        details.notes?.let { DetailsValue("Notes", it) }
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
