package com.example.weanimals.profile.presentation

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Cores base do Bottom Sheet de Edição de Perfil
private val SheetBackground = Color(0xFFFFFDF7)
private val HandleGray = Color(0xFFD8D3C4)
private val TitleColor = Color(0xFF1A1A18)
private val DefaultAvatarBg = Color(0xFFB45C33)
private val DefaultAvatarText = Color(0xFFFFF8ED)
private val CameraBadgeBg = Color(0xFFF7F2E7)
private val CameraBadgeBorder = Color(0xFFFFFDF7)
private val CameraBadgeIcon = Color(0xFF1E3B2C)
private val LabelColor = Color(0xFF1A1A18)
private val InputBgColor = Color(0xFFF7F2E7)
private val InputBorderColor = Color(0xFFD8D3C4)
private val CounterColor = Color(0xFF8C8880)
private val PlaceholderColor = Color(0xFF8C8880)
private val SaveButtonBg = Color(0xFFB45C33)
private val SaveButtonTextColor = Color(0xFFFFF8ED)
private val CancelButtonTextColor = Color(0xFF6B6A63)

/**
 * Bottom Sheet para Edição de Perfil (Nome e Sobre mim).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilBottomSheet(
    nomeAtual: String,
    bioAtual: String?,
    fotoUrl: String?,
    onDismissRequest: () -> Unit,
    onSalvarClick: (nome: String, bio: String) -> Unit,
    onEditarFotoClick: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    erro: String? = null,
    isLoading: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    var nomeState by remember { mutableStateOf(nomeAtual) }
    var nomeError by remember { mutableStateOf(false) }

    var bioState by remember { mutableStateOf(bioAtual ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SheetBackground,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            // Handle centralizado no topo (36dp x 4dp, cor #D8D3C4, arredondado)
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Título "Editar perfil" (15sp, negrito, cor #1A1A18, centralizado)
            Text(
                text = "Editar perfil",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = TitleColor,
                textAlign = TextAlign.Center,
            )

            // Avatar circular de 64dp centralizado com Badge de Câmera sobreposto (24dp)
            Box(
                modifier = Modifier.size(64.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (fotoUrl.isNullOrEmpty()) DefaultAvatarBg else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!fotoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = fotoUrl,
                            contentDescription = "Foto do perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        val initial = nomeState.ifBlank { nomeAtual }
                            .firstOrNull()?.uppercase() ?: "U"
                        Text(
                            text = initial,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = DefaultAvatarText,
                        )
                    }
                }

                // Badge de Câmera sobreposto no canto inferior direito
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(CameraBadgeBg)
                        .border(2.dp, CameraBadgeBorder, CircleShape)
                        .clickable(onClick = onEditarFotoClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Editar foto",
                        tint = CameraBadgeIcon,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            // Campo "Nome"
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Nome",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabelColor,
                )

                OutlinedTextField(
                    value = nomeState,
                    onValueChange = { newValue ->
                        if (newValue.length <= 40) {
                            nomeState = newValue
                            if (newValue.isNotBlank()) nomeError = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nomeError,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputBgColor,
                        unfocusedContainerColor = InputBgColor,
                        errorContainerColor = InputBgColor,
                        focusedBorderColor = TitleColor,
                        unfocusedBorderColor = InputBorderColor,
                        focusedTextColor = TitleColor,
                        unfocusedTextColor = TitleColor,
                    ),
                )

                // Linha de Contador + Erro abaixo do campo Nome
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (nomeError) {
                        Text(
                            text = "Preencha este campo",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Spacer(modifier = Modifier.height(1.dp))
                    }

                    Text(
                        text = "${nomeState.length}/40",
                        fontSize = 11.sp,
                        color = CounterColor,
                    )
                }
            }

            // Campo "Sobre mim"
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Sobre mim",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = LabelColor,
                )

                OutlinedTextField(
                    value = bioState,
                    onValueChange = { newValue ->
                        if (newValue.length <= 150) {
                            bioState = newValue
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    placeholder = {
                        Text(
                            text = "Conte um pouco sobre você e sua relação com animais",
                            fontSize = 13.sp,
                            color = PlaceholderColor,
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputBgColor,
                        unfocusedContainerColor = InputBgColor,
                        focusedBorderColor = TitleColor,
                        unfocusedBorderColor = InputBorderColor,
                        focusedTextColor = TitleColor,
                        unfocusedTextColor = TitleColor,
                    ),
                )

                Text(
                    text = "${bioState.length}/150",
                    fontSize = 11.sp,
                    color = CounterColor,
                    modifier = Modifier.align(Alignment.End),
                )
            }

            // Mensagem de Erro externa (se houver)
            if (!erro.isNullOrEmpty()) {
                Text(
                    text = erro,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Botão "Salvar"
            Button(
                onClick = {
                    if (nomeState.isBlank()) {
                        nomeError = true
                    } else {
                        nomeError = false
                        onSalvarClick(nomeState.trim(), bioState.trim())
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaveButtonBg,
                    contentColor = SaveButtonTextColor,
                    disabledContainerColor = SaveButtonBg.copy(alpha = 0.7f),
                    disabledContentColor = SaveButtonTextColor,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = SaveButtonTextColor,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Salvar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaveButtonTextColor,
                    )
                }
            }

            // Botão "Cancelar"
            TextButton(
                onClick = onCancelar,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Cancelar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CancelButtonTextColor,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Preview do Bottom Sheet de Edição de Perfil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun EditarPerfilBottomSheetPreview() {
    MaterialTheme {
        EditarPerfilBottomSheet(
            nomeAtual = "Matheus",
            bioAtual = "Tutor do Toby 🐶 Apaixonado por animais.",
            fotoUrl = null,
            onDismissRequest = {},
            onSalvarClick = { _, _ -> },
            onEditarFotoClick = {},
            onCancelar = {},
        )
    }
}
