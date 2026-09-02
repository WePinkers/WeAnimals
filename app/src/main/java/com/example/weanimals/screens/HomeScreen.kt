package com.example.weanimals.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weanimals.theme.Alert100
import com.example.weanimals.theme.Alert600
import com.example.weanimals.theme.Brass100
import com.example.weanimals.theme.Brass500
import com.example.weanimals.theme.Clay600
import com.example.weanimals.theme.Ink600
import com.example.weanimals.theme.Ink900
import com.example.weanimals.theme.LineBorder
import com.example.weanimals.theme.Pine700
import com.example.weanimals.theme.Pine800
import com.example.weanimals.theme.Pine900
import com.example.weanimals.theme.Sand100
import com.example.weanimals.theme.Sand50

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToMap: () -> Unit = {},
    onReportClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Sand50)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        // Greeting
        Text(
            text = "BOA TARDE, RENATA",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Ink600,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "O que você viu hoje?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Pine900
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Report CTA Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Clay600)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "EMERGÊNCIA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Encontrou um animal em risco?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onReportClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Clay600
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Denunciar agora",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section Title: Perto de você
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Perto de você",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Ink900
            )
            Text(
                text = "ver mapa",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Pine700,
                modifier = Modifier.clickable { onNavigateToMap() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mini Map Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clickable { onNavigateToMap() },
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEADC))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spacing = 20.dp.toPx()
                    val gridColor = Color(0x101F3D2E)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                        x += spacing
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                        y += spacing
                    }
                }

                // Sample pins on mini map
                Box(
                    modifier = Modifier
                        .offset { IntOffset(80, 50) }
                        .size(10.dp)
                        .background(Alert600, CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(220, 100) }
                        .size(10.dp)
                        .background(Brass500, CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(380, 60) }
                        .size(10.dp)
                        .background(Pine700, CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                )

                // Badge at bottom left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "6 ocorrências em 2km",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ink600
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section Title: Suas denúncias
        Text(
            text = "Suas denúncias",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Ink900
        )

        Spacer(modifier = Modifier.height(10.dp))

        // User reports list
        HomeReportCard(
            title = "Filhote — Rua Aimberê",
            subtitle = "Protocolo #4471",
            badgeText = "Novo",
            badgeBg = Alert100,
            badgeTextColor = Alert600,
            indicatorColor = Alert600
        )

        Spacer(modifier = Modifier.height(10.dp))

        HomeReportCard(
            title = "Cão idoso — Vila Marlene",
            subtitle = "Equipe a caminho",
            badgeText = "Em curso",
            badgeBg = Brass100,
            badgeTextColor = Color(0xFF8A6413),
            indicatorColor = Brass500
        )
    }
}

@Composable
fun HomeReportCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeBg: Color,
    badgeTextColor: Color,
    indicatorColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(5.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                    .background(indicatorColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Sand100)
                            .border(1.dp, LineBorder, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = Clay600,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(11.dp))

                    Column {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink900
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = Ink600
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }
        }
    }
}
