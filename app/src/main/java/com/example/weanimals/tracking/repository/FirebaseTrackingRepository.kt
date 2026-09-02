package com.example.weanimals.tracking.repository

import com.example.weanimals.report.repository.ReportRepository

class FirebaseTrackingRepository(
    private val reportRepository: ReportRepository
) : TrackingRepository {
    override fun observeReport(reportId: String) = reportRepository.observeReport(reportId)
}
