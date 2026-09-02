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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.weanimals.theme.Brass100
import com.example.weanimals.theme.Clay600
import com.example.weanimals.theme.Ink600
import com.example.weanimals.theme.LineBorder
import com.example.weanimals.theme.Pine800
import com.example.weanimals.theme.Pine900
import com.example.weanimals.theme.Sand100
import com.example.weanimals.theme.Sand50

@Composable
fun AdoptionScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Sand50)
            .padding(top = 18.dp)
    ) {
        Text(
            text = "REDE DE ABRIGOS PARCEIROS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Ink600,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Prontos para um lar",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Pine900,
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PetCard(
                    name = "Canela",
                    tag = "SRD · 2 anos",
                    meta = "Vacinada · castrada · dócil com crianças"
                )
            }
            item {
                PetCard(
                    name = "Girassol",
                    tag = "Felino · filhote",
                    meta = "Castrado · brincalhão · gosta de sol"
                )
            }
        }
    }
}

@Composable
fun PetCard(
    name: String,
    tag: String,
    meta: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Sand100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = Clay600.copy(alpha = 0.6f),
                    modifier = Modifier.size(44.dp)
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White
                ) {
                    Text(
                        text = tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pine800,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(13.dp)) {
                Text(text = name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Pine900)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = meta, fontSize = 10.5.sp, color = Ink600)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pine800)
                ) {
                    Text(text = "Quero conhecer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
