package com.example.weanimals.lostandfound.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

// Cores base da paleta WeAnimals
private val BeigeBackground = Color(0xFFF7F2E7)
private val HeaderDarkGreen = Color(0xFF1C3322)
private val BackButtonBg = Color(0xFFEAE5D9)
private val SectionTitleGray = Color(0xFF6E6A63)
private val DescriptionTextColor = Color(0xFF524F48)
private val CardBorderGray = Color(0xFFC7C2B8)
private val DividerGray = Color(0xFFE5E0D8)
private val TagBgColor = Color(0xFFF3EFE6)
private val PrimaryRust = Color(0xFFB35D38)
private val TextDark = Color(0xFF1C201D)
private val SubtitleGray = Color(0xFF6E716E)
private val HighMatchCircleColor = Color(0xFF1C3322)
private val LowMatchCircleColor = Color(0xFF6E6A63)

data class MatchTagData(
    val label: String,
    val isMatched: Boolean = true,
)

data class PetMatchCardData(
    val id: String,
    val matchPercentage: Int,
    val title: String,
    val subtitle: String,
    val tags: List<MatchTagData>,
)

/**
 * Retorna lista simulada de matches conforme a espécie selecionada.
 */
fun getMockMatchesForSpecies(species: AnimalSpecies): List<PetMatchCardData> {
    return when (species) {
        AnimalSpecies.DOG -> listOf(
            PetMatchCardData(
                id = "dog_1",
                matchPercentage = 91,
                title = "Encontrado — Praça Central",
                subtitle = "1,1 km · achado há 2 dias",
                tags = listOf(
                    MatchTagData("Cor: caramelo", isMatched = true),
                    MatchTagData("Porte: médio", isMatched = true),
                    MatchTagData("Coleira vermelha", isMatched = true),
                    MatchTagData("Raio compatível", isMatched = true),
                ),
            ),
            PetMatchCardData(
                id = "dog_2",
                matchPercentage = 68,
                title = "Encontrado — Zona Norte",
                subtitle = "4,6 km · achado há 5 dias",
                tags = listOf(
                    MatchTagData("Cor: caramelo", isMatched = false),
                    MatchTagData("Porte: médio", isMatched = false),
                    MatchTagData("Sem coleira", isMatched = false),
                ),
            ),
        )

        AnimalSpecies.CAT -> listOf(
            PetMatchCardData(
                id = "cat_1",
                matchPercentage = 88,
                title = "Encontrado — Vila Marlene",
                subtitle = "0,8 km · achado há 1 dia",
                tags = listOf(
                    MatchTagData("Espécie: Gato", isMatched = true),
                    MatchTagData("Cor: siamês / marrom", isMatched = true),
                    MatchTagData("Coleira azul", isMatched = true),
                    MatchTagData("Raio compatível", isMatched = true),
                ),
            ),
            PetMatchCardData(
                id = "cat_2",
                matchPercentage = 72,
                title = "Encontrado — Centro Histórico",
                subtitle = "3,2 km · achado há 3 dias",
                tags = listOf(
                    MatchTagData("Espécie: Gato", isMatched = false),
                    MatchTagData("Cor: branco e cinza", isMatched = false),
                    MatchTagData("Sem coleira", isMatched = false),
                ),
            ),
        )
    }
}

