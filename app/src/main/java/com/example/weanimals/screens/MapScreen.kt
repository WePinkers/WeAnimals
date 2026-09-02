package com.example.weanimals.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weanimals.theme.*

enum class UrgencyLevel(
    val label: String,
    val color: Color
) {
    EMERGENCY("Emergência", Alert600),
    VERY_URGENT("Muito urgente", Clay600),
    URGENT("Urgente", Brass500),
    LOW_URGENCY("Pouco urgente", Pine700)
}

enum class OccurrenceStatus(
    val label: String,
    val badgeBg: Color,
    val badgeText: Color
) {
    NEW("Novo", Alert100, Alert600),
    IN_PROGRESS("Em curso", Brass100, Color(0xFF8A6413))
}

data class Occurrence(
    val id: String,
    val title: String,
    val location: String,
    val distance: String,
    val statusText: String,
    val urgency: UrgencyLevel,
    val status: OccurrenceStatus,
    val icon: ImageVector,
    val posXPercent: Float,
    val posYPercent: Float
)

val sampleOccurrences = listOf(
    Occurrence(
        id = "1",
        title = "Filhote",
        location = "Rua Aimberê",
        distance = "420 m",
        statusText = "protocolo #4471",
        urgency = UrgencyLevel.EMERGENCY,
        status = OccurrenceStatus.NEW,
        icon = Icons.Outlined.FavoriteBorder,
        posXPercent = 0.26f,
        posYPercent = 0.16f
    ),
    Occurrence(
        id = "2",
        title = "Cão idoso",
        location = "Vila Marlene",
        distance = "1,1 km",
        statusText = "equipe a caminho",
        urgency = UrgencyLevel.URGENT,
        status = OccurrenceStatus.IN_PROGRESS,
        icon = Icons.Outlined.Pets,
        posXPercent = 0.67f,
        posYPercent = 0.20f
    ),
    Occurrence(
        id = "3",
        title = "Gato preso em terreno",
        location = "Rua Voluntários",
        distance = "850 m",
        statusText = "protocolo #4473",
        urgency = UrgencyLevel.VERY_URGENT,
        status = OccurrenceStatus.NEW,
        icon = Icons.Outlined.Pets,
        posXPercent = 0.75f,
        posYPercent = 0.72f
    ),
    Occurrence(
        id = "4",
        title = "Cão comunitário",
        location = "Praça Central",
        distance = "1,8 km",
        statusText = "em acompanhamento",
        urgency = UrgencyLevel.LOW_URGENCY,
        status = OccurrenceStatus.IN_PROGRESS,
        icon = Icons.Outlined.Pets,
        posXPercent = 0.34f,
        posYPercent = 0.62f
    )
)

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    onOccurrenceClick: (Occurrence) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("Todas · 9") }
    var selectedOccurrenceId by remember { mutableStateOf<String?>(null) }

    val filterOptions = listOf("Todas · 9", "Urgente", "Em curso")

    val filteredOccurrences = remember(selectedFilter) {
        when (selectedFilter) {
            "Urgente" -> sampleOccurrences.filter {
                it.urgency == UrgencyLevel.URGENT ||
                        it.urgency == UrgencyLevel.EMERGENCY ||
                        it.urgency == UrgencyLevel.VERY_URGENT
            }
            "Em curso" -> sampleOccurrences.filter { it.status == OccurrenceStatus.IN_PROGRESS }
            else -> sampleOccurrences
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFBF8F2))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFEFEADC))
        ) {
            val canvasWidth = maxWidth
            val canvasHeight = maxHeight

            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 22.dp.toPx()
                val gridColor = Color(0x181F3D2E)

                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += gridSpacing
                }

                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += gridSpacing
                }
            }

            Box(
                modifier = Modifier
                    .offset(x = canvasWidth * 0.50f - 11.dp, y = canvasHeight * 0.42f - 11.dp)
                    .size(22.dp)
                    .background(Pine800.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .background(Pine800, CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            filteredOccurrences.forEach { occurrence ->
                val isSelected = selectedOccurrenceId == occurrence.id
                val pinSize by animateDpAsState(if (isSelected) 20.dp else 14.dp, label = "pinSize")

                Box(
                    modifier = Modifier
                        .offset(
                            x = canvasWidth * occurrence.posXPercent - (pinSize / 2),
                            y = canvasHeight * occurrence.posYPercent - (pinSize / 2)
                        )
                        .shadow(4.dp, shape = CircleShape)
                        .size(pinSize)
                        .rotate(-45f)
                        .background(
                            color = occurrence.urgency.color,
                            shape = RoundedCornerShape(
                                topStart = 10.dp,
                                topEnd = 10.dp,
                                bottomStart = 10.dp,
                                bottomEnd = 1.dp
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(
                                topStart = 10.dp,
                                topEnd = 10.dp,
                                bottomStart = 10.dp,
                                bottomEnd = 1.dp
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedOccurrenceId = if (isSelected) null else occurrence.id
                        }
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        onClick = { selectedFilter = filter },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Pine800 else Color.White,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Ink900,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.15f)
                .offset(y = (-18).dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(LineBorder)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "9 ocorrências ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink900
                    )
                    Text(
                        text = "em um raio de 2 km",
                        fontSize = 13.sp,
                        color = Ink600
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendBullet(label = "Emergência", color = Alert600)
                        LegendBullet(label = "Muito urgente", color = Clay600)
                        LegendBullet(label = "Urgente", color = Brass500)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LegendBullet(label = "Pouco urgente", color = Pine700)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredOccurrences) { occurrence ->
                        val isCardSelected = selectedOccurrenceId == occurrence.id
                        OccurrenceCard(
                            occurrence = occurrence,
                            isSelected = isCardSelected,
                            onClick = {
                                selectedOccurrenceId = if (isCardSelected) null else occurrence.id
                                onOccurrenceClick(occurrence)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendBullet(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink900
        )
    }
}

@Composable
fun OccurrenceCard(
    occurrence: Occurrence,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) Pine800 else LineBorder,
        label = "cardBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Sand50 else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                    .background(occurrence.urgency.color)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Sand100)
                            .border(1.dp, LineBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = occurrence.icon,
                            contentDescription = null,
                            tint = Clay600,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${occurrence.title} — ${occurrence.location}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink900,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${occurrence.distance} · ${occurrence.statusText}",
                            fontSize = 10.5.sp,
                            color = Ink600
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(occurrence.status.badgeBg)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = occurrence.status.label,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = occurrence.status.badgeText
                    )
                }
            }
        }
    }
}