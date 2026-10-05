package com.example.weanimals.lostandfound.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cores base do Bottom Sheet de Denúncia
private val SheetBackground = Color(0xFFFFFFFF)
private val HandleGray = Color(0xFFD8D3C4)
private val TitleColor = Color(0xFF1A1A18)
private val SubtitleColor = Color(0xFF6B6A63)
private val OptionBgColor = Color(0xFFF7F2E7)
private val OptionBorderColor = Color(0xFFD8D3C4)
private val OptionTextColor = Color(0xFF2C2C2A)
private val RadioAccentColor = Color(0xFFB45C33)
private val RadioUnselectedColor = Color(0xFF8C8880)
private val DenounceButtonBg = Color(0xFFA32D2D)
private val DenounceButtonTextColor = Color(0xFFFCEBEB)
private val PlaceholderColor = Color(0xFF8C8880)
private val ConfirmCircleBg = Color(0xFFE1EEE1)
private val ConfirmCheckColor = Color(0xFF1F4D2C)
private val ConfirmOkButtonBg = Color(0xFF2C2C2A)
private val ConfirmOkButtonTextColor = Color(0xFFFFF8ED)

/**
 * Bottom Sheet "Denunciar anúncio".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenunciarAnuncioBottomSheet(
    onDismissRequest: () -> Unit,
    onEnviarDenuncia: (motivo: String, detalhes: String) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val opcoesMotivo = listOf(
        "Anúncio falso ou golpe",
        "Está pedindo pagamento",
        "Conteúdo ofensivo ou inadequado",
        "Animal já foi encontrado",
        "Outro motivo",
    )

    var selectedMotivo by remember { mutableStateOf<String?>(null) }
    var detalhesTexto by remember { mutableStateOf("") }
    var showValidationError by remember { mutableStateOf(false) }
    var foiEnviado by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SheetBackground,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            // Handle centralizado no topo (36dp x 4dp, cinza, arredondado)
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
        if (foiEnviado) {
            // Substitui o formulário pela tela de confirmação
            DenunciaConfirmadaContent(
                onOkClick = onDismissRequest,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Título "Denunciar anúncio" (14sp, negrito, cor #1A1A18)
                Text(
                    text = "Denunciar anúncio",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = TitleColor,
                )

                // Subtítulo "Nossa equipe revisa denúncias em até 24h." (12sp, cor #6B6A63)
                Text(
                    text = "Nossa equipe revisa denúncias em até 24h.",
                    fontSize = 12.sp,
                    color = SubtitleColor,
                    fontFamily = FontFamily.SansSerif,
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Grupo de RadioButton com 5 opções
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    opcoesMotivo.forEach { motivo ->
                        val isSelected = selectedMotivo == motivo

                        Row(
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
                                    selectedMotivo = motivo
                                    showValidationError = false
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedMotivo = motivo
                                    showValidationError = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = RadioAccentColor,
                                    unselectedColor = RadioUnselectedColor,
                                ),
                            )

                            Text(
                                text = motivo,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif,
                                color = OptionTextColor,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }

                if (showValidationError) {
                    Text(
                        text = "Selecione um motivo para a denúncia",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Label "Detalhes adicionais (opcional)" (12sp, cor #6B6A63)
                Text(
                    text = "Detalhes adicionais (opcional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SubtitleColor,
                )

                // TextField multilinha (fundo #F7F2E7, borda #D8D3C4, cantos 10dp)
                OutlinedTextField(
                    value = detalhesTexto,
                    onValueChange = { detalhesTexto = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    placeholder = {
                        Text(
                            text = "Descreva o que você notou...",
                            fontSize = 13.sp,
                            color = PlaceholderColor,
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = OptionBgColor,
                        unfocusedContainerColor = OptionBgColor,
                        focusedBorderColor = TitleColor,
                        unfocusedBorderColor = OptionBorderColor,
                        focusedTextColor = OptionTextColor,
                        unfocusedTextColor = OptionTextColor,
                    ),
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Botão "Enviar denúncia"
                Button(
                    onClick = {
                        if (selectedMotivo != null) {
                            onEnviarDenuncia(selectedMotivo!!, detalhesTexto.trim())
                            foiEnviado = true
                        } else {
                            showValidationError = true
                        }
                    },
                    enabled = selectedMotivo != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DenounceButtonBg,
                        contentColor = DenounceButtonTextColor,
                        disabledContainerColor = DenounceButtonBg.copy(alpha = 0.5f),
                        disabledContentColor = DenounceButtonTextColor.copy(alpha = 0.5f),
                    ),
                ) {
                    Text(
                        text = "Enviar denúncia",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DenounceButtonTextColor,
                    )
                }

                // Botão de texto "Cancelar" (sem fundo, cor #6B6A63)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = onCancelar) {
                        Text(
                            text = "Cancelar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SubtitleColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Conteúdo de Confirmação da Denúncia (exibido dentro do ModalBottomSheet).
 * Padding: 32dp topo / 24dp laterais / 28dp inferior.
 */
@Composable
fun DenunciaConfirmadaContent(
    onOkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Um círculo de 52dp, fundo #E1EEE1, com ícone de check (cor #1F4D2C) centralizado
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ConfirmCircleBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Denúncia recebida",
                tint = ConfirmCheckColor,
                modifier = Modifier.size(26.dp),
            )
        }

        // Título "Denúncia recebida" (15sp, negrito, cor #1A1A18)
        Text(
            text = "Denúncia recebida",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = TitleColor,
            textAlign = TextAlign.Center,
        )

        // Texto "Vamos analisar em até 24h. Enquanto isso, o anúncio continua visível para outras pessoas." (13sp, cor #6B6A63, centralizado)
        Text(
            text = "Vamos analisar em até 24h. Enquanto isso, o anúncio continua visível para outras pessoas.",
            fontSize = 13.sp,
            color = SubtitleColor,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            fontFamily = FontFamily.SansSerif,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Botão largura total, fundo #2C2C2A, texto #FFF8ED, cantos bem arredondados: "Ok, entendi" -> onOkClick()
        Button(
            onClick = onOkClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ConfirmOkButtonBg,
                contentColor = ConfirmOkButtonTextColor,
            ),
        ) {
            Text(
                text = "Ok, entendi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ConfirmOkButtonTextColor,
            )
        }
    }
}

/**
 * Preview do Bottom Sheet de Denúncia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun DenunciarAnuncioBottomSheetPreview() {
    MaterialTheme {
        DenunciarAnuncioBottomSheet(
            onDismissRequest = {},
            onEnviarDenuncia = { _, _ -> },
            onCancelar = {},
        )
    }
}

/**
 * Preview do Conteúdo de Denúncia Confirmada.
 */
@Preview(showBackground = true)
@Composable
fun DenunciaConfirmadaContentPreview() {
    MaterialTheme {
        DenunciaConfirmadaContent(
            onOkClick = {},
        )
    }
}
