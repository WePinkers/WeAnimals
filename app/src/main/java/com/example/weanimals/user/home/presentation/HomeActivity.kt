package com.example.weanimals.user.home.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.ActivityHomeBinding
import com.example.weanimals.databinding.ItemReportBinding
import com.example.weanimals.user.home.presenter.HomeContract
import com.example.weanimals.user.home.presenter.HomePresenter
import com.example.weanimals.user.map.overview.domain.PublicOccurrence
import com.example.weanimals.user.map.overview.interactor.NearbyOccurrences
import com.example.weanimals.user.map.overview.presentation.MapActivity
import com.example.weanimals.user.map.overview.presenter.MapContract
import com.example.weanimals.user.reporting.report.domain.Report
import com.example.weanimals.user.reporting.report.presentation.ReportActivity
import com.example.weanimals.user.reporting.tracking.presentation.TrackingActivity
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

class HomeActivity : AppCompatActivity(), HomeContract.View {

    private lateinit var binding: ActivityHomeBinding
    private val presenter: HomePresenter by lazy {
        (application as WeAnimalsApplication).appContainer.createHomePresenter()
    }
    private val mapPresenter by lazy {
        (application as WeAnimalsApplication).appContainer.createMapPresenter()
    }
    private var previewMap: MapLibreMap? = null
    private var nearbyData: NearbyOccurrences? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.w(TAG, "Notification permission was not granted by user.")
        }
    }

    private val mapViewContract = object : MapContract.View {
        override fun showLoading() {
            binding.mapPreview.mapChip.setText(R.string.map_loading)
        }

        override fun showOccurrences(result: NearbyOccurrences) {
            nearbyData = result
            val count = result.occurrences.size
            binding.mapPreview.mapChip.text = resources.getQuantityString(
                R.plurals.map_occurrences_nearby, count, count
            )
            renderPreviewMarkers()
        }

        override fun showError() {
            binding.mapPreview.mapChip.setText(R.string.map_error)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        configureSystemBars()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mapPreview.mapView.onCreate(savedInstanceState)
        setupInteractions()
        configureMapPreview()
        checkNotificationPermission()
        MainNavigation.bind(this, binding.mainNavigation, MainNavigation.Destination.HOME)
    }

    override fun onStart() {
        super.onStart()
        binding.mapPreview.mapView.onStart()
        presenter.attachView(this)
        presenter.loadReports()
        mapPresenter.attachView(mapViewContract)
        mapPresenter.load()
    }

    override fun onResume() {
        super.onResume()
        binding.mapPreview.mapView.onResume()
    }

    override fun onPause() {
        binding.mapPreview.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        presenter.detachView()
        mapPresenter.detachView()
        binding.mapPreview.mapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        previewMap = null
        binding.mapPreview.mapView.onDestroy()
        presenter.destroy()
        mapPresenter.destroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapPreview.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapPreview.mapView.onSaveInstanceState(outState)
    }

    override fun showGreeting(name: String?) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val timeGreetingRes = when (hour) {
            in 5..11 -> R.string.greeting_bom_dia
            in 12..17 -> R.string.greeting_boa_tarde
            else -> R.string.greeting_boa_noite
        }
        val timeGreeting = getString(timeGreetingRes)

        val cleanName = name?.trim()?.uppercase()
        val formattedGreeting = if (!cleanName.isNullOrBlank() && cleanName != getString(R.string.profile_default_name).uppercase()) {
            getString(R.string.home_greeting_with_name, timeGreeting, cleanName)
        } else {
            val fallbackName = getString(R.string.profile_default_name).uppercase()
            getString(R.string.home_greeting_with_name, timeGreeting, fallbackName)
        }

        binding.greetingText.text = formattedGreeting
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun configureMapPreview() {
        binding.mapPreview.mapView.getMapAsync { readyMap ->
            previewMap = readyMap
            readyMap.uiSettings.setAllGesturesEnabled(false)
            readyMap.setStyle("https://tiles.openfreemap.org/styles/positron") {
                renderPreviewMarkers()
            }
        }
    }

    private fun setupInteractions() {
        binding.reportNowButton.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }
        val openMap = View.OnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
        binding.viewMapButton.setOnClickListener(openMap)
        binding.mapPreview.mapRoot.setOnClickListener(openMap)
        binding.mapPreview.mapClickOverlay.setOnClickListener(openMap)
    }

    private fun renderPreviewMarkers() {
        val readyMap = previewMap ?: return
        val data = nearbyData ?: return

        fun drawOnMap() {
            readyMap.removeAnnotations()
            val iconFactory = IconFactory.getInstance(this)

            // 1. User Location Marker: Stylized Blue Circular Avatar
            data.userLocation?.let { location ->
                val userIconDrawable = ContextCompat.getDrawable(this, R.drawable.ic_user_location)
                val userBitmap = userIconDrawable?.toBitmap()
                val userIcon = userBitmap?.let(iconFactory::fromBitmap)

                val userMarker = MarkerOptions()
                    .position(LatLng(location.latitude, location.longitude))
                    .title(getString(R.string.map_your_location))
                userIcon?.let(userMarker::icon)

                readyMap.addMarker(userMarker)
                readyMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude), 15.0
                ))
            }

            // 2. Group Occurrences by Location
            val locationGroups = mutableMapOf<Pair<Long, Long>, MutableList<PublicOccurrence>>()
            data.occurrences.forEach { occurrence ->
                val latKey = (occurrence.coordinates.latitude * 10000).toLong()
                val lonKey = (occurrence.coordinates.longitude * 10000).toLong()
                val key = latKey to lonKey
                locationGroups.getOrPut(key) { mutableListOf() }.add(occurrence)
            }

            // 3. Render One Badged Marker per Location Group
            locationGroups.values.forEach { group ->
                val representative = group.first()
                val count = group.size
                val highestUrgency = highestUrgencyInGroup(group)
                val badgedBitmap = createBadgedPinBitmap(urgencyColor(highestUrgency), count)
                val icon = iconFactory.fromBitmap(badgedBitmap)

                val marker = MarkerOptions()
                    .position(LatLng(representative.coordinates.latitude, representative.coordinates.longitude))
                    .icon(icon)

                readyMap.addMarker(marker)
            }
        }

        if (readyMap.style != null) {
            drawOnMap()
        } else {
            readyMap.setStyle("https://tiles.openfreemap.org/styles/positron") {
                drawOnMap()
            }
        }
    }

    private fun highestUrgencyInGroup(group: List<PublicOccurrence>): String {
        return when {
            group.any { it.urgency == "high" } -> "high"
            group.any { it.urgency == "medium" } -> "medium"
            else -> "low"
        }
    }

    private fun urgencyColor(urgency: String) = when (urgency) {
        "high" -> R.color.alert600
        "medium" -> R.color.brass500
        else -> R.color.pine700
    }

    private fun createBadgedPinBitmap(colorRes: Int, count: Int): Bitmap {
        val baseDrawable = ContextCompat.getDrawable(this, R.drawable.ic_map_pin)
            ?.mutate()?.apply { setTint(ContextCompat.getColor(this@HomeActivity, colorRes)) }
            ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val density = resources.displayMetrics.density
        val pinSize = (24 * density).toInt()
        val badgeRadius = (6 * density).toInt()

        val badgeOffsetY = if (count > 1) (4 * density).toInt() else 0
        val bitmapWidth = pinSize + if (count > 1) (4 * density).toInt() else 0
        val bitmapHeight = pinSize + badgeOffsetY

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        baseDrawable.setBounds(0, badgeOffsetY, pinSize, pinSize + badgeOffsetY)
        baseDrawable.draw(canvas)

        if (count > 1) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(this@HomeActivity, R.color.clay600)
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(this@HomeActivity, R.color.white)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * density
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ContextCompat.getColor(this@HomeActivity, R.color.white)
                textSize = 8 * density
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

    fun showError() {}

    override fun showLoading() {
        Log.d("DEBUG_DENUNCIAS", "HomeActivity.showLoading chamado")
        binding.reportsContainer.removeAllViews()
        binding.reportsEmpty.setText(R.string.reports_loading)
        binding.reportsEmpty.visibility = View.VISIBLE
    }

    override fun showReports(reports: List<Report>) {
        Log.d("DEBUG_DENUNCIAS", "HomeActivity.showReports chamado com ${reports.size} itens")
        binding.reportsContainer.removeAllViews()
        binding.reportsEmpty.text = getString(R.string.no_reports)
        binding.reportsEmpty.visibility = if (reports.isEmpty()) View.VISIBLE else View.GONE
        reports.forEachIndexed { index, report ->
            val reportBinding = ItemReportBinding.inflate(
                layoutInflater,
                binding.reportsContainer,
                false
            )
            bindReport(reportBinding, ReportCardMapper.map(report))
            reportBinding.reportCard.setOnClickListener { presenter.openReport(report) }
            binding.reportsContainer.addView(reportBinding.root)
            Log.d("DEBUG_DENUNCIAS", "Item adicionado ao container [$index]: Protocolo=#${report.protocolNumber}, Endereço=${report.address}")
        }
    }

    override fun showReportsError(error: Throwable) {
        Log.e("DEBUG_DENUNCIAS", "HomeActivity.showReportsError chamado com erro: ${error.message}", error)
        binding.reportsContainer.removeAllViews()
        binding.reportsEmpty.text = getString(R.string.reports_load_error)
        binding.reportsEmpty.visibility = View.VISIBLE
    }

    private fun bindReport(binding: ItemReportBinding, report: ReportCardUiModel) {
        val animalName = getString(report.animalNameRes)
        val address = report.address.ifBlank { getString(R.string.location_unknown) }
        binding.reportTitle.text = getString(R.string.report_card_title, animalName, address)
        binding.reportProtocol.text = getString(R.string.report_protocol_format, report.protocolNumber)
        binding.reportAccent.setBackgroundColor(
            ContextCompat.getColor(this, report.urgencyAccentColorRes)
        )
        binding.reportStatus.text = getString(report.statusLabelRes)
        binding.reportStatus.visibility = if (report.showStatus) View.VISIBLE else View.GONE
        binding.reportStatus.setTextColor(ContextCompat.getColor(this, report.statusTextColorRes))
        binding.reportStatus.setBackgroundResource(report.statusBackgroundRes)
        binding.reportCard.contentDescription = getString(
            R.string.report_accessibility,
            animalName,
            address,
            getString(report.urgencyLabelRes),
            getString(report.statusLabelRes)
        )
    }

    override fun openReport(report: Report) {
        startActivity(TrackingActivity.newIntent(this, report.id))
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private companion object {
        const val TAG = "HomeActivity"
    }
}
