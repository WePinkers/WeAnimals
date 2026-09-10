package com.example.weanimals.adoption.listing.presentation

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.adoption.detail.presentation.AnimalDetailsActivity
import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.SpeciesFilter
import com.example.weanimals.adoption.listing.presenter.AdoptionContract
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.ActivityAdoptionBinding
import com.google.android.material.chip.Chip

class AdoptionActivity : AppCompatActivity(), AdoptionContract.View {
    private lateinit var binding: ActivityAdoptionBinding
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createAdoptionPresenter()
    }
    private val animalAdapter = AnimalAdapter { presenter.onPetClicked(it) }
    private var selectedFilter = SpeciesFilter.ALL
    private var updatingFilter = false
    private var requestedPermission = false

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { presenter.onLocationPermissionResult(hasLocationPermission()) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasLocationPermission()) useLocationOrOpenSettings()
        else presenter.onLocationPermissionResult(false)
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy >= 0) loadMoreIfNeeded()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAdoptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
        selectedFilter = SpeciesFilter.entries.firstOrNull {
            it.name == savedInstanceState?.getString(STATE_FILTER)
        } ?: SpeciesFilter.ALL
        requestedPermission = savedInstanceState?.getBoolean(STATE_PERMISSION_REQUESTED) ?: false
        binding.animalsList.adapter = animalAdapter
        binding.animalsList.addOnScrollListener(scrollListener)
        binding.filterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (!updatingFilter) checkedIds.firstOrNull()?.let { presenter.onFilterSelected(filterForId(it)) }
        }
        binding.stateAction.setOnClickListener { presenter.onRetryClicked() }
        binding.pageRetry.setOnClickListener { presenter.onRetryClicked() }
        MainNavigation.bind(this, binding.mainNavigation, MainNavigation.Destination.ADOPTION)
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.start(selectedFilter)
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_FILTER, selectedFilter.name)
        outState.putBoolean(STATE_PERMISSION_REQUESTED, requestedPermission)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        binding.animalsList.removeOnScrollListener(scrollListener)
        binding.animalsList.adapter = null
        presenter.destroy()
        super.onDestroy()
    }

    override fun showAnimals(animals: List<Animal>) {
        binding.emptyState.isVisible = false
        binding.loadingState.isVisible = false
        binding.animalsList.isVisible = true
        animalAdapter.showAnimals(animals, ::checkAfterListUpdate)
    }

    override fun appendAnimals(animals: List<Animal>) {
        binding.emptyState.isVisible = false
        binding.animalsList.isVisible = true
        animalAdapter.appendAnimals(animals, ::checkAfterListUpdate)
    }

    override fun showFilterSelected(filter: SpeciesFilter) {
        if (selectedFilter != filter) binding.animalsList.scrollToPosition(0)
        selectedFilter = filter
        updatingFilter = true
        binding.filterGroup.check(when (filter) {
            SpeciesFilter.ALL -> R.id.filter_all
            SpeciesFilter.DOGS -> R.id.filter_dogs
            SpeciesFilter.CATS -> R.id.filter_cats
            SpeciesFilter.NEAREST -> R.id.filter_nearest
        })
        updateFilterAppearance(filter)
        updatingFilter = false
    }

    override fun showEmptyState(filter: SpeciesFilter) {
        showState(
            title = when (filter) {
                SpeciesFilter.ALL -> R.string.adoption_empty_all
                SpeciesFilter.DOGS -> R.string.adoption_empty_dogs
                SpeciesFilter.CATS -> R.string.adoption_empty_cats
                SpeciesFilter.NEAREST -> R.string.adoption_empty_nearest
            },
            description = R.string.adoption_empty_description
        )
    }

    override fun showLoading(nextPage: Boolean) {
        binding.pageError.isVisible = false
        binding.pageRetry.isVisible = false
        if (nextPage) {
            binding.pageState.isVisible = true
            binding.pageLoading.isVisible = true
        } else {
            binding.pageState.isVisible = false
            binding.animalsList.isVisible = false
            binding.emptyState.isVisible = false
            binding.loadingState.isVisible = true
        }
    }

    override fun hideLoading() {
        binding.loadingState.isVisible = false
        binding.pageState.isVisible = false
    }

    override fun showError(error: Throwable, hasAnimals: Boolean) {
        Log.e(TAG, "Could not load adoption catalog", error)
        if (hasAnimals) {
            binding.pageState.isVisible = true
            binding.pageLoading.isVisible = false
            binding.pageError.isVisible = true
            binding.pageRetry.isVisible = true
        } else {
            showState(R.string.adoption_error_title, R.string.adoption_error_description, R.string.adoption_retry)
            binding.stateIcon.setImageResource(R.drawable.ic_warning)
        }
    }

    override fun showLocationRequired() {
        showState(
            R.string.adoption_location_title,
            R.string.adoption_location_description,
            R.string.adoption_location_action
        )
        binding.stateIcon.setImageResource(R.drawable.ic_location_pin)
    }

    override fun requestLocationPermission() {
        when {
            hasLocationPermission() -> useLocationOrOpenSettings()
            requestedPermission && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) ->
                settingsLauncher.launch(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:$packageName".toUri()
                    )
                )
            else -> {
                requestedPermission = true
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ))
            }
        }
    }

    override fun openAnimalDetails(animalId: String) {
        startActivity(AnimalDetailsActivity.newIntent(this, animalId))
    }

    private fun useLocationOrOpenSettings() {
        val manager = getSystemService(LocationManager::class.java)
        if (manager != null && !LocationManagerCompat.isLocationEnabled(manager)) {
            settingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            presenter.onLocationPermissionResult(true)
        }
    }

    private fun hasLocationPermission() = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
    ).any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private fun showState(@StringRes title: Int, @StringRes description: Int, @StringRes action: Int? = null) {
        hideLoading()
        binding.animalsList.isVisible = false
        binding.emptyState.isVisible = true
        binding.stateIcon.setImageResource(R.drawable.ic_adoption_heart)
        binding.stateTitle.setText(title)
        binding.stateDescription.setText(description)
        binding.stateAction.isVisible = action != null
        action?.let(binding.stateAction::setText)
    }

    private fun checkAfterListUpdate() {
        binding.animalsList.post { loadMoreIfNeeded() }
    }

    private fun loadMoreIfNeeded() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) || !binding.animalsList.isVisible) return
        val layout = binding.animalsList.layoutManager as? LinearLayoutManager ?: return
        val last = layout.findLastVisibleItemPosition()
        if (last != RecyclerView.NO_POSITION && last >= animalAdapter.itemCount - PREFETCH_DISTANCE) {
            presenter.onScrolledToEnd()
        }
    }

    private fun filterForId(id: Int) = when (id) {
        R.id.filter_dogs -> SpeciesFilter.DOGS
        R.id.filter_cats -> SpeciesFilter.CATS
        R.id.filter_nearest -> SpeciesFilter.NEAREST
        else -> SpeciesFilter.ALL
    }

    private fun updateFilterAppearance(selected: SpeciesFilter) {
        val filters = listOf(
            binding.filterAll to SpeciesFilter.ALL,
            binding.filterDogs to SpeciesFilter.DOGS,
            binding.filterCats to SpeciesFilter.CATS,
            binding.filterNearest to SpeciesFilter.NEAREST
        )
        filters.forEach { (chip, filter) ->
            applyFilterAppearance(chip, filter == selected)
        }
    }

    private fun applyFilterAppearance(chip: Chip, selected: Boolean) {
        val backgroundColor = color(if (selected) R.color.adoption_primary else R.color.surface)
        val contentColor = color(if (selected) R.color.white else R.color.muted)
        val strokeColor = color(if (selected) R.color.adoption_primary else R.color.border)
        chip.chipBackgroundColor = ColorStateList.valueOf(backgroundColor)
        chip.chipStrokeColor = ColorStateList.valueOf(strokeColor)
        chip.setTextColor(contentColor)
    }

    private fun color(colorRes: Int) = ContextCompat.getColor(this, colorRes)

    private companion object {
        const val TAG = "AdoptionActivity"
        const val STATE_FILTER = "adoption_filter"
        const val STATE_PERMISSION_REQUESTED = "adoption_permission_requested"
        const val PREFETCH_DISTANCE = 3
    }
}
