package com.example.weanimals.organization.dashboard.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.databinding.ActivityOrganizationReportMapBinding
import com.example.weanimals.organization.dashboard.domain.OrganizationReport
import com.example.weanimals.organization.dashboard.domain.OrganizationReportStatus
import com.example.weanimals.organization.dashboard.presenter.OrganizationReportDetailContract
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

class OrganizationReportMapActivity : AppCompatActivity(), OrganizationReportDetailContract.View {

    private lateinit var binding: ActivityOrganizationReportMapBinding
    private var map: MapLibreMap? = null
    private var currentReport: OrganizationReport? = null
    private val reportId by lazy { intent.getStringExtra(EXTRA_REPORT_ID).orEmpty() }
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer
            .createOrganizationReportDetailPresenter(reportId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityOrganizationReportMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.organizationMapView.onCreate(savedInstanceState)
        configureMap()
        binding.organizationMapBack.setOnClickListener { finish() }
        binding.organizationMapAttribution.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MAP_ATTRIBUTION_URL)))
        }
        binding.organizationMapRouteButton.setOnClickListener {
            currentReport?.let(::openRouteChooser)
        }
        binding.organizationMapReportCard.visibility = View.INVISIBLE
    }

    override fun onStart() {
        super.onStart()
        binding.organizationMapView.onStart()
        presenter.attachView(this)
        presenter.load()
    }

    override fun onResume() {
        super.onResume()
        binding.organizationMapView.onResume()
    }

    override fun onPause() {
        binding.organizationMapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        presenter.detachView()
        binding.organizationMapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        map = null
        binding.organizationMapView.onDestroy()
        presenter.destroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.organizationMapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.organizationMapView.onSaveInstanceState(outState)
    }

    override fun showLoading() {
        binding.organizationMapReportCard.visibility = View.INVISIBLE
    }

    override fun showReport(report: OrganizationReport) {
        currentReport = report
        val style = classificationStyle(report.triageClassification)
        binding.organizationMapReportCard.visibility = View.VISIBLE
        binding.organizationMapReportClassification.text = getString(style.labelRes)
        binding.organizationMapReportClassification.setTextColor(getColor(style.textColorRes))
        binding.organizationMapReportClassification.setBackgroundResource(style.backgroundRes)
        binding.organizationMapReportTitle.text = getString(
            R.string.organization_map_report_title_format,
            report.protocolNumber,
            animalName(report.animalType),
            shortAddress(report.address)
        )
        binding.organizationMapReportProtocol.text = getString(
            R.string.organization_map_protocol_format,
            report.protocolNumber,
            getString(style.labelRes).lowercase()
        )
        binding.organizationMapReportAddress.text = fullAddress(report)
        renderMapLocation(report)
    }

    override fun showError(error: Throwable) {
        Toast.makeText(this, R.string.organization_reports_error, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun showStatusError(error: Throwable) = Unit

    private fun configureMap() {
        binding.organizationMapView.getMapAsync { readyMap ->
            map = readyMap
            readyMap.uiSettings.setAllGesturesEnabled(true)
            readyMap.uiSettings.isLogoEnabled = false
            readyMap.uiSettings.isAttributionEnabled = false
            readyMap.setStyle(MAP_STYLE_URL) {
                currentReport?.let(::renderMapLocation)
            }
        }
    }

    private fun renderMapLocation(report: OrganizationReport) {
        val readyMap = map ?: return
        if (readyMap.style == null) {
            readyMap.setStyle(MAP_STYLE_URL) { renderMapLocation(report) }
            return
        }
        val location = LatLng(report.latitude, report.longitude)
        readyMap.removeAnnotations()
        val markerDrawable = ContextCompat.getDrawable(this, R.drawable.ic_map_pin)
            ?.mutate()
            ?.apply { setTint(ContextCompat.getColor(this@OrganizationReportMapActivity, markerColor(report.triageClassification))) }
        val markerIcon = markerDrawable?.toBitmap()?.let(IconFactory.getInstance(this)::fromBitmap)
        val marker = MarkerOptions()
            .position(location)
            .title(getString(R.string.organization_detail_map_pin))
        markerIcon?.let(marker::icon)
        readyMap.addMarker(marker)
        readyMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, MAP_DETAIL_ZOOM))
    }

    private fun openRouteChooser(report: OrganizationReport) {
        val address = fullAddress(report)
        val google = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=${Uri.encode(address)}&mode=d")
        ).setPackage(GOOGLE_MAPS_PACKAGE)
        val waze = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("waze://?ll=${report.latitude},${report.longitude}&navigate=yes")
        ).setPackage(WAZE_PACKAGE)
        val available = listOf(google, waze)
            .filter { it.resolveActivity(packageManager) != null }
        val fallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:${report.latitude},${report.longitude}?q=${report.latitude},${report.longitude}(${Uri.encode(address)})")
        )

        try {
            when {
                available.size >= 2 -> {
                    val chooser = Intent.createChooser(
                        available.first(),
                        getString(R.string.organization_map_route_chooser)
                    ).apply {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, available.drop(1).toTypedArray())
                    }
                    startActivity(chooser)
                }
                available.size == 1 -> startActivity(available.first())
                fallback.resolveActivity(packageManager) != null -> startActivity(fallback)
                else -> showRouteError()
            }
        } catch (_: Exception) {
            showRouteError()
        }
    }

    private fun showRouteError() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.organization_map_route_error)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun classificationStyle(value: String?): ClassificationStyle = when (value?.trim()?.lowercase()) {
        TRIAGE_CLASSIFICATION_VERY_URGENT -> ClassificationStyle(R.string.organization_badge_very_urgent, R.drawable.bg_org_classification_very_urgent, R.color.alert600)
        TRIAGE_CLASSIFICATION_URGENT -> ClassificationStyle(R.string.organization_badge_urgent, R.drawable.bg_org_classification_urgent, R.color.terracotta_dark)
        TRIAGE_CLASSIFICATION_LOW -> ClassificationStyle(R.string.organization_badge_low, R.drawable.bg_org_classification_low, R.color.low_urgency)
        else -> ClassificationStyle(R.string.organization_badge_priority, R.drawable.bg_org_classification_priority, R.color.terracotta_dark)
    }

    private fun markerColor(value: String?): Int = when (value?.trim()?.lowercase()) {
        TRIAGE_CLASSIFICATION_VERY_URGENT -> R.color.alert600
        TRIAGE_CLASSIFICATION_URGENT -> R.color.brass500
        TRIAGE_CLASSIFICATION_LOW -> R.color.low_urgency
        else -> R.color.gold
    }

    private fun animalName(type: String) = when (type) {
        "cat" -> getString(R.string.cat)
        "other" -> getString(R.string.other)
        else -> getString(R.string.dog)
    }

    private fun shortAddress(address: String): String {
        val parts = address.split("—").map(String::trim).filter(String::isNotBlank)
        val compact = if (parts.size >= 2) parts.takeLast(2).joinToString(" — ") else address.trim()
        return compact.take(MAX_ADDRESS_LENGTH).trimEnd().let { value ->
            if (compact.length > MAX_ADDRESS_LENGTH) "$value…" else value
        }
    }

    private fun fullAddress(report: OrganizationReport): String = listOf(
        report.address,
        report.addressDetail
    ).map(String::trim).filter(String::isNotBlank).joinToString(", ")

    private data class ClassificationStyle(
        val labelRes: Int,
        val backgroundRes: Int,
        val textColorRes: Int
    )

    companion object {
        private const val EXTRA_REPORT_ID = "extra_organization_report_id"
        private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
        private const val MAP_ATTRIBUTION_URL = "https://openfreemap.org/"
        private const val MAP_DETAIL_ZOOM = 15.0
        private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
        private const val WAZE_PACKAGE = "com.waze"
        private const val TRIAGE_CLASSIFICATION_VERY_URGENT = "very_urgent"
        private const val TRIAGE_CLASSIFICATION_URGENT = "urgent"
        private const val TRIAGE_CLASSIFICATION_LOW = "low"
        private const val MAX_ADDRESS_LENGTH = 72

        fun newIntent(context: Context, reportId: String) =
            Intent(context, OrganizationReportMapActivity::class.java)
                .putExtra(EXTRA_REPORT_ID, reportId)
    }
}
