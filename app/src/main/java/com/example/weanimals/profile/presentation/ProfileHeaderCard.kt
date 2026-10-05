package com.example.weanimals.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Cores base do card de perfil
private val CardBgGreen = Color(0xFF12271F)
private val CardTitleText = Color(0xFFF7F2E7)
private val EditIconColor = Color(0xFFF7F2E7)
private val EditIconBg = Color.White.copy(alpha = 0.12f)
private val DefaultAvatarBg = Color(0xFFB45C33)
private val DefaultAvatarText = Color(0xFFFFF8ED)
private val CameraBadgeBg = Color(0xFFF7F2E7)
private val CameraBadgeIcon = Color(0xFF12271F)
private val BioPlaceholderText = Color(0xFF90A395)
private val BioContentText = Color(0xFFC9D6CC)

/**
 * Card de Cabeçalho do Perfil com Avatar, Câmera badge, Bio e Lápis de Edição.
 */
@Composable
fun ProfileHeaderCard(
    nome: String,
    fotoUrl: String?,
    bio: String?,
    onEditarClick: () -> Unit,
    onEditarFotoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBgGreen)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
    ) {
        // Ícone de lápis no canto superior direito (30dp, fundo branco 12% opacidade, ícone edit 16dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(30.dp)
                .clip(CircleShape)
                .background(EditIconBg)
                .clickable(onClick = onEditarClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editar perfil",
                tint = EditIconColor,
                modifier = Modifier.size(16.dp),
            )
        }

        // Linha horizontal principal (Avatar + Badge de câmera + Coluna de textos)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 36.dp), // Espaço para não sobrepor o botão de editar no topo
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Container do Avatar de 56dp com Badge de Câmera sobreposto
            Box(
                modifier = Modifier.size(56.dp),
            ) {
                // Avatar circular de 56dp
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
                        val initial = nome.firstOrNull()?.uppercase() ?: "U"
                        Text(
                            text = initial,
                            fontSize = 20.sp,
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
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(CameraBadgeBg)
                        .border(2.dp, CardBgGreen, CircleShape)
                        .clickable(onClick = onEditarFotoClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Editar foto",
                        tint = CameraBadgeIcon,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Coluna com "Meu perfil" e texto de Bio
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = nome,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = CardTitleText,
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (bio.isNullOrEmpty()) {
                    Text(
                        text = "Toque para adicionar bio",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = BioPlaceholderText,
                        modifier = Modifier.clickable(onClick = onEditarClick),
                    )
                } else {
                    Text(
                        text = bio,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Normal,
                        color = BioContentText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Previews do ProfileHeaderCard (com e sem bio/foto).
 */
@Preview(showBackground = true)
@Composable
fun ProfileHeaderCardWithoutBioPreview() {
    MaterialTheme {
        Surface(
            color = Color(0xFFF7F2E7),
            modifier = Modifier.padding(16.dp),
        ) {
            ProfileHeaderCard(
                nome = "Usuario",
                fotoUrl = null,
                bio = null,
                onEditarClick = {},
                onEditarFotoClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileHeaderCardWithBioPreview() {
    MaterialTheme {
        Surface(
            color = Color(0xFFF7F2E7),
            modifier = Modifier.padding(16.dp),
        ) {
            ProfileHeaderCard(
                nome = "Matheus",
                fotoUrl = null,
                bio = "Tutor do Toby 🐶 Apaixonado por animais e protetor independente em São Paulo.",
                onEditarClick = {},
                onEditarFotoClick = {},
            )
        }
    }
}
