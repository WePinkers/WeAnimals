package com.example.weanimals.adoption.compatibility.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.adoption.compatibility.domain.AnimalRecommendation
import com.example.weanimals.adoption.compatibility.domain.MatchLevel
import com.example.weanimals.adoption.compatibility.presenter.CompatibleProfileContract
import com.example.weanimals.adoption.detail.presentation.AnimalDetailsActivity
import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.domain.AnimalEnergyLevel
import com.example.weanimals.adoption.listing.domain.Species
import com.example.weanimals.adoption.questionnaire.domain.AdoptionProfile
import com.example.weanimals.adoption.questionnaire.domain.AgeProfile
import com.example.weanimals.adoption.questionnaire.domain.AvailableSpaceOption
import com.example.weanimals.adoption.questionnaire.domain.EnergyProfile
import com.example.weanimals.adoption.questionnaire.domain.RoutineOption
import com.example.weanimals.adoption.questionnaire.domain.SizeProfile
import com.example.weanimals.adoption.questionnaire.domain.TemperamentProfile
import com.example.weanimals.adoption.questionnaire.presentation.AdoptionQuestionnaireActivity
import com.example.weanimals.databinding.ActivityCompatibleProfileBinding
import com.example.weanimals.databinding.ItemCompatibleAnimalBinding
import java.text.NumberFormat
import java.util.Locale

class CompatibleProfileActivity : AppCompatActivity(), CompatibleProfileContract.View {

