package com.example.weanimals.profile.presentation

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modelo de dados extensível para itens da lista de atividade do perfil.
 */
data class ActivityItemData(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit = {},
)

// Definição das cores da paleta visual (baseada nas telas do WeAnimals)
private val BeigeBackground = Color(0xFFF7F2E7)
private val TitleGray = Color(0xFF6E6A63)
private val ItemTitleBlack = Color(0xFF1C201D)
private val SubtitleGray = Color(0xFF6E716E)
private val IconDarkGray = Color(0xFF2D312E)
private val ChevronLightGray = Color(0xFFA09D96)
private val DividerLightGray = Color(0xFFE5E0D8)

/**
 * Componente de item de lista reusável ("ActivityListItem").
 *
 * Recebe o ícone à esquerda, título em negrito, subtítulo opcional e a ação ao clicar.
 */
@Composable
fun ActivityListItem(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícone à esquerda (tamanho 24dp, cor cinza escuro)
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = IconDarkGray
        )

        // Espaçamento de 16dp entre o ícone e a coluna de texto
        Spacer(modifier = Modifier.width(16.dp))

        // Coluna de Texto (Título e Subtítulo)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = ItemTitleBlack
            )

            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    color = SubtitleGray
                )
            }
        }

        // Ícone de seta (chevron) à direita (tamanho 20dp, cor cinza claro)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = ChevronLightGray
        )
    }
}

/**
 * Componente genérico para seções de perfil/atividade.
 * Estruturado com título em caixa alta, divisor superior e itens com divisores entre/após eles.
 */
@Composable
fun ActivitySection(
    sectionTitle: String,
    items: List<ActivityItemData>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BeigeBackground)
    ) {
        // Título da Seção (pequeno, maiúsculo, cinza, letter-spacing levemente aumentado, alinhado à esquerda)
        Text(
            text = sectionTitle.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 1.2.sp,
            color = TitleGray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Linha divisória fina na cor cinza claro ocupando toda a largura
        HorizontalDivider(
            color = DividerLightGray,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )

        // Itens da seção
        items.forEach { item ->
            ActivityListItem(
                icon = item.icon,
                title = item.title,
                subtitle = item.subtitle,
                onClick = item.onClick
            )
            // Divisor fino após cada item
            HorizontalDivider(
                color = DividerLightGray,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Componente específico da seção "ANIMAIS ACHADOS E PERDIDOS".
 */
@Composable
fun LostAndFoundSection(
    modifier: Modifier = Modifier,
    onFindPetClick: () -> Unit = {}
) {
    val items = listOf(
        ActivityItemData(
            id = "find_pet",
            icon = Icons.Default.Search,
            title = "Encontrar meu animal",
            subtitle = "Encontre seu animal perdido",
            onClick = onFindPetClick
        )
    )

    ActivitySection(
        sectionTitle = "ANIMAIS ACHADOS E PERDIDOS",
        items = items,
        modifier = modifier
    )
}

/**
 * Preview da Seção de Animais Achados e Perdidos no Android Studio.
 */
@Preview(showBackground = true)
@Composable
fun LostAndFoundSectionPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BeigeBackground
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp)
            ) {
                LostAndFoundSection()
            }
        }
    }
}
