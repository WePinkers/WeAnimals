package com.example.weanimals.adoption.confirmation.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import coil.dispose
import coil.load
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.adoption.confirmation.domain.AdoptionCandidate
import com.example.weanimals.adoption.confirmation.domain.DemoApplicationUnavailableException
import com.example.weanimals.adoption.confirmation.presenter.AdoptionConfirmationContract
import com.example.weanimals.databinding.ActivityAdoptionConfirmationBinding

class AdoptionConfirmationActivity : AppCompatActivity(), AdoptionConfirmationContract.View {
    private lateinit var binding: ActivityAdoptionConfirmationBinding
    private val animalId by lazy { intent.getStringExtra(EXTRA_ANIMAL_ID).orEmpty() }
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createAdoptionConfirmationPresenter(animalId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAdoptionConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.confirmationHeader.headerTitle.setText(R.string.adoption_confirmation_title)
        binding.confirmationHeader.backButton.setOnClickListener { presenter.onBackClicked() }
        binding.confirmationRetry.setOnClickListener { presenter.onRetryClicked() }
        binding.submitApplicationButton.setOnClickListener { presenter.onSubmitClicked() }
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.start()
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
        binding.confirmationContent.isVisible = false
        binding.confirmationState.isVisible = true
        binding.confirmationProgress.isVisible = true
        binding.confirmationStateTitle.setText(R.string.adoption_confirmation_loading)
        binding.confirmationRetry.isVisible = false
    }

    override fun showCandidate(candidate: AdoptionCandidate) = with(binding) {
        confirmationContent.isVisible = true
        confirmationState.isVisible = false
        candidateName.text = candidate.animal.name
        candidateMeta.text = listOf(
            candidate.animal.breed,
            candidate.animal.ageText,
            candidate.animal.shelterName.ifBlank { getString(R.string.adoption_detail_shelter_fallback) }
        ).filter(String::isNotBlank).joinToString(" · ")
        candidateMatch.text = getString(
            R.string.compatible_profile_match_value,
            candidate.matchPercentage
        )
        candidatePhoto.contentDescription = getString(
            R.string.adoption_photo_accessibility,
            candidate.animal.name
        )
        val photoUrl = candidate.animal.photoUrl?.takeIf(String::isNotBlank)
        candidatePhotoPlaceholder.isVisible = true
        candidatePhoto.isVisible = photoUrl != null
        if (photoUrl == null) {
            candidatePhoto.dispose()
            candidatePhoto.setImageDrawable(null)
        } else {
            candidatePhoto.load(photoUrl) {
                placeholder(R.drawable.bg_status_card)
                error(R.drawable.bg_status_card)
                listener(
                    onSuccess = { _, _ -> candidatePhotoPlaceholder.isVisible = false },
                    onError = { _, _ -> candidatePhotoPlaceholder.isVisible = true }
                )
            }
        }
        reviewStepBody.text = getString(
            R.string.adoption_confirmation_step_review_body,
            candidate.animal.name
        )
        candidacyNote.text = getString(R.string.adoption_confirmation_note, candidate.animal.name)
    }

    override fun showLoadError(error: Throwable) {
        Log.e(TAG, "Could not load adoption candidate", error)
        binding.confirmationContent.isVisible = false
        binding.confirmationState.isVisible = true
        binding.confirmationProgress.isVisible = false
        binding.confirmationStateTitle.setText(R.string.adoption_confirmation_load_error)
        binding.confirmationRetry.isVisible = true
    }

    override fun showSubmitting(submitting: Boolean) {
        binding.submitApplicationButton.isEnabled = !submitting
        binding.submitApplicationButton.setText(
            if (submitting) R.string.adoption_confirmation_submitting
            else R.string.adoption_confirmation_submit
        )
        if (submitting) binding.submitFeedback.isVisible = false
    }

    override fun showSubmitted() {
        binding.submitApplicationButton.isEnabled = false
        binding.submitApplicationButton.setText(R.string.adoption_confirmation_submitted)
        binding.submitFeedback.setText(R.string.adoption_confirmation_success)
        binding.submitFeedback.setTextColor(ContextCompat.getColor(this, R.color.adoption_primary))
        binding.submitFeedback.isVisible = true
    }

    override fun showSubmitError(error: Throwable) {
        Log.e(TAG, "Could not submit adoption application", error)
        binding.submitFeedback.setText(
            if (error is DemoApplicationUnavailableException) {
                R.string.adoption_confirmation_demo_error
            } else {
                R.string.adoption_confirmation_submit_error
            }
        )
        binding.submitFeedback.setTextColor(ContextCompat.getColor(this, R.color.terracotta_dark))
        binding.submitFeedback.isVisible = true
    }

    override fun closeScreen() = finish()

    companion object {
        private const val TAG = "AdoptionConfirmation"
        private const val EXTRA_ANIMAL_ID = "extra_animal_id"

        fun newIntent(context: Context, animalId: String) =
            Intent(context, AdoptionConfirmationActivity::class.java).apply {
                putExtra(EXTRA_ANIMAL_ID, animalId)
            }
    }
}
