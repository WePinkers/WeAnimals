package com.example.weanimals.adoption.sent.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.adoption.confirmation.domain.AdoptionCandidate
import com.example.weanimals.adoption.listing.presentation.AdoptionActivity
import com.example.weanimals.adoption.sent.domain.AdoptionSentSummary
import com.example.weanimals.adoption.sent.presenter.AdoptionSentContract
import com.example.weanimals.adoption.tracking.presentation.AdoptionTrackingActivity
import com.example.weanimals.databinding.ActivityReportSentBinding

/** Reuses the sent-report layout, with adoption-specific content and actions. */
class AdoptionSentActivity : AppCompatActivity(), AdoptionSentContract.View {
    private lateinit var binding: ActivityReportSentBinding
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
        (application as WeAnimalsApplication).appContainer.createAdoptionSentPresenter(summary)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityReportSentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.followCaseButton.setOnClickListener { presenter.onTrackClicked() }
        binding.backHomeButton.setOnClickListener { presenter.onOtherAnimalsClicked() }
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

    override fun showSummary(summary: AdoptionSentSummary) = with(binding) {
        val shelterName = summary.shelterName.ifBlank {
            getString(R.string.adoption_detail_shelter_fallback)
        }
        sentProtocol.setText(R.string.adoption_sent_eyebrow)
        sentClassification.text = getString(
            R.string.adoption_sent_summary,
            summary.animalName,
            shelterName,
            summary.matchPercentage
        )
        sentClassification.setBackgroundResource(R.drawable.bg_adoption_sent_summary)
        sentClassification.setTextColor(
            ContextCompat.getColor(this@AdoptionSentActivity, R.color.adoption_primary)
        )
        sentClassification.isVisible = true
        sentTitle.setText(R.string.adoption_sent_title)
        sentDescription.text = getString(
            R.string.adoption_sent_description,
            summary.animalName,
            shelterName
        )
        statusTitle.setText(R.string.adoption_sent_now)
        reviewStep.setText(R.string.adoption_sent_step_received)
        assignedStep.setText(R.string.adoption_sent_step_review)
        rescueStep.setText(R.string.adoption_sent_step_visit)
        followCaseButton.setText(R.string.adoption_sent_track)
        followCaseButton.isVisible = true
        backHomeButton.setText(R.string.adoption_sent_other_animals)
    }

    override fun openTracking(summary: AdoptionSentSummary) {
        startActivity(AdoptionTrackingActivity.newIntent(this, summary))
    }

    override fun openAdoptionListing() {
        startActivity(Intent(this, AdoptionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }

    companion object {
        private const val EXTRA_ANIMAL_ID = "extra_animal_id"
        private const val EXTRA_ANIMAL_NAME = "extra_animal_name"
        private const val EXTRA_SHELTER_NAME = "extra_shelter_name"
        private const val EXTRA_MATCH_PERCENTAGE = "extra_match_percentage"
        private const val EXTRA_BREED = "extra_breed"
        private const val EXTRA_AGE_TEXT = "extra_age_text"
        private const val EXTRA_PHOTO_URL = "extra_photo_url"

        fun newIntent(context: Context, candidate: AdoptionCandidate) =
            Intent(context, AdoptionSentActivity::class.java).apply {
                putExtra(EXTRA_ANIMAL_ID, candidate.animal.id)
                putExtra(EXTRA_ANIMAL_NAME, candidate.animal.name)
                putExtra(EXTRA_SHELTER_NAME, candidate.animal.shelterName)
                putExtra(EXTRA_MATCH_PERCENTAGE, candidate.matchPercentage)
                putExtra(EXTRA_BREED, candidate.animal.breed)
                putExtra(EXTRA_AGE_TEXT, candidate.animal.ageText)
                putExtra(EXTRA_PHOTO_URL, candidate.animal.photoUrl)
            }
    }
}
