package com.example.weanimals.screens

import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.R
import com.example.weanimals.databinding.FragmentMapBinding
import com.example.weanimals.databinding.ItemOccurrenceBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

data class OccurrenceItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val urgencyLevel: String,
    val badgeText: String,
    val badgeBgColorRes: Int,
    val badgeTextColorRes: Int,
    val indicatorColorRes: Int,
    val pinColorTintRes: Int,
    val geoPoint: GeoPoint
)

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding: FragmentMapBinding get() = _binding!!

    private lateinit var adapter: OccurrenceAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val userLocation = GeoPoint(-23.5350, -46.6730)

    private val allOccurrences = listOf(
        OccurrenceItem(
            id = "1",
            title = "Filhote — Rua Aimberê",
            subtitle = "420 m · protocolo #4471",
            urgencyLevel = "Emergência",
            badgeText = "Novo",
            badgeBgColorRes = R.drawable.bg_new_badge,
            badgeTextColorRes = R.color.alert600,
            indicatorColorRes = R.color.alert600,
            pinColorTintRes = R.color.alert600,
            geoPoint = GeoPoint(-23.5325, -46.6710)
        ),
        OccurrenceItem(
            id = "2",
            title = "Cão idoso — Vila Marlene",
            subtitle = "1,1 km · equipe a caminho",
            urgencyLevel = "Urgente",
            badgeText = "Em curso",
            badgeBgColorRes = R.drawable.bg_progress_badge,
            badgeTextColorRes = R.color.brass500,
            indicatorColorRes = R.color.brass500,
            pinColorTintRes = R.color.brass500,
            geoPoint = GeoPoint(-23.5380, -46.6760)
        ),
        OccurrenceItem(
            id = "3",
            title = "Gato preso em terreno",
            subtitle = "850 m · protocolo #4473",
            urgencyLevel = "Muito urgente",
            badgeText = "Novo",
            badgeBgColorRes = R.drawable.bg_new_badge,
            badgeTextColorRes = R.color.alert600,
            indicatorColorRes = R.color.clay600,
            pinColorTintRes = R.color.clay600,
            geoPoint = GeoPoint(-23.5310, -46.6780)
        ),
        OccurrenceItem(
            id = "4",
            title = "Cão Comunitário",
            subtitle = "1,8 km · em acompanhamento",
            urgencyLevel = "Pouco urgente",
            badgeText = "Em curso",
            badgeBgColorRes = R.drawable.bg_progress_badge,
            badgeTextColorRes = R.color.brass500,
            indicatorColorRes = R.color.pine700,
            pinColorTintRes = R.color.pine700,
            geoPoint = GeoPoint(-23.5410, -46.6690)
        )
    )

    private val markersMap = mutableMapOf<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = requireContext().applicationContext
        val config = Configuration.getInstance()
        config.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        config.userAgentValue = "WeAnimalsApp/1.0 (com.example.weanimals)"

        // Clear stale 403 tiles
        try {
            val tileCache = config.osmdroidTileCache
            if (tileCache.exists()) {
                tileCache.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Allow network tile fetching smoothly on main thread
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mapBinding = FragmentMapBinding.inflate(inflater, container, false)
        _binding = mapBinding
        return mapBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMap()
        setupBottomSheet()
        setupRecyclerView()
        setupFilterChips()

        updateMapMarkers(allOccurrences)
        adapter.submitList(allOccurrences)
        updateCountText(allOccurrences.size)
    }

    private fun setupMap() {
        val mapView = binding.mapView

        // Clean modern pastel CartoDB Positron tile source (free, no 403, no API key required)
        val cartoDbSource = XYTileSource(
            "CartoDB-Positron",
            0, 19, 256, ".png",
            arrayOf(
                "https://a.basemaps.cartocdn.com/light_all/",
                "https://b.basemaps.cartocdn.com/light_all/",
                "https://c.basemaps.cartocdn.com/light_all/"
            )
        )
        mapView.setTileSource(cartoDbSource)
        mapView.setBuiltInZoomControls(false)
        mapView.setMultiTouchControls(true)

        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(userLocation)
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        val density = resources.displayMetrics.density
        bottomSheetBehavior.peekHeight = (260 * density).toInt()
        bottomSheetBehavior.isHideable = false

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    binding.legendContainer.visibility = View.GONE
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    binding.legendContainer.visibility = View.VISIBLE
                    binding.legendContainer.alpha = 1.0f
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val alpha = (1.0f - (slideOffset * 1.5f)).coerceIn(0.0f, 1.0f)
                binding.legendContainer.alpha = alpha
                binding.legendContainer.visibility = if (alpha <= 0.05f) View.GONE else View.VISIBLE
            }
        })
    }

    private fun setupRecyclerView() {
        adapter = OccurrenceAdapter { item ->
            selectOccurrenceItem(item, animateMap = true)
        }
        binding.recyclerOccurrences.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerOccurrences.adapter = adapter
    }

    private fun setupFilterChips() {
        // Ensure checked icon ✔ is hidden and initial style is applied
        updateChipsVisualState(R.id.chip_all)

        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedChipId = checkedIds.firstOrNull() ?: R.id.chip_all

            updateChipsVisualState(selectedChipId)

            val filteredList = when (selectedChipId) {
                R.id.chip_urgente -> allOccurrences.filter { it.urgencyLevel == "Urgente" }
                R.id.chip_em_curso -> allOccurrences.filter { it.badgeText == "Em curso" }
                R.id.chip_emergencia -> allOccurrences.filter { it.urgencyLevel == "Emergência" }
                else -> allOccurrences
            }

            adapter.submitList(filteredList)
            updateCountText(filteredList.size)
            updateMapMarkers(filteredList)
        }
    }

    private fun updateChipsVisualState(selectedChipId: Int) {
        val chips = listOf(
            binding.chipAll,
            binding.chipUrgente,
            binding.chipEmCurso,
            binding.chipEmergencia
        )
        val density = resources.displayMetrics.density

        chips.forEach { chip ->
            chip.isCheckedIconVisible = false
            val isSelected = (chip.id == selectedChipId)
            if (isSelected) {
                chip.setChipBackgroundColorResource(R.color.pine800)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                chip.chipStrokeWidth = 0f
                chip.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start()
                chip.requestRectangleOnScreen(android.graphics.Rect(0, 0, chip.width, chip.height))
            } else {
                chip.setChipBackgroundColorResource(R.color.white)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink900))
                chip.setChipStrokeColorResource(R.color.line_border)
                chip.chipStrokeWidth = 1f * density
                chip.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
            }
        }
    }

    private fun updateMapMarkers(items: List<OccurrenceItem>) {
        val mapView = binding.mapView
        mapView.overlays.clear()
        markersMap.clear()

        // Add User Location Marker
        val userMarker = Marker(mapView)
        userMarker.position = userLocation
        userMarker.title = "Sua localização"
        val userIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pin)?.mutate()
        userIcon?.setTint(ContextCompat.getColor(requireContext(), R.color.pine800))
        userMarker.icon = userIcon
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(userMarker)

        // Add Occurrence Markers
        items.forEach { item ->
            val marker = Marker(mapView)
            marker.position = item.geoPoint
            marker.title = item.title
            marker.snippet = item.subtitle

            val pinIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pin)?.mutate()
            pinIcon?.setTint(ContextCompat.getColor(requireContext(), item.pinColorTintRes))
            marker.icon = pinIcon
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            marker.setOnMarkerClickListener { _, _ ->
                selectOccurrenceItem(item, animateMap = false)
                true
            }

            mapView.overlays.add(marker)
            markersMap[item.id] = marker
        }

        mapView.invalidate()
    }

    private fun selectOccurrenceItem(item: OccurrenceItem, animateMap: Boolean) {
        val index = adapter.currentList.indexOfFirst { it.id == item.id }
        if (index != -1) {
            adapter.selectedPosition = index
            adapter.notifyItemChanged(index)
            binding.recyclerOccurrences.smoothScrollToPosition(index)

            highlightLegendCategory(item.urgencyLevel)

            if (animateMap) {
                binding.mapView.controller.animateTo(item.geoPoint)
            }

            val marker = markersMap[item.id]
            marker?.showInfoWindow()

            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun highlightLegendCategory(urgencyLevel: String) {
        val isRed = urgencyLevel == "Emergência" || urgencyLevel == "Muito urgente"
        val isYellow = urgencyLevel == "Urgente"
        val isGreen = urgencyLevel == "Pouco urgente"

        binding.indicatorRed.visibility = if (isRed) View.VISIBLE else View.INVISIBLE
        binding.indicatorYellow.visibility = if (isYellow) View.VISIBLE else View.INVISIBLE
        binding.indicatorGreen.visibility = if (isGreen) View.VISIBLE else View.INVISIBLE

        binding.legendRed.alpha = if (isRed) 1.0f else 0.6f
        binding.legendYellow.alpha = if (isYellow) 1.0f else 0.6f
        binding.legendGreen.alpha = if (isGreen) 1.0f else 0.6f

        binding.legendRed.animate().scaleX(if (isRed) 1.05f else 1.0f).scaleY(if (isRed) 1.05f else 1.0f).setDuration(150).start()
        binding.legendYellow.animate().scaleX(if (isYellow) 1.05f else 1.0f).scaleY(if (isYellow) 1.05f else 1.0f).setDuration(150).start()
        binding.legendGreen.animate().scaleX(if (isGreen) 1.05f else 1.0f).scaleY(if (isGreen) 1.05f else 1.0f).setDuration(150).start()
    }

    private fun updateCountText(count: Int) {
        binding.occurrenceCountText.text = "$count ocorrências em um raio de 2 km"
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDetach()
        _binding = null
    }
}

class OccurrenceAdapter(
    private val onItemClick: (OccurrenceItem) -> Unit
) : ListAdapter<OccurrenceItem, OccurrenceAdapter.OccurrenceViewHolder>(DiffCallback) {

    var selectedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OccurrenceViewHolder {
        val binding = ItemOccurrenceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OccurrenceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OccurrenceViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    inner class OccurrenceViewHolder(
        private val binding: ItemOccurrenceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OccurrenceItem, isSelected: Boolean) {
            binding.occurrenceTitle.text = item.title
            binding.occurrenceSubtitle.text = item.subtitle
            binding.occurrenceBadge.text = item.badgeText
            binding.occurrenceBadge.setBackgroundResource(item.badgeBgColorRes)
            binding.occurrenceBadge.setTextColor(
                ContextCompat.getColor(binding.root.context, item.badgeTextColorRes)
            )
            binding.urgencyIndicator.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, item.indicatorColorRes)
            )

            binding.cardRoot.alpha = if (isSelected) 1.0f else 0.95f

            binding.root.setOnClickListener {
                val prev = selectedPosition
                selectedPosition = bindingAdapterPosition
                if (prev != -1) notifyItemChanged(prev)
                notifyItemChanged(selectedPosition)
                onItemClick(item)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<OccurrenceItem>() {
        override fun areItemsTheSame(oldItem: OccurrenceItem, newItem: OccurrenceItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OccurrenceItem, newItem: OccurrenceItem): Boolean {
            return oldItem == newItem
        }
    }
}
