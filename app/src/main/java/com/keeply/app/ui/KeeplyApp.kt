package com.keeply.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keeply.app.ui.theme.KeeplyTheme
import kotlinx.coroutines.delay

private enum class AppDestination {
    HOME,
    MY_THINGS
}

@Composable
fun KeeplyApp() {
    var showStartup by remember { mutableStateOf(true) }
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }

    LaunchedEffect(Unit) {
        delay(900)
        showStartup = false
    }

    if (showStartup) {
        KeeplyStartupScreen()
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            KeeplyNavigationBar(
                destination = destination,
                onDestinationSelected = { destination = it }
            )
        }
    ) { innerPadding ->
        when (destination) {
            AppDestination.HOME -> EmptyHomeScreen(
                onRememberSomething = {
                    // Create flow is intentionally deferred to Milestone 2.
                },
                modifier = Modifier.padding(innerPadding)
            )

            AppDestination.MY_THINGS -> MyThingsShell(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun KeeplyNavigationBar(
    destination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .height(72.dp)
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
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
                    onClick = { /* Create flow is intentionally deferred to Milestone 2. */ },
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
        EmptyHeroIllustration()
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
        ExampleThings()
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
        Text(
            text = "Keeply",
            modifier = Modifier.weight(1f),
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
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
private fun EmptyHeroIllustration() {
    val teal = MaterialTheme.colorScheme.primary
    val mint = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .semantics { contentDescription = "A calm illustration of Keeply remembering important dates" }
    ) {
        drawOval(
            color = mint.copy(alpha = 0.50f),
            topLeft = Offset(size.width * 0.17f, size.height * 0.79f),
            size = Size(size.width * 0.66f, size.height * 0.12f)
        )
        // Foliage is anchored to the calendar base and painted behind all foreground objects.
        drawBotanicalStem(
            Offset(size.width * .25f, size.height * .84f),
            Offset(size.width * .15f, size.height * .18f),
            scale = .45f,
            bendX = -42.dp.toPx()
        )
        drawBotanicalStem(
            Offset(size.width * .75f, size.height * .84f),
            Offset(size.width * .85f, size.height * .17f),
            scale = .43f,
            bendX = 42.dp.toPx()
        )
        drawRoundRect(
            color = surface,
            topLeft = Offset(size.width * 0.24f, size.height * 0.04f),
            size = Size(size.width * 0.52f, size.height * 0.80f),
            cornerRadius = CornerRadius(12.dp.toPx())
        )
        drawRoundRect(
            color = outline.copy(alpha = 0.45f),
            topLeft = Offset(size.width * 0.24f, size.height * 0.04f),
            size = Size(size.width * 0.52f, size.height * 0.80f),
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(1.5.dp.toPx())
        )
        drawRoundRect(
            color = teal,
            topLeft = Offset(size.width * 0.24f, size.height * 0.04f),
            size = Size(size.width * 0.52f, size.height * 0.19f),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawLine(outline, Offset(size.width * 0.37f, 0f), Offset(size.width * 0.37f, size.height * 0.14f), 4.dp.toPx(), StrokeCap.Round)
        drawLine(outline, Offset(size.width * 0.63f, 0f), Offset(size.width * 0.63f, size.height * 0.14f), 4.dp.toPx(), StrokeCap.Round)
        repeat(3) { row ->
            repeat(4) { column ->
                if (!(row == 0 && column == 3)) {
                    drawRoundRect(
                        color = mint.copy(alpha = .58f),
                        topLeft = Offset(
                            size.width * (.31f + column * .105f),
                            size.height * (.31f + row * .17f)
                        ),
                        size = Size(17.dp.toPx(), 17.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
            }
        }
        val checkCenter = Offset(size.width * .66f, size.height * .36f)
        drawCircle(teal, 11.dp.toPx(), checkCenter)
        drawLine(surface, Offset(checkCenter.x - 5.dp.toPx(), checkCenter.y), Offset(checkCenter.x - 1.dp.toPx(), checkCenter.y + 4.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        drawLine(surface, Offset(checkCenter.x - 1.dp.toPx(), checkCenter.y + 4.dp.toPx()), Offset(checkCenter.x + 6.dp.toPx(), checkCenter.y - 5.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        val heroBell = Path().apply {
            moveTo(size.width * .20f, size.height * .82f)
            quadraticTo(size.width * .23f, size.height * .73f, size.width * .23f, size.height * .64f)
            quadraticTo(size.width * .23f, size.height * .52f, size.width * .28f, size.height * .52f)
            quadraticTo(size.width * .33f, size.height * .52f, size.width * .33f, size.height * .64f)
            quadraticTo(size.width * .33f, size.height * .73f, size.width * .36f, size.height * .82f)
            close()
        }
        drawPath(heroBell, teal)
        drawRoundRect(
            color = teal,
            topLeft = Offset(size.width * .19f, size.height * .79f),
            size = Size(size.width * .18f, 8.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        drawCircle(teal, 5.dp.toPx(), Offset(size.width * .28f, size.height * .865f))
        val clockCenter = Offset(size.width * .72f, size.height * .73f)
        drawCircle(surface, 27.dp.toPx(), clockCenter)
        drawCircle(Color(0xFF82BDB4), 27.dp.toPx(), clockCenter, style = Stroke(3.dp.toPx()))
        repeat(4) { index ->
            val angle = Math.toRadians(index * 90.0)
            drawCircle(
                color = Color(0xFFB9DDD7),
                radius = 1.5.dp.toPx(),
                center = Offset(
                    clockCenter.x + kotlin.math.cos(angle).toFloat() * 20.dp.toPx(),
                    clockCenter.y + kotlin.math.sin(angle).toFloat() * 20.dp.toPx()
                )
            )
        }
        drawLine(teal, clockCenter, Offset(clockCenter.x, clockCenter.y - 13.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
        drawLine(teal, clockCenter, Offset(clockCenter.x + 9.dp.toPx(), clockCenter.y + 7.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
        drawCircle(teal, 2.5.dp.toPx(), clockCenter)
    }
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
            text = "Keeply will notify you on time\nso you'll never miss what matters.",
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

private enum class CategoryGlyph {
    DOCUMENT, OWNED, PAYMENT, EXCHANGE, MONEY, VEHICLE, HOME, MEDICINE
}

@Composable
private fun ExampleThings() {
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
                    ExampleCard(example, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ExampleCard(example: ExampleThing, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(116.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(13.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
                RoundedCornerShape(14.dp)
            )
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
    val teal = MaterialTheme.colorScheme.primary
    val mint = MaterialTheme.colorScheme.primaryContainer
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = "An empty clipboard with calm foliage" }
    ) {
        drawOval(mint.copy(alpha = .55f), Offset(size.width * .24f, size.height * .73f), Size(size.width * .52f, size.height * .15f))
        drawBotanicalStem(
            Offset(size.width * .35f, size.height * .79f),
            Offset(size.width * .22f, size.height * .32f),
            scale = .52f,
            bendX = -45.dp.toPx()
        )
        drawBotanicalStem(
            Offset(size.width * .65f, size.height * .79f),
            Offset(size.width * .78f, size.height * .33f),
            scale = .52f,
            bendX = 45.dp.toPx()
        )
        drawRoundRect(
            color = Color(0xFFF0F8F6),
            topLeft = Offset(size.width * .36f, size.height * .20f),
            size = Size(size.width * .28f, size.height * .58f),
            cornerRadius = CornerRadius(12.dp.toPx())
        )
        drawRoundRect(
            color = teal,
            topLeft = Offset(size.width * .36f, size.height * .20f),
            size = Size(size.width * .28f, size.height * .58f),
            cornerRadius = CornerRadius(12.dp.toPx()),
            style = Stroke(3.dp.toPx())
        )
        drawRoundRect(
            color = teal,
            topLeft = Offset(size.width * .44f, size.height * .14f),
            size = Size(size.width * .12f, size.height * .12f),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
        repeat(4) { index ->
            val y = size.height * (.38f + index * .10f)
            drawCircle(Color(0xFF9CCFC6), 4.dp.toPx(), Offset(size.width * .43f, y))
            drawLine(Color(0xFF9CCFC6), Offset(size.width * .48f, y), Offset(size.width * .58f, y), 5.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun MyThingsShell(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeeplyHeader()
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
private fun KeeplyStartupScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val mint = MaterialTheme.colorScheme.primaryContainer
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFFF2F8F7))
            val hillBack = Path().apply {
                moveTo(0f, size.height * .91f)
                quadraticTo(size.width * .17f, size.height * .85f, size.width * .41f, size.height * .93f)
                quadraticTo(size.width * .69f, size.height * .84f, size.width, size.height * .89f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(hillBack, Color(0xFFB9DDD7).copy(alpha = .34f))
            val hillFront = Path().apply {
                moveTo(0f, size.height * .87f)
                quadraticTo(size.width * .15f, size.height * .82f, size.width * .37f, size.height * .95f)
                quadraticTo(size.width * .48f, size.height * .98f, size.width * .61f, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(hillFront, Color(0xFF9CCFC6).copy(alpha = .22f))
            val hillRight = Path().apply {
                moveTo(size.width * .34f, size.height)
                quadraticTo(size.width * .67f, size.height * .86f, size.width, size.height * .92f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(hillRight, Color(0xFF83BFB5).copy(alpha = .17f))
            drawBotanicalStem(Offset(size.width * .08f, size.height), Offset(size.width * .12f, size.height * .60f), 1.25f)
            drawBotanicalStem(Offset(size.width * .26f, size.height), Offset(size.width * .22f, size.height * .78f), .78f)
            drawBotanicalStem(Offset(size.width * .72f, size.height), Offset(size.width * .78f, size.height * .61f), 1.18f)
            drawBotanicalStem(Offset(size.width * .90f, size.height), Offset(size.width * .87f, size.height * .76f), .80f)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KeeplyMark(markSize = 136.dp, strokeWidth = 5.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Keeply",
                fontSize = 58.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "One place to remember\nthe things you don't\nwant to forget.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CategoryIcon(glyph: CategoryGlyph) {
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
        }
    }
}

private val MaterialThemeColorFallback = Color(0xFFD8EFEC)

private fun DrawScope.drawBotanicalStem(
    base: Offset,
    tip: Offset,
    scale: Float = 1f,
    bendX: Float = 0f
) {
    val stemColor = Color(0xFF75B9AE).copy(alpha = .40f)
    val leafColor = Color(0xFF8CC8BE).copy(alpha = .27f)
    val stem = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo((base.x + tip.x) * .5f + bendX, (base.y + tip.y) * .5f, tip.x, tip.y)
    }
    drawPath(stem, stemColor, style = Stroke(2.dp.toPx() * scale, cap = StrokeCap.Round))
    listOf(.25f, .44f, .62f, .79f).forEachIndexed { index, fraction ->
        val inverse = 1f - fraction
        val controlX = (base.x + tip.x) * .5f + bendX
        val controlY = (base.y + tip.y) * .5f
        val x = inverse * inverse * base.x + 2f * inverse * fraction * controlX + fraction * fraction * tip.x
        val y = inverse * inverse * base.y + 2f * inverse * fraction * controlY + fraction * fraction * tip.y
        val direction = if (index % 2 == 0) -1f else 1f
        val leafWidth = 25.dp.toPx() * scale
        val leafHeight = 35.dp.toPx() * scale
        val start = Offset(x, y)
        val end = Offset(x + direction * leafWidth, y - leafHeight)
        val leaf = Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(x + direction * leafWidth * .92f, y - leafHeight * .20f, end.x, end.y)
            quadraticTo(x + direction * leafWidth * .05f, y - leafHeight * .72f, start.x, start.y)
            close()
        }
        drawPath(leaf, leafColor)
        val oppositeEnd = Offset(x - direction * leafWidth * .72f, y - leafHeight * .74f)
        val oppositeLeaf = Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(x - direction * leafWidth * .70f, y - leafHeight * .08f, oppositeEnd.x, oppositeEnd.y)
            quadraticTo(x - direction * leafWidth * .04f, y - leafHeight * .58f, start.x, start.y)
            close()
        }
        drawPath(oppositeLeaf, leafColor.copy(alpha = .82f))
    }
}

@Composable
private fun MiniBellIcon() {
    val teal = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(24.dp).semantics { contentDescription = "Reminder" }) {
        val bell = Path().apply {
            moveTo(size.width * .20f, size.height * .70f)
            quadraticTo(size.width * .30f, size.height * .58f, size.width * .30f, size.height * .38f)
            quadraticTo(size.width * .30f, size.height * .16f, size.width * .50f, size.height * .16f)
            quadraticTo(size.width * .70f, size.height * .16f, size.width * .70f, size.height * .38f)
            quadraticTo(size.width * .70f, size.height * .58f, size.width * .80f, size.height * .70f)
            close()
        }
        drawPath(bell, teal)
        drawCircle(teal, 2.5.dp.toPx(), Offset(size.width * .50f, size.height * .82f))
    }
}

@Composable
private fun KeeplyMark(
    markSize: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp
) {
    val teal = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .size(markSize)
            .semantics { contentDescription = "Keeply bell logo" }
    ) {
        val stroke = strokeWidth.toPx()
        drawCircle(color = teal, style = Stroke(width = stroke))
        val bell = Path().apply {
            moveTo(size.width * 0.35f, size.height * 0.59f)
            quadraticTo(
                size.width * 0.40f,
                size.height * 0.52f,
                size.width * 0.40f,
                size.height * 0.40f
            )
            quadraticTo(
                size.width * 0.40f,
                size.height * 0.27f,
                size.width * 0.50f,
                size.height * 0.27f
            )
            quadraticTo(
                size.width * 0.60f,
                size.height * 0.27f,
                size.width * 0.60f,
                size.height * 0.40f
            )
            quadraticTo(
                size.width * 0.60f,
                size.height * 0.52f,
                size.width * 0.65f,
                size.height * 0.59f
            )
            close()
        }
        drawPath(path = bell, color = teal, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawLine(
            color = teal,
            start = Offset(size.width * 0.46f, size.height * 0.66f),
            end = Offset(size.width * 0.54f, size.height * 0.66f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
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

@Preview(showBackground = true)
@Composable
private fun KeeplyAppPreview() {
    KeeplyTheme {
        KeeplyApp()
    }
}
