package com.example.weanimals.reporting.details.presentation

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.databinding.ActivityReportDetailsBinding
import com.example.weanimals.databinding.ItemDetailsTriageAnswerBinding
import com.example.weanimals.reporting.details.domain.ReportDetails
import com.example.weanimals.reporting.report.domain.Report
import com.example.weanimals.reporting.triage.domain.TriageClassification
import com.example.weanimals.reporting.triage.domain.TriageQuestion
import com.example.weanimals.reporting.details.presenter.DetailsContract
import com.example.weanimals.reporting.details.presenter.DetailsPresenter
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportDetailsActivity : AppCompatActivity(), DetailsContract.View {

    private lateinit var binding: ActivityReportDetailsBinding
    private val reportId: String by lazy { intent.getStringExtra(EXTRA_REPORT_ID).orEmpty() }
    private val presenter: DetailsPresenter by lazy {
        (application as WeAnimalsApplication).appContainer.createDetailsPresenter(reportId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityReportDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.detailsHeader.headerTitle.setText(R.string.details_title)
        binding.detailsHeader.backButton.setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.loadDetails()
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun showLoading() {
        binding.detailsState.visibility = View.VISIBLE
        binding.detailsState.setText(R.string.details_loading)
        binding.detailsContent.visibility = View.GONE
    }

    override fun showDetails(details: ReportDetails) {
        binding.detailsState.visibility = View.GONE
        binding.detailsContent.visibility = View.VISIBLE
        bindReport(details.report)
    }

    override fun showDetailsError(error: Throwable) {
        Log.e(TAG, "Could not load report details", error)
        binding.detailsState.visibility = View.VISIBLE
        binding.detailsState.setText(R.string.details_error)
        binding.detailsContent.visibility = View.GONE
    }

    private fun bindReport(report: Report) {
        binding.detailsProtocol.text = getString(
            R.string.details_protocol_format,
            report.protocolNumber
        )
        binding.detailsSentMeta.text = getString(
            R.string.details_sent_meta_format,
            formatDate(report.createdAtMillis)
        )
        bindPhoto(report)
        bindAnimal(report.animalType)
        binding.descriptionText.text = report.description.ifBlank {
            getString(R.string.tracking_no_description)
        }
        bindLocation(report.address)
        bindTriageAnswers(report)
        bindClassification(report.triageClassification)
    }

    private fun bindPhoto(report: Report) {
        val photoBytes = report.photoData
        if (photoBytes != null) {
            binding.photoPreview.setImageBitmap(
                BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
            )
            binding.photoPreview.visibility = View.VISIBLE
            binding.photoPlaceholder.visibility = View.GONE
        } else {
            binding.photoPreview.visibility = View.GONE
            binding.photoPlaceholder.visibility = View.VISIBLE
            binding.photoPlaceholder.text = if (report.hasPhoto) {
                report.photoFileName ?: getString(R.string.details_photo_fallback)
            } else {
                getString(R.string.details_no_photo)
            }
        }
    }

    private fun bindAnimal(animalType: String) {
        binding.animalChip.text = animalName(animalType)
        binding.animalChip.backgroundTintList = colorStateList(R.color.ink)
        binding.animalChip.strokeColor = colorStateList(R.color.ink)
    }

    private fun bindLocation(address: String) {
        val normalizedAddress = address.ifBlank { getString(R.string.location_unknown) }
        binding.locationTitle.text = normalizedAddress.substringBefore(" —").trim()
        binding.locationSubtitle.text = normalizedAddress.substringAfter(" —", "").trim()
        binding.locationSubtitle.visibility = if (binding.locationSubtitle.text.isNullOrBlank()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun bindTriageAnswers(report: Report) {
        binding.triageAnswersContainer.removeAllViews()
        TriageQuestion.entries.forEach { question ->
            val answerBinding = ItemDetailsTriageAnswerBinding.inflate(
                LayoutInflater.from(this),
                binding.triageAnswersContainer,
                false
            )
            answerBinding.triageQuestion.setText(question.titleRes())
            val answer = report.triageAnswers[question.storageKey]
            answerBinding.triageAnswer.text = when (answer) {
                true -> getString(R.string.triage_yes)
                false -> getString(R.string.triage_no)
                null -> getString(R.string.details_not_answered)
            }
            styleAnswerButton(answerBinding.triageAnswer, answer != null)
            binding.triageAnswersContainer.addView(answerBinding.root)
        }
    }

    private fun bindClassification(classificationValue: String?) {
        val classification = classificationValue?.let { value ->
            TriageClassification.entries.firstOrNull { it.storageValue == value }
        }
        if (classification == null) {
            binding.classificationCard.visibility = View.GONE
            binding.classificationLabel.visibility = View.GONE
            return
        }

        binding.classificationLabel.visibility = View.VISIBLE
        binding.classificationCard.visibility = View.VISIBLE
        val presentation = classificationPresentation(classification)
        binding.classificationEyebrow.setText(presentation.titleRes)
        binding.classificationTitle.text = getString(
            R.string.triage_response_format,
            getString(presentation.timeRes)
        )
        binding.classificationDescription.setText(presentation.descriptionRes)
    }

    private fun styleAnswerButton(button: MaterialButton, answered: Boolean) {
        button.backgroundTintList = colorStateList(
            if (answered) R.color.ink else R.color.surface
        )
        button.setTextColor(
            ContextCompat.getColor(this, if (answered) R.color.white else R.color.muted)
        )
        button.strokeColor = colorStateList(if (answered) R.color.ink else R.color.border)
    }

    private fun colorStateList(colorRes: Int) =
        android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))

    private fun animalName(animalType: String) = when (animalType) {
        Report.ANIMAL_CAT -> getString(R.string.cat)
        Report.ANIMAL_OTHER -> getString(R.string.other)
        else -> getString(R.string.dog)
    }

    private fun formatDate(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return getString(R.string.details_date_unknown)
        val date = Date(timestampMillis)
        val today = SimpleDateFormat("yyyyMMdd", Locale.forLanguageTag("pt-BR"))
        val time = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR"))
        return if (today.format(date) == today.format(Date())) {
            getString(R.string.details_today_at, time.format(date))
        } else {
            SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.forLanguageTag("pt-BR")).format(date)
        }
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

    private fun TriageQuestion.titleRes() = when (this) {
        TriageQuestion.IMMEDIATE_DANGER -> R.string.triage_immediate_danger
        TriageQuestion.ACTIVE_ABUSE -> R.string.triage_active_abuse
        TriageQuestion.INJURED_OR_IMMOBILIZED -> R.string.triage_injured_or_immobilized
        TriageQuestion.STREET_SITUATION -> R.string.triage_street_situation
    }

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
        private const val TAG = "ReportDetailsActivity"
        private const val EXTRA_REPORT_ID = "extra_report_id"

        fun newIntent(context: Context, reportId: String) =
            Intent(context, ReportDetailsActivity::class.java)
                .putExtra(EXTRA_REPORT_ID, reportId)
    }
}
