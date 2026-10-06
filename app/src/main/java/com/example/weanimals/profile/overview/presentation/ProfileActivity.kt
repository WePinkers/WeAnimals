package com.example.weanimals.profile.overview.presentation

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.community.feed.repository.CommunityRepositoryFactory
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.core.theme.ThemeManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.example.weanimals.databinding.ActivityProfileBinding
import com.example.weanimals.databinding.DialogEditProfileBinding
import com.example.weanimals.databinding.DialogLogoutConfirmationBinding
import com.example.weanimals.databinding.DialogPhotoOptionsBinding
import com.example.weanimals.entry.presentation.EntryActivity
import com.example.weanimals.lostandfound.presentation.ReportLostPetActivity
import com.example.weanimals.profile.campaigns.presentation.CampaignsActivity
import com.example.weanimals.profile.favorites.presentation.FavoritesActivity
import com.example.weanimals.profile.overview.presenter.ProfileContract
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

    private var nomeAtual = "Usuário"
    private var bioAtual: String? = null
    private var fotoUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> updatePhoto(uri) }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            binding.profileContent.profileHeader.profileAvatarImage.setImageBitmap(bitmap)
            binding.profileContent.profileHeader.profileAvatarImage.visibility = android.view.View.VISIBLE
            binding.profileContent.profileHeader.profileAvatarInitial.visibility = android.view.View.GONE
        }
    }

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
        setupProfileActions()
    }

    private fun setupProfileActions() {
        binding.profileContent.profileHeader.profileEdit.setOnClickListener {
            showEditProfileDialog()
        }
        binding.profileContent.profileHeader.profileBio.setOnClickListener {
            showEditProfileDialog()
        }
        binding.profileContent.profileHeader.profileEditPhoto.setOnClickListener {
            showPhotoOptions()
        }
        binding.profileContent.lostAndFoundSection.reportLostPet.setOnClickListener {
            startActivity(Intent(this, ReportLostPetActivity::class.java))
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

        val switchDarkMode = binding.profileContent.root.findViewById<SwitchMaterial>(R.id.switch_dark_mode)
        switchDarkMode?.isChecked = ThemeManager.isDarkModeEnabled(this)
        switchDarkMode?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != ThemeManager.isDarkModeEnabled(this)) {
                ThemeManager.setDarkModeEnabled(this, isChecked)
            }
        }

        binding.profileContent.logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }
        updateHeader()
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
            updateHeader()
        }
    }

    override fun showReportCount(count: Int) {
        binding.profileContent.reportCount.text = count.toString()
    }

    override fun showReportsError() {
        binding.profileContent.reportCount.text = "—"
    }

    private fun updateHeader() {
        binding.profileContent.profileHeader.profileName.text = nomeAtual
        binding.profileContent.profileHeader.profileAvatarInitial.text =
            nomeAtual.firstOrNull()?.uppercase() ?: "U"
        binding.profileContent.profileHeader.profileBio.text =
            bioAtual?.takeIf(String::isNotBlank) ?: "Toque para adicionar bio"
    }

    private fun updatePhoto(uri: Uri?) {
        fotoUri = uri
        val image = binding.profileContent.profileHeader.profileAvatarImage
        val initial = binding.profileContent.profileHeader.profileAvatarInitial
        if (uri == null) {
            image.setImageDrawable(null)
            image.visibility = android.view.View.GONE
            initial.visibility = android.view.View.VISIBLE
        } else {
            image.setImageURI(uri)
            image.visibility = android.view.View.VISIBLE
            initial.visibility = android.view.View.GONE
        }
    }

    private fun showEditProfileDialog() {
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        dialogBinding.editName.setText(nomeAtual)
        dialogBinding.editBio.setText(bioAtual.orEmpty())
        AlertDialog.Builder(this)
            .setTitle("Editar perfil")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                val newName = dialogBinding.editName.text.toString().trim()
                if (newName.isNotBlank()) nomeAtual = newName
                bioAtual = dialogBinding.editBio.text.toString().trim().takeIf(String::isNotBlank)
                updateHeader()
            }
            .show()
    }

    private fun showPhotoOptions() {
        val dialog = Dialog(this)
        val dialogBinding = DialogPhotoOptionsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setTitle("Foto do perfil")
        dialogBinding.takePhotoButton.setOnClickListener {
            dialog.dismiss()
            cameraLauncher.launch(null)
        }
        dialogBinding.choosePhotoButton.setOnClickListener {
            dialog.dismiss()
            galleryLauncher.launch("image/*")
        }
        dialogBinding.removePhotoButton.setOnClickListener {
            dialog.dismiss()
            updatePhoto(null)
        }
        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.WHITE))
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.86f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
        }
        dialog.show()
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
            .addOnCompleteListener { openEntry() }
    }

    private fun openEntry() {
        startActivity(
            Intent(this, EntryActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
    }
}
