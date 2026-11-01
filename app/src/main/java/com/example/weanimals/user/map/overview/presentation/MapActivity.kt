package com.example.weanimals.user.map.overview.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import com.example.weanimals.user.map.overview.interactor.SearchMapAddress
import com.example.weanimals.user.map.overview.interactor.AndroidMapAddressGeocoder
import com.example.weanimals.user.map.overview.interactor.MapAddressSearchResult
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import coil.load
import coil.dispose
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import com.example.weanimals.user.lostandfound.presentation.AdActionNavigator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.weanimals.user.map.overview.interactor.PublicAdMapLocation
import com.example.weanimals.user.map.overview.interactor.InitialMapPosition
import com.example.weanimals.user.map.overview.interactor.InitialMapPositionPolicy
import com.example.weanimals.core.location.domain.Coordinates
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.ActivityMapBinding
import com.example.weanimals.databinding.ItemOccurrenceBinding
import com.example.weanimals.user.lostandfound.model.FoundPetAd
import com.example.weanimals.user.lostandfound.repository.LostAndFoundRepository
import com.example.weanimals.user.lostandfound.presentation.PublicAdDetailActivity
import com.example.weanimals.user.map.overview.domain.PublicOccurrence
import com.example.weanimals.user.map.overview.interactor.NearbyOccurrences
import com.example.weanimals.user.map.overview.presenter.MapContract
import com.example.weanimals.user.reporting.tracking.presentation.TrackingActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import java.text.NumberFormat
import java.util.Locale

sealed class MapDisplayItem {
    data class Report(val occurrence: PublicOccurrence) : MapDisplayItem()
    data class Ad(val ad: FoundPetAd, val publicLocation: Pair<Double, Double>) : MapDisplayItem()

    val id: String
        get() = when (this) {
            is Report -> occurrence.id
            is Ad -> ad.id
        }

    val latitude: Double
        get() = when (this) {
            is Report -> occurrence.coordinates.latitude
            is Ad -> publicLocation.first
        }

    val longitude: Double
        get() = when (this) {
            is Report -> occurrence.coordinates.longitude
            is Ad -> publicLocation.second
        }
}

