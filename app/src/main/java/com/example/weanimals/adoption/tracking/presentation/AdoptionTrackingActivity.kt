package com.example.weanimals.adoption.tracking.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.graphics.Paint
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import coil.dispose
import coil.load
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.adoption.listing.presentation.AdoptionActivity
import com.example.weanimals.adoption.sent.domain.AdoptionApplicationStatus
import com.example.weanimals.adoption.sent.domain.AdoptionSentSummary
import com.example.weanimals.adoption.tracking.domain.AdoptionApplication
import com.example.weanimals.adoption.tracking.presenter.AdoptionTrackingContract
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.reporting.chat.presentation.CaseChatActivity
import com.example.weanimals.databinding.ActivityAdoptionTrackingBinding
import com.example.weanimals.databinding.ItemTrackingStepBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdoptionTrackingActivity : AppCompatActivity(), AdoptionTrackingContract.View {
    private lateinit var binding: ActivityAdoptionTrackingBinding
    private val summary by lazy {
        AdoptionSentSummary(
            animalId = intent.getStringExtra(EXTRA_ANIMAL_ID).orEmpty(),
            animalName = intent.getStringExtra(EXTRA_ANIMAL_NAME).orEmpty(),
            shelterName = intent.getStringExtra(EXTRA_SHELTER_NAME).orEmpty(),
            matchPercentage = intent.getIntExtra(EXTRA_MATCH_PERCENTAGE, 0),
            breed = intent.getStringExtra(EXTRA_BREED).orEmpty(),
            ageText = intent.getStringExtra(EXTRA_AGE_TEXT).orEmpty(),
            photoUrl = intent.getStringExtra(EXTRA_PHOTO_URL)
        )
    }
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createAdoptionTrackingPresenter(summary)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAdoptionTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MainNavigation.bind(this, binding.mainNavigation, MainNavigation.Destination.ADOPTION)
        binding.withdrawApplication.paintFlags =
            binding.withdrawApplication.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.shelterChatCard.setOnClickListener { presenter.onChatClicked() }
        binding.withdrawApplication.setOnClickListener { presenter.onWithdrawClicked() }
        binding.outcomeOtherAnimals.setOnClickListener { presenter.onOtherAnimalsClicked() }
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.start()
    }

    override fun onStop() {
        presenter.stop()
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun showLoading() = with(binding) {
        applicationState.isVisible = true
        applicationState.setText(R.string.adoption_tracking_loading)
        applicationSummary.isVisible = false
        applicationSteps.isVisible = false
        applicationActions.isVisible = false
        applicationOutcome.isVisible = false
    }

    override fun showApplication(application: AdoptionApplication) = with(binding) {
        val animal = application.summary
        val shelter = animal.shelterName.ifBlank {
            getString(R.string.adoption_detail_shelter_fallback)
        }
        applicationEyebrow.setText(when (application.status) {
            AdoptionApplicationStatus.REJECTED -> R.string.adoption_tracking_rejected_eyebrow
            AdoptionApplicationStatus.CANCELLED -> R.string.adoption_tracking_cancelled_eyebrow
            else -> R.string.adoption_tracking_eyebrow
        })
        applicationTitle.text = getString(R.string.adoption_tracking_title, animal.animalName, shelter)
        applicationAnimalName.text = animalLabel(animal)
        applicationMatch.text = getString(R.string.adoption_tracking_match, animal.matchPercentage)
        shelterChatTitle.text = getString(
            R.string.adoption_tracking_chat_title,
            animal.shelterName.ifBlank { getString(R.string.adoption_chat_organization_fallback) }
        )
        val photoUrl = animal.photoUrl?.takeIf(String::isNotBlank)
        applicationPhoto.dispose()
        applicationPhoto.isVisible = photoUrl != null
        applicationPhotoPlaceholder.isVisible = photoUrl == null
        if (photoUrl != null) {
            applicationPhoto.load(photoUrl) {
                crossfade(true)
                listener(onError = { _, _ ->
                    applicationPhoto.isVisible = false
                    applicationPhotoPlaceholder.isVisible = true
                })
            }
        }
        applicationState.isVisible = false
        applicationSummary.isVisible = true
        applicationSteps.isVisible = true
        renderSteps(application)
        val isOutcome = application.status in setOf(
            AdoptionApplicationStatus.REJECTED, AdoptionApplicationStatus.CANCELLED
        )
        applicationOutcome.isVisible = isOutcome
        applicationActions.isVisible = !isOutcome
        withdrawApplication.isVisible = application.status in setOf(
            AdoptionApplicationStatus.PENDING, AdoptionApplicationStatus.REVIEWING
        )
        if (isOutcome) {
            outcomeTitle.setText(if (application.status == AdoptionApplicationStatus.REJECTED)
                R.string.adoption_tracking_rejected_title else R.string.adoption_tracking_cancelled_title)
            outcomeDescription.text = if (application.status == AdoptionApplicationStatus.REJECTED)
                getString(R.string.adoption_tracking_rejected_description, shelter, animal.animalName)
            else getString(R.string.adoption_tracking_cancelled_description, animal.animalName)
        }
    }

    override fun showLoadError(error: Throwable) {
        Log.e(TAG, "Could not load adoption application", error)
        binding.applicationState.isVisible = true
        binding.applicationState.setText(R.string.adoption_tracking_load_error)
        binding.applicationSummary.isVisible = false
        binding.applicationSteps.isVisible = false
        binding.applicationActions.isVisible = false
        binding.applicationOutcome.isVisible = false
    }

    override fun confirmWithdrawal() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.adoption_tracking_withdraw_confirm_title)
            .setMessage(getString(R.string.adoption_tracking_withdraw_confirm, summary.animalName))
            .setNegativeButton(R.string.adoption_tracking_cancel, null)
            .setPositiveButton(R.string.adoption_tracking_withdraw_action) { _, _ ->
                presenter.onWithdrawConfirmed()
            }.show()
    }

    override fun showWithdrawalLoading(loading: Boolean) {
        binding.withdrawApplication.isEnabled = !loading
    }

    override fun showWithdrawalError(error: Throwable) {
        Log.e(TAG, "Could not withdraw adoption application", error)
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.adoption_tracking_withdraw_error)
            .setPositiveButton(R.string.adoption_sent_status_close, null)
            .show()
    }

    override fun openAdoptionChat(summary: AdoptionSentSummary) {
        startActivity(CaseChatActivity.newAdoptionIntent(
            context = this,
            animalId = summary.animalId,
            animalName = summary.animalName,
            organizationName = summary.shelterName
        ))
    }

    override fun openAdoptionListing() {
        startActivity(Intent(this, AdoptionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }

    private fun renderSteps(application: AdoptionApplication) {
        binding.applicationSteps.removeAllViews()
        val status = application.status
        val sentDate = formatDate(application.submittedAtMillis)
        val updatedDate = formatDate(application.updatedAtMillis)
        val steps = mutableListOf(
            Step(
                getString(R.string.adoption_tracking_sent),
                sentDate?.let { getString(R.string.adoption_tracking_sent_date, it) }
                    ?: getString(R.string.adoption_tracking_sent_fallback),
                StepState.COMPLETE
            )
        )
        if (status == AdoptionApplicationStatus.CANCELLED) {
            addSteps(steps)
            return
        }
        if (status == AdoptionApplicationStatus.REJECTED) {
            steps += Step(
                getString(R.string.adoption_tracking_review_done),
                updatedDate?.let { getString(R.string.adoption_tracking_review_rejected, it) }
                    ?: getString(R.string.adoption_tracking_review_rejected_fallback),
                StepState.COMPLETE
            )
            addSteps(steps)
            return
        }
        val reviewComplete = status in setOf(
            AdoptionApplicationStatus.APPROVED,
            AdoptionApplicationStatus.VISIT_SCHEDULED,
            AdoptionApplicationStatus.ADOPTED
        )
        steps += Step(
            getString(when (status) {
                AdoptionApplicationStatus.PENDING -> R.string.adoption_tracking_waiting_review
                else -> R.string.adoption_tracking_in_review
            }),
            if (reviewComplete) updatedDate?.takeIf {
                status == AdoptionApplicationStatus.APPROVED
            }?.let {
                getString(R.string.adoption_tracking_review_approved, it)
            } ?: getString(R.string.adoption_tracking_review_approved_fallback)
            else getString(R.string.adoption_tracking_review_hint),
            if (reviewComplete) StepState.COMPLETE else StepState.CURRENT
        )
        steps += Step(
            getString(R.string.adoption_tracking_visit),
            if (status == AdoptionApplicationStatus.VISIT_SCHEDULED) updatedDate
                ?: getString(R.string.adoption_tracking_visit_confirmed)
            else if (status == AdoptionApplicationStatus.ADOPTED)
                getString(R.string.adoption_tracking_visit_confirmed)
            else getString(R.string.adoption_tracking_visit_pending),
            when (status) {
                AdoptionApplicationStatus.APPROVED -> StepState.CURRENT
                AdoptionApplicationStatus.VISIT_SCHEDULED, AdoptionApplicationStatus.ADOPTED -> StepState.COMPLETE
                else -> StepState.PENDING
            }
        )
        steps += Step(
            getString(R.string.adoption_tracking_adopted),
            if (status == AdoptionApplicationStatus.ADOPTED) updatedDate
                ?: getString(R.string.adoption_tracking_adopted_confirmed)
            else getString(R.string.adoption_tracking_adopted_pending),
            when (status) {
                AdoptionApplicationStatus.ADOPTED -> StepState.COMPLETE
                AdoptionApplicationStatus.VISIT_SCHEDULED -> StepState.CURRENT
                else -> StepState.PENDING
            }
        )
        addSteps(steps)
    }

    private fun addSteps(steps: List<Step>) {
        steps.forEachIndexed { index, step ->
            val item = ItemTrackingStepBinding.inflate(layoutInflater, binding.applicationSteps, false)
            item.trackingStepTitle.text = step.title
            item.trackingStepSubtitle.text = step.subtitle
            item.trackingStepConnector.isVisible = index < steps.lastIndex
            val marker = when (step.state) {
                StepState.COMPLETE -> Triple(R.drawable.bg_tracking_marker_complete, "✓", R.color.white)
                StepState.CURRENT -> Triple(R.drawable.bg_tracking_marker_current, "•", R.color.white)
                StepState.PENDING -> Triple(R.drawable.bg_tracking_marker_pending, "", R.color.muted)
            }
            item.trackingStepMarker.setBackgroundResource(marker.first)
            item.trackingStepMarker.text = marker.second
            item.trackingStepMarker.setTextColor(ContextCompat.getColor(this, marker.third))
            item.trackingStepTitle.setTextColor(ContextCompat.getColor(
                this, if (step.state == StepState.PENDING) R.color.muted else R.color.ink
            ))
            binding.applicationSteps.addView(item.root)
        }
    }

    private fun animalLabel(summary: AdoptionSentSummary): String {
        val detail = listOf(summary.breed, summary.ageText).filter(String::isNotBlank)
            .joinToString(", ")
        return if (detail.isBlank()) summary.animalName else "${summary.animalName} · $detail"
    }

    private fun formatDate(millis: Long): String? {
        if (millis <= 0L) return null
        val locale = Locale.forLanguageTag("pt-BR")
        val today = Calendar.getInstance()
        val date = Calendar.getInstance().apply { timeInMillis = millis }
        val pattern = if (today.get(Calendar.YEAR) == date.get(Calendar.YEAR)
            && today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR))
            "'Hoje,' HH:mm" else "dd/MM, HH:mm"
        return SimpleDateFormat(pattern, locale).format(Date(millis))
    }

    private enum class StepState { COMPLETE, CURRENT, PENDING }
    private data class Step(val title: String, val subtitle: String, val state: StepState)

    companion object {
        private const val TAG = "AdoptionTrackingActivity"
        private const val EXTRA_ANIMAL_ID = "extra_animal_id"
        private const val EXTRA_ANIMAL_NAME = "extra_animal_name"
        private const val EXTRA_SHELTER_NAME = "extra_shelter_name"
        private const val EXTRA_MATCH_PERCENTAGE = "extra_match_percentage"
        private const val EXTRA_BREED = "extra_breed"
        private const val EXTRA_AGE_TEXT = "extra_age_text"
        private const val EXTRA_PHOTO_URL = "extra_photo_url"

        fun newIntent(context: Context, summary: AdoptionSentSummary) =
            Intent(context, AdoptionTrackingActivity::class.java).apply {
                putExtra(EXTRA_ANIMAL_ID, summary.animalId)
                putExtra(EXTRA_ANIMAL_NAME, summary.animalName)
                putExtra(EXTRA_SHELTER_NAME, summary.shelterName)
                putExtra(EXTRA_MATCH_PERCENTAGE, summary.matchPercentage)
                putExtra(EXTRA_BREED, summary.breed)
                putExtra(EXTRA_AGE_TEXT, summary.ageText)
                putExtra(EXTRA_PHOTO_URL, summary.photoUrl)
            }
    }
}
