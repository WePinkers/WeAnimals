package com.example.weanimals.home.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.ActivityHomeBinding
import com.example.weanimals.databinding.ItemReportBinding
import com.example.weanimals.home.presenter.HomeContract
import com.example.weanimals.home.presenter.HomePresenter
import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.report.presentation.ReportActivity
import com.example.weanimals.reporting.tracking.presentation.TrackingActivity

class HomeActivity : AppCompatActivity(), HomeContract.View {

    private lateinit var binding: ActivityHomeBinding
    private val presenter: HomePresenter by lazy {
        (application as WeAnimalsApplication).appContainer.createHomePresenter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInteractions()
        MainNavigation.bind(this, binding.mainNavigation, MainNavigation.Destination.HOME)
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.loadReports()
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    private fun setupInteractions() {
        binding.reportNowButton.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }
        binding.viewMapButton.setOnClickListener {
            binding.contentScroll.smoothScrollTo(0, binding.mapPreview.mapRoot.top)
        }
    }

    override fun showLoading() {
        binding.reportsContainer.removeAllViews()
        binding.reportsEmpty.visibility = View.GONE
    }

    override fun showReports(reports: List<Report>) {
        binding.reportsContainer.removeAllViews()
        binding.reportsEmpty.text = getString(R.string.no_reports)
        binding.reportsEmpty.visibility = if (reports.isEmpty()) View.VISIBLE else View.GONE
        reports.forEach { report ->
            val reportBinding = ItemReportBinding.inflate(
                layoutInflater,
                binding.reportsContainer,
                false
            )
            bindReport(reportBinding, ReportCardMapper.map(report))
            reportBinding.reportCard.setOnClickListener { presenter.openReport(report) }
            binding.reportsContainer.addView(reportBinding.root)
        }
    }

    override fun showReportsError(error: Throwable) {
        Log.e(TAG, "Could not load reports", error)
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