class MapActivity : AppCompatActivity(), MapContract.View {
    private lateinit var binding: ActivityMapBinding
    private val presenter by lazy { (application as WeAnimalsApplication).appContainer.createMapPresenter(restrictToUserRadius = false) }
    private val adapter = MapItemAdapter(::selectCardOnMap)
    private var publicAds: List<MapDisplayItem.Ad> = emptyList()
    private var adsJob: Job? = null
    private var searchJob: Job? = null
    private var searchRequestId = 0
    private val searchAddress by lazy { SearchMapAddress(AndroidMapAddressGeocoder(this)) }
    private val publicAdLocation by lazy { PublicAdMapLocation(this) }
    private var nearby = NearbyOccurrences(null, emptyList())
    private var selectedUrgency: String? = null
    private var selectedPetCategory: String? = null
    private var currentCategoryFilter = FILTER_ALL
    private var focusAdId: String? = null
    private var focusedAdLocation: Pair<Double, Double>? = null
    private var cameraPositioned = false
    private var initialPosition: InitialMapPosition? = null
    private var restoredCamera: DoubleArray? = null
    private var restoringFilters = false
    private var selectedReportId: String? = null
    private var selectedAnnouncementId: String? = null
    private var renderedSelection: String? = null
    private val animalBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() { closeAnimalDetails() }
    }
    private var selectedGroup: List<MapDisplayItem>? = null
    private var sheet: BottomSheetBehavior<LinearLayout>? = null
    private var map: MapLibreMap? = null
    private val markerGroupItems = mutableMapOf<Long, List<MapDisplayItem>>()
    private var renderedItems: List<MapDisplayItem>? = null
    private var renderedUserLocation: com.example.weanimals.core.location.domain.Coordinates? = null
    private val distanceFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { presenter.load() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        val initialFilter = savedInstanceState?.getString("map_category") ?: intent.getStringExtra(EXTRA_INITIAL_FILTER)
        currentCategoryFilter = when (initialFilter) {
            FILTER_LOST_FOUND -> FILTER_LOST_FOUND
            FILTER_REPORTS -> FILTER_REPORTS
            else -> FILTER_ALL
        }
        selectedUrgency = savedInstanceState?.getString("map_urgency")
        selectedPetCategory = savedInstanceState?.getString("map_pet_category")
        restoringFilters = savedInstanceState != null
        restoredCamera = savedInstanceState?.getDoubleArray("map_camera")
        cameraPositioned = restoredCamera != null
        focusAdId = if (savedInstanceState == null) intent.getStringExtra(EXTRA_FOCUS_AD_ID) else null
        selectedReportId = savedInstanceState?.getString("selected_map_report")

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupMapSearch()
        selectedAnnouncementId = savedInstanceState?.getString("selected_map_announcement")
        onBackPressedDispatcher.addCallback(this, animalBackCallback)
        binding.mapContent.animalDetails.closeAnimalDetails.setOnClickListener { closeAnimalDetails() }
        binding.mapContent.animalDetails.viewAnimalAd.setOnClickListener {
            selectedAnnouncementId?.let { id ->
                startActivity(Intent(this, PublicAdDetailActivity::class.java)
                    .putExtra(PublicAdDetailActivity.EXTRA_AD_ID, id))
            } ?: selectedReportId?.let { id ->
                nearby.occurrences.firstOrNull { it.id == id }?.let { report ->
                    startActivity(TrackingActivity.newIntent(this, report.id).apply {
                        putExtra("extra_title", title(report))
                        putExtra("extra_urgency", report.urgency)
                    })
                }
            }
        }
        binding.mapContent.animalDetails.animalMainAction.setOnClickListener {
            selectedAnnouncementId?.let { AdActionNavigator.open(this, it) }
        }
        val navigation = binding.mainNavigation.root
        val navigationHeight = navigation.layoutParams.height
        val navigationBottomPadding = navigation.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(navigation) { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, navigationBottomPadding + bottom)
            view.updateLayoutParams { height = navigationHeight + bottom }
            insets
        }
        ViewCompat.requestApplyInsets(navigation)
        val controls = binding.mapContent.mapSearchControls
        val controlsTopMargin = (controls.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        ViewCompat.setOnApplyWindowInsetsListener(controls) { view, insets ->
            val safeInsets = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = controlsTopMargin + safeInsets.top
            }
            binding.mapContent.filterScrollView.doOnLayout { updateSheetSafeArea(it.top) }
            insets
        }
        ViewCompat.requestApplyInsets(controls)
        binding.mapContent.filterScrollView.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            updateSheetSafeArea(view.top)
        }
        binding.mapContent.mapView.onCreate(savedInstanceState)
        MainNavigation.bind(this, binding.mainNavigation, MainNavigation.Destination.MAP)
        configureMap()

        applyFilterSelection()

        binding.mapContent.mapAttribution.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MAP_ATTRIBUTION_URL)))
        }
        binding.mapContent.btnMyLocation.setOnClickListener { view ->
            view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(80)
                .withEndAction {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
                }
                .start()
            recenterToUserLocation()
        }
        binding.mapContent.recyclerOccurrences.layoutManager = LinearLayoutManager(this)
        binding.mapContent.recyclerOccurrences.adapter = adapter
        sheet = BottomSheetBehavior.from(binding.mapContent.bottomSheet).apply {
            peekHeight = (268 * resources.displayMetrics.density).toInt()
            isHideable = false
            isFitToContents = false
        }
        binding.mapContent.filterScrollView.doOnLayout { updateSheetSafeArea(it.top) }
        binding.mapContent.bottomSheet.post {
            if (selectedAnnouncementId != null) return@post
            val density = resources.displayMetrics.density
            val headerArea = binding.mapContent.dragHandle.height +
                    binding.mapContent.occurrenceCountText.height +
                    binding.mapContent.bottomSheet.paddingTop +
                    (52 * density).toInt()
            val twoCardsHeight = (156 * density).toInt()
            val calculatedPeek = headerArea + twoCardsHeight
            sheet?.peekHeight = calculatedPeek.coerceIn((260 * density).toInt(), (280 * density).toInt())
        }
        binding.mapContent.chipGroupFilters.setOnCheckedStateChangeListener { _, checked ->
            if (restoringFilters) return@setOnCheckedStateChangeListener
            closeAnimalDetails()
            val checkedId = checked.firstOrNull()
            currentCategoryFilter = when (checkedId) {
                R.id.chip_reports -> FILTER_REPORTS
                R.id.chip_lost_found -> FILTER_LOST_FOUND
                else -> FILTER_ALL
            }
            selectedUrgency = null
            selectedPetCategory = null
            binding.mapContent.chipGroupSecondary.check(R.id.chip_secondary_all)
            selectedGroup = null
            updateChips()
            renderOccurrences()
        }
        binding.mapContent.chipGroupSecondary.setOnCheckedStateChangeListener { _, checked ->
            if (restoringFilters) return@setOnCheckedStateChangeListener
            closeAnimalDetails()
            val checkedId = checked.firstOrNull()
            selectedPetCategory = if (currentCategoryFilter == FILTER_LOST_FOUND) when (checkedId) {
                R.id.chip_lost -> "PERDIDO"
                R.id.chip_found -> "ENCONTRADO"
                else -> null
            } else null
            selectedUrgency = if (currentCategoryFilter == FILTER_REPORTS) when (checkedId) {
                R.id.chip_high -> "high"
                R.id.chip_medium -> "medium"
                R.id.chip_low -> "low"
                else -> null
            } else null
            selectedGroup = null
            updateChips()
            renderOccurrences()
        }
        updateChips()
        if (!hasLocationPermission()) permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        closeAnimalDetails()
        focusedAdLocation = null
        focusAdId = intent.getStringExtra(EXTRA_FOCUS_AD_ID)
        currentCategoryFilter = when (intent.getStringExtra(EXTRA_INITIAL_FILTER)) {
            FILTER_LOST_FOUND -> FILTER_LOST_FOUND
            FILTER_REPORTS -> FILTER_REPORTS
            else -> FILTER_ALL
        }
        selectedUrgency = null
        selectedPetCategory = null
        binding.mapContent.chipGroupSecondary.check(R.id.chip_secondary_all)
        selectedGroup = null
        val chipId = when (currentCategoryFilter) {
            FILTER_LOST_FOUND -> R.id.chip_lost_found
            FILTER_REPORTS -> R.id.chip_reports
            else -> R.id.chip_all
        }
        binding.mapContent.chipGroupFilters.check(chipId)
        binding.mapContent.filterScrollView.scrollTo(0, 0)
        updateChips()
        renderOccurrences()
        focusOnAd()
    }

    private fun focusOnAd() {
        val adId = focusAdId ?: return
        val item = publicAds.firstOrNull { it.ad.id == adId } ?: return
        val coordinates = item.publicLocation
        val readyMap = map ?: return
        if (focusedAdLocation == coordinates) return
        readyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(coordinates.first, coordinates.second), 15.0
        ))
        focusedAdLocation = coordinates
        cameraPositioned = true
    }

    override fun onStart() {
        super.onStart()
        binding.mapContent.mapView.onStart()
        presenter.attachView(this)
        presenter.load()
        adsJob = lifecycleScope.launch {
            LostAndFoundRepository.observeAds(this@MapActivity).collectLatest { ads ->
                while (isActive) {
                    publicAds = ads.filter { PublicAdMapLocation.isActive(it) }.mapNotNull { ad ->
                        publicAdLocation.resolve(ad)?.let { MapDisplayItem.Ad(ad, it) }
                    }
                    renderOccurrences()
                    focusOnAd()
                    positionInitialCamera()
                    // Expiration must remove a marker even without a repository write.
                    delay(60_000)
                }
            }
        }
    }

    override fun onStop() {
        cancelMapSearch()
        adsJob?.cancel()
        adsJob = null
        presenter.detachView()
        binding.mapContent.mapView.onStop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        binding.mapContent.mapView.onResume()
        selectedAnnouncementId?.let { showAnimalDetails(it) }
        selectedReportId?.let { id -> nearby.occurrences.firstOrNull { it.id == id }?.let(::showReportDetails) }
    }

    override fun onPause() {
        binding.mapContent.mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        map = null
        binding.mapContent.mapView.onDestroy()
        presenter.destroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapContent.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("selected_map_announcement", selectedAnnouncementId)
        outState.putString("selected_map_report", selectedReportId)
        outState.putString("map_category", currentCategoryFilter)
        outState.putString("map_urgency", selectedUrgency)
        outState.putString("map_pet_category", selectedPetCategory)
        map?.cameraPosition?.let { position ->
            position.target?.let { target ->
                outState.putDoubleArray("map_camera", doubleArrayOf(target.latitude, target.longitude, position.zoom))
            }
        }
        binding.mapContent.mapView.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        restoringFilters = true
        super.onRestoreInstanceState(savedInstanceState)
        applyFilterSelection()
        restoringFilters = false
    }

    private fun applyFilterSelection() {
        val previous = restoringFilters
        restoringFilters = true
        binding.mapContent.chipGroupFilters.check(when (currentCategoryFilter) {
            FILTER_LOST_FOUND -> R.id.chip_lost_found
            FILTER_REPORTS -> R.id.chip_reports
            else -> R.id.chip_all
        })
        binding.mapContent.chipGroupSecondary.check(when {
            currentCategoryFilter == FILTER_LOST_FOUND && selectedPetCategory == "PERDIDO" -> R.id.chip_lost
            currentCategoryFilter == FILTER_LOST_FOUND && selectedPetCategory == "ENCONTRADO" -> R.id.chip_found
            currentCategoryFilter == FILTER_REPORTS && selectedUrgency == "high" -> R.id.chip_high
            currentCategoryFilter == FILTER_REPORTS && selectedUrgency == "medium" -> R.id.chip_medium
            currentCategoryFilter == FILTER_REPORTS && selectedUrgency == "low" -> R.id.chip_low
            else -> R.id.chip_secondary_all
        })
        restoringFilters = previous
        updateChips()
    }

    private fun positionInitialCamera() {
        val readyMap = map ?: return
        if (cameraPositioned || focusAdId != null || selectedAnnouncementId != null || selectedReportId != null) return
        val records = getFilteredItems().map { Coordinates(it.latitude, it.longitude) }
        val position = InitialMapPositionPolicy.choose(nearby.userLocation, records)
        if (initialPosition == position) return
        initialPosition = position
        readyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(position.latitude, position.longitude), position.zoom))
        if (InitialMapPositionPolicy.valid(nearby.userLocation)) cameraPositioned = true
    }

    private fun hasLocationPermission() = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
    ).any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private fun configureMap() {
        binding.mapContent.mapView.getMapAsync { readyMap ->
            map = readyMap
            val saved = restoredCamera
            if (saved != null && saved.size == 3 && InitialMapPositionPolicy.valid(Coordinates(saved[0], saved[1]))) {
                readyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(saved[0], saved[1]), saved[2]))
            } else {
                val initial = InitialMapPositionPolicy.choose(null, emptyList())
                readyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(initial.latitude, initial.longitude), initial.zoom))
            }
            readyMap.setOnMarkerClickListener { marker ->
                markerGroupItems[marker.id]?.let { group ->
                    val animals = group.filterIsInstance<MapDisplayItem.Ad>()
                    when {
                        animals.size == 1 -> {
                            showAnimalDetails(animals.first().id)
                            readyMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                        }
                        animals.size > 1 -> MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.map_records_title)
                            .setItems(animals.map { PublicAnimalDetails.title(it.ad) }.toTypedArray()) { _, index ->
                                showAnimalDetails(animals[index].id)
                            }.show()
                        group.size == 1 -> showReportDetails((group.first() as MapDisplayItem.Report).occurrence)
                        else -> MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.map_report_category)
                            .setItems(group.map { title((it as MapDisplayItem.Report).occurrence) }.toTypedArray()) { _, index ->
                                showReportDetails((group[index] as MapDisplayItem.Report).occurrence)
                            }.show()
                    }
                    true
                } ?: false
            }
            readyMap.addOnMapClickListener {
                if (selectedAnnouncementId != null || selectedReportId != null) {
                    closeAnimalDetails()
                    return@addOnMapClickListener true
                }
                if (selectedGroup != null) {
                    resetGroupSelection()
                    true
                } else {
                    false
                }
            }
            readyMap.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) cameraPositioned = true
            }
            readyMap.addOnCameraIdleListener { renderVisibleRecords() }
            readyMap.setStyle(MAP_STYLE_URL) {
                renderedItems = null
                focusOnAd()
                positionInitialCamera()
                renderOccurrences()
            }
        }
    }

    private fun setupMapSearch() {
        val content = binding.mapContent
        content.mapSearchInput.doAfterTextChanged { text ->
            cancelMapSearch()
            content.mapSearchMessage.visibility = View.GONE
            content.mapSearchClear.visibility = if (text.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        }
        content.mapSearchSubmit.setOnClickListener { submitMapSearch() }
        content.mapSearchClear.setOnClickListener { content.mapSearchInput.text?.clear() }
        content.mapSearchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP)) {
                submitMapSearch()
                true
            } else false
        }
    }

    private fun setMapSearchBusy(busy: Boolean) {
        binding.mapContent.mapSearchSubmit.isEnabled = !busy
        binding.mapContent.mapSearchSubmit.visibility = if (busy) View.INVISIBLE else View.VISIBLE
        binding.mapContent.mapSearchProgress.visibility = if (busy) View.VISIBLE else View.GONE
    }

    private fun cancelMapSearch() {
        searchRequestId++
        searchJob?.cancel()
        searchJob = null
        setMapSearchBusy(false)
    }

    private fun submitMapSearch() {
        if (searchJob?.isActive == true) return
        val content = binding.mapContent
        val query = content.mapSearchInput.text?.toString().orEmpty()
        if (query.isBlank()) {
            content.mapSearchMessage.setText(R.string.map_search_empty)
            content.mapSearchMessage.visibility = View.VISIBLE
            return
        }
        if (map == null) {
            content.mapSearchMessage.setText(R.string.map_search_map_loading)
            content.mapSearchMessage.visibility = View.VISIBLE
            return
        }
        val request = ++searchRequestId
        content.mapSearchMessage.visibility = View.GONE
        content.mapSearchInput.clearFocus()
        WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
        setMapSearchBusy(true)
        searchJob = lifecycleScope.launch {
            try {
                val result = searchAddress(query)
                if (!isActive || request != searchRequestId) return@launch
                if (result is MapAddressSearchResult.Found) {
                    closeAnimalDetails()
                    cameraPositioned = true
                    focusAdId = null
                    focusedAdLocation = null
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(result.coordinates.latitude, result.coordinates.longitude), 14.0))
                } else {
                    content.mapSearchMessage.setText(when (result) {
                        MapAddressSearchResult.EmptyQuery -> R.string.map_search_empty
                        MapAddressSearchResult.NotFound -> R.string.map_search_not_found
                        MapAddressSearchResult.Offline -> R.string.map_search_offline
                        MapAddressSearchResult.Unavailable -> R.string.map_search_unavailable
                        else -> R.string.map_search_failed
                    })
                    content.mapSearchMessage.visibility = View.VISIBLE
                }
            } finally {
                if (request == searchRequestId) {
                    setMapSearchBusy(false)
                    searchJob = null
                }
            }
        }
    }

    private fun recenterToUserLocation() {
        val userLocation = nearby.userLocation
        if (InitialMapPositionPolicy.valid(userLocation) && userLocation != null && map != null) {
            cameraPositioned = true
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(userLocation.latitude, userLocation.longitude),
                    15.0
                )
            )
        } else {
            if (!hasLocationPermission()) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            } else {
                presenter.load()
            }
        }
    }

    override fun showLoading() {
        binding.mapContent.occurrenceCountText.setText(R.string.map_loading)
        binding.mapContent.mapStateText.setText(R.string.map_loading)
        binding.mapContent.mapStateText.visibility = View.VISIBLE
    }

    override fun showOccurrences(result: NearbyOccurrences) {
        nearby = result
        positionInitialCamera()
        renderOccurrences()
        if (result.sourceError && adapter.currentList.isEmpty()) {
            binding.mapContent.mapStateText.setText(R.string.map_error)
            binding.mapContent.mapStateText.visibility = View.VISIBLE
        }
    }

    override fun showError() {
        nearby = NearbyOccurrences(nearby.userLocation, emptyList())
        renderOccurrences()
        if (getFilteredItems().isNotEmpty()) return
        binding.mapContent.mapStateText.setText(R.string.map_error)
        binding.mapContent.mapStateText.visibility = View.VISIBLE

    }

    private fun getFilteredItems(): List<MapDisplayItem> {
        val list = mutableListOf<MapDisplayItem>()
        val reports = nearby.occurrences.filter {
            currentCategoryFilter != FILTER_REPORTS || selectedUrgency == null || it.urgency == selectedUrgency
        }
        val ads = publicAds.filter {
            PublicAdMapLocation.isActive(it.ad) &&
                (currentCategoryFilter != FILTER_LOST_FOUND || selectedPetCategory == null ||
                    it.ad.category.equals(selectedPetCategory, ignoreCase = true))
        }

        if (currentCategoryFilter == FILTER_ALL || currentCategoryFilter == FILTER_REPORTS) {
            if (selectedUrgency == null || reports.isNotEmpty()) {
                list.addAll(reports.map { MapDisplayItem.Report(it) })
            }
        }
        if (currentCategoryFilter == FILTER_ALL || currentCategoryFilter == FILTER_LOST_FOUND) {
            list.addAll(ads)
        }
        return list
    }

    private fun renderOccurrences() {
        selectedAnnouncementId?.let { id ->
            val ad = LostAndFoundRepository.getAdById(this, id)
            if (ad == null || !PublicAdMapLocation.isActive(ad)) closeAnimalDetails()
            else bindAnimalDetails(ad)
        }
        selectedReportId?.let { id ->
            val report = nearby.occurrences.firstOrNull { it.id == id }
            if (report == null) closeAnimalDetails() else bindReportDetails(report)
        }
        updateMarkers(getFilteredItems())
        renderVisibleRecords()
    }

    private fun renderVisibleRecords() {
        val readyMap = map
        val bounds = readyMap?.projection?.visibleRegion?.latLngBounds
        val visibleItems = if (readyMap?.style == null || bounds == null) emptyList() else {
            readyMap.markers.filter { bounds.contains(it.position) }
                .flatMap { markerGroupItems[it.id].orEmpty() }
        }
        adapter.submitList(visibleItems)
        binding.mapContent.occurrenceCountText.text =
            getString(R.string.map_records_in_region, visibleItems.size)
        binding.mapContent.mapStateText.setText(R.string.map_records_empty_region)
        binding.mapContent.mapStateText.visibility = if (visibleItems.isEmpty()) View.VISIBLE else View.GONE
        binding.mapContent.sheetUrgencyLegend.visibility =
            if (currentCategoryFilter == FILTER_LOST_FOUND) View.GONE else View.VISIBLE
    }

    private fun selectCardOnMap(item: MapDisplayItem) {
        val readyMap = map ?: return
        // Use both the model type and ID: an ad and a report may share an ID.
        val markerId = markerGroupItems.entries.firstOrNull { entry ->
            entry.value.any { it.id == item.id && it::class == item::class }
        }?.key ?: return
        val marker = readyMap.markers.firstOrNull { it.id == markerId } ?: return
        readyMap.selectMarker(marker)
        readyMap.animateCamera(
            CameraUpdateFactory.newLatLng(marker.position),
            350,
            object : MapLibreMap.CancelableCallback {
                override fun onCancel() {}
                override fun onFinish() {
                    if (isFinishing || isDestroyed) return
                    if (item is MapDisplayItem.Ad) {
                        showAnimalDetails(item.id)
                        return
                    }
                    if (item is MapDisplayItem.Report) showReportDetails(item.occurrence)
                }
            }
        )
    }

    private fun updateMarkers(items: List<MapDisplayItem>) {
        val readyMap = map ?: return
        if (readyMap.style == null) return
        if (renderedItems == items && renderedUserLocation == nearby.userLocation && renderedSelection == selectedAnnouncementId) return
        renderedSelection = selectedAnnouncementId
        renderedItems = items
        renderedUserLocation = nearby.userLocation
        readyMap.removeAnnotations()
        markerGroupItems.clear()
        val iconFactory = IconFactory.getInstance(this)

        // 1. User Location Marker
        nearby.userLocation?.let { location ->
            val userIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_user_location)
            val userBitmap = userIconDrawable?.toBitmap()
            val userIcon = userBitmap?.let(iconFactory::fromBitmap)

            val userMarker = MarkerOptions()
                .position(LatLng(location.latitude, location.longitude))
                .title(getString(R.string.map_your_location))
            userIcon?.let(userMarker::icon)

            readyMap.addMarker(userMarker)
        }

        // 2. Group Items by Quantized Location (~10m precision)
        val locationGroups = mutableMapOf<Triple<String, Long, Long>, MutableList<MapDisplayItem>>()
        items.forEach { item ->
            val latKey = (item.latitude * 10000).toLong()
            val lonKey = (item.longitude * 10000).toLong()
            val category = when (item) {
                is MapDisplayItem.Report -> "report"
                is MapDisplayItem.Ad -> item.ad.category.uppercase(Locale.ROOT)
            }
            val key = Triple(category, latKey, lonKey)
            locationGroups.getOrPut(key) { mutableListOf() }.add(item)
        }

        // 3. Render Markers
        locationGroups.values.forEach { group ->
            val representative = group.first()
            val count = group.size

            val bitmap: Bitmap = when (representative) {
                is MapDisplayItem.Report -> {
                    val highestUrgency = highestUrgencyInGroup(group)
                    createBadgedPinBitmap(urgencyColor(highestUrgency), count)
                }
                is MapDisplayItem.Ad -> {
                    val isLost = representative.ad.category.equals("PERDIDO", ignoreCase = true)
                    val colorRes = if (isLost) R.color.terracotta else R.color.pine800
                    createBadgedPinBitmap(colorRes, count, showPaw = true)
                }
            }
            val highlighted = group.any { it is MapDisplayItem.Ad && it.id == selectedAnnouncementId }
            val displayBitmap = if (highlighted) Bitmap.createScaledBitmap(bitmap,
                (bitmap.width * 1.25f).toInt(), (bitmap.height * 1.25f).toInt(), true) else bitmap
            val icon = iconFactory.fromBitmap(displayBitmap)

            val markerTitle = when (representative) {
                is MapDisplayItem.Report -> if (count == 1) title(representative.occurrence) else "$count ocorrências neste local"
                is MapDisplayItem.Ad -> if (count == 1) representative.ad.getDisplayTitle() else "$count animais neste local"
            }

            val marker = MarkerOptions()
                .position(LatLng(representative.latitude, representative.longitude))
                .title(markerTitle)
                .icon(icon)

            val addedMarker = readyMap.addMarker(marker)
            markerGroupItems[addedMarker.id] = group
        }
    }

    private fun selectGroupOnMap(group: List<MapDisplayItem>) {
        selectedGroup = group
        renderOccurrences()
        val first = group.first()
        map?.animateCamera(CameraUpdateFactory.newLatLng(
            LatLng(first.latitude, first.longitude)
        ))
    }

    private fun resetGroupSelection() {
        selectedGroup = null
        renderOccurrences()
    }

    private fun highestUrgencyInGroup(group: List<MapDisplayItem>): String {
        val reports = group.mapNotNull { (it as? MapDisplayItem.Report)?.occurrence }
        return when {
            reports.any { it.urgency == "high" } -> "high"
            reports.any { it.urgency == "medium" } -> "medium"
            else -> "low"
        }
    }

    private fun createBadgedPinBitmap(colorRes: Int, count: Int, showPaw: Boolean = false): Bitmap {
        val baseDrawable = ContextCompat.getDrawable(this, R.drawable.ic_map_pin)
            ?.mutate()?.apply { setTint(ContextCompat.getColor(this@MapActivity, colorRes)) }
            ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val density = resources.displayMetrics.density
        val pinSize = (26 * density).toInt()
        val badgeRadius = (7 * density).toInt()

        val badgeOffsetY = if (count > 1) (5 * density).toInt() else 0
        val bitmapWidth = pinSize + if (count > 1) (4 * density).toInt() else 0
        val bitmapHeight = pinSize + badgeOffsetY

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        baseDrawable.setBounds(0, badgeOffsetY, pinSize, pinSize + badgeOffsetY)
        baseDrawable.draw(canvas)
        if (showPaw) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(this@MapActivity, colorRes)
            }
            canvas.drawCircle(13 * density, badgeOffsetY + 10 * density, 9 * density, fill)
            ContextCompat.getDrawable(this, R.drawable.ic_paw_print)?.mutate()?.apply {
                setTint(ContextCompat.getColor(this@MapActivity, R.color.white))
                setBounds((5 * density).toInt(), badgeOffsetY + (2 * density).toInt(),
                    (21 * density).toInt(), badgeOffsetY + (18 * density).toInt())
                draw(canvas)
            }
        }

        if (count > 1) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(this@MapActivity, R.color.clay600)
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(this@MapActivity, R.color.white)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(this@MapActivity, R.color.white)
                textSize = 9 * density
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }

            val badgeCenterX = (pinSize - (2 * density))
            val badgeCenterY = badgeRadius.toFloat()

            canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius.toFloat(), badgePaint)
            canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius.toFloat(), borderPaint)

            val textY = badgeCenterY - ((textPaint.descent() + textPaint.ascent()) / 2)
            val countText = if (count > 9) "9+" else count.toString()
            canvas.drawText(countText, badgeCenterX, textY, textPaint)
        }

        return bitmap
    }

    private fun updateChips() {
        val content = binding.mapContent
        content.legendContainer.visibility = if (currentCategoryFilter == FILTER_LOST_FOUND) View.GONE else View.VISIBLE
        content.secondaryFilterScroll.visibility = if (currentCategoryFilter == FILTER_ALL) View.GONE else View.VISIBLE
        listOf(content.chipLost, content.chipFound).forEach {
            it.visibility = if (currentCategoryFilter == FILTER_LOST_FOUND) View.VISIBLE else View.GONE
        }
        listOf(content.chipHigh, content.chipMedium, content.chipLow).forEach {
            it.visibility = if (currentCategoryFilter == FILTER_REPORTS) View.VISIBLE else View.GONE
        }
        val chips = listOf(
            content.chipSecondaryAll,
            content.chipLost,
            content.chipFound,
            binding.mapContent.chipAll,
            binding.mapContent.chipReports,
            binding.mapContent.chipLostFound,
            binding.mapContent.chipHigh,
            binding.mapContent.chipMedium,
            binding.mapContent.chipLow
        )
        chips.forEach { chip ->
            val active = chip.isChecked
            chip.isCheckedIconVisible = false
            chip.setChipStrokeColorResource(R.color.line_border)
            chip.chipStrokeWidth = if (active) 0f else resources.displayMetrics.density
            chip.setChipBackgroundColorResource(if (active) R.color.map_filter_green else R.color.white)
            chip.setTextColor(ContextCompat.getColor(this, if (active) R.color.white else R.color.map_filter_green))
        }
    }

    private fun showAnimalDetails(announcementId: String) {
        val ad = LostAndFoundRepository.getAdById(this, announcementId) ?: return
        if (!PublicAdMapLocation.isActive(ad)) return
        selectedReportId = null
        selectedAnnouncementId = ad.id
        cameraPositioned = true
        animalBackCallback.isEnabled = true
        val content = binding.mapContent
        if (content.root.height <= 0) {
            content.root.doOnLayout {
                if (selectedAnnouncementId == announcementId) showAnimalDetails(announcementId)
            }
            return
        }
        content.mapRecordsPanel.visibility = View.GONE
        content.animalDetails.root.visibility = View.VISIBLE
        val panelHeight = (content.root.height * 0.62f).toInt().coerceAtMost(430.dp())
        content.animalDetails.root.updateLayoutParams { height = panelHeight }
        content.bottomSheet.updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
        sheet?.apply {
            isFitToContents = true
            maxHeight = panelHeight + content.bottomSheet.paddingTop + content.dragHandle.height
            peekHeight = maxHeight
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        bindAnimalDetails(ad)
        val top = if (content.secondaryFilterScroll.visibility == View.VISIBLE)
            content.secondaryFilterScroll.bottom else content.filterScrollView.bottom
        map?.setPadding(0, top, 0, panelHeight + content.bottomSheet.paddingTop + content.dragHandle.height)
        publicAds.firstOrNull { it.id == ad.id }?.let { item ->
            map?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(item.latitude, item.longitude)))
        }
        updateMarkers(getFilteredItems())
    }

    private fun bindAnimalDetails(ad: FoundPetAd) {
        val detail = binding.mapContent.animalDetails
        val lost = ad.category.equals("PERDIDO", true)
        detail.viewAnimalAd.setText(R.string.map_view_animal_ad)
        detail.animalMainAction.visibility = View.VISIBLE
        detail.animalDate.visibility = View.VISIBLE
        detail.animalPhysicalDetails.visibility = View.VISIBLE
        detail.animalPhoto.imageTintList = null
        detail.animalCategory.setText(if (lost) R.string.map_record_lost else R.string.map_record_found)
        detail.animalCategory.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, if (lost) R.color.terracotta else R.color.pine800))
        detail.animalStatus.text = if (PublicAdMapLocation.isActive(ad)) {
            getString(R.string.map_animal_active) + " · " + ad.currentSituation
        } else ad.currentSituation
        detail.animalTitle.text = PublicAnimalDetails.title(ad)
        detail.animalLocation.text = PublicAnimalDetails.location(ad)
        detail.animalDate.text = (if (lost) "Desaparecimento: " else "Encontro: ") + PublicAnimalDetails.date(ad)
        detail.animalPhysicalDetails.text = PublicAnimalDetails.physicalDescription(ad)
        val photo = ad.photoUris.firstOrNull { it.isNotBlank() }
        if (detail.animalPhoto.tag != photo || detail.animalPhoto.drawable == null) {
            detail.animalPhoto.tag = photo
            detail.animalPhoto.load(photo) {
                placeholder(R.drawable.ic_paw_print)
                error(R.drawable.ic_paw_print)
                fallback(R.drawable.ic_paw_print)
                transformations(coil.transform.RoundedCornersTransformation(12.dp().toFloat()))
            }
        }
        val action = AdActionNavigator.action(this, ad)
        detail.animalMainAction.isEnabled = action != AdActionNavigator.Action.OWN_AD &&
            action != AdActionNavigator.Action.INACTIVE
        detail.animalMainAction.setText(when (action) {
            AdActionNavigator.Action.OWN_AD -> R.string.map_animal_own_ad
            AdActionNavigator.Action.STATUS -> R.string.map_animal_request_status
            AdActionNavigator.Action.CHAT -> R.string.map_animal_chat
            else -> if (lost) R.string.map_animal_sighting else R.string.map_animal_verification
        })
    }

    private fun showReportDetails(report: PublicOccurrence) {
        selectedAnnouncementId = null
        selectedReportId = report.id
        cameraPositioned = true
        animalBackCallback.isEnabled = true
        val content = binding.mapContent
        if (content.root.height <= 0) {
            content.root.doOnLayout { if (selectedReportId == report.id) showReportDetails(report) }
            return
        }
        content.mapRecordsPanel.visibility = View.GONE
        content.animalDetails.root.visibility = View.VISIBLE
        val panelHeight = (content.root.height * 0.48f).toInt().coerceAtMost(340.dp())
        content.animalDetails.root.updateLayoutParams { height = panelHeight }
        content.bottomSheet.updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
        sheet?.apply {
            isFitToContents = true
            maxHeight = panelHeight + content.bottomSheet.paddingTop + content.dragHandle.height
            peekHeight = maxHeight
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        bindReportDetails(report)
        val top = if (content.secondaryFilterScroll.visibility == View.VISIBLE)
            content.secondaryFilterScroll.bottom else content.filterScrollView.bottom
        map?.setPadding(0, top, 0, sheet?.maxHeight ?: panelHeight)
        map?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(report.coordinates.latitude, report.coordinates.longitude)))
        updateMarkers(getFilteredItems())
    }

    private fun bindReportDetails(report: PublicOccurrence) {
        val detail = binding.mapContent.animalDetails
        detail.animalCategory.setText(R.string.map_report_category)
        val color = ContextCompat.getColor(this, urgencyColor(report.urgency))
        detail.animalCategory.backgroundTintList = ColorStateList.valueOf(color)
        detail.animalStatus.setText(when (report.urgency) {
            "high" -> R.string.map_filter_high
            "medium" -> R.string.map_filter_medium
            else -> R.string.map_filter_low
        })
        detail.animalTitle.text = title(report)
        detail.animalLocation.setText(R.string.map_public_location)
        detail.animalDate.visibility = if (report.createdAtMillis > 0) View.VISIBLE else View.GONE
        if (report.createdAtMillis > 0) {
            detail.animalDate.text = "Publicado em " + java.text.SimpleDateFormat(
                "dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).format(java.util.Date(report.createdAtMillis))
        }
        detail.animalPhysicalDetails.visibility = View.GONE
        detail.animalPhoto.dispose()
        detail.animalPhoto.tag = "public-report"
        detail.animalPhoto.imageTintList = ColorStateList.valueOf(color)
        detail.animalPhoto.setImageResource(R.drawable.ic_map_pin)
        detail.viewAnimalAd.setText(R.string.map_view_report)
        detail.animalMainAction.visibility = View.GONE
    }

    private fun closeAnimalDetails() {
        if (selectedAnnouncementId == null && selectedReportId == null) return
        selectedAnnouncementId = null
        selectedReportId = null
        map?.setPadding(0, 0, 0, 0)
        animalBackCallback.isEnabled = false
        val content = binding.mapContent
        content.animalDetails.root.visibility = View.GONE
        content.mapRecordsPanel.visibility = View.VISIBLE
        content.bottomSheet.updateLayoutParams { height = ViewGroup.LayoutParams.MATCH_PARENT }
        sheet?.apply {
            isFitToContents = false
            peekHeight = 268.dp()
            state = BottomSheetBehavior.STATE_COLLAPSED
        }
        updateSheetSafeArea(content.filterScrollView.top)
        renderOccurrences()
    }

    private fun updateSheetSafeArea(top: Int) {
        if (selectedAnnouncementId != null || selectedReportId != null) return
        val behavior = sheet ?: return
        behavior.expandedOffset = top
        val availableHeight = binding.mapContent.root.height - top
        if (availableHeight > 0 && behavior.maxHeight != availableHeight) {
            behavior.maxHeight = availableHeight
            binding.mapContent.bottomSheet.requestLayout()
        }
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    private fun title(item: PublicOccurrence): String {
        val animal = when (item.animalType) {
            "dog" -> R.string.map_animal_dog
            "cat" -> R.string.map_animal_cat
            else -> R.string.map_animal_other
        }
        return getString(R.string.map_point_title, getString(animal))
    }

    private fun urgencyColor(urgency: String) = when (urgency) {
        "high" -> R.color.alert600
        "medium" -> R.color.brass500
        else -> R.color.pine700
    }

    private inner class MapItemAdapter(
        private val onItemClick: (MapDisplayItem) -> Unit
    ) : ListAdapter<MapDisplayItem, MapItemAdapter.Holder>(object : DiffUtil.ItemCallback<MapDisplayItem>() {
        override fun areItemsTheSame(oldItem: MapDisplayItem, newItem: MapDisplayItem) = oldItem::class == newItem::class && oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MapDisplayItem, newItem: MapDisplayItem) = oldItem == newItem
    }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            ItemOccurrenceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

        inner class Holder(private val itemBinding: ItemOccurrenceBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(item: MapDisplayItem) {
                itemBinding.root.setOnClickListener { onItemClick(item) }
                // RecyclerView reuses rows across categories: always reset the image and text state.
                itemBinding.occurrenceIcon.dispose()
                itemBinding.occurrenceIcon.setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
                itemBinding.occurrenceIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                itemBinding.occurrenceIcon.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this@MapActivity, R.color.clay600))
                itemBinding.occurrenceIcon.setImageResource(R.drawable.ic_paw_print)
                itemBinding.occurrencePublication.visibility = View.GONE
                when (item) {
                    is MapDisplayItem.Report -> {
                        val rep = item.occurrence
                        itemBinding.occurrenceTitle.text = title(rep)
                        itemBinding.occurrenceSubtitle.text = getString(
                            R.string.map_point_distance, distanceFormat.format(rep.distanceKm ?: 0.0)
                        )
                        itemBinding.occurrenceBadge.setText(when (rep.urgency) {
                            "high" -> R.string.map_filter_high
                            "medium" -> R.string.map_filter_medium
                            else -> R.string.map_filter_low
                        })
                        itemBinding.occurrenceBadge.setTextColor(
                            ContextCompat.getColor(this@MapActivity, urgencyColor(rep.urgency))
                        )
                        val badgeBackground = when (rep.urgency) {
                            "high" -> R.color.soft_red
                            "medium" -> R.color.medium_urgency_light
                            else -> R.color.low_urgency_light
                        }
                        itemBinding.occurrenceBadge.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(this@MapActivity, badgeBackground)
                        )
                        itemBinding.urgencyIndicator.setBackgroundColor(
                            ContextCompat.getColor(this@MapActivity, urgencyColor(rep.urgency))
                        )

                    }
                    is MapDisplayItem.Ad -> {
                        val ad = item.ad
                        val isLost = ad.category.equals("PERDIDO", ignoreCase = true)
                        val name = if (isLost) ad.calledNames.firstOrNull()?.substringBefore(":")?.trim() else null
                        itemBinding.occurrenceTitle.text = name?.takeIf { it.isNotBlank() } ?: ad.getDisplayTitle()
                        itemBinding.occurrenceSubtitle.text = listOfNotNull(
                            ad.neighborhood?.takeIf { it.isNotBlank() },
                            ad.city?.takeIf { it.isNotBlank() }
                        ).joinToString(" — ").ifBlank { ad.getDisplayLocation() }
                        itemBinding.occurrencePublication.text = ad.getRelativeTimeText()
                        itemBinding.occurrencePublication.visibility = View.VISIBLE
                        itemBinding.occurrenceBadge.setText(if (isLost) R.string.map_record_lost else R.string.map_record_found)
                        val photo = ad.photoUris.firstOrNull { it.isNotBlank() }
                        if (photo != null) {
                            itemBinding.occurrenceIcon.setPadding(0, 0, 0, 0)
                            itemBinding.occurrenceIcon.imageTintList = null
                            itemBinding.occurrenceIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                            itemBinding.occurrenceIcon.load(photo) {
                                placeholder(R.drawable.ic_paw_print)
                                error(R.drawable.ic_paw_print)
                                transformations(coil.transform.RoundedCornersTransformation(8.dp().toFloat()))
                            }
                        }
                        itemBinding.occurrenceBadge.setTextColor(
                            ContextCompat.getColor(this@MapActivity, if (isLost) R.color.terracotta else R.color.pine800)
                        )
                        val badgeBg = if (isLost) R.color.soft_red else R.color.low_urgency_light
                        itemBinding.occurrenceBadge.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(this@MapActivity, badgeBg)
                        )
                        val indicatorColor = if (isLost) R.color.terracotta else R.color.pine800
                        itemBinding.urgencyIndicator.setBackgroundColor(
                            ContextCompat.getColor(this@MapActivity, indicatorColor)
                        )

                    }
                }
            }
        }
    }

    companion object {
        // Reorder keeps the caller below the map so Back returns to the entry screen.
        fun newIntent(context: Context, initialFilter: String = FILTER_ALL, focusAdId: String? = null) =
            Intent(context, MapActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_INITIAL_FILTER, initialFilter)
                putExtra(EXTRA_FOCUS_AD_ID, focusAdId)
            }

        const val EXTRA_INITIAL_FILTER = "extra_initial_filter"
        const val EXTRA_FOCUS_AD_ID = "extra_focus_ad_id"
        const val FILTER_ALL = "all"
        const val FILTER_REPORTS = "reports"
        const val FILTER_LOST_FOUND = "lost_found"
        private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
        private const val MAP_ATTRIBUTION_URL = "https://openfreemap.org/"
    }
}
