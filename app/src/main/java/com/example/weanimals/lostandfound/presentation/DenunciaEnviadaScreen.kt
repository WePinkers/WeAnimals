package com.example.weanimals.lostandfound.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenBackground = Color(0xFFF7F2E7)
private val CircleBgColor = Color(0xFFE1EEE1)
private val IconGreenColor = Color(0xFF1F4D2C)
private val TitleTextColor = Color(0xFF1A1A18)
private val DescriptionTextColor = Color(0xFF6B6A63)
private val ButtonBgColor = Color(0xFF2C2C2A)
private val ButtonTextColor = Color(0xFFFFF8ED)

/**
 * Tela de Confirmação de Denúncia Enviada.
 */
@Composable
fun DenunciaEnviadaScreen(
    onEntendiClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScreenBackground,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {

                // Círculo com ícone de check verde
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CircleBgColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Denúncia recebida",
                        tint = IconGreenColor,
                        modifier = Modifier.size(28.dp),
                    )
                }

                // Título
                Text(
                    text = "Denúncia recebida",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = TitleTextColor,
                    textAlign = TextAlign.Center,
                )

                // Texto explicativo
                Text(
                    text = "Nossa equipe irá analisar o anúncio em até 24h. Agradecemos por ajudar a manter a comunidade segura.",
                    fontSize = 13.sp,
                    color = DescriptionTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    fontFamily = FontFamily.SansSerif,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Botão Entendi
                Button(
                    onClick = onEntendiClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonBgColor,
                        contentColor = ButtonTextColor,
                    ),
                ) {
                    Text(
                        text = "Entendi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ButtonTextColor,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DenunciaEnviadaScreenPreview() {
    MaterialTheme {
        DenunciaEnviadaScreen()
    }
}
