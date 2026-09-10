package com.example.weanimals.core.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.weanimals.adoption.compatibility.interactor.GetAnimalRecommendationsInteractor
import com.example.weanimals.adoption.compatibility.presenter.CompatibleProfilePresenter
import com.example.weanimals.adoption.compatibility.repository.AnimalRecommendationRepositoryImpl
import com.example.weanimals.adoption.detail.interactor.GetAnimalDetailsInteractor
import com.example.weanimals.adoption.detail.interactor.GetShelterDistanceInteractor
import com.example.weanimals.adoption.detail.presenter.AnimalDetailsPresenter
import com.example.weanimals.adoption.detail.repository.DemoAnimalDetailsRepository
import com.example.weanimals.adoption.detail.repository.FirebaseAnimalDetailsRepository
import com.example.weanimals.adoption.listing.interactor.GetAvailableAnimalsInteractor
import com.example.weanimals.adoption.listing.presenter.AdoptionPresenter
import com.example.weanimals.adoption.listing.repository.AnimalRepositoryImpl
import com.example.weanimals.adoption.listing.repository.DemoAnimalRepository
import com.example.weanimals.adoption.questionnaire.interactor.BuildAdoptionProfileInteractor
import com.example.weanimals.adoption.questionnaire.interactor.GetAdoptionProfileInteractor
import com.example.weanimals.adoption.questionnaire.interactor.SaveAdoptionProfileInteractor
import com.example.weanimals.adoption.questionnaire.presenter.AdoptionQuestionnairePresenter
import com.example.weanimals.adoption.questionnaire.repository.FirebaseAdoptionProfileRepository
import com.example.weanimals.reporting.chat.interactor.ObserveCaseMessagesInteractor
import com.example.weanimals.reporting.chat.interactor.SendCaseMessageInteractor
import com.example.weanimals.reporting.chat.presenter.ChatPresenter
import com.example.weanimals.reporting.chat.repository.FirebaseChatRepository
import com.example.weanimals.reporting.details.interactor.ObserveReportDetailsInteractor
import com.example.weanimals.reporting.details.presenter.DetailsPresenter
import com.example.weanimals.reporting.details.repository.FirebaseDetailsRepository
import com.example.weanimals.home.interactor.ObserveUserReportsInteractor
import com.example.weanimals.home.repository.FirebaseHomeRepository
import com.example.weanimals.home.presenter.HomePresenter
import com.example.weanimals.reporting.locationsearch.interactor.GetCurrentLocationInteractor
import com.example.weanimals.reporting.locationsearch.interactor.SearchLocationsInteractor
import com.example.weanimals.reporting.locationsearch.repository.AndroidLocationRepository
import com.example.weanimals.reporting.locationsearch.repository.LocationRepository
import com.example.weanimals.reporting.locationsearch.presenter.LocationSearchPresenter
import com.example.weanimals.reporting.report.repository.FirebaseReportRepository
import com.example.weanimals.reporting.report.repository.ReportRepository
import com.example.weanimals.reporting.report.presenter.ReportPresenter
import com.example.weanimals.reporting.report.domain.ReportDraft
import com.example.weanimals.reporting.report.interactor.PrepareReportDraftInteractor
import com.example.weanimals.reporting.triage.interactor.CalculateTriageInteractor
import com.example.weanimals.reporting.triage.interactor.SubmitTriagedReportInteractor
import com.example.weanimals.reporting.triage.presenter.TriagePresenter
import com.example.weanimals.reporting.triage.repository.FirebaseTriageRepository
import com.example.weanimals.reporting.tracking.interactor.ObserveReportInteractor
import com.example.weanimals.reporting.tracking.presenter.TrackingPresenter
import com.example.weanimals.reporting.tracking.repository.FirebaseTrackingRepository
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
import com.example.weanimals.core.location.repository.AndroidUserLocationRepository
import com.example.weanimals.core.location.domain.Coordinates
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val isDebuggable = applicationContext.applicationInfo.flags and
        ApplicationInfo.FLAG_DEBUGGABLE != 0

    private val reportRepository: ReportRepository by lazy {
        FirebaseReportRepository(
            context = applicationContext,
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }

    private val locationRepository: LocationRepository by lazy {
        AndroidLocationRepository(applicationContext)
    }

    private val homeRepository by lazy {
        FirebaseHomeRepository(reportRepository)
    }

    private val trackingRepository by lazy {
        FirebaseTrackingRepository(reportRepository)
    }

    private val detailsRepository by lazy {
        FirebaseDetailsRepository(reportRepository)
    }

    private val chatRepository by lazy {
        FirebaseChatRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }

    private val adoptionListingRepository by lazy {
        val firebaseRepository = AnimalRepositoryImpl(
            FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance()
        )
        if (isDebuggable) DemoAnimalRepository(firebaseRepository) else firebaseRepository
    }

    private val animalDetailsRepository by lazy {
        val firebaseRepository = FirebaseAnimalDetailsRepository(
            FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance()
        )
        if (isDebuggable) {
            DemoAnimalDetailsRepository(
                delegate = firebaseRepository,
                demoAnimalId = DemoAnimalRepository.DEMO_ANIMAL_ID,
                demoShelterCoordinates = Coordinates(
                    latitude = DemoAnimalRepository.DEMO_SHELTER_LATITUDE,
                    longitude = DemoAnimalRepository.DEMO_SHELTER_LONGITUDE
                )
            )
        } else {
            firebaseRepository
        }
    }

    private val adoptionProfileRepository by lazy {
        FirebaseAdoptionProfileRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }

    private val animalRecommendationRepository by lazy {
        AnimalRecommendationRepositoryImpl(adoptionListingRepository)
    }

    private val getAdoptionProfileInteractor by lazy {
        GetAdoptionProfileInteractor(adoptionProfileRepository)
    }

    private val userLocationRepository by lazy {
        AndroidUserLocationRepository(applicationContext)
    }

    private val getUserLocationInteractor by lazy {
        GetUserLocationInteractor(userLocationRepository)
    }

    private val calculateDistanceInteractor by lazy {
        CalculateDistanceInteractor()
    }

    private val observeUserReportsInteractor by lazy {
        ObserveUserReportsInteractor(homeRepository)
    }

    private val markReportAsViewedInteractor by lazy {
        com.example.weanimals.home.interactor.MarkReportAsViewedInteractor(homeRepository)
    }

    private val observeReportInteractor by lazy {
        ObserveReportInteractor(trackingRepository)
    }

    private val observeReportDetailsInteractor by lazy {
        ObserveReportDetailsInteractor(detailsRepository)
    }

    private val observeCaseMessagesInteractor by lazy {
        ObserveCaseMessagesInteractor(chatRepository)
    }

    private val sendCaseMessageInteractor by lazy {
        SendCaseMessageInteractor(chatRepository)
    }

    private val prepareReportDraftInteractor by lazy {
        PrepareReportDraftInteractor()
    }

    private val triageRepository by lazy {
        FirebaseTriageRepository(reportRepository)
    }

    private val calculateTriageInteractor by lazy {
        CalculateTriageInteractor()
    }

    private val submitTriagedReportInteractor by lazy {
        SubmitTriagedReportInteractor(triageRepository)
    }

    private val getCurrentLocationInteractor by lazy {
        GetCurrentLocationInteractor(locationRepository)
    }

    private val searchLocationsInteractor by lazy {
        SearchLocationsInteractor(locationRepository)
    }

    fun createHomePresenter() = HomePresenter(
        observeUserReportsInteractor = observeUserReportsInteractor,
        markReportAsViewedInteractor = markReportAsViewedInteractor
    )

    fun createAdoptionPresenter() = AdoptionPresenter(
        getAvailableAnimals = GetAvailableAnimalsInteractor(adoptionListingRepository),
        getUserLocation = getUserLocationInteractor
    )

    fun createAnimalDetailsPresenter(animalId: String) =
        AnimalDetailsPresenter(
            animalId = animalId,
            getAnimalDetails = GetAnimalDetailsInteractor(animalDetailsRepository),
            getShelterDistance = GetShelterDistanceInteractor(
                getUserLocation = getUserLocationInteractor,
                calculateDistance = calculateDistanceInteractor
            ),
            getAdoptionProfile = getAdoptionProfileInteractor
        )

    fun createAdoptionQuestionnairePresenter(animalId: String) =
        AdoptionQuestionnairePresenter(
            animalId = animalId,
            buildProfile = BuildAdoptionProfileInteractor(),
            saveProfile = SaveAdoptionProfileInteractor(adoptionProfileRepository)
        )

    fun createCompatibleProfilePresenter(animalId: String) =
        CompatibleProfilePresenter(
            originAnimalId = animalId,
            getProfile = getAdoptionProfileInteractor,
            getRecommendations = GetAnimalRecommendationsInteractor(
                repository = animalRecommendationRepository,
                getUserLocation = getUserLocationInteractor,
                calculateDistance = calculateDistanceInteractor
            )
        )

    fun createReportPresenter() = ReportPresenter(
        getCurrentLocationInteractor = getCurrentLocationInteractor,
        prepareReportDraftInteractor = prepareReportDraftInteractor
    )

    fun createLocationSearchPresenter() = LocationSearchPresenter(searchLocationsInteractor)

    fun createTriagePresenter(draft: ReportDraft) = TriagePresenter(
        draft = draft,
        calculateTriageInteractor = calculateTriageInteractor,
        submitTriagedReportInteractor = submitTriagedReportInteractor
    )

    fun createTrackingPresenter(reportId: String) = TrackingPresenter(
        reportId = reportId,
        observeReportInteractor = observeReportInteractor
    )

    fun createDetailsPresenter(reportId: String) = DetailsPresenter(
        reportId = reportId,
        observeReportDetailsInteractor = observeReportDetailsInteractor
    )

    fun createChatPresenter(reportId: String) = ChatPresenter(
        reportId = reportId,
        observeCaseMessagesInteractor = observeCaseMessagesInteractor,
        sendCaseMessageInteractor = sendCaseMessageInteractor
    )
}
