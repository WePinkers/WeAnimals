package com.example.weanimals.lostandfound.presentation

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
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

// Cores base da tela
private val ScreenBackground = Color(0xFFF7F2E7)
private val CircleBgColor = Color(0xFFEFE9D8)
private val ClockIconColor = Color(0xFF8C6A3F)
private val TitleTextColor = Color(0xFF1A1A18)
private val DescriptionTextColor = Color(0xFF6B6A63)
private val CardBgColor = Color(0xFFEFE9D8)
private val CardLabelColor = Color(0xFF8C8880)
private val CheckIconColor = Color(0xFF1F4D2C)
private val AnswerTextColor = Color(0xFF2C2C2A)
private val ButtonBgColor = Color(0xFF2C2C2A)
private val ButtonTextColor = Color(0xFFFFF8ED)
private val SubFooterTextColor = Color(0xFF8C8880)

/**
 * Tela "Aguardando Verificação" de dono do pet.
 */
@Composable
fun AguardandoVerificacaoScreen(
    respostas: List<String> = emptyList(),
    onVoltarClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
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

                // Círculo de 52dp de diâmetro com ícone de relógio
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CircleBgColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Aguardando",
                        tint = ClockIconColor,
                        modifier = Modifier.size(26.dp),
                    )
                }

                // Título "Respostas enviadas" (15sp, negrito)
                Text(
                    text = "Respostas enviadas",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = TitleTextColor,
                    textAlign = TextAlign.Center,
                )

                // Texto explicativo (13sp, centralizado)
                Text(
                    text = "Quem encontrou o animal vai revisar suas respostas. Você recebe um aviso assim que o contato for liberado.",
                    fontSize = 13.sp,
                    color = DescriptionTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    fontFamily = FontFamily.SansSerif,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Card de Respostas (fundo #EFE9D8, cantos 12dp, padding 14dp, alinhado à esquerda)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBgColor)
                        .padding(14.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Suas respostas",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = CardLabelColor,
                        )

                        // Lista de respostas enviadas
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            respostas.forEach { resposta ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Confirmado",
                                        tint = CheckIconColor,
                                        modifier = Modifier.size(18.dp),
                                    )

                                    Text(
                                        text = resposta,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = AnswerTextColor,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botão "Voltar aos matches"
                Button(
                    onClick = onVoltarClick,
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
                        text = "Voltar aos matches",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ButtonTextColor,
                    )
                }

                // Texto pequeno no rodapé
                Text(
                    text = "Tempo médio de resposta: algumas horas",
                    fontSize = 11.sp,
                    color = SubFooterTextColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Preview da tela Aguardando Verificação.
 */
@Preview(showBackground = true)
@Composable
fun AguardandoVerificacaoScreenPreview() {
    MaterialTheme {
        AguardandoVerificacaoScreen(
            respostas = listOf(
                "Nome: Toby",
                "Sinal: Mancha em formato de coração na pata",
                "Foto de comprovação enviada",
            ),
        )
    }
}