    private lateinit var binding: ActivityCompatibleProfileBinding
    private val animalId by lazy { intent.getStringExtra(EXTRA_ANIMAL_ID).orEmpty() }
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createCompatibleProfilePresenter(animalId)
    }
    private val portugueseLocale = Locale.forLanguageTag("pt-BR")
    private val distanceFormat = NumberFormat.getNumberInstance(portugueseLocale).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityCompatibleProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.compatibleProfileHeader.headerTitle.setText(R.string.compatible_profile_title)
        binding.compatibleProfileHeader.backButton.setOnClickListener { presenter.onBackClicked() }
        binding.profileStateAction.setOnClickListener { presenter.onRetryClicked() }
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
        binding.profileContent.isVisible = false
        binding.profileState.isVisible = true
        binding.profileProgress.isVisible = true
        binding.profileStateTitle.setText(R.string.compatible_profile_loading)
        binding.profileStateAction.isVisible = false
    }

    override fun showProfile(
        profile: AdoptionProfile,
        recommendations: List<AnimalRecommendation>
    ) {
        showProfileContent(profile)
        binding.recommendationsEmpty.isVisible = false
        binding.recommendationsContainer.isVisible = true
        bindRecommendations(recommendations)
    }

    override fun showEmpty(profile: AdoptionProfile) {
        showProfileContent(profile)
        binding.recommendationsContainer.removeAllViews()
        binding.recommendationsContainer.isVisible = false
        binding.recommendationsEmpty.isVisible = true
    }

    override fun showError(error: Throwable) {
        Log.e(TAG, "Could not load compatible profile", error)
        binding.profileContent.isVisible = false
        binding.profileState.isVisible = true
        binding.profileProgress.isVisible = false
        binding.profileStateTitle.setText(R.string.compatible_profile_error)
        binding.profileStateAction.isVisible = true
    }

    override fun openQuestionnaire(animalId: String) {
        startActivity(AdoptionQuestionnaireActivity.newIntent(this, animalId))
        finish()
    }

    override fun openAnimalDetails(animalId: String) {
        startActivity(AnimalDetailsActivity.newIntent(this, animalId))
    }

    override fun closeScreen() = finish()

    private fun showProfileContent(profile: AdoptionProfile) = with(binding) {
        profileState.isVisible = false
        profileContent.isVisible = true
        sizeProfile.setText(when (profile.sizeProfile) {
            SizeProfile.SMALL -> R.string.compatible_profile_size_small
            SizeProfile.SMALL_TO_MEDIUM -> R.string.compatible_profile_size_small_medium
            SizeProfile.ANY -> R.string.compatible_profile_size_any
        })
        temperamentProfile.setText(when (profile.temperamentProfile) {
            TemperamentProfile.CALM_AND_INDEPENDENT ->
                R.string.compatible_profile_temperament_calm
            TemperamentProfile.BALANCED -> R.string.compatible_profile_temperament_balanced
            TemperamentProfile.SOCIAL_AND_ACTIVE -> R.string.compatible_profile_temperament_social
        })
        energyProfile.setText(when (profile.energyProfile) {
            EnergyProfile.LOW -> R.string.compatible_profile_energy_low
            EnergyProfile.MODERATE -> R.string.compatible_profile_energy_moderate
            EnergyProfile.HIGH -> R.string.compatible_profile_energy_high
        })
        ageProfile.setText(when (profile.ageProfile) {
            AgeProfile.ADULT -> R.string.compatible_profile_age_adult
            AgeProfile.YOUNG_TO_ADULT -> R.string.compatible_profile_age_young_adult
            AgeProfile.ANY -> R.string.compatible_profile_age_any
        })
        profileDescription.text = getString(
            R.string.compatible_profile_description,
            getString(profile.answers.routine.toStringResource()).lowercase(portugueseLocale),
            getString(profile.answers.availableSpace.toStringResource()).lowercase(portugueseLocale)
        )
    }

    private fun RoutineOption?.toStringResource() = when (this) {
        RoutineOption.AWAY_ALL_DAY -> R.string.adoption_routine_away
        RoutineOption.HOME_OFFICE -> R.string.adoption_routine_home_office
        RoutineOption.OFTEN_HOME -> R.string.adoption_routine_home_often
        null -> R.string.adoption_routine_home_often
    }

    private fun AvailableSpaceOption?.toStringResource() = when (this) {
        AvailableSpaceOption.SMALL_APARTMENT -> R.string.adoption_space_small_apartment
        AvailableSpaceOption.APARTMENT_WITH_BALCONY -> R.string.adoption_space_balcony
        AvailableSpaceOption.HOUSE_WITH_YARD -> R.string.adoption_space_yard
        null -> R.string.adoption_space_small_apartment
    }

    private fun bindRecommendations(recommendations: List<AnimalRecommendation>) {
        binding.recommendationsContainer.removeAllViews()
        recommendations.forEach { recommendation ->
            val item = ItemCompatibleAnimalBinding.inflate(
                layoutInflater,
                binding.recommendationsContainer,
                false
            )
            bindRecommendation(item, recommendation)
            binding.recommendationsContainer.addView(item.root)
        }
    }

    private fun bindRecommendation(
        binding: ItemCompatibleAnimalBinding,
        recommendation: AnimalRecommendation
    ) = with(binding) {
        val animal = recommendation.animal
        matchValue.text = getString(
            R.string.compatible_profile_match_value,
            recommendation.matchPercentage
        )
        val matchStrokeColor = ContextCompat.getColor(
            this@CompatibleProfileActivity,
            recommendation.matchLevel.strokeColorRes()
        )
        val matchTextColor = ContextCompat.getColor(
            this@CompatibleProfileActivity,
            recommendation.matchLevel.textColorRes()
        )
        val matchBackgroundColor = ContextCompat.getColor(
            this@CompatibleProfileActivity,
            recommendation.matchLevel.backgroundColorRes()
        )
        matchBadge.strokeColor = matchStrokeColor
        matchBadge.setCardBackgroundColor(matchBackgroundColor)
        matchValue.setTextColor(matchTextColor)
        matchLabel.setTextColor(matchTextColor)
        root.strokeColor = ContextCompat.getColor(
            this@CompatibleProfileActivity,
            if (recommendation.matchLevel == MatchLevel.HIGH) {
                R.color.adoption_primary
            } else {
                R.color.border
            }
        )
        root.contentDescription = getString(
            R.string.compatible_profile_match_accessibility,
            recommendation.matchPercentage
        )
        animalTitle.text = getString(
            R.string.compatible_profile_animal_title,
            animal.name,
            animal.breed.ifBlank { speciesName(animal.species) },
            animal.ageText
        )
        shelterMeta.text = shelterMeta(animal, recommendation.distanceKm)
        shelterMeta.isVisible = shelterMeta.text.isNotEmpty()

        val traits = recommendationTraits(animal)
        val traitViews = listOf(traitOne, traitTwo, traitThree)
        traitViews.forEachIndexed { index, view -> bindTrait(view, traits.getOrNull(index)) }
        traitsGroup.isVisible = traits.isNotEmpty()
        viewAnimalButton.setOnClickListener { presenter.onAnimalClicked(animal.id) }
        root.setOnClickListener { presenter.onAnimalClicked(animal.id) }
    }

    private fun recommendationTraits(animal: Animal): List<String> = buildList {
        addAll(animal.characteristics.take(2))
        animal.energyLevel?.let { energy ->
            add(getString(when (energy) {
                AnimalEnergyLevel.LOW -> R.string.adoption_detail_energy_low
                AnimalEnergyLevel.MODERATE -> R.string.adoption_detail_energy_moderate
                AnimalEnergyLevel.HIGH -> R.string.adoption_detail_energy_high
            }))
        }
    }.distinct().take(3)

    private fun bindTrait(view: TextView, value: String?) {
        view.text = value.orEmpty()
        view.isVisible = !value.isNullOrBlank()
    }

    private fun shelterMeta(animal: Animal, distanceKm: Double?): String {
        val shelter = animal.shelterName.trim()
        val distance = distanceKm?.let(::formatDistance).orEmpty()
        return when {
            shelter.isNotEmpty() && distance.isNotEmpty() -> getString(
                R.string.compatible_profile_shelter_distance,
                shelter,
                distance
            )
            shelter.isNotEmpty() -> shelter
            else -> distance
        }
    }

    private fun speciesName(species: Species) = getString(when (species) {
        Species.DOG -> R.string.compatible_profile_species_dog
        Species.CAT -> R.string.compatible_profile_species_cat
        Species.OTHER -> R.string.compatible_profile_species_other
    })

    private fun formatDistance(distanceKm: Double) = getString(
        R.string.adoption_distance,
        distanceFormat.format(distanceKm)
    )

    @ColorRes
    private fun MatchLevel.strokeColorRes() = when (this) {
        MatchLevel.HIGH -> R.color.adoption_primary
        MatchLevel.MEDIUM -> R.color.gold
        MatchLevel.LOW -> R.color.muted
    }

    @ColorRes
    private fun MatchLevel.textColorRes() = when (this) {
        MatchLevel.HIGH -> R.color.adoption_primary
        MatchLevel.MEDIUM -> R.color.terracotta_dark
        MatchLevel.LOW -> R.color.muted
    }

    @ColorRes
    private fun MatchLevel.backgroundColorRes() = when (this) {
        MatchLevel.HIGH -> R.color.low_urgency_light
        MatchLevel.MEDIUM -> R.color.soft_gold
        MatchLevel.LOW -> R.color.soft_cream
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        private const val TAG = "CompatibleProfileActivity"
        private const val EXTRA_ANIMAL_ID = "extra_animal_id"

        fun newIntent(context: Context, animalId: String) =
            Intent(context, CompatibleProfileActivity::class.java).apply {
                putExtra(EXTRA_ANIMAL_ID, animalId)
            }
    }
}
