package com.example.weanimals.profile.reports.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.databinding.ActivityMyReportsBinding
import com.example.weanimals.databinding.ItemProtocolBinding
import com.example.weanimals.home.presentation.ReportCardMapper
import com.example.weanimals.profile.reports.presenter.MyReportsContract
import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.tracking.presentation.TrackingActivity

class MyReportsActivity : AppCompatActivity(), MyReportsContract.View {
    private lateinit var binding: ActivityMyReportsBinding
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createMyReportsPresenter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.screenHeader.headerTitle.setText(R.string.title_my_reports)
        binding.screenHeader.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.start()
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun showReports(reports: List<Report>) {
        binding.reportsContainer.removeAllViews()
        binding.reportsSummary.text = if (reports.isEmpty()) getString(R.string.profile_reports_empty)
            else resources.getQuantityString(R.plurals.profile_reports_count, reports.size, reports.size)
        reports.forEach { report ->
            val item = ItemProtocolBinding.inflate(layoutInflater, binding.reportsContainer, false)
            val card = ReportCardMapper.map(report)
            item.protocolNumber.text = getString(R.string.report_protocol_format, report.protocolNumber)
            item.protocolDescription.text = report.address.ifBlank { getString(R.string.location_unknown) }
            item.protocolStatus.text = getString(card.statusLabelRes)
            item.root.setOnClickListener { startActivity(TrackingActivity.newIntent(this, report.id)) }
            binding.reportsContainer.addView(item.root)
        }
    }

    override fun showError() {
        binding.reportsContainer.removeAllViews()
        binding.reportsSummary.text = getString(R.string.reports_load_error)
    }
}
