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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Sand50)
            .verticalScroll(rememberScrollState())
    ) {
        // Profile Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Pine900)
                .padding(horizontal = 18.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Clay600),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "R",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(13.dp))
                Column {
                    Text(
                        text = "Renata Ferreira",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Vila Marlene · membro desde mar 2025",
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(18.dp)) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileStatCard(number = "6", label = "denúncias", modifier = Modifier.weight(1f))
                ProfileStatCard(number = "3", label = "resgates", modifier = Modifier.weight(1f))
                ProfileStatCard(number = "12", label = "campanhas", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Reputation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Sand100)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONFIABILIDADE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink600,
                            letterSpacing = 0.6.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Brass500
                        ) {
                            Text(
                                text = "Nível ouro",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "94%",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pine900
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.94f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Pine700)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "6 denúncias confirmadas · 0 descartadas · histórico validado por ONGs",
                        fontSize = 9.5.sp,
                        color = Ink600
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section Title
            Text(
                text = "ATIVIDADE",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = Ink600,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileRowItem(
                icon = Icons.Default.Report,
                title = "Minhas denúncias",
                subtitle = "6 protocolos, 4 concluídos"
            )
            ProfileRowItem(
                icon = Icons.Default.Favorite,
                title = "Animais favoritados",
                subtitle = "4 salvos para adoção"
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "CONTA",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = Ink600,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileRowItem(
                icon = Icons.Default.LocationOn,
                title = "Endereço e bairro",
                subtitle = "Vila Marlene, Zona Norte"
            )
            ProfileRowItem(
                icon = Icons.Default.Notifications,
                title = "Notificações",
                subtitle = "Denúncias, campanhas e chat"
            )
        }
    }
}

@Composable
fun ProfileStatCard(
    number: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Sand100
    ) {
        Column(
            modifier = Modifier.padding(vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = number,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Pine900
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink600
            )
        }
    }
}

@Composable
fun ProfileRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Pine800, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(11.dp))
            Column {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink900)
                Text(text = subtitle, fontSize = 9.5.sp, color = Ink600)
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Ink600, modifier = Modifier.size(16.dp))
    }
}
