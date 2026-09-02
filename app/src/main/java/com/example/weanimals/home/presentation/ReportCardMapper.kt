package com.example.weanimals.home.presentation

import com.example.weanimals.R
import com.example.weanimals.report.domain.Report
import com.example.weanimals.report.domain.ReportStatus

data class ReportCardUiModel(
    val animalNameRes: Int,
    val address: String,
    val protocolNumber: Int,
    val urgencyLabelRes: Int,
    val urgencyAccentColorRes: Int,
    val statusLabelRes: Int,
    val statusBackgroundRes: Int,
    val statusTextColorRes: Int,
    val showStatus: Boolean
)

object ReportCardMapper {
    fun map(report: Report): ReportCardUiModel {
        val urgencyPresentation = urgency(report.urgency)
        val statusPresentation = status(report.status)
        return ReportCardUiModel(
            animalNameRes = animalName(report.animalType),
            address = report.address.substringBefore(" —").trim(),
            protocolNumber = report.protocolNumber,
            urgencyLabelRes = urgencyPresentation.labelRes,
            urgencyAccentColorRes = urgencyPresentation.accentColorRes,
            statusLabelRes = statusPresentation.labelRes,
            statusBackgroundRes = statusPresentation.backgroundRes,
            statusTextColorRes = statusPresentation.textColorRes,
            showStatus = report.status != ReportStatus.NEW || !report.isViewed
        )
    }

    private fun animalName(animalType: String) = when (animalType) {
        Report.ANIMAL_CAT -> R.string.cat
        Report.ANIMAL_OTHER -> R.string.other
        else -> R.string.dog
    }

    private fun urgency(urgency: String) = when (urgency) {
        Report.URGENCY_LOW -> UrgencyPresentation(R.string.urgency_low_label, R.color.low_urgency)
        Report.URGENCY_MEDIUM -> UrgencyPresentation(
            R.string.urgency_medium_label,
            R.color.gold
        )
        else -> UrgencyPresentation(R.string.urgency_high_label, R.color.red)
    }

    private fun status(status: String) = when (status) {
        ReportStatus.UNDER_REVIEW -> StatusPresentation(
            R.string.status_under_review,
            R.drawable.bg_new_badge,
            R.color.terracotta_dark
        )
        ReportStatus.IN_PROGRESS -> StatusPresentation(
            R.string.in_progress_status,
            R.drawable.bg_progress_badge,
            R.color.terracotta_dark
        )
        ReportStatus.RESCUED -> StatusPresentation(
            R.string.status_rescued,
            R.color.low_urgency_light,
            R.color.low_urgency
        )
        ReportStatus.CLOSED -> StatusPresentation(
            R.string.status_closed,
            R.drawable.bg_status_card,
            R.color.muted
        )
        else -> StatusPresentation(R.string.new_status, R.drawable.bg_new_badge, R.color.terracotta_dark)
    }

    private data class UrgencyPresentation(val labelRes: Int, val accentColorRes: Int)

    private data class StatusPresentation(
        val labelRes: Int,
        val backgroundRes: Int,
        val textColorRes: Int
    )
}
