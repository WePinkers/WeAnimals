package com.example.weanimals.lostandfound.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.core.utils.ImageThumbnailLoader
import com.example.weanimals.databinding.ActivityLostAndFoundMapBinding
import com.example.weanimals.lostandfound.model.FoundPetAd
import com.example.weanimals.lostandfound.repository.LostAndFoundRepository
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LostAndFoundMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLostAndFoundMapBinding
    private var map: MapLibreMap? = null
    private var currentFilter = MapFilter.ALL
    private var selectedAd: FoundPetAd? = null
    private var isPanelExpanded = false

    private val adMarkerMap = mutableMapOf<Long, FoundPetAd>()

    private enum class MapFilter { ALL, LOST, FOUND }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { loadMapData() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        binding = ActivityLostAndFoundMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapView.onCreate(savedInstanceState)

        binding.includeHeader.headerTitleTv.text = "Mapa de animais"
        binding.includeHeader.headerBackBtn.setOnClickListener { finish() }

        setupFilters()
        setupListeners()

        binding.mapView.getMapAsync { readyMap ->
            map = readyMap
            readyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-23.5135, -46.5812), 13.0))

            readyMap.setOnMarkerClickListener { marker ->
                adMarkerMap[marker.id]?.let { ad ->
                    selectPetOnMap(ad, marker.position)
                    true
                } ?: false
            }

            readyMap.addOnMapClickListener {
                binding.cardSelectedPetDetail.visibility = View.GONE
                selectedAd = null
                false
            }

            loadMapData()

            // Check if launched with a focused ad ID (e.g. from Matches "Ver no mapa")
            val focusAdId = intent.getStringExtra(EXTRA_FOCUS_AD_ID)
            if (!focusAdId.isNullOrBlank()) {
                val focusAd = LostAndFoundRepository.getAdById(this, focusAdId)
                if (focusAd != null) {
                    val coords = focusAd.getMapLatLng()
                    val targetPos = LatLng(coords.first, coords.second)
                    readyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetPos, 15.0))
                    selectPetOnMap(focusAd, targetPos)
                }
            }
        }

        if (!hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        loadMapData()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding.mapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        map = null
        binding.mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    private fun hasLocationPermission(): Boolean {
        return listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun setupFilters() {
        binding.chipMapAll.setOnClickListener { selectFilter(MapFilter.ALL) }
        binding.chipMapLost.setOnClickListener { selectFilter(MapFilter.LOST) }
        binding.chipMapFound.setOnClickListener { selectFilter(MapFilter.FOUND) }
    }

    private fun selectFilter(filter: MapFilter) {
        currentFilter = filter
        val activeBg = R.drawable.bg_chip_selected
        val inactiveBg = R.drawable.bg_chip_unselected
        val activeColor = getColor(R.color.white)
        val inactiveColor = getColor(R.color.ink)

        binding.chipMapAll.setBackgroundResource(if (filter == MapFilter.ALL) activeBg else inactiveBg)
        binding.chipMapAll.setTextColor(if (filter == MapFilter.ALL) activeColor else inactiveColor)

        binding.chipMapLost.setBackgroundResource(if (filter == MapFilter.LOST) activeBg else inactiveBg)
        binding.chipMapLost.setTextColor(if (filter == MapFilter.LOST) activeColor else inactiveColor)

        binding.chipMapFound.setBackgroundResource(if (filter == MapFilter.FOUND) activeBg else inactiveBg)
        binding.chipMapFound.setTextColor(if (filter == MapFilter.FOUND) activeColor else inactiveColor)

        loadMapData()
    }

    private fun setupListeners() {
        binding.btnCloseSelectedCard.setOnClickListener {
            binding.cardSelectedPetDetail.visibility = View.GONE
            selectedAd = null
        }

        binding.headerTogglePanel.setOnClickListener {
            isPanelExpanded = !isPanelExpanded
            binding.scrollMapReports.visibility = if (isPanelExpanded) View.VISIBLE else View.GONE
            binding.ivPanelChevron.animate().rotation(if (isPanelExpanded) 90f else 270f).setDuration(150).start()
        }

        binding.btnSelectedViewAd.setOnClickListener {
            val ad = selectedAd ?: return@setOnClickListener
            val intent = Intent(this, PublicAdDetailActivity::class.java).apply {
                putExtra(PublicAdDetailActivity.EXTRA_AD_ID, ad.id)
            }
            startActivity(intent)
        }

        binding.btnSelectedAction.setOnClickListener {
            val ad = selectedAd ?: return@setOnClickListener
            if (ad.category.equals("PERDIDO", ignoreCase = true)) {
                val intent = Intent(this, SightingFormActivity::class.java).apply {
                    putExtra(SightingFormActivity.EXTRA_AD_ID, ad.id)
                }
                startActivity(intent)
            } else {
                val intent = Intent(this, TutorVerificationActivity::class.java).apply {
                    putExtra(TutorVerificationActivity.EXTRA_AD_ID, ad.id)
                }
                startActivity(intent)
            }
        }
    }

    private fun loadMapData() {
        val readyMap = map ?: return
        readyMap.clear()
        adMarkerMap.clear()

        val allAds = LostAndFoundRepository.getAllAds(this)
        val activeAds = allAds
            .filter { !it.isEnded && !it.isReunited && it.getRemainingDays() > 0 }
            .filter { ad ->
                when (currentFilter) {
                    MapFilter.ALL -> true
                    MapFilter.LOST -> ad.category.equals("PERDIDO", ignoreCase = true)
                    MapFilter.FOUND -> ad.category.equals("ENCONTRADO", ignoreCase = true)
                }
            }

        binding.tvReportsCountSubtitle.text = "${activeAds.size} ${if (activeAds.size == 1) "nesta região" else "nesta região"}"

        val lostIcon = createMarkerIcon(getColor(R.color.terracotta))
        val foundIcon = createMarkerIcon(getColor(R.color.pine800))

        for (ad in activeAds) {
            val coords = ad.getMapLatLng()
            val pos = LatLng(coords.first, coords.second)
            val icon = if (ad.category.equals("PERDIDO", ignoreCase = true)) lostIcon else foundIcon

            val marker = readyMap.addMarker(
                MarkerOptions()
                    .position(pos)
                    .title(ad.getDisplayTitle())
                    .snippet(ad.getDisplayLocation())
                    .icon(icon)
            )
            adMarkerMap[marker.id] = ad
        }

        renderReportsList(activeAds)
    }

    private fun selectPetOnMap(ad: FoundPetAd, pos: LatLng) {
        selectedAd = ad
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15.0), 300)

        val isLost = ad.category.equals("PERDIDO", ignoreCase = true)

        binding.tvSelectedPetCategoryBadge.text = if (isLost) "ANIMAL PERDIDO" else "ANIMAL ENCONTRADO"
        binding.tvSelectedPetCategoryBadge.setBackgroundResource(if (isLost) R.drawable.bg_badge_lost else R.drawable.bg_badge_found)

        binding.tvSelectedPetTitle.text = ad.getDisplayTitle()

        val verbLocation = if (isLost) "Onde foi visto pela última vez" else "Onde foi encontrado"
        binding.tvSelectedPetLocation.text = "$verbLocation: ${ad.getDisplayLocation()}"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
        val dateStr = dateFormat.format(Date(ad.publicationTimestamp))
        binding.tvSelectedPetDate.text = "Data: $dateStr"

        binding.btnSelectedAction.text = if (isLost) "Acho que encontrei" else "Acho que é meu"

        if (ad.photoUris.isNotEmpty()) {
            lifecycleScope.launch {
                val bitmap = ImageThumbnailLoader.loadThumbnail(this@LostAndFoundMapActivity, ad.photoUris.first(), 120, 120)
                if (bitmap != null) {
                    binding.ivSelectedPetThumb.setImageBitmap(bitmap)
                } else {
                    binding.ivSelectedPetThumb.setImageResource(R.drawable.ic_paw_print)
                }
            }
        } else {
            binding.ivSelectedPetThumb.setImageResource(R.drawable.ic_paw_print)
        }

        binding.panelReportsList.visibility = View.GONE
        binding.cardSelectedPetDetail.visibility = View.VISIBLE
    }

    private fun renderReportsList(ads: List<FoundPetAd>) {
        val container = binding.containerMapReportsList
        container.removeAllViews()

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

        for (ad in ads) {
            val itemRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 14, 16, 14)
                gravity = android.view.Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setBackgroundResource(R.drawable.bg_card_white_border)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 10)
                layoutParams = params

                setOnClickListener {
                    val coords = ad.getMapLatLng()
                    val pos = LatLng(coords.first, coords.second)
                    selectPetOnMap(ad, pos)
                }
            }

            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(40, 40)
                setImageResource(R.drawable.ic_paw_print)
                setColorFilter(getColor(R.color.pine800))
            }

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 12
                }
            }

            val titleTv = TextView(this).apply {
                text = ad.getDisplayTitle()
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.pine800))
            }

            val subTv = TextView(this).apply {
                text = "${ad.getDisplayLocation()} · ${dateFormat.format(Date(ad.publicationTimestamp))}"
                textSize = 11f
                setTextColor(getColor(R.color.muted))
            }

            info.addView(titleTv)
            info.addView(subTv)

            val badge = TextView(this).apply {
                val isLost = ad.category.equals("PERDIDO", ignoreCase = true)
                text = if (isLost) "PERDIDO" else "ENCONTRADO"
                textSize = 10f
                setTypeface(null, Typeface.BOLD)
                setTextColor(getColor(R.color.white))
                setBackgroundResource(if (isLost) R.drawable.bg_badge_lost else R.drawable.bg_badge_found)
                setPadding(12, 4, 12, 4)
            }

            itemRow.addView(icon)
            itemRow.addView(info)
            itemRow.addView(badge)

            container.addView(itemRow)
        }
    }

    private fun createMarkerIcon(colorInt: Int): Icon {
        val size = (32 * resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = colorInt
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 4f, paint)
        canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 4f, borderPaint)

        return IconFactory.getInstance(this).fromBitmap(bitmap)
    }

    companion object {
        const val EXTRA_FOCUS_AD_ID = "extra_focus_ad_id"
    }
}
