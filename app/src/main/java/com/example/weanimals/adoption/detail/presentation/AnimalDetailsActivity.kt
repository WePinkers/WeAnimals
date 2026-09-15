package com.example.weanimals.adoption.detail.presentation

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.location.LocationManagerCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.adoption.confirmation.presentation.AdoptionConfirmationActivity
import com.example.weanimals.adoption.detail.domain.AnimalDetails
import com.example.weanimals.adoption.detail.domain.AnimalShareDocument
import com.example.weanimals.adoption.detail.domain.EnergyLevel
import com.example.weanimals.adoption.detail.presenter.AnimalDetailsContract
import com.example.weanimals.adoption.questionnaire.presentation.AdoptionQuestionnaireActivity
import com.example.weanimals.databinding.ActivityAnimalDetailsBinding
import com.example.weanimals.databinding.ItemAdoptionRequirementBinding
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.io.File
import java.util.Locale
import kotlin.math.max

class AnimalDetailsActivity : AppCompatActivity(), AnimalDetailsContract.View {

    private lateinit var binding: ActivityAnimalDetailsBinding
    private val photoAdapter = AnimalPhotoAdapter()
    private val animalId by lazy { intent.getStringExtra(EXTRA_ANIMAL_ID).orEmpty() }
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createAnimalDetailsPresenter(
            animalId = animalId
        )
    }
    private var requestedLocationPermission = false

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        presenter.onLocationAccessResult(canUseLocation())
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasLocationPermission()) useLocationOrOpenSettings()
        else presenter.onLocationAccessResult(false)
    }
    private val distanceFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
    private val photoPageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            showPhotoIndicator(photoAdapter.photoCount, position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityAnimalDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPhotoGallery()
        requestedLocationPermission = savedInstanceState?.getBoolean(
            STATE_LOCATION_PERMISSION_REQUESTED
        ) ?: false
        setupInteractions()
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_LOCATION_PERMISSION_REQUESTED, requestedLocationPermission)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        binding.photoPager.unregisterOnPageChangeCallback(photoPageCallback)
        binding.photoPager.adapter = null
        presenter.destroy()
        super.onDestroy()
    }

    override fun showLoading() {
        binding.contentRoot.isVisible = false
        binding.stateGroup.isVisible = true
        binding.stateProgress.isVisible = true
        binding.stateTitle.setText(R.string.adoption_detail_loading)
        binding.stateAction.isVisible = false
    }

    override fun showAnimal(details: AnimalDetails) {
        with(binding) {
            stateGroup.isVisible = false
            contentRoot.isVisible = true
            photoAdapter.submitPhotos(details.photoUrls, details.name)
            photoPager.setCurrentItem(0, false)
            showPhotoIndicator(details.photoUrls.size, 0)

            animalName.text = details.name
            val months = waitingMonths(details.createdAtMillis)
            waitingTime.text = resources.getQuantityString(
                R.plurals.adoption_detail_waiting_months,
                months,
                months
            )

            bindChip(breedChip, details.breed)
            bindChip(ageChip, details.ageText)
            bindChip(sizeChip, details.size)
            vaccinatedChip.isVisible = details.vaccinated
            neuteredChip.isVisible = details.neutered

            childrenCard.isVisible = details.goodWithChildren != null
            childrenText.setText(
                if (details.goodWithChildren == false) {
                    R.string.adoption_detail_not_good_with_children
                } else {
                    R.string.adoption_detail_good_with_children
                }
            )
            animalsCard.isVisible = details.goodWithOtherAnimals != null
            animalsText.setText(
                if (details.goodWithOtherAnimals == false) {
                    R.string.adoption_detail_not_good_with_animals
                } else {
                    R.string.adoption_detail_good_with_animals
                }
            )
            energyCard.isVisible = details.energyLevel != null
            energyText.setText(
                when (details.energyLevel) {
                    EnergyLevel.LOW -> R.string.adoption_detail_energy_low
                    EnergyLevel.MODERATE -> R.string.adoption_detail_energy_moderate
                    EnergyLevel.HIGH -> R.string.adoption_detail_energy_high
                    null -> R.string.adoption_detail_energy_moderate
                }
            )

            aboutLabel.isVisible = details.description.isNotBlank()
            aboutText.isVisible = details.description.isNotBlank()
            aboutLabel.text = getString(
                R.string.adoption_detail_about,
                details.name.uppercase(Locale.forLanguageTag("pt-BR"))
            )
            aboutText.text = details.description

            requirementsLabel.isVisible = details.adoptionRequirements.isNotEmpty()
            requirementsContainer.isVisible = details.adoptionRequirements.isNotEmpty()
            requirementsContainer.removeAllViews()
            details.adoptionRequirements.forEach { requirement ->
                val item = ItemAdoptionRequirementBinding.inflate(
                    layoutInflater,
                    requirementsContainer,
                    false
                )
                item.requirementText.text = requirement
                requirementsContainer.addView(item.root)
            }

            shelterGroup.isVisible = details.shelter != null
            details.shelter?.let { shelter ->
                shelterName.text = shelter.name
                shelterName.isVisible = shelter.name.isNotBlank()
                shelterAddress.text = shelter.address
                shelterAddress.isVisible = shelter.address.isNotBlank()
            }
        }
    }

    override fun showDistance(distanceKm: Double?, shelterAddress: String) {
        binding.distance.text = distanceKm?.let(::formatDistance)
        binding.distance.isVisible = distanceKm != null

        val address = shelterAddress.trim()
        binding.shelterAddress.text = when {
            address.isNotEmpty() && distanceKm != null -> getString(
                R.string.adoption_detail_shelter_address_distance,
                address,
                formatDistance(distanceKm)
            )
            address.isNotEmpty() -> address
            distanceKm != null -> getString(
                R.string.adoption_detail_shelter_distance,
                formatDistance(distanceKm)
            )
            else -> ""
        }
        binding.shelterAddress.isVisible = binding.shelterAddress.text.isNotEmpty()
    }

    override fun showError(error: Throwable) {
        Log.e(TAG, "Could not load animal details", error)
        binding.contentRoot.isVisible = false
        binding.stateGroup.isVisible = true
        binding.stateProgress.isVisible = false
        binding.stateTitle.setText(R.string.adoption_detail_error)
        binding.stateAction.isVisible = true
    }

    override fun showFavorite(favorite: Boolean) {
        binding.favoriteButton.setImageResource(
            if (favorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline
        )
        ImageViewCompat.setImageTintList(
            binding.favoriteButton,
            ColorStateList.valueOf(
                ContextCompat.getColor(this, if (favorite) R.color.terracotta else R.color.muted)
            )
        )
        binding.favoriteButton.contentDescription = getString(
            if (favorite) R.string.adoption_detail_remove_favorite
            else R.string.adoption_detail_add_favorite
        )
    }

    override fun showShareLoading(loading: Boolean) {
        binding.shareButton.isEnabled = !loading
        binding.shareButton.alpha = if (loading) DISABLED_ALPHA else ENABLED_ALPHA
        binding.shareButton.contentDescription = getString(
            if (loading) R.string.adoption_detail_share_generating
            else R.string.adoption_detail_share
        )
    }

    override fun shareAnimalDocument(document: AnimalShareDocument) {
        val file = File(document.absolutePath)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val animalName = binding.animalName.text.toString()
        val subject = getString(R.string.adoption_detail_share_subject, animalName)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = document.mimeType
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TITLE, subject)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(contentResolver, document.displayName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.adoption_detail_share)))
    }

    override fun showShareError(error: Throwable) {
        Log.e(TAG, "Could not generate animal PDF", error)
        Snackbar.make(
            binding.root,
            R.string.adoption_detail_share_error,
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun requestUserLocation() {
        when {
            hasLocationPermission() -> useLocationOrOpenSettings()
            requestedLocationPermission &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                locationSettingsLauncher.launch(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:$packageName".toUri()
                    )
                )
            }
            else -> {
                requestedLocationPermission = true
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }
    }

    override fun showAdoptionProfileLoading(loading: Boolean) {
        binding.adoptButton.isEnabled = !loading
        binding.adoptButton.setText(
            if (loading) R.string.adoption_profile_checking
            else R.string.adoption_detail_adopt
        )
    }

    override fun showAdoptionProfileError(error: Throwable) {
        Log.e(TAG, "Could not check adoption profile", error)
        Snackbar.make(
            binding.root,
            R.string.adoption_profile_check_error,
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun openAdoptionQuestionnaire(animalId: String) {
        startActivity(AdoptionQuestionnaireActivity.newIntent(this, animalId))
    }

    override fun openAdoptionConfirmation(animalId: String) {
        startActivity(AdoptionConfirmationActivity.newIntent(this, animalId))
    }

    override fun closeScreen() = finish()

    override fun showActionUnavailable() {
        Snackbar.make(binding.root, R.string.adoption_detail_future_action, Snackbar.LENGTH_SHORT).show()
    }

    private fun setupInteractions() = with(binding) {
        backButton.setOnClickListener { presenter.onBackClicked() }
        shareButton.setOnClickListener { presenter.onShareClicked() }
        favoriteButton.setOnClickListener { presenter.onFavoriteClicked() }
        conversationButton.setOnClickListener { presenter.onConversationClicked() }
        adoptButton.setOnClickListener { presenter.onAdoptClicked() }
        stateAction.setOnClickListener { presenter.onRetryClicked() }
    }

    private fun setupPhotoGallery() {
        binding.photoPager.adapter = photoAdapter
        binding.photoPager.registerOnPageChangeCallback(photoPageCallback)
    }

    private fun showPhotoIndicator(photoCount: Int, selectedPosition: Int) {
        binding.photoIndicator.isVisible = photoCount > 1
        binding.photoIndicator.removeAllViews()
        if (photoCount <= 1) return

        val activeWidth = resources.getDimensionPixelSize(
            R.dimen.adoption_detail_indicator_active_width
        )
        val activeHeight = resources.getDimensionPixelSize(
            R.dimen.adoption_detail_indicator_active_height
        )
        val inactiveSize = resources.getDimensionPixelSize(
            R.dimen.adoption_detail_indicator_inactive_size
        )
        val spacing = resources.getDimensionPixelSize(
            R.dimen.adoption_detail_indicator_spacing
        )
        repeat(photoCount) { position ->
            val selected = position == selectedPosition
            val indicator = View(this).apply {
                setBackgroundResource(
                    if (selected) R.drawable.bg_status_active_dot
                    else R.drawable.bg_status_pending_dot
                )
                layoutParams = LinearLayout.LayoutParams(
                    if (selected) activeWidth else inactiveSize,
                    if (selected) activeHeight else inactiveSize
                ).apply {
                    if (position > 0) marginStart = spacing
                }
            }
            binding.photoIndicator.addView(indicator)
        }
    }

    private fun bindChip(view: android.widget.TextView, value: String) {
        view.text = value
        view.isVisible = value.isNotBlank()
    }

    private fun waitingMonths(createdAtMillis: Long): Int {
        if (createdAtMillis <= 0L) return 1
        return max(1, ((System.currentTimeMillis() - createdAtMillis) / MONTH_MILLIS).toInt())
    }

    private fun formatDistance(distanceKm: Double): String =
        getString(R.string.adoption_distance, distanceFormat.format(distanceKm))

    private fun useLocationOrOpenSettings() {
        if (isLocationEnabled()) {
            presenter.onLocationAccessResult(true)
        } else {
            locationSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun canUseLocation() = hasLocationPermission() && isLocationEnabled()

    private fun hasLocationPermission() = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LocationManager::class.java) ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        private const val TAG = "AnimalDetailsActivity"
        private const val EXTRA_ANIMAL_ID = "extra_animal_id"
        private const val STATE_LOCATION_PERMISSION_REQUESTED = "location_permission_requested"
        private const val MONTH_MILLIS = 30L * 24 * 60 * 60 * 1000
        private const val ENABLED_ALPHA = 1f
        private const val DISABLED_ALPHA = 0.55f

        fun newIntent(context: Context, animalId: String) =
            Intent(context, AnimalDetailsActivity::class.java).apply {
                putExtra(EXTRA_ANIMAL_ID, animalId)
            }
    }
}
