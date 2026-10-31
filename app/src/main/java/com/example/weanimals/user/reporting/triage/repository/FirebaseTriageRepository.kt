package com.example.weanimals.user.reporting.triage.repository

import com.example.weanimals.user.reporting.report.domain.NewReport
import com.example.weanimals.user.reporting.report.domain.Report
import com.example.weanimals.user.reporting.report.domain.ReportDraft
import com.example.weanimals.user.reporting.report.repository.ReportRepository
import com.example.weanimals.user.reporting.triage.domain.TriageClassification
import com.example.weanimals.user.reporting.triage.domain.TriageQuestion

class FirebaseTriageRepository(
    private val reportRepository: ReportRepository
) : TriageRepository {
    override suspend fun submit(
        draft: ReportDraft,
        answers: Map<TriageQuestion, Boolean>,
        classification: TriageClassification
    ): Result<Report> {
        return reportRepository.submitReport(
            NewReport(
                animalType = draft.animalType,
                urgency = draft.urgency,
                description = draft.description,
                address = draft.address,
                latitude = draft.latitude,
                longitude = draft.longitude,
                photoUri = draft.photoUri,
                triageClassification = classification.storageValue,
                triageAnswers = answers.mapKeys { it.key.storageKey }
            )
        )
    }
}
