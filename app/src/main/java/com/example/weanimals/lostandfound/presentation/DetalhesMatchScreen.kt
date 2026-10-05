package com.example.weanimals.lostandfound.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Cores base da tela de Detalhes
private val ScreenBackground = Color(0xFFF7F2E7)
private val TopTitleColor = Color(0xFF2C2C2A)
private val PagerPlaceholderBg = Color(0xFFE4DECB)
private val PagerIconColor = Color(0xFF8C6A3F)
private val DotActiveColor = Color(0xFF8C6A3F)
private val DotInactiveColor = Color(0xFFD8D3C4)
private val MatchBadgeBg = Color(0xFFE1EEE1)
private val MatchBadgeTextColor = Color(0xFF1F4D2C)
private val CardBgColor = Color(0xFFEFE9D8)
private val TextDarkColor = Color(0xFF1A1A18)
private val TextMutedColor = Color(0xFF6B6A63)
private val DividerColor = Color(0xFFD8D3C4)
private val LabelColor = Color(0xFF8C8880)
private val RefChipBg = Color(0xFFFFFDF7)
private val ChipTextColor = Color(0xFF4A4A45)
private val ConfirmButtonBg = Color(0xFFB45C33)
private val ConfirmButtonTextColor = Color(0xFFFFF8ED)
private val DenounceBorderColor = Color(0xFFD8D3C4)

/**
 * Tela de Detalhes do Match de Animal.
 * Todos os textos e listas são passados como parâmetros dinâmicos.
 */
