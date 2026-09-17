package com.example.weanimals.community.create.presentation

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
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.create.presenter.CreateCommunityPostContract
import com.example.weanimals.community.feed.domain.CommunityCategory
import com.example.weanimals.databinding.ActivityCreateCommunityPostBinding
import com.example.weanimals.reporting.locationsearch.domain.LocationDetails
import com.example.weanimals.reporting.locationsearch.presentation.LocationSearchActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.material.chip.Chip

class CreateCommunityPostActivity : AppCompatActivity(), CreateCommunityPostContract.View {
    private lateinit var binding: ActivityCreateCommunityPostBinding
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createCommunityPostPresenter()
    }
    private var selectedPhotoUri: Uri? = null
    private var selectedLocation: LocationDetails? = null
    private var locationRequestStarted = false

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        selectedPhotoUri = uri
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Some document providers grant temporary read access only.
        }
        showSelectedPhoto(uri)
    }

    private val locationSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val address = data.getStringExtra(LocationSearchActivity.EXTRA_ADDRESS)
            ?.takeIf(String::isNotBlank) ?: return@registerForActivityResult
        presenter.selectLocation(
            LocationDetails(
                latitude = data.getDoubleExtra(LocationSearchActivity.EXTRA_LATITUDE, Double.NaN),
                longitude = data.getDoubleExtra(LocationSearchActivity.EXTRA_LONGITUDE, Double.NaN),
                address = address,
                secondaryAddress = data.getStringExtra(LocationSearchActivity.EXTRA_SECONDARY_ADDRESS).orEmpty()
            )
        )
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) checkLocationSettingsAndLoad()
        else presenter.onLocationPermissionDenied()
    }

    private val locationSettingsResolutionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) presenter.loadCurrentLocation()
        else presenter.onLocationUnavailable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityCreateCommunityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.createHeader.headerTitle.setText(R.string.community_create_title)
        binding.createHeader.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        selectedPhotoUri = savedInstanceState?.getString(STATE_PHOTO_URI)?.let(Uri::parse)
        selectedPhotoUri?.let(::showSelectedPhoto)
        if (savedInstanceState?.containsKey(STATE_LATITUDE) == true) {
            val location = LocationDetails(
                latitude = savedInstanceState.getDouble(STATE_LATITUDE),
                longitude = savedInstanceState.getDouble(STATE_LONGITUDE),
                address = savedInstanceState.getString(STATE_ADDRESS).orEmpty(),
                secondaryAddress = savedInstanceState.getString(STATE_SECONDARY_ADDRESS).orEmpty()
            )
            selectedLocation = location
            presenter.selectLocation(location)
        }
        val category = savedInstanceState?.getString(STATE_CATEGORY)?.let {
            runCatching { CommunityCategory.valueOf(it) }.getOrNull()
        } ?: CommunityCategory.FOUND
        presenter.selectCategory(category)

        binding.campaignTab.setOnClickListener {
            Toast.makeText(this, R.string.community_campaign_ngo_only, Toast.LENGTH_LONG).show()
        }
        categoryChips().forEach { (chip, categoryValue) ->
            chip.setOnClickListener { presenter.selectCategory(categoryValue) }
        }
        binding.photoDropzone.setOnClickListener { photoPicker.launch(arrayOf("image/*")) }
        binding.locationCard.setOnClickListener {
            locationSearchLauncher.launch(
                Intent(this, LocationSearchActivity::class.java).putExtra(
                    LocationSearchActivity.EXTRA_INITIAL_QUERY,
                    selectedLocation?.address.orEmpty()
                )
            )
        }
        binding.storyEdit.doAfterTextChanged {
            if (!it.isNullOrBlank()) binding.storyError.visibility = View.GONE
        }
        binding.publishButton.setOnClickListener {
            presenter.publish(binding.storyEdit.text?.toString().orEmpty(), selectedPhotoUri?.toString())
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        if (!locationRequestStarted && selectedLocation == null) {
            locationRequestStarted = true
            if (hasLocationPermission()) checkLocationSettingsAndLoad()
            else requestLocationPermission()
        }
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_PHOTO_URI, selectedPhotoUri?.toString())
        selectedLocation?.let { location ->
            outState.putDouble(STATE_LATITUDE, location.latitude)
            outState.putDouble(STATE_LONGITUDE, location.longitude)
            outState.putString(STATE_ADDRESS, location.address)
            outState.putString(STATE_SECONDARY_ADDRESS, location.secondaryAddress)
        }
        outState.putString(STATE_CATEGORY, categoryChips().firstOrNull { it.first.isChecked }?.second?.name)
        super.onSaveInstanceState(outState)
    }

    override fun showCategorySelected(category: CommunityCategory) {
        categoryChips().forEach { (chip, option) ->
            val active = option == category
            chip.isChecked = active
            chip.setChipBackgroundColorResource(if (active) R.color.pine800 else R.color.surface)
            chip.setChipStrokeColorResource(R.color.border)
            chip.chipStrokeWidth = if (active) 0f else resources.displayMetrics.density
            chip.setTextColor(ContextCompat.getColor(this, if (active) R.color.white else R.color.ink900))
        }
    }

    override fun showLocationLoading() {
        binding.locationTitle.setText(R.string.location_detecting)
        binding.locationAddress.setText(R.string.location_loading)
    }

    override fun showLocation(location: LocationDetails) {
        selectedLocation = location
        binding.locationTitle.setText(R.string.current_location)
        binding.locationAddress.text = listOf(location.address, location.secondaryAddress)
            .filter(String::isNotBlank).joinToString(" — ")
            .ifBlank { getString(R.string.location_address_unavailable) }
    }

    override fun showLocationPermissionRequired() {
        binding.locationTitle.setText(R.string.location_detecting)
        binding.locationAddress.setText(R.string.location_permission_required)
    }

    override fun showLocationUnavailable() {
        binding.locationTitle.setText(R.string.location_detecting)
        binding.locationAddress.setText(R.string.location_unavailable)
    }

    override fun showDescriptionRequired() {
        binding.storyError.setText(R.string.community_story_required)
        binding.storyError.visibility = View.VISIBLE
        binding.storyEdit.requestFocus()
    }

    override fun showLocationRequired() {
        Toast.makeText(this, R.string.community_location_required, Toast.LENGTH_LONG).show()
    }

    override fun showSubmitting(submitting: Boolean) {
        binding.publishButton.isEnabled = !submitting
        binding.publishButton.setText(
            if (submitting) R.string.community_publishing else R.string.community_publish
        )
    }

    override fun showPublishError(error: Throwable) {
        Log.e(TAG, "Could not publish community post", error)
        Toast.makeText(this, R.string.community_publish_error, Toast.LENGTH_LONG).show()
    }

    override fun showPublished() {
        Toast.makeText(this, R.string.community_published, Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun categoryChips(): List<Pair<Chip, CommunityCategory>> = listOf(
        binding.categoryFound to CommunityCategory.FOUND,
        binding.categoryQuestion to CommunityCategory.QUESTION,
        binding.categoryNotice to CommunityCategory.NOTICE,
        binding.categoryOther to CommunityCategory.OTHER
    )

    private fun showSelectedPhoto(uri: Uri) {
        binding.photoPreview.setImageURI(uri)
        binding.photoPreview.visibility = View.VISIBLE
        binding.photoPromptContent.visibility = View.GONE
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    private fun checkLocationSettingsAndLoad() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L
        ).setMinUpdateIntervalMillis(5_000L).build()
        LocationServices.getSettingsClient(this)
            .checkLocationSettings(
                LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
            )
            .addOnSuccessListener { presenter.loadCurrentLocation() }
            .addOnFailureListener { error ->
                if (error is ResolvableApiException
                    && error.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED
                ) {
                    locationSettingsResolutionLauncher.launch(
                        IntentSenderRequest.Builder(error.resolution).build()
                    )
                } else presenter.onLocationUnavailable()
            }
    }

    private companion object {
        const val TAG = "CreateCommunityPost"
        const val STATE_PHOTO_URI = "community_photo_uri"
        const val STATE_LATITUDE = "community_location_latitude"
        const val STATE_LONGITUDE = "community_location_longitude"
        const val STATE_ADDRESS = "community_location_address"
        const val STATE_SECONDARY_ADDRESS = "community_location_secondary_address"
        const val STATE_CATEGORY = "community_post_category"
    }
}
