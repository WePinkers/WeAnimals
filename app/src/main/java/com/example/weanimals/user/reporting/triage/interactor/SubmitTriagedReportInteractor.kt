package com.example.weanimals.user.reporting.triage.interactor

import com.example.weanimals.user.reporting.report.domain.Report
import com.example.weanimals.user.reporting.report.domain.ReportDraft
import com.example.weanimals.user.reporting.triage.domain.TriageClassification
import com.example.weanimals.user.reporting.triage.domain.TriageQuestion
import com.example.weanimals.user.reporting.triage.repository.TriageRepository

class SubmitTriagedReportInteractor(
    private val repository: TriageRepository
) {
    suspend operator fun invoke(
        draft: ReportDraft,
        answers: Map<TriageQuestion, Boolean>,
        classification: TriageClassification
    ): Result<Report> {
        check(answers.keys.containsAll(TriageQuestion.entries)) {
            "All triage questions must be answered."
        }
        return repository.submit(draft, answers, classification)
    }
}
