package com.example.weanimals.triage.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.report.domain.ReportDraft
import com.example.weanimals.triage.domain.TriageClassification
import com.example.weanimals.triage.domain.TriageQuestion
import com.example.weanimals.triage.interactor.CalculateTriageInteractor
import com.example.weanimals.triage.interactor.SubmitTriagedReportInteractor
import kotlinx.coroutines.launch

class TriagePresenter(
    private val draft: ReportDraft,
    private val calculateTriageInteractor: CalculateTriageInteractor,
    private val submitTriagedReportInteractor: SubmitTriagedReportInteractor
) : BasePresenter<TriageContract.View>(), TriageContract.Presenter {

    private val answers: MutableMap<TriageQuestion, Boolean?> =
        TriageQuestion.entries.associateWith { null }.toMutableMap()
    private var classification: TriageClassification? = null
    private var isSubmitting = false

    override fun attachView(view: TriageContract.View) {
        super.attachView(view)
        answers.forEach { (question, answer) -> view.showAnswer(question, answer) }
        view.showClassification(classification)
        view.setConfirmEnabled(classification != null && !isSubmitting)
    }

    override fun selectAnswer(question: TriageQuestion, answer: Boolean) {
        if (isSubmitting) return
        answers[question] = answer
        withView { it.showAnswer(question, answer) }
        recalculate()
    }

    override fun confirmReport() {
        if (isSubmitting) return
        val completedAnswers = answers.mapNotNull { (question, answer) ->
            answer?.let { question to it }
        }.toMap()
        if (completedAnswers.size != TriageQuestion.entries.size) {
            withView { it.showIncompleteAnswers() }
            return
        }

        val calculatedClassification = classification ?: run {
            recalculate()
            return
        }
        isSubmitting = true
        withView {
            it.showSubmitting(true)
            it.setConfirmEnabled(false)
        }
        presenterScope.launch {
            submitTriagedReportInteractor(
                draft = draft,
                answers = completedAnswers,
                classification = calculatedClassification
            ).fold(
                onSuccess = { report -> withView { it.showSubmitted(report) } },
                onFailure = { error ->
                    isSubmitting = false
                    withView {
                        it.showSubmitting(false)
                        it.setConfirmEnabled(classification != null)
                        it.showSubmissionError(error)
                    }
                }
            )
        }
    }

    private fun recalculate() {
        val completedAnswers = answers.mapNotNull { (question, answer) ->
            answer?.let { question to it }
        }.toMap()
        classification = if (completedAnswers.size == TriageQuestion.entries.size) {
            calculateTriageInteractor(draft.urgency, completedAnswers)
        } else {
            null
        }
        withView {
            it.showClassification(classification)
            it.setConfirmEnabled(classification != null && !isSubmitting)
        }
    }
}
