package com.example.weanimals.reporting.report.presentation.sent

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityReportSentBinding
import com.example.weanimals.home.presentation.HomeActivity
import com.example.weanimals.reporting.report.domain.ReportProtocol
import com.example.weanimals.reporting.triage.domain.TriageClassification
import com.example.weanimals.reporting.tracking.presentation.TrackingActivity

class ReportSentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportSentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityReportSentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bindProtocol()
        bindClassification()
        setupInteractions()
    }

    private fun bindProtocol() {
        val protocolNumber = intent.getIntExtra(
            EXTRA_PROTOCOL_NUMBER,
            ReportProtocol.FIRST_NUMBER
        )
        binding.sentProtocol.text = getString(R.string.sent_protocol_format, protocolNumber)
    }

    private fun bindClassification() {
        val classification = intent.getStringExtra(EXTRA_TRIAGE_CLASSIFICATION)
            ?.takeIf(String::isNotBlank)
            ?: run {
                binding.sentClassification.visibility = android.view.View.GONE
                return
            }

        val titleRes = when (classification) {
            TriageClassification.VERY_URGENT.storageValue -> R.string.triage_very_urgent_title
            TriageClassification.URGENT.storageValue -> R.string.triage_urgent_title
            TriageClassification.PRIORITY.storageValue -> R.string.triage_priority_title
            TriageClassification.LOW.storageValue -> R.string.triage_low_title
            else -> R.string.triage_priority_title
        }
        binding.sentClassification.text = getString(
            R.string.sent_classification_format,
            getString(titleRes)
        )
        binding.sentClassification.visibility = android.view.View.VISIBLE
    }

    private fun setupInteractions() {
        binding.followCaseButton.setOnClickListener {
            val reportId = intent.getStringExtra(EXTRA_REPORT_ID).orEmpty()
            if (reportId.isNotBlank()) {
                startActivity(TrackingActivity.newIntent(this, reportId))
                finish()
            }
        }
        binding.backHomeButton.setOnClickListener {
            startActivity(
                Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
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
        const val EXTRA_PROTOCOL_NUMBER = "extra_protocol_number"
        const val EXTRA_REPORT_ID = "extra_report_id"
        const val EXTRA_TRIAGE_CLASSIFICATION = "extra_triage_classification"
    }
}