/**
 * Tela de Matches Encontrados com Suporte a Remoção de Card, BottomSheet e Snackbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetMatchesScreen(
    species: AnimalSpecies = AnimalSpecies.DOG,
    onBackClick: () -> Unit = {},
    onViewDetailsClick: (PetMatchCardData) -> Unit = {},
) {
    val initialMatches = remember(species) { getMockMatchesForSpecies(species) }
    val matchesList = remember(species) { mutableStateListOf(*initialMatches.toTypedArray()) }

    var selectedMatchForBottomSheet by remember { mutableStateOf<PetMatchCardData?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var lastRemovedItem by remember { mutableStateOf<Pair<Int, PetMatchCardData>?>(null) }

    // Função para remover o card e acionar o Snackbar com "Desfazer"
    val removeMatchAndShowSnackbar = { matchToRemove: PetMatchCardData ->
        val index = matchesList.indexOfFirst { it.id == matchToRemove.id }
        if (index != -1) {
            lastRemovedItem = Pair(index, matchToRemove)
            matchesList.removeAt(index)

            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = "Removido",
                    actionLabel = "Desfazer",
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    lastRemovedItem?.let { (targetIdx, item) ->
                        val safeIndex = targetIdx.coerceIn(0, matchesList.size)
                        matchesList.add(safeIndex, item)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = HeaderDarkGreen,
                    contentColor = Color.White,
                    actionColor = PrimaryRust,
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        containerColor = BeigeBackground,
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = BeigeBackground,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BackButtonBg),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = HeaderDarkGreen,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Possíveis matches",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = HeaderDarkGreen,
                    )
                }

                HorizontalDivider(
                    color = DividerGray,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Conteúdo Rolável
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Descrição superior
                    Text(
                        text = "Comparamos espécie, cor, porte, local e data com os animais encontrados pela comunidade nos últimos 7 dias.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        color = DescriptionTextColor,
                        lineHeight = 20.sp,
                    )

                    // Título de Seção
                    Text(
                        text = "MELHOR COMPATIBILIDADE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.2.sp,
                        color = SectionTitleGray,
                    )

                    // Lista Animada de Cards
                    initialMatches.forEach { matchItem ->
                        key(matchItem.id) {
                            AnimatedVisibility(
                                visible = matchesList.contains(matchItem),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically() + slideOutVertically(targetOffsetY = { -it / 2 }),
                            ) {
                                PetMatchCard(
                                    match = matchItem,
                                    onNotThisOneClick = {
                                        selectedMatchForBottomSheet = matchItem
                                        showBottomSheet = true
                                    },
                                    onViewDetailsClick = { onViewDetailsClick(matchItem) },
                                )
                            }
                        }
                    }

                    if (matchesList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Nenhum match restante.",
                                fontSize = 14.sp,
                                color = SubtitleGray,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Exibição do ModalBottomSheet "Por que não é ele?"
            if (showBottomSheet) {
                NaoEEleBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    },
                    onMotivoSelecionado = { _ ->
                        showBottomSheet = false
                        selectedMatchForBottomSheet?.let { match ->
                            removeMatchAndShowSnackbar(match)
                        }
                    },
                    onPular = {
                        showBottomSheet = false
                        selectedMatchForBottomSheet?.let { match ->
                            removeMatchAndShowSnackbar(match)
                        }
                    },
                )
            }
        }
    }
}

/**
 * Card individual de Match de Animal.
 */
@Composable
fun PetMatchCard(
    match: PetMatchCardData,
    onNotThisOneClick: () -> Unit = {},
    onViewDetailsClick: () -> Unit = {},
) {
    val isHighMatch = match.matchPercentage >= 80
    val circleColor = if (isHighMatch) HighMatchCircleColor else LowMatchCircleColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, CardBorderGray, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Linha Superior: Círculo de Porcentagem + Título/Subtítulo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Círculo de Match %
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .border(2.5.dp, circleColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "${match.matchPercentage}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = circleColor,
                        )
                        Text(
                            text = "MATCH",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = circleColor,
                        )
                    }
                }

                // Título e Distância/Dias
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = match.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = TextDark,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = match.subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        color = SubtitleGray,
                    )
                }
            }

            // Tags / Chips de Atributos (Divididos em linhas)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    match.tags.take(2).forEach { tag ->
                        MatchTagChip(tag = tag)
                    }
                }
                if (match.tags.size > 2) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        match.tags.drop(2).forEach { tag ->
                            MatchTagChip(tag = tag)
                        }
                    }
                }
            }

            // Botões de Ação ("Não é ele" e "Ver detalhes")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Botão Secundário "Não é ele"
                OutlinedButton(
                    onClick = onNotThisOneClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, CardBorderGray),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextDark,
                    ),
                ) {
                    Text(
                        text = "Não é ele",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                    )
                }

                // Botão Primário "Ver detalhes"
                Button(
                    onClick = onViewDetailsClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryRust,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = "Ver detalhes",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/**
 * Chip de Atributo de Match.
 */
@Composable
private fun MatchTagChip(tag: MatchTagData) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TagBgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        val labelText = if (tag.isMatched) "✓ ${tag.label}" else tag.label
        Text(
            text = labelText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Previews para Cão e Gato.
 */
@Preview(showBackground = true, name = "Cão Matches")
@Composable
fun PetMatchesScreenDogPreview() {
    MaterialTheme {
        PetMatchesScreen(species = AnimalSpecies.DOG)
    }
}

@Preview(showBackground = true, name = "Gato Matches")
@Composable
fun PetMatchesScreenCatPreview() {
    MaterialTheme {
        PetMatchesScreen(species = AnimalSpecies.CAT)
    }
}
