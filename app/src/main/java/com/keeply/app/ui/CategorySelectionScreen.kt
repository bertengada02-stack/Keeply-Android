package com.keeply.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class CategoryChoice(
    val label: String,
    val example: String,
    val glyph: CategoryGlyph
)

private val categoryChoices = listOf(
    CategoryChoice("Document", "Passport, license", CategoryGlyph.DOCUMENT),
    CategoryChoice("Something I own", "Warranty, receipt", CategoryGlyph.OWNED),
    CategoryChoice("Subscription/payment", "Bills, subscriptions", CategoryGlyph.PAYMENT),
    CategoryChoice("Lent/borrowed", "Something you lent or borrowed", CategoryGlyph.EXCHANGE),
    CategoryChoice("Money owed", "Money you owe or are owed", CategoryGlyph.MONEY),
    CategoryChoice("Vehicle", "Registration, insurance", CategoryGlyph.VEHICLE),
    CategoryChoice("Home/appliance", "Maintenance, warranty", CategoryGlyph.HOME),
    CategoryChoice("Medicine", "Expiration date", CategoryGlyph.MEDICINE),
    CategoryChoice("Something else", "Anything important", CategoryGlyph.OTHER)
)

@Composable
internal fun CategorySelectionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by rememberSaveable { mutableStateOf<CategoryGlyph?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.semantics { contentDescription = "Back" }
        ) {
            BackArrowIcon()
        }
        Text(
            text = "What do you want to remember?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Choose a category to get started.",
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            categoryChoices.forEach { category ->
                CategorySelectionRow(
                    category = category,
                    selected = selectedCategory == category.glyph,
                    onClick = { selectedCategory = category.glyph }
                )
            }
        }
    }
}

@Composable
private fun CategorySelectionRow(
    category: CategoryChoice,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val border = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(background, shape)
            .border(1.dp, border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(category.glyph)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = category.example,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        if (selected) {
            SelectedCheckIcon()
        } else {
            ChevronIcon()
        }
    }
}

@Composable
private fun BackArrowIcon() {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(24.dp)) {
        val stroke = 2.5.dp.toPx()
        drawLine(color, Offset(size.width * .78f, size.height * .50f), Offset(size.width * .24f, size.height * .50f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * .24f, size.height * .50f), Offset(size.width * .48f, size.height * .26f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * .24f, size.height * .50f), Offset(size.width * .48f, size.height * .74f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun ChevronIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    Canvas(Modifier.size(20.dp)) {
        val chevron = Path().apply {
            moveTo(size.width * .38f, size.height * .24f)
            lineTo(size.width * .64f, size.height * .50f)
            lineTo(size.width * .38f, size.height * .76f)
        }
        drawPath(chevron, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun SelectedCheckIcon() {
    val teal = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(teal, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(14.dp)) {
            drawLine(Color.White, Offset(size.width * .16f, size.height * .52f), Offset(size.width * .42f, size.height * .76f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(Color.White, Offset(size.width * .42f, size.height * .76f), Offset(size.width * .86f, size.height * .26f), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}
