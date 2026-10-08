package com.example.weanimals.organization.dashboard.presentation

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.widget.ImageViewCompat
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.databinding.ActivityOrganizationReportsBinding
import com.example.weanimals.databinding.ItemOrganizationReportBinding
import com.example.weanimals.organization.dashboard.domain.OrganizationReport
import com.example.weanimals.organization.dashboard.domain.OrganizationReportFilter
import com.example.weanimals.organization.dashboard.presenter.OrganizationReportsContract

class OrganizationReportsActivity : AppCompatActivity(), OrganizationReportsContract.View {

    private lateinit var binding: ActivityOrganizationReportsBinding
    private var selectedFilter = OrganizationReportFilter.ACTIVE
    private val filterPreferences by lazy {
        getSharedPreferences(FILTER_PREFERENCES, MODE_PRIVATE)
    }
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createOrganizationReportsPresenter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityOrganizationReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        selectedFilter = savedInstanceState?.getString(EXTRA_SELECTED_FILTER)
            ?.let { value -> runCatching { OrganizationReportFilter.valueOf(value) }.getOrNull() }
            ?: filterPreferences.getString(EXTRA_SELECTED_FILTER, null)
                ?.let { value -> runCatching { OrganizationReportFilter.valueOf(value) }.getOrNull() }
            ?: OrganizationReportFilter.ACTIVE
        setupFilters()
        presenter.selectFilter(selectedFilter)
        binding.organizationNotificationsButton.setOnClickListener {
            Toast.makeText(this, R.string.organization_notifications_empty, Toast.LENGTH_SHORT).show()
        }
        setupNavigationFeedback()
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

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_SELECTED_FILTER, selectedFilter.name)
        super.onSaveInstanceState(outState)
    }

    override fun showLoading() {
        binding.organizationReportsSummary.setText(R.string.organization_reports_loading)
        binding.organizationReportsContainer.removeAllViews()
        binding.organizationReportsEmpty.visibility = View.GONE
    }

    override fun showReports(
        reports: List<OrganizationReport>,
        filter: OrganizationReportFilter
    ) {
        updateFilterStyles(filter)
        binding.organizationReportsSummary.text = getString(
            R.string.organization_reports_count,
            reports.size
        )
        binding.organizationReportsContainer.removeAllViews()
        binding.organizationReportsEmpty.visibility = if (reports.isEmpty()) View.VISIBLE else View.GONE

        reports.forEach { report ->
            val item = ItemOrganizationReportBinding.inflate(
                layoutInflater,
                binding.organizationReportsContainer,
                false
            )
            bindReportCard(item, report)
            binding.organizationReportsContainer.addView(item.root)
        }
    }

    override fun showError(error: Throwable) {
        binding.organizationReportsSummary.setText(R.string.organization_reports_error)
        binding.organizationReportsContainer.removeAllViews()
        binding.organizationReportsEmpty.visibility = View.VISIBLE
    }

    override fun openReport(report: OrganizationReport) {
        startActivity(OrganizationReportDetailActivity.newIntent(this, report.id))
    }

    private fun setupFilters() {
        binding.organizationFilterActive.setOnClickListener {
            selectFilter(OrganizationReportFilter.ACTIVE)
        }
        binding.organizationFilterResolved.setOnClickListener {
            selectFilter(OrganizationReportFilter.RESOLVED)
        }
        binding.organizationFilterAll.setOnClickListener {
            selectFilter(OrganizationReportFilter.ALL)
        }
    }

    private fun selectFilter(filter: OrganizationReportFilter) {
        selectedFilter = filter
        filterPreferences.edit()
            .putString(EXTRA_SELECTED_FILTER, filter.name)
            .apply()
        presenter.selectFilter(filter)
    }

    private fun setupNavigationFeedback() {
        setupNavigationItem(
            binding.organizationNavigation.organizationNavHomeIndicator,
            binding.organizationNavigation.organizationNavHomeIcon,
            binding.organizationNavigation.organizationNavHomeLabel,
            isCurrent = false
        )
        setupNavigationItem(
            binding.organizationNavigation.organizationNavReportsIndicator,
            binding.organizationNavigation.organizationNavReportsIcon,
            binding.organizationNavigation.organizationNavReportsLabel,
            isCurrent = true
        )
        setupNavigationItem(
            binding.organizationNavigation.organizationNavCommunityIndicator,
            binding.organizationNavigation.organizationNavCommunityIcon,
            binding.organizationNavigation.organizationNavCommunityLabel,
            isCurrent = false
        )
        setupNavigationItem(
            binding.organizationNavigation.organizationNavAnimalsIndicator,
            binding.organizationNavigation.organizationNavAnimalsIcon,
            binding.organizationNavigation.organizationNavAnimalsLabel,
            isCurrent = false
        )
        setupNavigationItem(
            binding.organizationNavigation.organizationNavProfileIndicator,
            binding.organizationNavigation.organizationNavProfileIcon,
            binding.organizationNavigation.organizationNavProfileLabel,
            isCurrent = false
        )

        binding.organizationNavigation.root.setOnClickListener { }
        listOf(
            binding.organizationNavigation.organizationNavHome,
            binding.organizationNavigation.organizationNavCommunity,
            binding.organizationNavigation.organizationNavAnimals,
            binding.organizationNavigation.organizationNavProfile
        ).forEach { item ->
            item.setOnClickListener {
                Toast.makeText(this, R.string.organization_navigation_coming_soon, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNavigationItem(
        indicator: View,
        icon: ImageView,
        label: TextView,
        isCurrent: Boolean
    ) {
        val activeColor = ContextCompat.getColor(this, R.color.pine800)
        val mutedColor = ContextCompat.getColor(this, R.color.muted)
        if (isCurrent) {
            indicator.setBackgroundResource(R.drawable.bg_nav_active_indicator)
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(activeColor))
            label.setTextColor(activeColor)
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            label.setTypeface(null, Typeface.BOLD)
        } else {
            indicator.setBackgroundResource(0)
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(mutedColor))
            label.setTextColor(mutedColor)
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            label.setTypeface(null, Typeface.NORMAL)
        }
    }

    private fun bindReportCard(
        item: ItemOrganizationReportBinding,
        report: OrganizationReport
    ) {
        item.organizationReportBadge.text = badgeLabel(report)
        item.organizationReportBadge.setBackgroundResource(badgeBackground(report))
        item.organizationReportBadge.setTextColor(getColor(badgeTextColor(report)))
        item.organizationReportElapsed.text = report.elapsedLabel
        item.organizationReportTitle.text = getString(
            R.string.organization_report_title_format,
            report.protocolNumber,
            animalName(report.animalType),
            shortAddress(report.address)
        )
        item.organizationReportSummary.text = report.statusDescription
        item.root.setOnClickListener { presenter.openReport(report) }
    }

    private fun updateFilterStyles(selected: OrganizationReportFilter) {
        styleFilter(binding.organizationFilterActive, selected == OrganizationReportFilter.ACTIVE)
        styleFilter(binding.organizationFilterResolved, selected == OrganizationReportFilter.RESOLVED)
        styleFilter(binding.organizationFilterAll, selected == OrganizationReportFilter.ALL)
    }

    private fun styleFilter(view: TextView, selected: Boolean) {
        view.setBackgroundResource(
            if (selected) R.drawable.bg_org_filter_selected else R.drawable.bg_org_filter_unselected
        )
        view.setTextColor(getColor(if (selected) R.color.white else R.color.ink))
        view.paint.isFakeBoldText = selected
    }

    private fun badgeLabel(report: OrganizationReport): String = getString(
        when (classificationValue(report)) {
            TRIAGE_CLASSIFICATION_VERY_URGENT -> R.string.organization_badge_very_urgent
            TRIAGE_CLASSIFICATION_URGENT -> R.string.organization_badge_urgent
            TRIAGE_CLASSIFICATION_LOW -> R.string.organization_badge_low
            else -> R.string.organization_badge_priority
        }
    )

    private fun badgeBackground(report: OrganizationReport): Int = when (classificationValue(report)) {
        TRIAGE_CLASSIFICATION_VERY_URGENT -> R.drawable.bg_org_classification_very_urgent
        TRIAGE_CLASSIFICATION_URGENT -> R.drawable.bg_org_classification_urgent
        TRIAGE_CLASSIFICATION_PRIORITY -> R.drawable.bg_org_classification_priority
        TRIAGE_CLASSIFICATION_LOW -> R.drawable.bg_org_classification_low
        else -> R.drawable.bg_org_classification_priority
    }

    private fun badgeTextColor(report: OrganizationReport): Int = when (classificationValue(report)) {
        TRIAGE_CLASSIFICATION_VERY_URGENT -> R.color.alert600
        TRIAGE_CLASSIFICATION_URGENT -> R.color.terracotta_dark
        TRIAGE_CLASSIFICATION_PRIORITY -> R.color.terracotta_dark
        TRIAGE_CLASSIFICATION_LOW -> R.color.low_urgency
        else -> R.color.terracotta_dark
    }

    private fun classificationValue(report: OrganizationReport): String =
        report.triageClassification?.trim()?.lowercase().orEmpty()

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
        return compact.take(MAX_LIST_ADDRESS_LENGTH).trimEnd().let { value ->
            if (compact.length > MAX_LIST_ADDRESS_LENGTH) "$value…" else value
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
        private const val EXTRA_SELECTED_FILTER = "extra_organization_selected_filter"
        private const val FILTER_PREFERENCES = "organization_reports_preferences"
        private const val MAX_LIST_ADDRESS_LENGTH = 48
        private const val TRIAGE_CLASSIFICATION_VERY_URGENT = "very_urgent"
        private const val TRIAGE_CLASSIFICATION_URGENT = "urgent"
        private const val TRIAGE_CLASSIFICATION_PRIORITY = "priority"
        private const val TRIAGE_CLASSIFICATION_LOW = "low"
    }
}
