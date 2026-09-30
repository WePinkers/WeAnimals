package com.example.weanimals.lostandfound.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat

import androidx.compose.material3.ExperimentalMaterial3Api

enum class LostPetStep { FORM, MATCHES, DETAILS, VERIFICACAO, AGUARDANDO, DENUNCIA_ENVIADA }

class ReportLostPetActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            var currentStep by remember { mutableStateOf(LostPetStep.FORM) }
            var selectedSpecies by remember { mutableStateOf(AnimalSpecies.DOG) }
            var selectedMatchDetails by remember { mutableStateOf<PetMatchCardData?>(null) }
            var respostasEnviadas by remember { mutableStateOf<List<String>>(emptyList()) }
            var showDenunciarBottomSheet by remember { mutableStateOf(false) }

            when (currentStep) {
                LostPetStep.FORM -> {
                    ReportLostPetScreen(
                        onBackClick = { finish() },
                        onSearchMatchesClick = { species, _, _, _, _, _ ->
                            selectedSpecies = species
                            currentStep = LostPetStep.MATCHES
                        },
                    )
                }

                LostPetStep.MATCHES -> {
                    PetMatchesScreen(
                        species = selectedSpecies,
                        onBackClick = { currentStep = LostPetStep.FORM },
                        onViewDetailsClick = { match ->
                            selectedMatchDetails = match
                            currentStep = LostPetStep.DETAILS
                        },
                    )
                }

                LostPetStep.DETAILS -> {
                    val match = selectedMatchDetails
                    val locationTitle = match?.title?.replace("Encontrado — ", "") ?: "Praça Central"
                    val percentage = match?.matchPercentage ?: 91

                    DetalhesMatchScreen(
                        nomeLocal = locationTitle,
                        matchPercentage = percentage,
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
                        onBackClick = { currentStep = LostPetStep.MATCHES },
                        onConfirmarClick = {
                            currentStep = LostPetStep.VERIFICACAO
                        },
                        onDenunciarClick = {
                            showDenunciarBottomSheet = true
                        },
                    )

                    if (showDenunciarBottomSheet) {
                        DenunciarAnuncioBottomSheet(
                            onDismissRequest = { showDenunciarBottomSheet = false },
                            onCancelar = { showDenunciarBottomSheet = false },
                            onEnviarDenuncia = { _, _ ->
                                showDenunciarBottomSheet = false
                                currentStep = LostPetStep.DENUNCIA_ENVIADA
                            },
                        )
                    }
                }

                LostPetStep.VERIFICACAO -> {
                    VerificacaoDonoScreen(
                        onBackClick = { currentStep = LostPetStep.DETAILS },
                        onEnviarClick = { nomePet, marca, fotoUri ->
                            respostasEnviadas = buildList {
                                add("Nome: $nomePet")
                                add("Sinal: $marca")
                                if (fotoUri != null) {
                                    add("Foto de comprovação anexada")
                                }
                            }
                            currentStep = LostPetStep.AGUARDANDO
                        },
                    )
                }

                LostPetStep.AGUARDANDO -> {
                    AguardandoVerificacaoScreen(
                        respostas = respostasEnviadas,
                        onVoltarClick = {
                            currentStep = LostPetStep.MATCHES
                        },
                    )
                }

                LostPetStep.DENUNCIA_ENVIADA -> {
                    DenunciaEnviadaScreen(
                        onEntendiClick = {
                            currentStep = LostPetStep.MATCHES
                        },
                    )
                }
            }
        }
    }
}
