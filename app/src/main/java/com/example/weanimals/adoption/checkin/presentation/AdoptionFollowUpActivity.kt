package com.example.weanimals.adoption.checkin.presentation

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import coil.dispose
import coil.load
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.adoption.checkin.domain.AdaptationRating
import com.example.weanimals.adoption.checkin.domain.AdoptionFollowUp
import com.example.weanimals.adoption.checkin.domain.FollowUpMilestone
import com.example.weanimals.adoption.checkin.domain.FollowUpAnswer
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestion
import com.example.weanimals.adoption.checkin.domain.FollowUpQuestionId
import com.example.weanimals.adoption.checkin.domain.MilestoneState
import com.example.weanimals.adoption.checkin.presenter.AdoptionFollowUpContract
import com.example.weanimals.databinding.ActivityAdoptionFollowUpBinding
import com.example.weanimals.databinding.ItemFollowUpMilestoneBinding
import com.example.weanimals.databinding.ItemFollowUpQuestionBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Profile can open this screen later through [newIntent]; no Profile navigation is owned here. */
class AdoptionFollowUpActivity : AppCompatActivity(), AdoptionFollowUpContract.View {
    private lateinit var binding: ActivityAdoptionFollowUpBinding
    private val animalId by lazy { intent.getStringExtra(EXTRA_ANIMAL_ID).orEmpty() }
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createAdoptionFollowUpPresenter(animalId)
    }
    private var updatingRating = false
    private val questionRows = mutableMapOf<FollowUpQuestionId, ItemFollowUpQuestionBinding>()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityAdoptionFollowUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.followUpHeader.headerTitle.setText(R.string.adoption_follow_up_title)
        binding.followUpHeader.backButton.setOnClickListener { presenter.onBackClicked() }
        binding.followUpRetry.setOnClickListener { presenter.onRetryClicked() }
        binding.followUpRatingGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (updatingRating) return@setOnCheckedStateChangeListener
            binding.followUpFormError.isVisible = false
            presenter.onRatingSelected(checkedIds.firstOrNull()?.let(::ratingFor))
            styleRatingChips()
        }
        binding.followUpSubmit.setOnClickListener {
            presenter.onSubmitClicked(binding.followUpNote.text?.toString().orEmpty())
        }
        styleRatingChips()
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
        binding.followUpPhoto.dispose()
        presenter.destroy()
        super.onDestroy()
    }

    override fun showLoading() = with(binding) {
        followUpScroll.isVisible = false
        followUpState.isVisible = true
        followUpProgress.isVisible = true
        followUpRetry.isVisible = false
        followUpStateMessage.setText(R.string.adoption_follow_up_loading)
    }

    override fun showFollowUp(
        followUp: AdoptionFollowUp,
        milestones: List<FollowUpMilestone>,
        selectedRating: AdaptationRating?,
        questions: List<FollowUpQuestion>,
        selectedAnswers: Map<FollowUpQuestionId, FollowUpAnswer>
    ) = with(binding) {
        followUpState.isVisible = false
        followUpScroll.isVisible = true
        val date = dateFormat.format(Date(followUp.adoptedAtMillis))
        followUpAnimal.text = getString(R.string.adoption_follow_up_summary, followUp.animalName, date)
        val name = intent.getStringExtra(EXTRA_ADOPTER_NAME)
            ?.takeIf(String::isNotBlank)
            ?: followUp.adopterName?.takeIf(String::isNotBlank)
        followUpAdopter.isVisible = name != null
        if (name != null) followUpAdopter.text = getString(R.string.adoption_follow_up_adopter, name)
        renderPhoto(followUp.photoUrl)
        renderMilestones(milestones)
        val available = milestones.any { it.state == MilestoneState.AVAILABLE }
        followUpForm.isVisible = available
        val next = milestones.firstOrNull { it.state == MilestoneState.SCHEDULED }
        followUpNextHint.isVisible = !available
        if (!available) {
            followUpNextHint.text = if (next == null) {
                getString(R.string.adoption_follow_up_all_done)
            } else {
                getString(R.string.adoption_follow_up_next, dateFormat.format(Date(next.dueAtMillis)))
            }
        }
        updatingRating = true
        if (selectedRating == null) followUpRatingGroup.clearCheck()
        else followUpRatingGroup.check(idFor(selectedRating))
        updatingRating = false
        styleRatingChips()
        renderQuestions(questions, selectedAnswers)
        followUpFormError.isVisible = false
    }

    override fun showUnavailable() = showState(R.string.adoption_follow_up_unavailable, retry = false)

    override fun showAdoptionDateMissing() =
        showState(R.string.adoption_follow_up_date_missing, retry = true)

    override fun showLoadError(error: Throwable) {
        Log.e(TAG, "Could not load adoption follow-up", error)
        showState(R.string.adoption_follow_up_load_error, retry = true)
    }

    override fun showRatingRequired() {
        binding.followUpFormError.setText(R.string.adoption_follow_up_rating_required)
        binding.followUpFormError.isVisible = true
    }

    override fun showQuestionRequired(questionId: FollowUpQuestionId) {
        val row = questionRows[questionId] ?: return
        row.questionError.isVisible = true
        binding.followUpScroll.post {
            binding.followUpScroll.smoothScrollTo(
                0,
                binding.followUpForm.top + binding.followUpQuestions.top + row.root.top
            )
        }
    }

    override fun showSending(sending: Boolean) {
        binding.followUpSubmit.isEnabled = !sending
        binding.followUpSubmit.setText(
            if (sending) R.string.adoption_follow_up_sending else R.string.adoption_follow_up_submit
        )
        binding.followUpNote.isEnabled = !sending
        for (index in 0 until binding.followUpRatingGroup.childCount) {
            binding.followUpRatingGroup.getChildAt(index).isEnabled = !sending
        }
        questionRows.values.forEach { row ->
            for (index in 0 until row.questionOptions.childCount) {
                row.questionOptions.getChildAt(index).isEnabled = !sending
            }
        }
        binding.followUpFormError.isVisible = false
    }

    override fun showSubmitError(error: Throwable) {
        Log.e(TAG, "Could not submit adoption check-in", error)
        binding.followUpFormError.setText(R.string.adoption_follow_up_submit_error)
        binding.followUpFormError.isVisible = true
    }

    override fun showSubmitSuccess() {
        Snackbar.make(binding.root, R.string.adoption_follow_up_submit_success, Snackbar.LENGTH_LONG)
            .show()
    }

    override fun clearForm() {
        updatingRating = true
        binding.followUpRatingGroup.clearCheck()
        updatingRating = false
        binding.followUpNote.text?.clear()
        styleRatingChips()
    }

    override fun closeScreen() = finish()

    private fun showState(message: Int, retry: Boolean) = with(binding) {
        followUpScroll.isVisible = false
        followUpState.isVisible = true
        followUpProgress.isVisible = false
        followUpStateMessage.setText(message)
        followUpRetry.isVisible = retry
    }

    private fun renderPhoto(photoUrl: String?) = with(binding) {
        val url = photoUrl?.takeIf(String::isNotBlank)
        followUpPhoto.dispose()
        followUpPhoto.isVisible = url != null
        followUpPhotoPlaceholder.isVisible = true
        if (url != null) followUpPhoto.load(url) {
            crossfade(true)
            listener(
                onSuccess = { _, _ -> followUpPhotoPlaceholder.isVisible = false },
                onError = { _, _ -> followUpPhoto.isVisible = false }
            )
        }
    }

    private fun renderMilestones(milestones: List<FollowUpMilestone>) {
        val container = binding.followUpMilestones
        container.removeAllViews()
        milestones.forEachIndexed { index, milestone ->
            val row = ItemFollowUpMilestoneBinding.inflate(layoutInflater, container, false)
            row.milestoneTitle.text = getString(R.string.adoption_follow_up_step, milestone.days)
            when (milestone.state) {
                MilestoneState.COMPLETED -> {
                    row.milestoneMarker.setBackgroundResource(R.drawable.bg_tracking_marker_complete)
                    row.milestoneIcon.setImageResource(R.drawable.ic_check)
                    row.milestoneSubtitle.text = getString(
                        R.string.adoption_follow_up_completed,
                        ratingName(requireNotNull(milestone.checkIn).rating)
                            .lowercase(Locale.forLanguageTag("pt-BR"))
                    )
                }
                MilestoneState.AVAILABLE -> {
                    row.milestoneMarker.setBackgroundResource(R.drawable.bg_follow_up_marker_available)
                    row.milestoneIcon.setImageResource(R.drawable.ic_adoption_photo_marker)
                    row.milestoneIcon.scaleX = 0.38f
                    row.milestoneIcon.scaleY = 0.38f
                    row.milestoneSubtitle.setText(R.string.adoption_follow_up_available)
                    row.milestoneAction.isVisible = true
                    row.milestoneAction.setOnClickListener {
                        binding.followUpScroll.post {
                            binding.followUpScroll.smoothScrollTo(0, binding.followUpForm.top)
                        }
                    }
                }
                MilestoneState.SCHEDULED -> {
                    row.milestoneIcon.setImageResource(R.drawable.ic_calendar_outline)
                    row.milestoneSubtitle.text = getString(
                        R.string.adoption_follow_up_scheduled,
                        dateFormat.format(Date(milestone.dueAtMillis))
                    )
                }
            }
            container.addView(row.root)
            if (index < milestones.lastIndex) {
                val divider = View(this).apply {
                    setBackgroundColor(ContextCompat.getColor(this@AdoptionFollowUpActivity, R.color.border))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        (resources.displayMetrics.density + 0.5f).toInt().coerceAtLeast(1)
                    )
                }
                container.addView(divider)
            }
        }
    }

    private fun renderQuestions(
        questions: List<FollowUpQuestion>,
        selectedAnswers: Map<FollowUpQuestionId, FollowUpAnswer>
    ) {
        val container = binding.followUpQuestions
        container.removeAllViews()
        questionRows.clear()
        questions.forEach { question ->
            val row = ItemFollowUpQuestionBinding.inflate(layoutInflater, container, false)
            val eyebrow = question.id in setOf(
                FollowUpQuestionId.REGULAR_VET_VISITS,
                FollowUpQuestionId.RECOMMEND_ADOPTION
            )
            row.questionTitle.isVisible = !eyebrow
            row.questionEyebrow.isVisible = eyebrow
            if (eyebrow) row.questionEyebrow.setText(labelFor(question.id))
            else row.questionTitle.setText(labelFor(question.id))
            val optionIds = mutableMapOf<Int, FollowUpAnswer>()
            question.options.forEach { answer ->
                val chip = layoutInflater.inflate(
                    R.layout.view_follow_up_answer_chip, row.questionOptions, false
                ) as Chip
                chip.id = View.generateViewId()
                chip.setText(labelFor(answer))
                optionIds[chip.id] = answer
                row.questionOptions.addView(chip)
            }
            val selectedId = optionIds.entries.firstOrNull {
                it.value == selectedAnswers[question.id]
            }?.key
            if (selectedId != null) row.questionOptions.check(selectedId)
            styleQuestionChips(row.questionOptions)
            row.questionOptions.setOnCheckedStateChangeListener { group, checkedIds ->
                val answer = checkedIds.firstOrNull()?.let(optionIds::get)
                presenter.onQuestionAnswered(question.id, answer)
                row.questionError.isVisible = false
                styleQuestionChips(group)
            }
            container.addView(row.root, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) })
            questionRows[question.id] = row
        }
    }

    private fun styleQuestionChips(group: ChipGroup) {
        for (index in 0 until group.childCount) {
            val chip = group.getChildAt(index) as? Chip ?: continue
            styleChip(chip)
        }
    }

    private fun styleRatingChips() {
        val group = binding.followUpRatingGroup
        for (index in 0 until group.childCount) {
            val chip = group.getChildAt(index) as? Chip ?: continue
            styleChip(chip)
        }
    }

    private fun styleChip(chip: Chip) {
        val selected = chip.isChecked
        chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(
            this, if (selected) R.color.adoption_primary else R.color.surface
        ))
        chip.chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(
            this, if (selected) R.color.adoption_primary else R.color.border
        ))
        chip.setTextColor(ContextCompat.getColor(
            this, if (selected) R.color.white else R.color.ink
        ))
    }

    private fun dp(value: Int) = (resources.displayMetrics.density * value + 0.5f).toInt()

    private fun labelFor(questionId: FollowUpQuestionId) = when (questionId) {
        FollowUpQuestionId.ROUTINE_ADJUSTED -> R.string.adoption_follow_up_routine_adjusted
        FollowUpQuestionId.HEALTH_OR_BEHAVIOR_ISSUE -> R.string.adoption_follow_up_health_issue
        FollowUpQuestionId.WALKS_AND_FEEDING_ESTABLISHED -> R.string.adoption_follow_up_walks_and_feeding
        FollowUpQuestionId.BEHAVIOR_CHANGED -> R.string.adoption_follow_up_behavior_changed
        FollowUpQuestionId.REGULAR_VET_VISITS -> R.string.adoption_follow_up_regular_vet_visits
        FollowUpQuestionId.STRONG_BOND -> R.string.adoption_follow_up_strong_bond
        FollowUpQuestionId.SHELTER_SUPPORT_NEEDED -> R.string.adoption_follow_up_shelter_support
        FollowUpQuestionId.RECOMMEND_ADOPTION -> R.string.adoption_follow_up_recommend_adoption
    }

    private fun labelFor(answer: FollowUpAnswer) = when (answer) {
        FollowUpAnswer.YES -> R.string.adoption_follow_up_answer_yes
        FollowUpAnswer.NO -> R.string.adoption_follow_up_answer_no
        FollowUpAnswer.PARTLY -> R.string.adoption_follow_up_answer_partly
        FollowUpAnswer.YES_CERTAINLY -> R.string.adoption_follow_up_answer_certainly
        FollowUpAnswer.MAYBE -> R.string.adoption_follow_up_answer_maybe
    }

    private fun ratingName(rating: AdaptationRating) = getString(when (rating) {
        AdaptationRating.DIFFICULT -> R.string.adoption_follow_up_difficult
        AdaptationRating.SOME_CHALLENGES -> R.string.adoption_follow_up_challenges
        AdaptationRating.GOOD -> R.string.adoption_follow_up_good
        AdaptationRating.EXCELLENT -> R.string.adoption_follow_up_excellent
    })

    private fun idFor(rating: AdaptationRating) = when (rating) {
        AdaptationRating.DIFFICULT -> R.id.follow_up_difficult
        AdaptationRating.SOME_CHALLENGES -> R.id.follow_up_challenges
        AdaptationRating.GOOD -> R.id.follow_up_good
        AdaptationRating.EXCELLENT -> R.id.follow_up_excellent
    }

    private fun ratingFor(id: Int) = when (id) {
        R.id.follow_up_difficult -> AdaptationRating.DIFFICULT
        R.id.follow_up_challenges -> AdaptationRating.SOME_CHALLENGES
        R.id.follow_up_good -> AdaptationRating.GOOD
        R.id.follow_up_excellent -> AdaptationRating.EXCELLENT
        else -> null
    }

    companion object {
        private const val TAG = "AdoptionFollowUp"
        private const val EXTRA_ANIMAL_ID = "animal_id"
        private const val EXTRA_ADOPTER_NAME = "adopter_name"

        fun newIntent(context: Context, animalId: String, adopterName: String? = null) =
            Intent(context, AdoptionFollowUpActivity::class.java).apply {
                putExtra(EXTRA_ANIMAL_ID, animalId)
                adopterName?.let { putExtra(EXTRA_ADOPTER_NAME, it) }
            }
    }
}
