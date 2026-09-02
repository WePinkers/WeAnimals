package com.example.weanimals.triage.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.databinding.ActivityTriageBinding
import com.example.weanimals.report.domain.Report
import com.example.weanimals.report.domain.ReportDraft
import com.example.weanimals.report.presentation.sent.ReportSentActivity
import com.example.weanimals.triage.domain.TriageClassification
import com.example.weanimals.triage.domain.TriageQuestion
import com.example.weanimals.triage.presenter.TriageContract
import com.example.weanimals.triage.presenter.TriagePresenter
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TriageActivity : AppCompatActivity(), TriageContract.View {

    private lateinit var binding: ActivityTriageBinding
    private val draft: ReportDraft by lazy { draftFromIntent() }
    private val presenter: TriagePresenter by lazy {
        (application as WeAnimalsApplication).appContainer.createTriagePresenter(draft)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityTriageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInteractions()
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    private fun setupInteractions() {
        binding.backButton.setOnClickListener { finish() }
        binding.triageInfoButton.setOnClickListener { showTriageInfo() }

        setAnswerListeners(
            TriageQuestion.IMMEDIATE_DANGER,
            binding.immediateDangerYesButton,
            binding.immediateDangerNoButton
        )
        setAnswerListeners(
            TriageQuestion.ACTIVE_ABUSE,
            binding.activeAbuseYesButton,
            binding.activeAbuseNoButton
        )
        setAnswerListeners(
            TriageQuestion.INJURED_OR_IMMOBILIZED,
            binding.injuredOrImmobilizedYesButton,
            binding.injuredOrImmobilizedNoButton
        )
        setAnswerListeners(
            TriageQuestion.STREET_SITUATION,
            binding.streetSituationYesButton,
            binding.streetSituationNoButton
        )
        binding.confirmReportButton.setOnClickListener { presenter.confirmReport() }
    }

    private fun setAnswerListeners(
        question: TriageQuestion,
        yesButton: MaterialButton,
        noButton: MaterialButton
    ) {
        yesButton.setOnClickListener { presenter.selectAnswer(question, true) }
        noButton.setOnClickListener { presenter.selectAnswer(question, false) }
    }

    override fun showAnswer(question: TriageQuestion, answer: Boolean?) {
        val buttons = when (question) {
            TriageQuestion.IMMEDIATE_DANGER -> binding.immediateDangerYesButton to
                binding.immediateDangerNoButton
            TriageQuestion.ACTIVE_ABUSE -> binding.activeAbuseYesButton to binding.activeAbuseNoButton
            TriageQuestion.INJURED_OR_IMMOBILIZED -> binding.injuredOrImmobilizedYesButton to
                binding.injuredOrImmobilizedNoButton
            TriageQuestion.STREET_SITUATION -> binding.streetSituationYesButton to
                binding.streetSituationNoButton
        }
        styleAnswerButton(buttons.first, answer == true)
        styleAnswerButton(buttons.second, answer == false)
        binding.triageError.visibility = android.view.View.GONE
    }

    override fun showClassification(classification: TriageClassification?) {
        if (classification == null) {
            binding.classificationCard.visibility = android.view.View.GONE
            return
        }

        val presentation = classificationPresentation(classification)
        binding.classificationCard.visibility = android.view.View.VISIBLE
        binding.classificationTitle.setText(presentation.titleRes)
        binding.classificationResponse.text = getString(
            R.string.triage_response_format,
            getString(presentation.timeRes)
        )
        binding.classificationDescription.setText(presentation.descriptionRes)
    }

    override fun setConfirmEnabled(enabled: Boolean) {
        binding.confirmReportButton.isEnabled = enabled
    }

    override fun showIncompleteAnswers() {
        binding.triageError.setText(R.string.triage_incomplete)
        binding.triageError.visibility = android.view.View.VISIBLE
    }

    override fun showSubmitting(isSubmitting: Boolean) {
        binding.confirmReportButton.setText(
            if (isSubmitting) R.string.triage_sending else R.string.triage_confirm
        )
    }

    override fun showSubmitted(report: Report) {
        startActivity(
            Intent(this, ReportSentActivity::class.java)
                .putExtra(ReportSentActivity.EXTRA_PROTOCOL_NUMBER, report.protocolNumber)
                .putExtra(ReportSentActivity.EXTRA_REPORT_ID, report.id)
                .putExtra(
                    ReportSentActivity.EXTRA_TRIAGE_CLASSIFICATION,
                    report.triageClassification
                )
        )
        finish()
    }

    override fun showSubmissionError(error: Throwable) {
        Log.e(TAG, "Could not submit triaged report", error)
        binding.triageError.setText(R.string.report_submit_error)
        binding.triageError.visibility = android.view.View.VISIBLE
    }

    private fun styleAnswerButton(button: MaterialButton, selected: Boolean) {
        val backgroundColor = if (selected) R.color.ink else R.color.surface
        val textColor = if (selected) R.color.white else R.color.muted
        val strokeColor = if (selected) R.color.ink else R.color.border
        button.backgroundTintList = colorStateList(backgroundColor)
        button.setTextColor(ContextCompat.getColor(this, textColor))
        button.strokeColor = colorStateList(strokeColor)
    }

    private fun showTriageInfo() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.triage_info_title)
            .setMessage(R.string.triage_info_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun classificationPresentation(classification: TriageClassification) = when (classification) {
        TriageClassification.VERY_URGENT -> ClassificationPresentation(
            R.string.triage_very_urgent_title,
            R.string.triage_very_urgent_time,
            R.string.triage_very_urgent_description
        )
        TriageClassification.URGENT -> ClassificationPresentation(
            R.string.triage_urgent_title,
            R.string.triage_urgent_time,
            R.string.triage_urgent_description
        )
        TriageClassification.PRIORITY -> ClassificationPresentation(
            R.string.triage_priority_title,
            R.string.triage_priority_time,
            R.string.triage_priority_description
        )
        TriageClassification.LOW -> ClassificationPresentation(
            R.string.triage_low_title,
            R.string.triage_low_time,
            R.string.triage_low_description
        )
    }

    private fun draftFromIntent() = ReportDraft(
        animalType = intent.getStringExtra(EXTRA_ANIMAL_TYPE) ?: Report.ANIMAL_DOG,
        urgency = intent.getStringExtra(EXTRA_URGENCY) ?: Report.URGENCY_MEDIUM,
        description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty(),
        address = intent.getStringExtra(EXTRA_ADDRESS).orEmpty(),
        latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0),
        longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0),
        photoUri = intent.getStringExtra(EXTRA_PHOTO_URI)
    )

    private fun colorStateList(colorRes: Int) =
        android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private data class ClassificationPresentation(
        val titleRes: Int,
        val timeRes: Int,
        val descriptionRes: Int
    )

    companion object {
        private const val TAG = "TriageActivity"
        private const val EXTRA_ANIMAL_TYPE = "extra_animal_type"
        private const val EXTRA_URGENCY = "extra_urgency"
        private const val EXTRA_DESCRIPTION = "extra_description"
        private const val EXTRA_ADDRESS = "extra_address"
        private const val EXTRA_LATITUDE = "extra_latitude"
        private const val EXTRA_LONGITUDE = "extra_longitude"
        private const val EXTRA_PHOTO_URI = "extra_photo_uri"

        fun newIntent(context: Context, draft: ReportDraft) =
            Intent(context, TriageActivity::class.java).apply {
                putExtra(EXTRA_ANIMAL_TYPE, draft.animalType)
                putExtra(EXTRA_URGENCY, draft.urgency)
                putExtra(EXTRA_DESCRIPTION, draft.description)
                putExtra(EXTRA_ADDRESS, draft.address)
                putExtra(EXTRA_LATITUDE, draft.latitude)
                putExtra(EXTRA_LONGITUDE, draft.longitude)
                putExtra(EXTRA_PHOTO_URI, draft.photoUri)
            }
    }
}
