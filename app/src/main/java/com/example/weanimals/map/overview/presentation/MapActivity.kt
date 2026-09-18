package com.example.weanimals.map.overview.presentation

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.ActivityMapBinding
import com.example.weanimals.databinding.ItemOccurrenceBinding
import com.example.weanimals.map.overview.domain.PublicOccurrence
import com.example.weanimals.map.overview.interactor.NearbyOccurrences
import com.example.weanimals.map.overview.presenter.MapContract
import com.example.weanimals.reporting.tracking.presentation.TrackingActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import java.text.NumberFormat
import java.util.Locale

class MapActivity : AppCompatActivity(), MapContract.View {
    private lateinit var binding: ActivityMapBinding
    private val presenter by lazy { (application as WeAnimalsApplication).appContainer.createMapPresenter() }
    private val adapter = OccurrenceAdapter(::onCardClicked)
    private var nearby = NearbyOccurrences(null, emptyList())
    private var selectedUrgency: String? = null
    private var selectedOccurrence: PublicOccurrence? = null
    private var sheet: BottomSheetBehavior<LinearLayout>? = null
    private var map: MapLibreMap? = null
    private val markerOccurrences = mutableMapOf<Long, PublicOccurrence>()
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
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mapContent.mapView.onCreate(savedInstanceState)
        MainNavigation.bind(this, binding.mainNavigation, MainNavigation.Destination.MAP)
        configureMap()
        binding.mapContent.mapAttribution.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MAP_ATTRIBUTION_URL)))
        }
        binding.mapContent.recyclerOccurrences.layoutManager = LinearLayoutManager(this)
        binding.mapContent.recyclerOccurrences.adapter = adapter
        sheet = BottomSheetBehavior.from(binding.mapContent.bottomSheet).apply {
            peekHeight = (260 * resources.displayMetrics.density).toInt()
            isHideable = false
        }
        binding.mapContent.chipGroupFilters.setOnCheckedStateChangeListener { _, checked ->
            selectedUrgency = when (checked.firstOrNull()) {
                R.id.chip_high -> "high"
                R.id.chip_medium -> "medium"
                R.id.chip_low -> "low"
                else -> null
            }
            selectedOccurrence = null
            updateChips()
            renderOccurrences()
        }
        updateChips()
        if (!hasLocationPermission()) permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    override fun onStart() {
        super.onStart()
        binding.mapContent.mapView.onStart()
        presenter.attachView(this)
        presenter.load()
    }

    override fun onStop() {
        presenter.detachView()
        binding.mapContent.mapView.onStop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        binding.mapContent.mapView.onResume()
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
        binding.mapContent.mapView.onSaveInstanceState(outState)
    }

    private fun hasLocationPermission() = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
    ).any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private fun configureMap() {
        binding.mapContent.mapView.getMapAsync { readyMap ->
            map = readyMap
            readyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 2.0))
            readyMap.setOnMarkerClickListener { marker ->
                markerOccurrences[marker.id]?.let {
                    selectSingleOccurrenceOnMap(it)
                    true
                } ?: false
            }
            readyMap.addOnMapClickListener {
                if (selectedOccurrence != null) {
                    resetSingleOccurrenceSelection()
                    true
                } else {
                    false
                }
            }
            readyMap.setStyle(MAP_STYLE_URL) {
                nearby.userLocation?.let { location ->
                    readyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude), 15.0
                    ))
                }
                updateMarkers(filteredOccurrences())
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
        result.userLocation?.let { location ->
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude), 15.0
            ))
        }
        renderOccurrences()
    }

    override fun showError() {
        nearby = NearbyOccurrences(null, emptyList())
        adapter.submitList(emptyList())
        binding.mapContent.mapStateText.setText(R.string.map_error)
        binding.mapContent.mapStateText.visibility = View.VISIBLE
        binding.mapContent.occurrenceCountText.setText(R.string.map_error)
        map?.removeAnnotations()
        markerOccurrences.clear()
    }

    private fun renderOccurrences() {
        if (selectedOccurrence != null) {
            val single = listOf(selectedOccurrence!!)
            adapter.submitList(single)
            binding.mapContent.occurrenceCountText.text = "1 ocorrência selecionada"
            binding.mapContent.mapStateText.visibility = View.GONE
            updateMarkers(single)
        } else {
            val items = filteredOccurrences()
            adapter.submitList(items)
            binding.mapContent.occurrenceCountText.text = resources.getQuantityString(
                R.plurals.map_occurrences_nearby, items.size, items.size
            )
            binding.mapContent.mapStateText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.mapContent.mapStateText.setText(
                if (nearby.userLocation == null) R.string.map_location_unavailable else R.string.map_empty
            )
            updateMarkers(items)
        }
    }

    private fun filteredOccurrences() = nearby.occurrences.filter {
        selectedUrgency == null || it.urgency == selectedUrgency
    }

    private fun updateMarkers(items: List<PublicOccurrence>) {
        val readyMap = map ?: return
        if (readyMap.style == null) return
        readyMap.removeAnnotations()
        markerOccurrences.clear()
        val iconFactory = IconFactory.getInstance(this)
        fun pinIcon(colorRes: Int) = ContextCompat.getDrawable(this, R.drawable.ic_map_pin)
            ?.mutate()?.apply { setTint(ContextCompat.getColor(this@MapActivity, colorRes)) }
            ?.toBitmap()?.let(iconFactory::fromBitmap)
        nearby.userLocation?.let { location ->
            val marker = MarkerOptions()
                .position(LatLng(location.latitude, location.longitude))
                .title(getString(R.string.map_your_location))
            pinIcon(R.color.pine800)?.let(marker::icon)
            readyMap.addMarker(marker)
        }
        items.forEach { occurrence ->
            val marker = MarkerOptions()
                .position(LatLng(occurrence.coordinates.latitude, occurrence.coordinates.longitude))
                .title(title(occurrence))
                .snippet(getString(R.string.map_point_distance, distanceFormat.format(occurrence.distanceKm)))
            pinIcon(urgencyColor(occurrence.urgency))?.let(marker::icon)
            markerOccurrences[readyMap.addMarker(marker).id] = occurrence
        }
    }

    private fun selectSingleOccurrenceOnMap(item: PublicOccurrence) {
        selectedOccurrence = item
        adapter.submitList(listOf(item))
        binding.mapContent.occurrenceCountText.text = "1 ocorrência selecionada"
        binding.mapContent.mapStateText.visibility = View.GONE

        highlightLegendCategory(item.urgency)

        map?.animateCamera(CameraUpdateFactory.newLatLng(
            LatLng(item.coordinates.latitude, item.coordinates.longitude)
        ))
    }

    private fun resetSingleOccurrenceSelection() {
        selectedOccurrence = null
        highlightLegendCategory(null)
        renderOccurrences()
    }

    private fun highlightLegendCategory(urgency: String?) {
        val isHigh = urgency == "high"
        val isMedium = urgency == "medium"
        val isLow = urgency == "low"

        binding.mapContent.indicatorRed.visibility = if (isHigh) View.VISIBLE else View.INVISIBLE
        binding.mapContent.indicatorYellow.visibility = if (isMedium) View.VISIBLE else View.INVISIBLE
        binding.mapContent.indicatorGreen.visibility = if (isLow) View.VISIBLE else View.INVISIBLE

        binding.mapContent.legendRed.alpha = if (urgency == null || isHigh) 1.0f else 0.5f
        binding.mapContent.legendYellow.alpha = if (urgency == null || isMedium) 1.0f else 0.5f
        binding.mapContent.legendGreen.alpha = if (urgency == null || isLow) 1.0f else 0.5f
    }

    private fun onCardClicked(item: PublicOccurrence) {
        val intent = TrackingActivity.newIntent(this, item.id).apply {
            putExtra("extra_protocol", "#4487")
            putExtra("extra_title", title(item))
            putExtra("extra_urgency", item.urgency)
        }
        startActivity(intent)
    }

    private fun updateChips() {
        val chips = listOf(binding.mapContent.chipAll, binding.mapContent.chipHigh,
            binding.mapContent.chipMedium, binding.mapContent.chipLow)
        chips.forEach { chip ->
            val active = chip.isChecked
            chip.isCheckedIconVisible = false
            chip.setChipBackgroundColorResource(if (active) R.color.pine800 else R.color.white)
            chip.setTextColor(ContextCompat.getColor(this, if (active) R.color.white else R.color.ink900))
        }
    }

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

    private inner class OccurrenceAdapter(
        private val onClick: (PublicOccurrence) -> Unit
    ) : ListAdapter<PublicOccurrence, OccurrenceAdapter.Holder>(object : DiffUtil.ItemCallback<PublicOccurrence>() {
        override fun areItemsTheSame(oldItem: PublicOccurrence, newItem: PublicOccurrence) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PublicOccurrence, newItem: PublicOccurrence) = oldItem == newItem
    }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            ItemOccurrenceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

        inner class Holder(private val itemBinding: ItemOccurrenceBinding) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(item: PublicOccurrence) {
                itemBinding.occurrenceTitle.text = title(item)
                itemBinding.occurrenceSubtitle.text = getString(
                    R.string.map_point_distance, distanceFormat.format(item.distanceKm)
                )
                itemBinding.occurrenceBadge.setText(when (item.urgency) {
                    "high" -> R.string.map_filter_high
                    "medium" -> R.string.map_filter_medium
                    else -> R.string.map_filter_low
                })
                itemBinding.occurrenceBadge.setTextColor(
                    ContextCompat.getColor(this@MapActivity, urgencyColor(item.urgency))
                )
                val badgeBackground = when (item.urgency) {
                    "high" -> R.color.soft_red
                    "medium" -> R.color.medium_urgency_light
                    else -> R.color.low_urgency_light
                }
                itemBinding.occurrenceBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this@MapActivity, badgeBackground)
                )
                itemBinding.urgencyIndicator.setBackgroundColor(
                    ContextCompat.getColor(this@MapActivity, urgencyColor(item.urgency))
                )
                itemBinding.root.setOnClickListener { onClick(item) }
            }
        }
    }

    private companion object {
        const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
        const val MAP_ATTRIBUTION_URL = "https://openfreemap.org/"
    }
}
