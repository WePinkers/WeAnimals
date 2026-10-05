package com.example.weanimals.lostandfound.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Cores base da tela
private val ScreenBackground = Color(0xFFF7F2E7)
private val TopTitleColor = Color(0xFF1A1A18)
private val ExplanatoryTextColor = Color(0xFF6B6A63)
private val FieldLabelColor = Color(0xFF1A1A18)
private val FieldBgColor = Color(0xFFFFFDF7)
private val BorderColor = Color(0xFFD8D3C4)
private val PlaceholderColor = Color(0xFF8C8880)
private val DashedBorderColor = Color(0xFFB9B4A2)
private val CameraIconColor = Color(0xFF8C8880)
private val ButtonBgColor = Color(0xFFB45C33)
private val ButtonTextColor = Color(0xFFFFF8ED)
private val FooterTextColor = Color(0xFF8C8880)

/**
 * Tela de Verificação de Dono.
 */
@Composable
fun VerificacaoDonoScreen(
    onBackClick: () -> Unit = {},
    onEnviarClick: (nomePet: String, marcaCaracteristica: String, fotoUri: Uri?) -> Unit = { _, _, _ -> },
) {
    var nomePet by remember { mutableStateOf("") }
    var nomePetError by remember { mutableStateOf(false) }

    var marcaCaracteristica by remember { mutableStateOf("") }
    var marcaCaracteristicaError by remember { mutableStateOf(false) }

    var fotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { fotoUri = it }
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
            // TOPO: TopAppBar transparente com seta de voltar e título
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
                    text = "Verificação de dono",
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
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {

                // TEXTO EXPLICATIVO
                Text(
                    text = "Responda para confirmar que este é o seu pet. Suas respostas são vistas só por quem encontrou o animal, para evitar contatos falsos.",
                    fontSize = 13.sp,
                    color = ExplanatoryTextColor,
                    lineHeight = 19.sp,
                    fontFamily = FontFamily.SansSerif,
                )

                // CAMPO 1: Nome do pet
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "1. Qual é o nome do seu pet?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FieldLabelColor,
                    )

                    OutlinedTextField(
                        value = nomePet,
                        onValueChange = {
                            nomePet = it
                            if (it.isNotBlank()) nomePetError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Ex: Toby",
                                fontSize = 13.sp,
                                color = PlaceholderColor,
                            )
                        },
                        isError = nomePetError,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FieldBgColor,
                            unfocusedContainerColor = FieldBgColor,
                            errorContainerColor = FieldBgColor,
                            focusedBorderColor = TopTitleColor,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TopTitleColor,
                            unfocusedTextColor = TopTitleColor,
                        ),
                    )

                    if (nomePetError) {
                        Text(
                            text = "Preencha este campo",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }

                // CAMPO 2: Marca, cicatriz ou acessório
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "2. Alguma marca, cicatriz ou acessório que só você saberia?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FieldLabelColor,
                    )

                    OutlinedTextField(
                        value = marcaCaracteristica,
                        onValueChange = {
                            marcaCaracteristica = it
                            if (it.isNotBlank()) marcaCaracteristicaError = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        placeholder = {
                            Text(
                                text = "Ex: mancha em formato de coração na pata",
                                fontSize = 13.sp,
                                color = PlaceholderColor,
                            )
                        },
                        isError = marcaCaracteristicaError,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FieldBgColor,
                            unfocusedContainerColor = FieldBgColor,
                            errorContainerColor = FieldBgColor,
                            focusedBorderColor = TopTitleColor,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TopTitleColor,
                            unfocusedTextColor = TopTitleColor,
                        ),
                    )

                    if (marcaCaracteristicaError) {
                        Text(
                            text = "Preencha este campo",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }

                // CAMPO 3: Foto de comprovação (opcional)
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "3. Foto de comprovação (opcional)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FieldLabelColor,
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FieldBgColor)
                            .drawDashedBorder(color = DashedBorderColor, cornerRadiusDp = 10)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (fotoUri != null) {
                            AsyncImage(
                                model = fotoUri,
                                contentDescription = "Foto de comprovação",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Adicionar foto",
                                    tint = CameraIconColor,
                                    modifier = Modifier.size(28.dp),
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Adicionar uma foto antiga do seu pet",
                                    fontSize = 12.sp,
                                    color = CameraIconColor,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // BOTÃO DE ENVIO
                Button(
                    onClick = {
                        val isNomeValido = nomePet.isNotBlank()
                        val isMarcaValida = marcaCaracteristica.isNotBlank()

                        nomePetError = !isNomeValido
                        marcaCaracteristicaError = !isMarcaValida

                        if (isNomeValido && isMarcaValida) {
                            onEnviarClick(
                                nomePet.trim(),
                                marcaCaracteristica.trim(),
                                fotoUri,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonBgColor,
                        contentColor = ButtonTextColor,
                    ),
                ) {
                    Text(
                        text = "Enviar e liberar contato",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ButtonTextColor,
                    )
                }

                // RODAPÉ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Cadeado",
                        tint = FooterTextColor,
                        modifier = Modifier.size(14.dp),
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Seu contato só é compartilhado depois que a pessoa aceitar as respostas.",
                        fontSize = 11.sp,
                        color = FooterTextColor,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Modifier para desenhar borda tracejada.
 */
private fun Modifier.drawDashedBorder(color: Color, cornerRadiusDp: Int): Modifier = this.drawWithContent {
    drawContent()
    val strokeWidth = 1.5.dp.toPx()
    val dashWidth = 6.dp.toPx()
    val gapWidth = 5.dp.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, gapWidth), 0f)

    drawRoundRect(
        color = color,
        style = Stroke(width = strokeWidth, pathEffect = pathEffect),
        cornerRadius = CornerRadius(cornerRadiusDp.dp.toPx()),
    )
}

/**
 * Preview da tela de Verificação de Dono.
 */
@Preview(showBackground = true)
@Composable
fun VerificacaoDonoScreenPreview() {
    MaterialTheme {
        VerificacaoDonoScreen()
    }
}
