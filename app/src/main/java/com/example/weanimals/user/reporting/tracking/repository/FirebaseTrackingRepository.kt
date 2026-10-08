package com.example.weanimals.user.reporting.tracking.repository

import com.example.weanimals.user.reporting.report.repository.ReportRepository

class FirebaseTrackingRepository(
    private val reportRepository: ReportRepository
) : TrackingRepository {
    override fun observeReport(reportId: String) = reportRepository.observeReport(reportId)
}
