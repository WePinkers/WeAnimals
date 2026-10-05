package com.example.weanimals.community.campaign.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.campaign.domain.CommunityCampaignDraft
import com.example.weanimals.community.campaign.repository.CommunityCampaignRepository
import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.databinding.ActivityCreateCommunityCampaignBinding
import com.example.weanimals.databinding.ItemCampaignAnimalInputBinding
import com.example.weanimals.reporting.locationsearch.domain.LocationDetails
import com.example.weanimals.reporting.locationsearch.presentation.LocationSearchActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import com.google.android.material.chip.Chip
import java.math.BigDecimal
import java.math.RoundingMode

class CreateCommunityCampaignActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateCommunityCampaignBinding
    private val repository: CommunityCampaignRepository by lazy {
        (application as WeAnimalsApplication).appContainer.createCommunityCampaignRepository()
    }
    private var selectedCategory = CommunityCategory.NEUTERING
    private var selectedLocation: LocationDetails? = null
    private var locationRequestStarted = false
    private val animalRows = mutableListOf<AnimalInputRow>()
    private var photoRowIndex = -1

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        val row = animalRows.getOrNull(photoRowIndex) ?: return@registerForActivityResult
        row.photoUri = uri
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Some document providers grant temporary access only.
        }
        row.binding.animalPhotoPreview.setImageURI(uri)
        row.binding.animalPhotoPreview.isVisible = true
        row.binding.animalPhotoPlaceholder.isVisible = false
    }

    private val locationSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val address = data.getStringExtra(LocationSearchActivity.EXTRA_ADDRESS)
            ?.takeIf(String::isNotBlank) ?: return@registerForActivityResult
        showLocation(
            LocationDetails(
                latitude = data.getDoubleExtra(LocationSearchActivity.EXTRA_LATITUDE, Double.NaN),
                longitude = data.getDoubleExtra(LocationSearchActivity.EXTRA_LONGITUDE, Double.NaN),
                address = address,
                secondaryAddress = data.getStringExtra(
                    LocationSearchActivity.EXTRA_SECONDARY_ADDRESS
                ).orEmpty()
            )
        )
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) checkLocationSettingsAndLoad()
        else showLocationUnavailable(R.string.location_permission_required)
    }

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) loadCurrentLocation()
        else showLocationUnavailable(R.string.location_unavailable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityCreateCommunityCampaignBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.campaignHeader.headerTitle.setText(R.string.community_create_campaign_title)
        binding.campaignHeader.backButton.setOnClickListener { finish() }
        bindCategoryChips()
        binding.locationCard.setOnClickListener {
            locationSearchLauncher.launch(
                Intent(this, LocationSearchActivity::class.java).putExtra(
                    LocationSearchActivity.EXTRA_INITIAL_QUERY,
                    selectedLocation?.address.orEmpty()
                )
            )
        }
        binding.publishCampaignButton.setOnClickListener { publish() }
    }

    override fun onStart() {
        super.onStart()
        if (!locationRequestStarted && selectedLocation == null) {
            locationRequestStarted = true
            if (hasLocationPermission()) checkLocationSettingsAndLoad()
            else locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun bindCategoryChips() {
        categoryChips().forEach { (chip, category) ->
            chip.setOnClickListener {
                selectedCategory = category
                updateCategoryAppearance()
            }
        }
        updateCategoryAppearance()
        binding.addConfirmedAnimalButton.setOnClickListener { addAnimalRow() }
    }

    private fun updateCategoryAppearance() {
        categoryChips().forEach { (chip, category) ->
            val selected = category == selectedCategory
            chip.isChecked = selected
            chip.setChipBackgroundColorResource(if (selected) R.color.pine800 else R.color.surface)
            chip.setChipStrokeColorResource(R.color.border)
            chip.chipStrokeWidth = if (selected) 0f else resources.displayMetrics.density
            chip.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.ink900))
        }
        binding.confirmedAnimalsSection.isVisible = selectedCategory == CommunityCategory.ADOPTION
        binding.neuteringFields.isVisible = selectedCategory == CommunityCategory.NEUTERING
        binding.donationFields.isVisible = selectedCategory == CommunityCategory.DONATION
    }

    private fun publish() {
        val title = binding.titleEdit.text?.toString().orEmpty().trim()
        val date = binding.dateEdit.text?.toString().orEmpty().trim()
        val description = binding.descriptionEdit.text?.toString().orEmpty().trim()
        val location = selectedLocation
        val totalSlotsInput = binding.totalSlotsEdit.text?.toString().orEmpty().trim()
        val totalSlots = totalSlotsInput.takeIf(String::isNotBlank)?.toIntOrNull()
        val donationGoalInput = binding.donationGoalEdit.text?.toString().orEmpty().trim()
        val donationGoalCents = donationGoalInput.takeIf(String::isNotBlank)?.let(::parseMoneyCents)
        val pixKey = binding.pixKeyEdit.text?.toString().orEmpty().trim()
        when {
            title.isBlank() -> showError(R.string.community_campaign_required_title, binding.titleEdit)
            date.isBlank() -> showError(R.string.community_campaign_required_date, binding.dateEdit)
            location == null -> showError(R.string.community_campaign_required_location, binding.locationCard)
            description.isBlank() -> showError(
                R.string.community_campaign_required_description,
                binding.descriptionEdit
            )
            selectedCategory == CommunityCategory.NEUTERING
                && totalSlotsInput.isNotBlank()
                && (totalSlots == null || totalSlots !in 1..100000) -> {
                showError(R.string.community_campaign_invalid_slots, binding.totalSlotsEdit)
            }
            selectedCategory == CommunityCategory.DONATION
                && donationGoalInput.isBlank() -> {
                showError(
                    R.string.community_campaign_required_donation_goal,
                    binding.donationGoalEdit
                )
            }
            selectedCategory == CommunityCategory.DONATION
                && donationGoalInput.isNotBlank()
                && donationGoalCents == null -> {
                showError(
                    R.string.community_campaign_invalid_donation_goal,
                    binding.donationGoalEdit
                )
            }
            selectedCategory == CommunityCategory.DONATION && pixKey.isBlank() -> {
                showError(R.string.community_campaign_required_pix_key, binding.pixKeyEdit)
            }
            selectedCategory == CommunityCategory.ADOPTION && animalRows.any { it.name().isBlank() } -> {
                val invalidRow = animalRows.first { it.name().isBlank() }
                showError(R.string.community_campaign_animal_required_name, invalidRow.binding.animalNameEdit)
            }
            else -> submit(
                CommunityCampaignDraft(
                    category = selectedCategory,
                    title = title,
                    dateText = date,
                    location = location,
                    availabilityText = binding.availabilityEdit.text?.toString().orEmpty(),
                    audienceText = binding.audienceEdit.text?.toString().orEmpty(),
                    description = description,
                    highlightText = binding.highlightEdit.text?.toString().orEmpty(),
                    confirmedAnimals = if (selectedCategory == CommunityCategory.ADOPTION) {
                        animalRows.map { it.toDraft() }
                    } else {
                        emptyList()
                    },
                    totalSlots = if (selectedCategory == CommunityCategory.NEUTERING) totalSlots else null,
                    donationGoalCents = if (selectedCategory == CommunityCategory.DONATION) {
                        donationGoalCents
                    } else {
                        null
                    },
                    donationNeeds = if (selectedCategory == CommunityCategory.DONATION) {
                        binding.donationNeedsEdit.text?.toString().orEmpty()
                            .lines()
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .take(20)
                    } else {
                        emptyList()
                    },
                    pixKey = if (selectedCategory == CommunityCategory.DONATION) pixKey else ""
                )
            )
        }
    }

    private fun addAnimalRow() {
        val rowBinding = ItemCampaignAnimalInputBinding.inflate(
            layoutInflater,
            binding.confirmedAnimalsContainer,
            false
        )
        val row = AnimalInputRow(rowBinding)
        animalRows += row
        binding.confirmedAnimalsContainer.addView(rowBinding.root)
        rowBinding.animalPhotoDropzone.setOnClickListener {
            photoRowIndex = animalRows.indexOf(row)
            photoPicker.launch(arrayOf("image/*"))
        }
        rowBinding.removeAnimalButton.setOnClickListener {
            animalRows.remove(row)
            binding.confirmedAnimalsContainer.removeView(rowBinding.root)
        }
        rowBinding.animalNameEdit.requestFocus()
    }

    private fun parseMoneyCents(value: String): Long? = runCatching {
        val normalized = value
            .replace("R$", "", ignoreCase = true)
            .replace(" ", "")
            .let { if (it.contains(',')) it.replace(".", "").replace(',', '.') else it }
        BigDecimal(normalized)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
            .takeIf { it in 1..10_000_000_000L }
    }.getOrNull()

    private fun submit(draft: CommunityCampaignDraft) {
        binding.publishCampaignButton.isEnabled = false
        binding.publishCampaignButton.setText(R.string.community_campaign_publishing)
        lifecycleScope.launch {
            repository.publish(draft).fold(
                onSuccess = {
                    Toast.makeText(
                        this@CreateCommunityCampaignActivity,
                        R.string.community_campaign_published,
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                },
                onFailure = { error ->
                    Log.e(TAG, "Could not publish campaign", error)
                    binding.publishCampaignButton.isEnabled = true
                    binding.publishCampaignButton.setText(R.string.community_campaign_publish)
                    binding.formError.setText(R.string.community_campaign_publish_error)
                    binding.formError.visibility = View.VISIBLE
                }
            )
        }
    }

    private fun showError(message: Int, focusView: View) {
        binding.formError.setText(message)
        binding.formError.visibility = View.VISIBLE
        focusView.requestFocus()
        binding.campaignCreateScroll.post {
            binding.campaignCreateScroll.smoothScrollTo(0, focusView.top)
        }
    }

    private fun showLocation(location: LocationDetails) {
        if (!location.latitude.isFinite() || !location.longitude.isFinite()) return
        selectedLocation = location
        binding.locationTitle.setText(R.string.current_location)
        binding.locationAddress.text = listOf(location.address, location.secondaryAddress)
            .filter(String::isNotBlank)
            .joinToString(" — ")
    }

    private fun showLocationUnavailable(message: Int) {
        binding.locationTitle.setText(R.string.location_detecting)
        binding.locationAddress.setText(message)
    }

    private fun checkLocationSettingsAndLoad() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10_000L
        ).setMinUpdateIntervalMillis(5_000L).build()
        LocationServices.getSettingsClient(this)
            .checkLocationSettings(LocationSettingsRequest.Builder().addLocationRequest(request).build())
            .addOnSuccessListener { loadCurrentLocation() }
            .addOnFailureListener { error ->
                if (error is ResolvableApiException
                    && error.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED
                ) {
                    locationSettingsLauncher.launch(
                        IntentSenderRequest.Builder(error.resolution).build()
                    )
                } else showLocationUnavailable(R.string.location_unavailable)
            }
    }

    private fun loadCurrentLocation() {
        lifecycleScope.launch {
            (application as WeAnimalsApplication).appContainer
                .getCurrentLocationForCommunity()
                .invoke()
                .fold(
                    onSuccess = ::showLocation,
                    onFailure = { showLocationUnavailable(R.string.location_unavailable) }
                )
        }
    }

    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private fun categoryChips(): List<Pair<Chip, CommunityCategory>> = listOf(
        binding.categoryNeutering to CommunityCategory.NEUTERING,
        binding.categoryVaccination to CommunityCategory.VACCINATION,
        binding.categoryAdoption to CommunityCategory.ADOPTION,
        binding.categoryDonation to CommunityCategory.DONATION
    )

    private class AnimalInputRow(
        val binding: ItemCampaignAnimalInputBinding,
        var photoUri: Uri? = null
    ) {
        fun name() = binding.animalNameEdit.text?.toString().orEmpty().trim()

        fun toDraft(): com.example.weanimals.community.campaign.domain.CommunityCampaignAnimalDraft {
            val meta = binding.animalMetaEdit.text?.toString().orEmpty().trim()
            val parts = meta.split("·", limit = 2).map(String::trim)
            return com.example.weanimals.community.campaign.domain.CommunityCampaignAnimalDraft(
                name = name(),
                breed = parts.firstOrNull().orEmpty(),
                ageText = parts.getOrNull(1).orEmpty(),
                photoUri = photoUri?.toString()
            )
        }
    }

    companion object {
        private const val TAG = "CreateCommunityCampaign"
    }
}
