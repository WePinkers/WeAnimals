package com.example.weanimals.adoption.questionnaire.presentation

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.adoption.compatibility.presentation.CompatibleProfileActivity
import com.example.weanimals.adoption.questionnaire.domain.AdoptionQuestionnaireAnswers
import com.example.weanimals.adoption.questionnaire.domain.AvailableSpaceOption
import com.example.weanimals.adoption.questionnaire.domain.DailyTimeOption
import com.example.weanimals.adoption.questionnaire.domain.HouseholdOption
import com.example.weanimals.adoption.questionnaire.domain.PetExperienceOption
import com.example.weanimals.adoption.questionnaire.domain.RoutineOption
import com.example.weanimals.adoption.questionnaire.presenter.AdoptionQuestionnaireContract
import com.example.weanimals.databinding.ActivityAdoptionQuestionnaireBinding
import com.google.android.material.chip.Chip

class AdoptionQuestionnaireActivity : AppCompatActivity(), AdoptionQuestionnaireContract.View {

    private lateinit var binding: ActivityAdoptionQuestionnaireBinding
    private val animalId by lazy { intent.getStringExtra(EXTRA_ANIMAL_ID).orEmpty() }
    private val isEditing by lazy { intent.getBooleanExtra(EXTRA_EDITING, false) }
    private val initialAnswers by lazy {
        if (!isEditing) null else AdoptionQuestionnaireAnswers(
            routine = RoutineOption.entries.firstOrNull {
                it.storageValue == intent.getStringExtra(EXTRA_ROUTINE)
            },
            availableSpace = AvailableSpaceOption.entries.firstOrNull {
                it.storageValue == intent.getStringExtra(EXTRA_SPACE)
            },
            petExperience = PetExperienceOption.entries.firstOrNull {
                it.storageValue == intent.getStringExtra(EXTRA_EXPERIENCE)
            },
            dailyTime = DailyTimeOption.entries.firstOrNull {
                it.storageValue == intent.getStringExtra(EXTRA_DAILY_TIME)
            },
            household = HouseholdOption.entries.firstOrNull {
                it.storageValue == intent.getStringExtra(EXTRA_HOUSEHOLD)
            }
        )
    }
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer
            .createAdoptionQuestionnairePresenter(animalId, initialAnswers)
    }
    private var updatingSelections = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityAdoptionQuestionnaireBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.questionnaireHeader.headerTitle.setText(R.string.adoption_questionnaire_title)
        if (isEditing) binding.submitButton.setText(R.string.adoption_questionnaire_update_submit)
        setupInteractions()
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

    override fun showAnswers(answers: AdoptionQuestionnaireAnswers) {
        updatingSelections = true
        binding.routineGroup.check(idFor(answers.routine))
        binding.spaceGroup.check(idFor(answers.availableSpace))
        binding.experienceGroup.check(idFor(answers.petExperience))
        binding.dailyTimeGroup.check(idFor(answers.dailyTime))
        binding.householdGroup.check(idFor(answers.household))
        updateChipAppearance()
        updatingSelections = false
        binding.questionnaireError.isVisible = false
    }

    override fun showIncompleteQuestionnaire() {
        binding.questionnaireError.setText(R.string.adoption_questionnaire_incomplete)
        binding.questionnaireError.isVisible = true
    }

    override fun showSaving() {
        binding.submitButton.isEnabled = false
        binding.submitButton.setText(R.string.adoption_questionnaire_saving)
        binding.questionnaireError.isVisible = false
    }

    override fun hideSaving() {
        binding.submitButton.isEnabled = true
        binding.submitButton.setText(
            if (isEditing) R.string.adoption_questionnaire_update_submit
            else R.string.adoption_questionnaire_submit
        )
    }

    override fun showSaveError(error: Throwable) {
        Log.e(TAG, "Could not save adoption profile", error)
        binding.questionnaireError.setText(R.string.adoption_questionnaire_save_error)
        binding.questionnaireError.isVisible = true
    }

    override fun openCompatibleProfile(animalId: String) {
        if (isEditing) {
            setResult(RESULT_OK)
            finish()
            return
        }
        startActivity(CompatibleProfileActivity.newIntent(this, animalId))
        finish()
    }

    override fun closeScreen() = finish()

    private fun setupInteractions() = with(binding) {
        questionnaireHeader.backButton.setOnClickListener { presenter.onBackClicked() }
        routineGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (!updatingSelections) checkedIds.firstOrNull()?.let { id ->
                routineFor(id)?.let(presenter::onRoutineSelected)
            }
        }
        spaceGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (!updatingSelections) checkedIds.firstOrNull()?.let { id ->
                spaceFor(id)?.let(presenter::onAvailableSpaceSelected)
            }
        }
        experienceGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (!updatingSelections) checkedIds.firstOrNull()?.let { id ->
                experienceFor(id)?.let(presenter::onPetExperienceSelected)
            }
        }
        dailyTimeGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (!updatingSelections) checkedIds.firstOrNull()?.let { id ->
                dailyTimeFor(id)?.let(presenter::onDailyTimeSelected)
            }
        }
        householdGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (!updatingSelections) checkedIds.firstOrNull()?.let { id ->
                householdFor(id)?.let(presenter::onHouseholdSelected)
            }
        }
        submitButton.setOnClickListener { presenter.onSubmitClicked() }
    }

    private fun updateChipAppearance() {
        val groups = listOf(
            binding.routineGroup,
            binding.spaceGroup,
            binding.experienceGroup,
            binding.dailyTimeGroup,
            binding.householdGroup
        )
        groups.forEach { group ->
            for (index in 0 until group.childCount) {
                val chip = group.getChildAt(index) as? Chip ?: continue
                styleChip(chip, chip.isChecked)
            }
        }
    }

    private fun styleChip(chip: Chip, selected: Boolean) {
        chip.chipBackgroundColor = colorStateList(
            if (selected) R.color.adoption_primary else R.color.surface
        )
        chip.chipStrokeColor = colorStateList(
            if (selected) R.color.adoption_primary else R.color.border
        )
        chip.setTextColor(
            ContextCompat.getColor(this, if (selected) R.color.white else R.color.muted)
        )
    }

    private fun idFor(option: RoutineOption?) = when (option) {
        RoutineOption.AWAY_ALL_DAY -> R.id.routine_away
        RoutineOption.HOME_OFFICE -> R.id.routine_home_office
        RoutineOption.OFTEN_HOME -> R.id.routine_home_often
        null -> -1
    }

    private fun idFor(option: AvailableSpaceOption?) = when (option) {
        AvailableSpaceOption.SMALL_APARTMENT -> R.id.space_small_apartment
        AvailableSpaceOption.APARTMENT_WITH_BALCONY -> R.id.space_balcony
        AvailableSpaceOption.HOUSE_WITH_YARD -> R.id.space_yard
        null -> -1
    }

    private fun idFor(option: PetExperienceOption?) = when (option) {
        PetExperienceOption.FIRST_TIME -> R.id.experience_first
        PetExperienceOption.HAD_PETS_BEFORE -> R.id.experience_had_pets
        PetExperienceOption.HAS_OTHER_PETS -> R.id.experience_has_pets
        null -> -1
    }

    private fun idFor(option: DailyTimeOption?) = when (option) {
        DailyTimeOption.LESS_THAN_ONE_HOUR -> R.id.daily_time_less_one
        DailyTimeOption.ONE_TO_THREE_HOURS -> R.id.daily_time_one_three
        DailyTimeOption.MORE_THAN_THREE_HOURS -> R.id.daily_time_more_three
        null -> -1
    }

    private fun idFor(option: HouseholdOption?) = when (option) {
        HouseholdOption.LIVES_ALONE -> R.id.household_alone
        HouseholdOption.YOUNG_CHILDREN -> R.id.household_children
        HouseholdOption.OTHER_PETS -> R.id.household_pets
        null -> -1
    }

    private fun routineFor(id: Int) = when (id) {
        R.id.routine_away -> RoutineOption.AWAY_ALL_DAY
        R.id.routine_home_office -> RoutineOption.HOME_OFFICE
        R.id.routine_home_often -> RoutineOption.OFTEN_HOME
        else -> null
    }

    private fun spaceFor(id: Int) = when (id) {
        R.id.space_small_apartment -> AvailableSpaceOption.SMALL_APARTMENT
        R.id.space_balcony -> AvailableSpaceOption.APARTMENT_WITH_BALCONY
        R.id.space_yard -> AvailableSpaceOption.HOUSE_WITH_YARD
        else -> null
    }

    private fun experienceFor(id: Int) = when (id) {
        R.id.experience_first -> PetExperienceOption.FIRST_TIME
        R.id.experience_had_pets -> PetExperienceOption.HAD_PETS_BEFORE
        R.id.experience_has_pets -> PetExperienceOption.HAS_OTHER_PETS
        else -> null
    }

    private fun dailyTimeFor(id: Int) = when (id) {
        R.id.daily_time_less_one -> DailyTimeOption.LESS_THAN_ONE_HOUR
        R.id.daily_time_one_three -> DailyTimeOption.ONE_TO_THREE_HOURS
        R.id.daily_time_more_three -> DailyTimeOption.MORE_THAN_THREE_HOURS
        else -> null
    }

    private fun householdFor(id: Int) = when (id) {
        R.id.household_alone -> HouseholdOption.LIVES_ALONE
        R.id.household_children -> HouseholdOption.YOUNG_CHILDREN
        R.id.household_pets -> HouseholdOption.OTHER_PETS
        else -> null
    }

    private fun colorStateList(colorRes: Int) = ColorStateList.valueOf(
        ContextCompat.getColor(this, colorRes)
    )

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        private const val TAG = "AdoptionQuestionnaireActivity"
        private const val EXTRA_ANIMAL_ID = "extra_animal_id"
        private const val EXTRA_EDITING = "extra_editing"
        private const val EXTRA_ROUTINE = "extra_routine"
        private const val EXTRA_SPACE = "extra_space"
        private const val EXTRA_EXPERIENCE = "extra_experience"
        private const val EXTRA_DAILY_TIME = "extra_daily_time"
        private const val EXTRA_HOUSEHOLD = "extra_household"

        fun newIntent(context: Context, animalId: String) =
            Intent(context, AdoptionQuestionnaireActivity::class.java).apply {
                putExtra(EXTRA_ANIMAL_ID, animalId)
            }

        fun newEditIntent(
            context: Context,
            animalId: String,
            answers: AdoptionQuestionnaireAnswers
        ) = newIntent(context, animalId).apply {
            putExtra(EXTRA_EDITING, true)
            putExtra(EXTRA_ROUTINE, answers.routine?.storageValue)
            putExtra(EXTRA_SPACE, answers.availableSpace?.storageValue)
            putExtra(EXTRA_EXPERIENCE, answers.petExperience?.storageValue)
            putExtra(EXTRA_DAILY_TIME, answers.dailyTime?.storageValue)
            putExtra(EXTRA_HOUSEHOLD, answers.household?.storageValue)
        }
    }
}
