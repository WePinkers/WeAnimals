package com.example.weanimals.reporting.tracking.repository

import com.example.weanimals.reporting.report.repository.ReportRepository

class FirebaseTrackingRepository(
    private val reportRepository: ReportRepository
) : TrackingRepository {
    override fun observeReport(reportId: String) = reportRepository.observeReport(reportId)
}
