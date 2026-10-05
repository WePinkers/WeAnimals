package com.example.weanimals.profile.overview.presentation

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
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
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.community.feed.repository.CommunityRepositoryFactory
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.DialogLogoutConfirmationBinding
import com.example.weanimals.databinding.ActivityProfileBinding
import com.example.weanimals.entry.presentation.EntryActivity
import com.example.weanimals.lostandfound.presentation.ReportLostPetActivity
import com.example.weanimals.profile.campaigns.presentation.CampaignsActivity
import com.example.weanimals.profile.favorites.presentation.FavoritesActivity
import com.example.weanimals.profile.overview.presenter.ProfileContract
import com.example.weanimals.profile.presentation.EditarPerfilBottomSheet
import com.example.weanimals.profile.presentation.LostAndFoundSection
import com.example.weanimals.profile.presentation.OpcoesFotoBottomSheet
import com.example.weanimals.profile.presentation.ProfileHeaderCard
import com.example.weanimals.profile.reports.presentation.MyReportsActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity(), ProfileContract.View {
    private lateinit var binding: ActivityProfileBinding
    private var logoutDialog: Dialog? = null
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createProfilePresenter()
    }
    private val communityRepository by lazy { CommunityRepositoryFactory.create() }

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
                        containerColor = ComposeColor(0xFF1C3322),
                        contentColor = ComposeColor.White,
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
        binding.profileContent.logoutButton.setOnClickListener {
            showLogoutConfirmation()
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
        loadCampaignSummary()
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

    private fun loadCampaignSummary() {
        lifecycleScope.launch {
            communityRepository.getFeed().fold(
                onSuccess = { items ->
                    val count = items
                        .filterIsInstance<CommunityFeedItem.Campaign>()
                        .count { it.participatingByCurrentUser }
                    binding.profileContent.campaignCount.text = count.toString()
                    binding.profileContent.itemCampaigns.campaignsDescription.text = if (count == 0) {
                        getString(R.string.profile_activity_campaigns_desc)
                    } else {
                        resources.getQuantityString(
                            R.plurals.profile_campaigns_participation_count,
                            count,
                            count
                        )
                    }
                },
                onFailure = {
                    binding.profileContent.campaignCount.text = "—"
                    binding.profileContent.itemCampaigns.campaignsDescription.setText(
                        R.string.profile_campaigns_count_error
                    )
                }
            )
        }
    }

    private fun showLogoutConfirmation() {
        logoutDialog?.dismiss()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogLogoutConfirmationBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)
        dialogBinding.dialogLogoutCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.dialogLogoutConfirm.setOnClickListener {
            dialog.dismiss()
            signOut()
        }
        dialog.setOnDismissListener {
            if (logoutDialog === dialog) logoutDialog = null
        }
        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.86f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                attributes = attributes.apply { dimAmount = 0.58f }
            }
        }
        logoutDialog = dialog
        dialog.show()
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
            .signOut()
            .addOnCompleteListener {
                openEntry()
            }
    }

    private fun openEntry() {
        startActivity(
            Intent(this, EntryActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
    }
}