@Composable
fun DetalhesMatchScreen(
    nomeLocal: String,
    matchPercentage: Int,
    rua: String,
    bairro: String,
    pontosReferencia: List<Pair<String, ImageVector>>,
    diasAchado: Int,
    distanciaKm: Double,
    caracteristicas: List<String>,
    imagesUris: List<String> = emptyList(),
    temVerificacaoPendente: Boolean = false,
    onBackClick: () -> Unit = {},
    onConfirmarClick: () -> Unit = {},
    onDenunciarClick: () -> Unit = {},
) {
    var isLoading by remember { mutableStateOf(false) }
    var showPendenteDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val executarConfirmacao = {
        isLoading = true
        coroutineScope.launch {
            delay(400)
            isLoading = false
            onConfirmarClick()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = ScreenBackground,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // TOPO: TopAppBar transparente com ícone de voltar e título
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TopTitleColor,
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Encontrado — $nomeLocal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = TopTitleColor,
                )
            }

            // CONTEÚDO ROLÁVEL
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // CARROSSEL DE FOTOS
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val pageCount = if (imagesUris.isEmpty()) 1 else imagesUris.size
                    val pagerState = rememberPagerState { pageCount }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PagerPlaceholderBg),
                    ) { page ->
                        if (imagesUris.isNotEmpty() && (page < imagesUris.size)) {
                            AsyncImage(
                                model = imagesUris[page],
                                contentDescription = "Foto do animal $page",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Sem foto",
                                    tint = PagerIconColor,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Indicadores de página (dots)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(pageCount) { index ->
                            val active = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (active) DotActiveColor else DotInactiveColor),
                            )
                        }
                    }
                }

                // SELO DE COMPATIBILIDADE (pill)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MatchBadgeBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$matchPercentage% match",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MatchBadgeTextColor,
                        )
                    }
                }

                // CARD DE LOCALIZAÇÃO (sem mapa)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBgColor)
                        .padding(14.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Linha com ícone de pin + coluna de texto (SEM número da casa)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Localização",
                                tint = PagerIconColor,
                                modifier = Modifier.size(20.dp),
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = rua,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkColor,
                                )
                                Text(
                                    text = bairro,
                                    fontSize = 12.sp,
                                    color = TextMutedColor,
                                )
                            }
                        }

                        // Divisória fina (0.5dp)
                        HorizontalDivider(
                            color = DividerColor,
                            thickness = 0.5.dp,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Label "Pontos de referência"
                        Text(
                            text = "Pontos de referência",
                            fontSize = 11.sp,
                            color = LabelColor,
                        )

                        // Chips pequenos de pontos de referência
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            pontosReferencia.chunked(2).forEach { rowItems ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    rowItems.forEach { (textoRef, icone) ->
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(RefChipBg)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = icone,
                                                contentDescription = null,
                                                tint = PagerIconColor,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = textoRef,
                                                fontSize = 12.sp,
                                                color = ChipTextColor,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Texto "Achado há X dias · Y km" com ícone de relógio
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Tempo",
                        tint = TextMutedColor,
                        modifier = Modifier.size(16.dp),
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val textoDias = if (diasAchado == 1) "há 1 dia" else "há $diasAchado dias"
                    val textoKm = String.format(Locale.getDefault(), "%.1f", distanciaKm).replace('.', ',')

                    Text(
                        text = "Achado $textoDias · $textoKm km",
                        fontSize = 13.sp,
                        color = TextMutedColor,
                    )
                }

                // SEÇÃO CARACTERÍSTICAS
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Características",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkColor,
                    )

                    // Lista de chips de características
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        caracteristicas.chunked(2).forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowItems.forEach { caracteristica ->
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(CardBgColor)
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = caracteristica,
                                            fontSize = 12.sp,
                                            color = ChipTextColor,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // CARD VERIFICAÇÃO DE DONO (Informativo, não clicável)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBgColor)
                        .padding(12.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Escudo",
                                tint = PagerIconColor,
                                modifier = Modifier.size(18.dp),
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Verificação de dono",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkColor,
                            )
                        }

                        Text(
                            text = "Responda 2 perguntas para liberar o contato de quem encontrou.",
                            fontSize = 12.sp,
                            color = TextMutedColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // BOTÕES INFERIORES
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Botão preenchido "Confirmar que é meu pet"
                    Button(
                        onClick = {
                            if (temVerificacaoPendente) {
                                showPendenteDialog = true
                            } else {
                                executarConfirmacao()
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ConfirmButtonBg,
                            contentColor = ConfirmButtonTextColor,
                            disabledContainerColor = ConfirmButtonBg.copy(alpha = 0.7f),
                            disabledContentColor = ConfirmButtonTextColor,
                        ),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = ConfirmButtonTextColor,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "Confirmar que é meu pet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ConfirmButtonTextColor,
                            )
                        }
                    }

                    // Botão outline "Denunciar anúncio"
                    OutlinedButton(
                        onClick = onDenunciarClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, DenounceBorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextMutedColor,
                        ),
                    ) {
                        Text(
                            text = "Denunciar anúncio",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMutedColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Diálogo "Verificação pendente"
        if (showPendenteDialog) {
            AlertDialog(
                onDismissRequest = { showPendenteDialog = false },
                title = {
                    Text(
                        text = "Verificação pendente",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextDarkColor,
                    )
                },
                text = {
                    Text(
                        text = "Você já enviou uma verificação para outro anúncio. Quer continuar mesmo assim?",
                        fontSize = 14.sp,
                        color = TextMutedColor,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPendenteDialog = false
                            executarConfirmacao()
                        },
                    ) {
                        Text(
                            text = "Continuar",
                            fontWeight = FontWeight.Bold,
                            color = ConfirmButtonBg,
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPendenteDialog = false },
                    ) {
                        Text(
                            text = "Cancelar",
                            color = TextMutedColor,
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

/**
 * Preview da tela de Detalhes do Match.
 */
@Preview(showBackground = true)
@Composable
fun DetalhesMatchScreenPreview() {
    MaterialTheme {
        DetalhesMatchScreen(
            nomeLocal = "Praça Central",
            matchPercentage = 91,
            rua = "Avenida Paulista",
            bairro = "Bela Vista",
            pontosReferencia = listOf(
                "Próximo à Padaria" to Icons.Default.Storefront,
                "Em frente à Escola" to Icons.Default.School,
            ),
            diasAchado = 2,
            distanciaKm = 1.1,
            caracteristicas = listOf(
                "Porte médio",
                "Cor caramelo",
                "Coleira vermelha",
                "Orelha esquerda caída",
            ),
        )
    }
}
