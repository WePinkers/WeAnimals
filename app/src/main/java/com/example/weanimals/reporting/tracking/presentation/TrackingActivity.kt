package com.example.weanimals.reporting.tracking.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.databinding.ActivityTrackingBinding
import com.example.weanimals.databinding.ItemTrackingStepBinding
import com.example.weanimals.reporting.chat.presentation.CaseChatActivity
import com.example.weanimals.reporting.details.presentation.ReportDetailsActivity
import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.report.domain.ReportStatus
import com.example.weanimals.reporting.tracking.presenter.TrackingContract
import com.example.weanimals.reporting.tracking.presenter.TrackingPresenter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackingActivity : AppCompatActivity(), TrackingContract.View {

    private lateinit var binding: ActivityTrackingBinding
    private val reportId: String by lazy { intent.getStringExtra(EXTRA_REPORT_ID).orEmpty() }
    private val presenter: TrackingPresenter by lazy {
        (application as WeAnimalsApplication).appContainer.createTrackingPresenter(reportId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.loadReport()
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun showLoading() {
        binding.trackingState.visibility = View.VISIBLE
        binding.trackingState.setText(R.string.tracking_loading)
        binding.trackingStepsContainer.visibility = View.GONE
        binding.detailsCard.visibility = View.GONE
        binding.updateCard.visibility = View.GONE
    }

    override fun showReport(report: Report) {
        binding.trackingState.visibility = View.GONE
        binding.trackingStepsContainer.visibility = View.VISIBLE
        binding.detailsCard.visibility = View.VISIBLE
        binding.updateCard.visibility = View.VISIBLE
        binding.trackingProtocol.text = getString(
            R.string.tracking_protocol_format,
            report.protocolNumber
        )
        binding.trackingTitle.text = getString(
            R.string.tracking_case_title_format,
            animalName(report.animalType),
            locationName(report.address)
        )
        renderTimeline(report)
        binding.detailsCard.contentDescription = getString(R.string.tracking_details_card)
        binding.updateCard.contentDescription = getString(R.string.tracking_update_card)
        binding.detailsCard.setOnClickListener {
            startActivity(ReportDetailsActivity.newIntent(this, report.id))
        }
        binding.updateCard.setOnClickListener {
            startActivity(CaseChatActivity.newIntent(this, report.id, report.protocolNumber))
        }
    }

    override fun showReportError(error: Throwable) {
        Log.e(TAG, "Could not load report tracking", error)
        binding.trackingState.visibility = View.VISIBLE
        binding.trackingState.setText(R.string.tracking_error)
        binding.trackingStepsContainer.visibility = View.GONE
        binding.detailsCard.visibility = View.GONE
        binding.updateCard.visibility = View.GONE
    }

    private fun renderTimeline(report: Report) {
        binding.trackingStepsContainer.removeAllViews()
        val currentIndex = currentStepIndex(report.status)
        val steps = listOf(
            TrackingStep(
                titleRes = R.string.tracking_received,
                subtitle = receivedSubtitle(report),
                index = 0
            ),
            TrackingStep(
                titleRes = R.string.tracking_assigned,
                subtitle = getString(R.string.tracking_assigned_subtitle),
                index = 1
            ),
            TrackingStep(
                titleRes = R.string.tracking_on_the_way,
                subtitle = getString(R.string.tracking_on_the_way_subtitle),
                index = 2
            ),
            TrackingStep(
                titleRes = R.string.tracking_rescue,
                subtitle = getString(R.string.tracking_rescue_subtitle),
                index = 3
            ),
            TrackingStep(
                titleRes = R.string.tracking_shelter,
                subtitle = getString(R.string.tracking_shelter_subtitle),
                index = 4
            )
        )

        steps.forEachIndexed { index, step ->
            val itemBinding = ItemTrackingStepBinding.inflate(
                layoutInflater,
                binding.trackingStepsContainer,
                false
            )
            val state = when {
                report.status == ReportStatus.CLOSED || step.index < currentIndex -> StepState.COMPLETED
                step.index == currentIndex -> StepState.CURRENT
                else -> StepState.PENDING
            }
            bindStep(itemBinding, step, state, index == steps.lastIndex)
            binding.trackingStepsContainer.addView(itemBinding.root)
        }
    }

    private fun bindStep(
        binding: ItemTrackingStepBinding,
        step: TrackingStep,
        state: StepState,
        isLast: Boolean
    ) {
        binding.trackingStepTitle.setText(step.titleRes)
        binding.trackingStepSubtitle.text = step.subtitle
        binding.trackingStepConnector.visibility = if (isLast) View.GONE else View.VISIBLE
        val marker = when (state) {
            StepState.COMPLETED -> MarkerPresentation(
                R.drawable.bg_tracking_marker_complete,
                "✓",
                R.color.white
            )
            StepState.CURRENT -> MarkerPresentation(
                R.drawable.bg_tracking_marker_current,
                "•",
                R.color.white
            )
            StepState.PENDING -> MarkerPresentation(
                R.drawable.bg_tracking_marker_pending,
                "",
                R.color.muted
            )
        }
        binding.trackingStepMarker.setBackgroundResource(marker.backgroundRes)
        binding.trackingStepMarker.text = marker.text
        binding.trackingStepMarker.setTextColor(ContextCompat.getColor(this, marker.textColorRes))
        binding.trackingStepTitle.setTextColor(
            ContextCompat.getColor(this, if (state == StepState.PENDING) R.color.muted else R.color.ink)
        )
        binding.trackingStepSubtitle.setTextColor(ContextCompat.getColor(this, R.color.muted))
    }

    private fun currentStepIndex(status: String) = when (status) {
        ReportStatus.UNDER_REVIEW -> 1
        ReportStatus.IN_PROGRESS -> 2
        ReportStatus.RESCUED -> 3
        ReportStatus.CLOSED -> 4
        else -> 0
    }

    private fun receivedSubtitle(report: Report): String {
        val timestamp = report.createdAtMillis.takeIf { it > 0L }?.let(::formatTime)
        return timestamp?.let { getString(R.string.tracking_received_subtitle, it) }
            ?: getString(R.string.tracking_received_fallback)
    }

    private fun formatTime(timestampMillis: Long): String =
        SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(timestampMillis))

    private fun animalName(animalType: String) = when (animalType) {
        Report.ANIMAL_CAT -> getString(R.string.cat)
        Report.ANIMAL_OTHER -> getString(R.string.other)
        else -> getString(R.string.dog)
    }

    private fun locationName(address: String): String {
        if (address.isBlank()) return getString(R.string.location_unknown)
        return address.substringAfter(" —", address)
            .substringBefore(" —")
            .trim()
            .ifBlank { address.substringBefore(",").trim() }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private enum class StepState { COMPLETED, CURRENT, PENDING }

    private data class TrackingStep(
        val titleRes: Int,
        val subtitle: String,
        val index: Int
    )

    private data class MarkerPresentation(
        val backgroundRes: Int,
        val text: String,
        val textColorRes: Int
    )

    companion object {
        private const val TAG = "TrackingActivity"
        private const val EXTRA_REPORT_ID = "extra_report_id"

        fun newIntent(context: Context, reportId: String) =
            Intent(context, TrackingActivity::class.java)
                .putExtra(EXTRA_REPORT_ID, reportId)
    }
}
