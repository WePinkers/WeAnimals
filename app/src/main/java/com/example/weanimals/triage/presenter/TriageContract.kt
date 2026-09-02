package com.example.weanimals.triage.presenter

import com.example.weanimals.report.domain.Report
import com.example.weanimals.triage.domain.TriageClassification
import com.example.weanimals.triage.domain.TriageQuestion

interface TriageContract {
    interface View {
        fun showAnswer(question: TriageQuestion, answer: Boolean?)
        fun showClassification(classification: TriageClassification?)
        fun setConfirmEnabled(enabled: Boolean)
        fun showIncompleteAnswers()
        fun showSubmitting(isSubmitting: Boolean)
        fun showSubmitted(report: Report)
        fun showSubmissionError(error: Throwable)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun selectAnswer(question: TriageQuestion, answer: Boolean)
        fun confirmReport()
    }
}
