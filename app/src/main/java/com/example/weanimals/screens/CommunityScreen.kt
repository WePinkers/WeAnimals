package com.example.weanimals.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun CommunityScreen(
    modifier: Modifier = Modifier
) {
    var selectedChip by remember { mutableStateOf("Tudo") }
    val chips = listOf("Tudo", "Campanhas", "Posts", "Achados", "Castração", "Vacinação")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Sand50)
            .padding(top = 18.dp)
    ) {
        // Title
        Text(
            text = "Comunidade",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Pine900,
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Chip Filter Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp)
        ) {
            items(chips) { chip ->
                val isSel = selectedChip == chip
                Surface(
                    onClick = { selectedChip = chip },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSel) Pine800 else Color.White,
                    border = if (isSel) null else androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                ) {
                    Text(
                        text = chip,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSel) Color.White else Ink600,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feed Items
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Campaign Card 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Sand100),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "CASTRAÇÃO · ABRIGO ESPERANÇA",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Clay600,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Castração gratuita — mutirão de sábado",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Pine900
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Ink600, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Sáb, 6 set · 8h às 13h", fontSize = 10.sp, color = Ink600)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Ink600, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Vila Marlene · 2,3 km", fontSize = 10.sp, color = Ink600)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                            ) {
                                Text(
                                    text = "32 vagas restantes",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Pine800,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                            Button(
                                onClick = {},
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Clay600)
                            ) {
                                Text(text = "Quero participar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                // Post Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Pine700),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "M", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(9.dp))
                            Column {
                                Text(text = "Marta Nunes", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Ink900)
                                Text(text = "há 2h · Vila Marlene", fontSize = 9.5.sp, color = Ink600)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Achei essa gatinha perto da praça, parece bem cuidada mas está sem coleira. Alguém reconhece?",
                            fontSize = 11.5.sp,
                            color = Ink600,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Ink600, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "18", fontSize = 10.5.sp, color = Ink600)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Message, contentDescription = null, tint = Ink600, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "7", fontSize = 10.5.sp, color = Ink600)
                            }
                        }
                    }
                }
            }
        }
    }
}
