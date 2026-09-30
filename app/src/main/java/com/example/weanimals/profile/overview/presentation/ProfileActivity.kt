package com.example.weanimals.profile.overview.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.ActivityProfileBinding
import com.example.weanimals.lostandfound.presentation.ReportLostPetActivity
import com.example.weanimals.profile.campaigns.presentation.CampaignsActivity
import com.example.weanimals.profile.favorites.presentation.FavoritesActivity
import com.example.weanimals.profile.overview.presenter.ProfileContract
import com.example.weanimals.profile.presentation.EditarPerfilBottomSheet
import com.example.weanimals.profile.presentation.LostAndFoundSection
import com.example.weanimals.profile.presentation.OpcoesFotoBottomSheet
import com.example.weanimals.profile.presentation.ProfileHeaderCard
import com.example.weanimals.profile.reports.presentation.MyReportsActivity
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity(), ProfileContract.View {
    private lateinit var binding: ActivityProfileBinding
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createProfilePresenter()
    }

    private var nomeAtual by mutableStateOf("Usuario")
    private var bioAtual by mutableStateOf<String?>(null)
    private var fotoUrlState by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.background)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MainNavigation.bind(this, binding.mainNavigation, MainNavigation.Destination.PROFILE)

        binding.profileContent.composeProfileHeader.setContent {
            var mostrarEdicaoPerfil by remember { mutableStateOf(false) }
            var mostrarOpcoesFoto by remember { mutableStateOf(false) }

            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()

            Box(modifier = Modifier.fillMaxWidth()) {
                ProfileHeaderCard(
                    nome = nomeAtual,
                    fotoUrl = fotoUrlState,
                    bio = bioAtual,
                    onEditarClick = { mostrarEdicaoPerfil = true },
                    onEditarFotoClick = { mostrarOpcoesFoto = true },
                )

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = Color(0xFF1C3322),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                if (mostrarEdicaoPerfil) {
                    EditarPerfilBottomSheet(
                        nomeAtual = nomeAtual,
                        bioAtual = bioAtual,
                        fotoUrl = fotoUrlState,
                        onDismissRequest = { mostrarEdicaoPerfil = false },
                        onSalvarClick = { novoNome, novaBio ->
                            nomeAtual = novoNome
                            bioAtual = novaBio
                            mostrarEdicaoPerfil = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Perfil atualizado")
                            }
                        },
                        onEditarFotoClick = {
                            mostrarEdicaoPerfil = false
                            mostrarOpcoesFoto = true
                        },
                        onCancelar = { mostrarEdicaoPerfil = false },
                    )
                }

                if (mostrarOpcoesFoto) {
                    OpcoesFotoBottomSheet(
                        temFotoCustomizada = !fotoUrlState.isNullOrEmpty(),
                        onDismissRequest = { mostrarOpcoesFoto = false },
                        onTirarFotoClick = {
                            mostrarOpcoesFoto = false
                        },
                        onEscolherGaleriaClick = {
                            mostrarOpcoesFoto = false
                        },
                        onRemoverFotoClick = {
                            fotoUrlState = null
                            mostrarOpcoesFoto = false
                        },
                        onCancelar = { mostrarOpcoesFoto = false },
                    )
                }
            }
        }

        binding.profileContent.itemMyReports.root.setOnClickListener {
            startActivity(Intent(this, MyReportsActivity::class.java))
        }
        binding.profileContent.itemCampaigns.root.setOnClickListener {
            startActivity(Intent(this, CampaignsActivity::class.java))
        }
        binding.profileContent.itemFavorites.root.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
        binding.profileContent.composeLostAndFound.setContent {
            LostAndFoundSection {
                startActivity(Intent(this, ReportLostPetActivity::class.java))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.start()
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun showIdentity(name: String?) {
        if (!name.isNullOrBlank()) {
            nomeAtual = name
        }
    }

    override fun showReportCount(count: Int) {
        binding.profileContent.reportCount.text = count.toString()
    }

    override fun showReportsError() {
        binding.profileContent.reportCount.text = "—"
    }
}
