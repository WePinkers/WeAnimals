package com.example.weanimals.map.overview.presentation

import android.Manifest
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.text.NumberFormat
import java.util.Locale

class MapActivity : AppCompatActivity(), MapContract.View {
    private lateinit var binding: ActivityMapBinding
    private val presenter by lazy { (application as WeAnimalsApplication).appContainer.createMapPresenter() }
    private val adapter = OccurrenceAdapter(::selectOccurrence)
    private var nearby = NearbyOccurrences(null, emptyList())
    private var selectedUrgency: String? = null
    private var sheet: BottomSheetBehavior<LinearLayout>? = null
    private val distanceFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { presenter.load() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            load(applicationContext, getSharedPreferences("map_tiles", MODE_PRIVATE))
            userAgentValue = packageName
        }
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MainNavigation.bind(this, binding.mainNavigation, MainNavigation.Destination.MAP)
        configureMap()
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
        presenter.attachView(this)
        presenter.load()
    }

    override fun onStop() {
        presenter.detachView()
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
        binding.mapContent.mapView.onDetach()
        presenter.destroy()
        super.onDestroy()
    }

    private fun hasLocationPermission() = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
    ).any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private fun configureMap() {
        binding.mapContent.mapView.apply {
            setTileSource(XYTileSource(
                "CartoDB-Positron", 0, 19, 256, ".png",
                arrayOf(
                    "https://a.basemaps.cartocdn.com/light_all/",
                    "https://b.basemaps.cartocdn.com/light_all/",
                    "https://c.basemaps.cartocdn.com/light_all/"
                )
            ))
            setBuiltInZoomControls(false)
            setMultiTouchControls(true)
            controller.setZoom(2.0)
            controller.setCenter(GeoPoint(0.0, 0.0))
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
            binding.mapContent.mapView.controller.setZoom(15.0)
            binding.mapContent.mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude))
        }
        renderOccurrences()
    }

    override fun showError() {
        nearby = NearbyOccurrences(null, emptyList())
        adapter.submitList(emptyList())
        binding.mapContent.mapStateText.setText(R.string.map_error)
        binding.mapContent.mapStateText.visibility = View.VISIBLE
        binding.mapContent.occurrenceCountText.setText(R.string.map_error)
        binding.mapContent.mapView.overlays.clear()
        binding.mapContent.mapView.invalidate()
    }

    private fun renderOccurrences() {
        val items = nearby.occurrences.filter { selectedUrgency == null || it.urgency == selectedUrgency }
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

    private fun updateMarkers(items: List<PublicOccurrence>) {
        val map = binding.mapContent.mapView
        map.overlays.clear()
        nearby.userLocation?.let { location ->
            val marker = Marker(map)
            marker.position = GeoPoint(location.latitude, location.longitude)
            marker.title = getString(R.string.map_your_location)
            marker.icon = ContextCompat.getDrawable(this, R.drawable.ic_map_pin)?.mutate()?.apply {
                setTint(ContextCompat.getColor(this@MapActivity, R.color.pine800))
            }
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(marker)
        }
        items.forEach { occurrence ->
            val marker = Marker(map)
            marker.position = GeoPoint(occurrence.coordinates.latitude, occurrence.coordinates.longitude)
            marker.title = title(occurrence)
            marker.snippet = getString(R.string.map_point_distance, distanceFormat.format(occurrence.distanceKm))
            marker.icon = ContextCompat.getDrawable(this, R.drawable.ic_map_pin)?.mutate()?.apply {
                setTint(ContextCompat.getColor(this@MapActivity, urgencyColor(occurrence.urgency)))
            }
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.setOnMarkerClickListener { _, _ -> selectOccurrence(occurrence); true }
            map.overlays.add(marker)
        }
        map.invalidate()
    }

    private fun selectOccurrence(item: PublicOccurrence) {
        val index = adapter.currentList.indexOfFirst { it.id == item.id }
        if (index >= 0) binding.mapContent.recyclerOccurrences.smoothScrollToPosition(index)
        sheet?.state = BottomSheetBehavior.STATE_EXPANDED
        binding.mapContent.mapView.controller.animateTo(
            GeoPoint(item.coordinates.latitude, item.coordinates.longitude)
        )
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
}
