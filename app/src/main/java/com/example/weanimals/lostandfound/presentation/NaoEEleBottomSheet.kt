package com.example.weanimals.lostandfound.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SheetBackground = Color(0xFFFFFFFF)
private val HandleGray = Color(0xFFD8D3C4)
private val TitleColor = Color(0xFF1A1A18)
private val SubtitleColor = Color(0xFF6B6A63)
private val OptionBgColor = Color(0xFFF7F2E7)
private val OptionBorderColor = Color(0xFFD8D3C4)
private val OptionTextColor = Color(0xFF2C2C2A)
private val PrimaryRust = Color(0xFFB35D38)
private val HeaderDarkGreen = Color(0xFF1C3322)
private val PlaceholderGray = Color(0xFF8C887F)

/**
 * Bottom Sheet "Por que não é ele?" para registrar motivo de descarte do match.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaoEEleBottomSheet(
    onDismissRequest: () -> Unit,
    onMotivoSelecionado: (motivo: String) -> Unit,
    onPular: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val motivosPadrao = listOf(
        "Cor não bate",
        "Porte não bate",
        "Local muito longe",
    )

    var showOutroInput by remember { mutableStateOf(false) }
    var outroTexto by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SheetBackground,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            // Handle (barrinha cinza de 36dp x 4dp, arredondada)
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(HandleGray),
            )
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Título em negrito, 14sp, cor #1A1A18
            Text(
                text = "Por que não é ele?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = TitleColor,
            )

            // Subtítulo abaixo, 12sp, cor #6B6A63
            Text(
                text = "Isso ajuda a melhorar seus próximos matches.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                color = SubtitleColor,
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Lista vertical dos 3 motivos padrão
            motivosPadrao.forEach { motivo ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(OptionBgColor)
                        .border(
                            width = 0.5.dp,
                            color = OptionBorderColor,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable {
                            onMotivoSelecionado(motivo)
                        }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = motivo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = OptionTextColor,
                    )
                }
            }

            // Opção "Outro motivo"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (showOutroInput) Color.White else OptionBgColor)
                    .border(
                        width = if (showOutroInput) 1.dp else 0.5.dp,
                        color = if (showOutroInput) HeaderDarkGreen else OptionBorderColor,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable {
                        showOutroInput = !showOutroInput
                    }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Outro motivo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    color = OptionTextColor,
                )
            }

            // Campo de Texto expandível quando "Outro motivo" estiver selecionado
            AnimatedVisibility(
                visible = showOutroInput,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = outroTexto,
                        onValueChange = { outroTexto = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        placeholder = {
                            Text(
                                text = "Digite o motivo do descarte...",
                                fontSize = 13.sp,
                                color = PlaceholderGray,
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = HeaderDarkGreen,
                            unfocusedBorderColor = OptionBorderColor,
                            focusedTextColor = OptionTextColor,
                            unfocusedTextColor = OptionTextColor,
                        ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(
                            onClick = {
                                if (outroTexto.isNotBlank()) {
                                    onMotivoSelecionado(outroTexto.trim())
                                } else {
                                    onMotivoSelecionado("Outro motivo")
                                }
                            },
                            enabled = outroTexto.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryRust,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = "Confirmar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Botão de texto "Pular" (sem borda, sem fundo, cor #6B6A63, 13sp)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(
                    onClick = onPular,
                ) {
                    Text(
                        text = "Pular",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = SubtitleColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun NaoEEleBottomSheetPreview() {
    MaterialTheme {
        NaoEEleBottomSheet(
            onDismissRequest = {},
            onMotivoSelecionado = {},
            onPular = {},
        )
    }
}
