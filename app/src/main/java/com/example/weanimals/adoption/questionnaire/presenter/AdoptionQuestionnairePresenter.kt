package com.example.weanimals.adoption.questionnaire.presenter

import com.example.weanimals.adoption.questionnaire.domain.AdoptionQuestionnaireAnswers
import com.example.weanimals.adoption.questionnaire.domain.AvailableSpaceOption
import com.example.weanimals.adoption.questionnaire.domain.DailyTimeOption
import com.example.weanimals.adoption.questionnaire.domain.HouseholdOption
import com.example.weanimals.adoption.questionnaire.domain.PetExperienceOption
import com.example.weanimals.adoption.questionnaire.domain.RoutineOption
import com.example.weanimals.adoption.questionnaire.interactor.BuildAdoptionProfileInteractor
import com.example.weanimals.adoption.questionnaire.interactor.SaveAdoptionProfileInteractor
import com.example.weanimals.core.base.BasePresenter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AdoptionQuestionnairePresenter(
    private val animalId: String,
    private val buildProfile: BuildAdoptionProfileInteractor,
    private val saveProfile: SaveAdoptionProfileInteractor
) : BasePresenter<AdoptionQuestionnaireContract.View>(), AdoptionQuestionnaireContract.Presenter {

    private var answers = AdoptionQuestionnaireAnswers()
    private var saving = false

    override fun start() = withView { it.showAnswers(answers) }

    override fun onRoutineSelected(option: RoutineOption) = update(answers.copy(routine = option))

    override fun onAvailableSpaceSelected(option: AvailableSpaceOption) =
        update(answers.copy(availableSpace = option))

    override fun onPetExperienceSelected(option: PetExperienceOption) =
        update(answers.copy(petExperience = option))

    override fun onDailyTimeSelected(option: DailyTimeOption) =
        update(answers.copy(dailyTime = option))

    override fun onHouseholdSelected(option: HouseholdOption) =
        update(answers.copy(household = option))

    override fun onSubmitClicked() {
        if (saving) return
        if (!answers.isComplete) {
            withView { it.showIncompleteQuestionnaire() }
            return
        }
        val profile = buildProfile(answers)
        saving = true
        withView { it.showSaving() }
        presenterScope.launch {
            try {
                saveProfile(profile).fold(
                    onSuccess = {
                        saving = false
                        withView {
                            it.hideSaving()
                            it.openCompatibleProfile(animalId)
                        }
                    },
                    onFailure = {
                        saving = false
                        withView { view ->
                            view.hideSaving()
                            view.showSaveError(it)
                        }
                    }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    override fun onBackClicked() = withView(AdoptionQuestionnaireContract.View::closeScreen)

    private fun update(newAnswers: AdoptionQuestionnaireAnswers) {
        answers = newAnswers
        withView { it.showAnswers(answers) }
    }
}
