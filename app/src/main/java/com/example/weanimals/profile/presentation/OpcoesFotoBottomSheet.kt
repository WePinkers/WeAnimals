package com.example.weanimals.profile.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cores base do Bottom Sheet de Opções de Foto
private val SheetBackground = Color(0xFFFFFDF7)
private val HandleGray = Color(0xFFD8D3C4)
private val OptionIconColor = Color(0xFF2C2C2A)
private val OptionTextColor = Color(0xFF1A1A18)
private val DeleteRedColor = Color(0xFFA32D2D)
private val DividerColor = Color(0xFFD8D3C4)
private val CancelTextColor = Color(0xFF6B6A63)

/**
 * Bottom Sheet com menu de opções de foto (Tirar foto, Escolher da galeria, Remover foto).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpcoesFotoBottomSheet(
    onDismissRequest: () -> Unit,
    onTirarFotoClick: () -> Unit,
    onEscolherGaleriaClick: () -> Unit,
    onRemoverFotoClick: () -> Unit,
    modifier: Modifier = Modifier,
    temFotoCustomizada: Boolean = false,
    onCancelar: () -> Unit = onDismissRequest,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SheetBackground,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            // Handle centralizado no topo (36dp x 4dp, cor #D8D3C4)
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
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Opção 1: Tirar foto
            FotoOptionRow(
                icon = Icons.Default.CameraAlt,
                text = "Tirar foto",
                textColor = OptionTextColor,
                iconColor = OptionIconColor,
                onClick = onTirarFotoClick,
            )

            // Opção 2: Escolher da galeria
            FotoOptionRow(
                icon = Icons.Default.PhotoLibrary,
                text = "Escolher da galeria",
                textColor = OptionTextColor,
                iconColor = OptionIconColor,
                onClick = onEscolherGaleriaClick,
            )

            // Opção 3 (Condicional): Remover foto
            if (temFotoCustomizada) {
                HorizontalDivider(
                    color = DividerColor,
                    thickness = 0.5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )

                FotoOptionRow(
                    icon = Icons.Default.Delete,
                    text = "Remover foto",
                    textColor = DeleteRedColor,
                    iconColor = DeleteRedColor,
                    onClick = onRemoverFotoClick,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Botão de texto "Cancelar"
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(onClick = onCancelar) {
                    Text(
                        text = "Cancelar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = CancelTextColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Linha clicável de opção do menu de foto.
 */
@Composable
private fun FotoOptionRow(
    icon: ImageVector,
    text: String,
    textColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = textColor,
        )
    }
}

/**
 * Previews do Bottom Sheet de Opções de Foto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Sem Foto Customizada")
@Composable
fun OpcoesFotoBottomSheetPreviewWithoutCustomPhoto() {
    MaterialTheme {
        OpcoesFotoBottomSheet(
            temFotoCustomizada = false,
            onDismissRequest = {},
            onTirarFotoClick = {},
            onEscolherGaleriaClick = {},
            onRemoverFotoClick = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Com Foto Customizada")
@Composable
fun OpcoesFotoBottomSheetPreviewWithCustomPhoto() {
    MaterialTheme {
        OpcoesFotoBottomSheet(
            temFotoCustomizada = true,
            onDismissRequest = {},
            onTirarFotoClick = {},
            onEscolherGaleriaClick = {},
            onRemoverFotoClick = {},
        )
    }
}
