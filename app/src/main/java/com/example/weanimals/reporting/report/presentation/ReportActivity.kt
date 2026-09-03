package com.example.weanimals.reporting.report.presentation

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import com.example.weanimals.databinding.ActivityReportBinding
import com.example.weanimals.reporting.locationsearch.domain.LocationDetails
import com.example.weanimals.reporting.locationsearch.presentation.LocationSearchActivity
import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.report.domain.ReportDraft
import com.example.weanimals.reporting.report.presenter.ReportContract
import com.example.weanimals.reporting.report.presenter.ReportPresenter
import com.example.weanimals.reporting.triage.presentation.TriageActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton

class ReportActivity : AppCompatActivity(), ReportContract.View {

    private enum class Urgency(
        @param:androidx.annotation.ColorRes val selectedBackgroundRes: Int,
        @param:androidx.annotation.ColorRes val accentColorRes: Int
    ) {
        LOW(R.color.low_urgency_light, R.color.low_urgency),
        MEDIUM(R.color.medium_urgency_light, R.color.gold),
        HIGH(R.color.soft_red, R.color.red)
    }

    private lateinit var binding: ActivityReportBinding
    private val presenter: ReportPresenter by lazy {
        (application as WeAnimalsApplication).appContainer.createReportPresenter()
    }
    private var selectedPhotoUri: Uri? = null
    private var locationRequestStarted = false

    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { selectedPhoto: Uri? ->
        selectedPhoto ?: return@registerForActivityResult
        selectedPhotoUri = selectedPhoto
        persistPhotoPermission(selectedPhoto)
        showSelectedPhoto(selectedPhoto)
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
                latitude = data.getDoubleExtra(LocationSearchActivity.EXTRA_LATITUDE, 0.0),
                longitude = data.getDoubleExtra(LocationSearchActivity.EXTRA_LONGITUDE, 0.0),
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
        ) {
            checkLocationSettingsAndLoad()
        } else {
            presenter.onLocationPermissionDenied()
        }
    }

    private val locationSettingsResolutionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) presenter.loadCurrentLocation()
        else presenter.onLocationUnavailable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.reportHeader.headerTitle.setText(R.string.report_title)

        savedInstanceState?.getString(STATE_SELECTED_PHOTO_URI)
            ?.let(Uri::parse)
            ?.also { uri ->
                selectedPhotoUri = uri
                showSelectedPhoto(uri)
            }

        binding.descriptionEdit.doAfterTextChanged { description ->
            if (!description.isNullOrBlank()) binding.descriptionEdit.error = null
        }
        setupInteractions()
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        if (!locationRequestStarted) {
            locationRequestStarted = true
            requestLocationIfNeeded()
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

    private fun setupInteractions() {
        binding.reportHeader.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.photoDropzone.setOnClickListener { photoPicker.launch(arrayOf("image/*")) }
        binding.locationCard.setOnClickListener {
            locationSearchLauncher.launch(
                Intent(this, LocationSearchActivity::class.java).putExtra(
                    LocationSearchActivity.EXTRA_INITIAL_QUERY,
                    currentLocationSearchQuery()
                )
            )
        }
        binding.dogButton.setOnClickListener { presenter.selectAnimalType(Report.ANIMAL_DOG) }
        binding.catButton.setOnClickListener { presenter.selectAnimalType(Report.ANIMAL_CAT) }
        binding.otherButton.setOnClickListener { presenter.selectAnimalType(Report.ANIMAL_OTHER) }
        binding.urgencyLowButton.setOnClickListener { presenter.selectUrgency(Report.URGENCY_LOW) }
        binding.urgencyMediumButton.setOnClickListener { presenter.selectUrgency(Report.URGENCY_MEDIUM) }
        binding.urgencyHighButton.setOnClickListener { presenter.selectUrgency(Report.URGENCY_HIGH) }
        binding.continueScreeningButton.setOnClickListener {
            presenter.continueToTriage(
                description = binding.descriptionEdit.text?.toString().orEmpty(),
                photoUri = selectedPhotoUri?.toString()
            )
        }
    }

    override fun showAnimalTypeSelected(animalType: String) {
        when (animalType) {
            Report.ANIMAL_CAT -> selectAnimalType(binding.catButton, binding.dogButton, binding.otherButton)
            Report.ANIMAL_OTHER -> selectAnimalType(binding.otherButton, binding.dogButton, binding.catButton)
            else -> selectAnimalType(binding.dogButton, binding.catButton, binding.otherButton)
        }
    }

    override fun showUrgencySelected(urgency: String) {
        when (urgency) {
            Report.URGENCY_LOW -> selectUrgency(
                Urgency.LOW,
                binding.urgencyLowButton,
                binding.urgencyMediumButton to Urgency.MEDIUM,
                binding.urgencyHighButton to Urgency.HIGH
            )
            Report.URGENCY_MEDIUM -> selectUrgency(
                Urgency.MEDIUM,
                binding.urgencyMediumButton,
                binding.urgencyLowButton to Urgency.LOW,
                binding.urgencyHighButton to Urgency.HIGH
            )
            else -> selectUrgency(
                Urgency.HIGH,
                binding.urgencyHighButton,
                binding.urgencyLowButton to Urgency.LOW,
                binding.urgencyMediumButton to Urgency.MEDIUM
            )
        }
    }

    override fun showLocationLoading() {
        binding.locationTitle.setText(R.string.location_detecting)
        binding.locationAddress.setText(R.string.location_loading)
    }

    override fun showLocation(location: LocationDetails) {
        binding.locationTitle.setText(R.string.current_location)
        binding.locationAddress.text = listOf(location.address, location.secondaryAddress)
            .filter(String::isNotBlank)
            .joinToString(" — ")
            .ifBlank {
            getString(R.string.location_address_unavailable)
        }
    }

    override fun showLocationPermissionRequired() {
        binding.locationTitle.setText(R.string.location_detecting)
        binding.locationAddress.setText(R.string.location_permission_required)
    }

    override fun showLocationUnavailable() {
        binding.locationTitle.setText(R.string.location_detecting)
        binding.locationAddress.setText(R.string.location_unavailable)
    }

    override fun setSubmitEnabled(enabled: Boolean) {
        binding.continueScreeningButton.isEnabled = enabled
    }

    override fun showDescriptionRequired() {
        binding.descriptionEdit.error = getString(R.string.description_required)
        binding.descriptionEdit.requestFocus()
    }

    override fun showLocationRequired() {
        Toast.makeText(this, R.string.location_required, Toast.LENGTH_LONG).show()
        if (hasLocationPermission()) checkLocationSettingsAndLoad() else requestLocationPermission()
    }

    override fun showTriage(draft: ReportDraft) {
        startActivity(TriageActivity.newIntent(this, draft))
    }

    private fun requestLocationIfNeeded() {
        if (hasLocationPermission()) checkLocationSettingsAndLoad()
        else requestLocationPermission()
    }

    private fun currentLocationSearchQuery(): String {
        val address = binding.locationAddress.text?.toString().orEmpty()
        return address
            .takeIf {
                it.isNotBlank()
                    && it != getString(R.string.location_loading)
                    && it != getString(R.string.location_unavailable)
                    && it != getString(R.string.location_permission_required)
                    && it != getString(R.string.location_address_unavailable)
            }
            ?.substringBefore(" —")
            .orEmpty()
    }

    private fun checkLocationSettingsAndLoad() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MILLIS
        ).setMinUpdateIntervalMillis(LOCATION_MIN_UPDATE_INTERVAL_MILLIS).build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()
        LocationServices.getSettingsClient(this)
            .checkLocationSettings(settingsRequest)
            .addOnSuccessListener { presenter.loadCurrentLocation() }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException
                    && exception.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED
                ) {
                    locationSettingsResolutionLauncher.launch(
                        IntentSenderRequest.Builder(exception.resolution).build()
                    )
                } else {
                    presenter.onLocationUnavailable()
                }
            }
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun showSelectedPhoto(uri: Uri) {
        binding.photoPreview.setImageURI(uri)
        binding.photoPreview.visibility = View.VISIBLE
        binding.photoIcon.visibility = View.GONE
        binding.photoPrompt.setText(R.string.photo_added)
    }

    private fun persistPhotoPermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Some providers do not expose persistable permissions.
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SELECTED_PHOTO_URI, selectedPhotoUri?.toString())
        super.onSaveInstanceState(outState)
    }

    private fun selectAnimalType(selected: MaterialButton, vararg others: MaterialButton) {
        styleAnimalButton(selected, true)
        others.forEach { styleAnimalButton(it, false) }
    }

    private fun styleAnimalButton(button: MaterialButton, selected: Boolean) {
        val backgroundColor = if (selected) R.color.ink else R.color.surface
        val foregroundColor = if (selected) R.color.white else R.color.muted
        val strokeColor = if (selected) R.color.ink else R.color.border
        button.backgroundTintList = colorStateList(backgroundColor)
        button.setTextColor(ContextCompat.getColor(this, foregroundColor))
        button.strokeColor = colorStateList(strokeColor)
    }

    private fun selectUrgency(
        urgency: Urgency,
        selected: MaterialButton,
        vararg others: Pair<MaterialButton, Urgency>
    ) {
        styleUrgencyButton(selected, urgency, true)
        others.forEach { (button, level) -> styleUrgencyButton(button, level, false) }
    }

    private fun styleUrgencyButton(button: MaterialButton, urgency: Urgency, selected: Boolean) {
        val backgroundColor = if (selected) urgency.selectedBackgroundRes else R.color.surface
        val textColor = if (selected) urgency.accentColorRes else R.color.muted
        val strokeColor = if (selected) urgency.accentColorRes else R.color.border
        button.backgroundTintList = colorStateList(backgroundColor)
        button.setTextColor(ContextCompat.getColor(this, textColor))
        button.strokeColor = colorStateList(strokeColor)
    }

    private fun colorStateList(colorRes: Int) =
        ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private companion object {
        const val STATE_SELECTED_PHOTO_URI = "selected_photo_uri"
        const val LOCATION_UPDATE_INTERVAL_MILLIS = 10_000L
        const val LOCATION_MIN_UPDATE_INTERVAL_MILLIS = 5_000L
    }
}
