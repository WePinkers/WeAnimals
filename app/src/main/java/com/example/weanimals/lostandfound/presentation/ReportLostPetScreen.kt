package com.example.weanimals.lostandfound.presentation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Palette de cores do app WeAnimals
private val BeigeBackground = Color(0xFFF7F2E7)
private val HeaderDarkGreen = Color(0xFF1C3322)
private val BackButtonBg = Color(0xFFEAE5D9)
private val SectionTitleGray = Color(0xFF6E6A63)
private val PrimaryRust = Color(0xFFB35D38)
private val BorderGray = Color(0xFFC7C2B8)
private val DividerGray = Color(0xFFE5E0D8)
private val ChipSelectedBg = Color(0xFF1C3322)
private val InputTextDark = Color(0xFF1C201D)
private val PlaceholderGray = Color(0xFF8C887F)

enum class AnimalSpecies { DOG, CAT }
enum class AnimalSize { SMALL, MEDIUM, LARGE }

/**
 * Tela "Perdi meu animal" para relatar e buscar matches de animais perdidos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportLostPetScreen(
    onBackClick: () -> Unit = {},
    onSearchMatchesClick: (
        species: AnimalSpecies,
        size: AnimalSize,
        description: String,
        date: String,
        location: String,
        photoUri: Uri?,
    ) -> Unit = { _, _, _, _, _, _ -> },
) {
    val context = LocalContext.current
    var selectedSpecies by remember { mutableStateOf(AnimalSpecies.DOG) }
    var selectedSize by remember { mutableStateOf(AnimalSize.MEDIUM) }
    var description by remember { mutableStateOf("") }
    var lastSeenDate by remember { mutableStateOf("25/08/2026") }
    var lastSeenLocation by remember { mutableStateOf("Vila Marlene") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { photoUri = it }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = BeigeBackground,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header Superior (Botão Voltar e Título)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Botão de Voltar circular
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
                    text = "Perdi meu animal",
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

            // Conteúdo Formulário (Com rolagem vertical)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {

                // 1. FOTO DO ANIMAL
                Column {
                    SectionLabel(text = "FOTO DO ANIMAL")

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .drawDashedBorder(color = BorderGray, cornerRadiusDp = 12)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Foto do animal",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Adicionar foto",
                                    tint = PrimaryRust,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Toque para adicionar foto",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = InputTextDark
                                )
                            }
                        }
                    }
                }

                // 2. ESPÉCIE E PORTE
                Column {
                    SectionLabel(text = "ESPÉCIE E PORTE")

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SelectableChip(
                            label = "Cão",
                            selected = selectedSpecies == AnimalSpecies.DOG
                        ) { selectedSpecies = AnimalSpecies.DOG }

                        SelectableChip(
                            label = "Gato",
                            selected = selectedSpecies == AnimalSpecies.CAT
                        ) { selectedSpecies = AnimalSpecies.CAT }

                        SelectableChip(
                            label = when (selectedSize) {
                                AnimalSize.SMALL -> "Pequeno porte"
                                AnimalSize.MEDIUM -> "Médio porte"
                                AnimalSize.LARGE -> "Grande porte"
                            },
                            selected = true
                        ) {
                            selectedSize = when (selectedSize) {
                                AnimalSize.SMALL -> AnimalSize.MEDIUM
                                AnimalSize.MEDIUM -> AnimalSize.LARGE
                                AnimalSize.LARGE -> AnimalSize.SMALL
                            }
                        }
                    }
                }

                // 3. NOME E CARACTERÍSTICAS MARCANTES
                Column {
                    SectionLabel(text = "NOME E CARACTERÍSTICAS MARCANTES")

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        placeholder = {
                            Text(
                                text = "Caramelo, coleira vermelha, mancha branca no peito, orelha esquerda caída...",
                                fontSize = 14.sp,
                                color = PlaceholderGray
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedBorderColor = HeaderDarkGreen,
                            unfocusedBorderColor = BorderGray,
                            focusedTextColor = InputTextDark,
                            unfocusedTextColor = InputTextDark
                        )
                    )
                }

                // 4. DATA E LOCAL ONDE FOI VISTO PELA ÚLTIMA VEZ
                Column {
                    SectionLabel(text = "DATA E LOCAL ONDE FOI VISTO PELA ÚLTIMA VEZ")

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Campo Data
                        OutlinedTextField(
                            value = lastSeenDate,
                            onValueChange = { lastSeenDate = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = HeaderDarkGreen,
                                unfocusedBorderColor = BorderGray,
                                focusedTextColor = InputTextDark,
                                unfocusedTextColor = InputTextDark
                            )
                        )

                        // Campo Local
                        OutlinedTextField(
                            value = lastSeenLocation,
                            onValueChange = { lastSeenLocation = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = HeaderDarkGreen,
                                unfocusedBorderColor = BorderGray,
                                focusedTextColor = InputTextDark,
                                unfocusedTextColor = InputTextDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. BOTÃO "Buscar possíveis matches"
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Buscando possíveis matches...",
                            Toast.LENGTH_SHORT
                        ).show()
                        onSearchMatchesClick(
                            selectedSpecies,
                            selectedSize,
                            description,
                            lastSeenDate,
                            lastSeenLocation,
                            photoUri
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryRust,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buscar possíveis matches",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Rotulo de seções em caixa alta com estilo padronizado.
 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        fontFamily = FontFamily.SansSerif,
        color = SectionTitleGray,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Chip customizado selecionável para espécies e portes.
 */
@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) ChipSelectedBg else Color.White)
            .border(
                width = 1.dp,
                color = if (selected) ChipSelectedBg else BorderGray,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else InputTextDark,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Modifier para desenhar borda tracejada em caixas.
 */
private fun Modifier.drawDashedBorder(color: Color, cornerRadiusDp: Int): Modifier = this.drawWithContent {
    drawContent()
    val strokeWidth = 2.dp.toPx()
    val dashWidth = 8.dp.toPx()
    val gapWidth = 6.dp.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, gapWidth), 0f)

    drawRoundRect(
        color = color,
        style = Stroke(width = strokeWidth, pathEffect = pathEffect),
        cornerRadius = CornerRadius(cornerRadiusDp.dp.toPx())
    )
}

/**
 * Preview da tela de relato/busca de animal perdido.
 */
@Preview(showBackground = true)
@Composable
fun ReportLostPetScreenPreview() {
    MaterialTheme {
        ReportLostPetScreen()
    }
}
