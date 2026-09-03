package com.example.weanimals.reporting.triage.interactor

import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.triage.domain.TriageClassification
import com.example.weanimals.reporting.triage.domain.TriageQuestion

class CalculateTriageInteractor {
    operator fun invoke(
        initialUrgency: String,
        answers: Map<TriageQuestion, Boolean>
    ): TriageClassification {
        if (answers[TriageQuestion.IMMEDIATE_DANGER] == true) {
            return TriageClassification.VERY_URGENT
        }

        var score = when (initialUrgency) {
            Report.URGENCY_HIGH -> 2
            Report.URGENCY_MEDIUM -> 1
            else -> 0
        }

        if (answers[TriageQuestion.ACTIVE_ABUSE] == true) score += 3
        if (answers[TriageQuestion.INJURED_OR_IMMOBILIZED] == true) score += 2
        if (answers[TriageQuestion.STREET_SITUATION] == true) score -= 1

        return when {
            score >= 7 -> TriageClassification.VERY_URGENT
            score >= 4 -> TriageClassification.URGENT
            score >= 2 -> TriageClassification.PRIORITY
            else -> TriageClassification.LOW
        }
    }
}
