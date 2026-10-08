package com.example.weanimals.organization.dashboard.presentation

import android.content.Context
import android.content.res.ColorStateList
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.ImageViewCompat
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.databinding.ActivityOrganizationReportDetailBinding
import com.example.weanimals.organization.dashboard.domain.OrganizationReport
import com.example.weanimals.organization.dashboard.domain.OrganizationReportStatus
import com.example.weanimals.organization.dashboard.presenter.OrganizationReportDetailContract
import com.google.android.material.button.MaterialButton
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

class OrganizationReportDetailActivity : AppCompatActivity(), OrganizationReportDetailContract.View {

    private lateinit var binding: ActivityOrganizationReportDetailBinding
    private var detailMap: MapLibreMap? = null
    private var currentReport: OrganizationReport? = null
    private val reportId by lazy { intent.getStringExtra(EXTRA_REPORT_ID).orEmpty() }
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer
            .createOrganizationReportDetailPresenter(reportId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        configureSystemBars()
        binding = ActivityOrganizationReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.organizationDetailMapView.onCreate(savedInstanceState)
        configureMap()
        binding.organizationDetailMapAttribution.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MAP_ATTRIBUTION_URL)))
        }
        binding.organizationDetailMapExpand.setOnClickListener {
            startActivity(OrganizationReportMapActivity.newIntent(this, reportId))
        }
        binding.organizationDetailHeader.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        setupStatusActions()
        binding.organizationDetailDescriptionAction.setOnClickListener {
            showDescriptionDialog()
        }
        binding.organizationDetailContent.visibility = View.INVISIBLE
    }

    override fun onStart() {
        super.onStart()
        binding.organizationDetailMapView.onStart()
        presenter.attachView(this)
        presenter.load()
    }

    override fun onResume() {
        super.onResume()
        binding.organizationDetailMapView.onResume()
    }

    override fun onPause() {
        binding.organizationDetailMapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        presenter.detachView()
        binding.organizationDetailMapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        detailMap = null
        binding.organizationDetailMapView.onDestroy()
        presenter.destroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.organizationDetailMapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.organizationDetailMapView.onSaveInstanceState(outState)
    }

    override fun showLoading() {
        binding.organizationDetailContent.visibility = View.INVISIBLE
    }

    override fun showReport(report: OrganizationReport) {
        currentReport = report
        binding.organizationDetailContent.visibility = View.VISIBLE
        binding.organizationDetailHeader.headerTitle.text = getString(
            R.string.organization_detail_title_format,
            report.protocolNumber
        )
        styleClassification(report.triageClassification)
        binding.organizationDetailTitle.text = getString(
            R.string.organization_report_title_format,
            report.protocolNumber,
            animalName(report.animalType),
            shortAddress(report.address)
        )
        binding.organizationDetailDescriptionPreview.text = report.description
        binding.organizationDetailAgent.text = report.assignedAgent
            ?: getString(R.string.organization_detail_agent_none)
        binding.organizationDetailStatusDescription.text = report.statusDescription
        styleStatusButton(binding.organizationStatusCalled, report.status == OrganizationReportStatus.TEAM_CALLED)
        styleStatusButton(binding.organizationStatusOnWay, report.status == OrganizationReportStatus.TEAM_ON_WAY)
        styleStatusButton(binding.organizationStatusRescue, report.status == OrganizationReportStatus.RESCUE)
        styleStatusButton(binding.organizationStatusShelter, report.status == OrganizationReportStatus.SHELTER)
        renderMapLocation(report)
    }

    override fun showError(error: Throwable) {
        Toast.makeText(this, R.string.organization_reports_error, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun showStatusError(error: Throwable) {
        Toast.makeText(this, R.string.organization_status_update_error, Toast.LENGTH_LONG).show()
    }

    private fun setupStatusActions() {
        binding.organizationStatusCalled.setOnClickListener {
            presenter.updateStatus(OrganizationReportStatus.TEAM_CALLED)
        }
        binding.organizationStatusOnWay.setOnClickListener {
            presenter.updateStatus(OrganizationReportStatus.TEAM_ON_WAY)
        }
        binding.organizationStatusRescue.setOnClickListener {
            presenter.updateStatus(OrganizationReportStatus.RESCUE)
        }
        binding.organizationStatusShelter.setOnClickListener {
            presenter.updateStatus(OrganizationReportStatus.SHELTER)
        }
    }

    private fun configureMap() {
        binding.organizationDetailMapView.getMapAsync { readyMap ->
            detailMap = readyMap
            readyMap.uiSettings.setAllGesturesEnabled(true)
            readyMap.uiSettings.isLogoEnabled = false
            readyMap.uiSettings.isAttributionEnabled = false
            readyMap.setStyle(MAP_STYLE_URL) {
                currentReport?.let(::renderMapLocation)
            }
        }
    }

    private fun renderMapLocation(report: OrganizationReport) {
        val readyMap = detailMap ?: return
        if (readyMap.style == null) {
            readyMap.setStyle(MAP_STYLE_URL) {
                renderMapLocation(report)
            }
            return
        }

        val location = LatLng(report.latitude, report.longitude)
        readyMap.removeAnnotations()

        val markerDrawable = ContextCompat.getDrawable(this, R.drawable.ic_map_pin)
            ?.mutate()
            ?.apply {
                setTint(
                    ContextCompat.getColor(
                        this@OrganizationReportDetailActivity,
                        markerColor(report.triageClassification)
                    )
                )
            }
        val markerIcon = markerDrawable
            ?.toBitmap()
            ?.let(IconFactory.getInstance(this)::fromBitmap)
        val marker = MarkerOptions()
            .position(location)
            .title(getString(R.string.organization_detail_map_pin))
        markerIcon?.let(marker::icon)
        readyMap.addMarker(marker)
        readyMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, MAP_DETAIL_ZOOM))
    }

    private fun markerColor(classificationValue: String?): Int = when (classificationValue?.trim()?.lowercase()) {
        TRIAGE_CLASSIFICATION_VERY_URGENT -> R.color.alert600
        TRIAGE_CLASSIFICATION_URGENT -> R.color.brass500
        TRIAGE_CLASSIFICATION_LOW -> R.color.low_urgency
        else -> R.color.gold
    }

    private fun styleStatusButton(button: MaterialButton, selected: Boolean) {
        button.background = getDrawable(
            if (selected) R.drawable.bg_org_status_selected else R.drawable.bg_org_status_unselected
        )
        button.setTextColor(getColor(if (selected) R.color.white else R.color.ink))
    }

    private fun showDescriptionDialog() {
        val description = binding.organizationDetailDescriptionPreview.text?.toString().orEmpty()
        AlertDialog.Builder(this)
            .setTitle(R.string.organization_detail_description_title)
            .setMessage(description)
            .setPositiveButton(R.string.organization_detail_close, null)
            .show()
    }

    private fun styleClassification(classificationValue: String?) {
        val style = when (classificationValue?.trim()?.lowercase()) {
            TRIAGE_CLASSIFICATION_VERY_URGENT -> ClassificationStyle(
                R.string.organization_badge_very_urgent,
                R.drawable.bg_org_classification_very_urgent,
                R.color.alert600
            )
            TRIAGE_CLASSIFICATION_URGENT -> ClassificationStyle(
                R.string.organization_badge_urgent,
                R.drawable.bg_org_classification_urgent,
                R.color.terracotta_dark
            )
            TRIAGE_CLASSIFICATION_LOW -> ClassificationStyle(
                R.string.organization_badge_low,
                R.drawable.bg_org_classification_low,
                R.color.low_urgency
            )
            else -> ClassificationStyle(
                R.string.organization_badge_priority,
                R.drawable.bg_org_classification_priority,
                R.color.terracotta_dark
            )
        }

        val color = getColor(style.textColorRes)
        binding.organizationDetailUrgency.text = getString(style.labelRes)
        binding.organizationDetailUrgency.setTextColor(color)
        binding.organizationUrgentCard.setBackgroundResource(style.backgroundRes)
        ImageViewCompat.setImageTintList(
            binding.organizationDetailUrgencyIcon,
            ColorStateList.valueOf(color)
        )
    }

    private fun animalName(type: String) = when (type) {
        "cat" -> getString(R.string.cat)
        "other" -> getString(R.string.other)
        else -> getString(R.string.dog)
    }

    private fun shortAddress(address: String): String {
        val parts = address.split("—")
            .map(String::trim)
            .filter(String::isNotBlank)
        val compact = if (parts.size >= 2) {
            parts.takeLast(2).joinToString(" — ")
        } else {
            address.trim()
        }
        return compact.take(MAX_DETAIL_ADDRESS_LENGTH).trimEnd().let { value ->
            if (compact.length > MAX_DETAIL_ADDRESS_LENGTH) "$value…" else value
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        private const val EXTRA_REPORT_ID = "extra_organization_report_id"
        private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"
        private const val MAP_ATTRIBUTION_URL = "https://openfreemap.org/"
        private const val MAP_DETAIL_ZOOM = 15.0
        private const val TRIAGE_CLASSIFICATION_VERY_URGENT = "very_urgent"
        private const val TRIAGE_CLASSIFICATION_URGENT = "urgent"
        private const val TRIAGE_CLASSIFICATION_PRIORITY = "priority"
        private const val TRIAGE_CLASSIFICATION_LOW = "low"
        private const val MAX_DETAIL_ADDRESS_LENGTH = 48

        fun newIntent(context: Context, reportId: String) =
            Intent(context, OrganizationReportDetailActivity::class.java)
                .putExtra(EXTRA_REPORT_ID, reportId)

        private data class ClassificationStyle(
            val labelRes: Int,
            val backgroundRes: Int,
            val textColorRes: Int
        )
    }
}
