package com.example.weanimals.adoption.detail.presenter

import com.example.weanimals.adoption.detail.domain.AnimalDetails
import com.example.weanimals.adoption.detail.interactor.GetAnimalDetailsInteractor
import com.example.weanimals.adoption.detail.interactor.GetShelterDistanceInteractor
import com.example.weanimals.adoption.detail.interactor.CreateAnimalSharePdfInteractor
import com.example.weanimals.adoption.questionnaire.interactor.GetAdoptionProfileInteractor
import com.example.weanimals.core.base.BasePresenter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AnimalDetailsPresenter(
    private val animalId: String,
    private val getAnimalDetails: GetAnimalDetailsInteractor,
    private val getShelterDistance: GetShelterDistanceInteractor,
    private val getAdoptionProfile: GetAdoptionProfileInteractor,
    private val createAnimalSharePdf: CreateAnimalSharePdfInteractor
) : BasePresenter<AnimalDetailsContract.View>(), AnimalDetailsContract.Presenter {

    private var started = false
    private var loading = false
    private var details: AnimalDetails? = null
    private var distanceKm: Double? = null
    private var error: Throwable? = null
    private var favorite = false
    private var locationRequestMade = false
    private var distanceJob: Job? = null
    private var checkingAdoptionProfile = false
    private var sharing = false

    override fun start() {
        if (!started) {
            started = true
            load()
        } else {
            render()
        }
    }

    override fun onRetryClicked() {
        if (!loading) load()
    }

    override fun onBackClicked() = withView(AnimalDetailsContract.View::closeScreen)

    override fun onShareClicked() {
        val animal = details ?: return
        if (sharing) return
        sharing = true
        withView { it.showShareLoading(true) }
        presenterScope.launch {
            try {
                createAnimalSharePdf(animal, distanceKm).fold(
                    onSuccess = { document ->
                        sharing = false
                        withView {
                            it.showShareLoading(false)
                            it.shareAnimalDocument(document)
                        }
                    },
                    onFailure = { cause ->
                        sharing = false
                        withView {
                            it.showShareLoading(false)
                            it.showShareError(cause)
                        }
                    }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    override fun onFavoriteClicked() {
        if (details == null) return
        favorite = !favorite
        withView { it.showFavorite(favorite) }
    }

    override fun onConversationClicked() = withView { it.showActionUnavailable() }

    override fun onAdoptClicked() {
        if (details == null || checkingAdoptionProfile) return
        checkingAdoptionProfile = true
        withView { it.showAdoptionProfileLoading(true) }
        presenterScope.launch {
            try {
                getAdoptionProfile().fold(
                    onSuccess = { profile ->
                        checkingAdoptionProfile = false
                        withView {
                            it.showAdoptionProfileLoading(false)
                            if (profile == null) it.openAdoptionQuestionnaire(animalId)
                            else it.openAdoptionConfirmation(animalId)
                        }
                    },
                    onFailure = { cause ->
                        checkingAdoptionProfile = false
                        withView {
                            it.showAdoptionProfileLoading(false)
                            it.showAdoptionProfileError(cause)
                        }
                    }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    override fun onLocationAccessResult(available: Boolean) {
        if (available) details?.let(::loadDistance)
    }

    private fun load() {
        distanceJob?.cancel()
        loading = true
        error = null
        distanceKm = null
        locationRequestMade = false
        withView { it.showLoading() }
        presenterScope.launch {
            try {
                getAnimalDetails(animalId).fold(
                    onSuccess = {
                        details = it
                        loading = false
                        render()
                        loadDistance(it)
                    },
                    onFailure = {
                        error = it
                        loading = false
                        render()
                    }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    private fun render() = withView { view ->
        when {
            loading -> view.showLoading()
            details != null -> {
                val animal = requireNotNull(details)
                view.showAnimal(animal)
                view.showDistance(distanceKm, animal.shelter?.address.orEmpty())
                view.showFavorite(favorite)
            }
            error != null -> view.showError(requireNotNull(error))
        }
    }

    private fun loadDistance(animal: AnimalDetails) {
        if (animal.shelter?.coordinates == null) return
        distanceJob?.cancel()
        distanceJob = presenterScope.launch {
            val calculatedDistance = getShelterDistance(animal.shelter)
            distanceKm = calculatedDistance
            withView {
                it.showDistance(calculatedDistance, animal.shelter.address)
                if (calculatedDistance == null && !locationRequestMade) {
                    locationRequestMade = true
                    it.requestUserLocation()
                }
            }
        }
    }
}
