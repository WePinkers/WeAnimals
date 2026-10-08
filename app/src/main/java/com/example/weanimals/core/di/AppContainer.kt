package com.example.weanimals.core.di

import android.content.Context
import com.example.weanimals.user.adoption.compatibility.interactor.GetAnimalRecommendationsInteractor
import com.example.weanimals.user.adoption.chat.repository.FirebaseAdoptionChatRepository
import com.example.weanimals.user.adoption.checkin.interactor.CalculateFollowUpMilestonesInteractor
import com.example.weanimals.user.adoption.checkin.interactor.GetAdoptionFollowUpInteractor
import com.example.weanimals.user.adoption.checkin.interactor.GetFollowUpQuestionsInteractor
import com.example.weanimals.user.adoption.checkin.interactor.SubmitAdoptionCheckInInteractor
import com.example.weanimals.user.adoption.checkin.presenter.AdoptionFollowUpPresenter
import com.example.weanimals.user.adoption.checkin.repository.FirebaseAdoptionFollowUpRepository
import com.example.weanimals.user.adoption.confirmation.interactor.GetAdoptionCandidateInteractor
import com.example.weanimals.user.adoption.confirmation.interactor.SubmitAdoptionApplicationInteractor
import com.example.weanimals.user.adoption.confirmation.presenter.AdoptionConfirmationPresenter
import com.example.weanimals.user.adoption.confirmation.repository.FirebaseAdoptionApplicationRepository
import com.example.weanimals.user.adoption.compatibility.presenter.CompatibleProfilePresenter
import com.example.weanimals.user.adoption.compatibility.repository.AnimalRecommendationRepositoryImpl
import com.example.weanimals.user.adoption.detail.interactor.GetAnimalDetailsInteractor
import com.example.weanimals.user.adoption.detail.interactor.GetShelterDistanceInteractor
import com.example.weanimals.user.adoption.detail.interactor.CreateAnimalSharePdfInteractor
import com.example.weanimals.user.adoption.detail.presenter.AnimalDetailsPresenter
import com.example.weanimals.user.adoption.detail.repository.AndroidAnimalShareRepository
import com.example.weanimals.user.adoption.detail.repository.FirebaseAnimalDetailsRepository
import com.example.weanimals.user.adoption.listing.interactor.GetAvailableAnimalsInteractor
import com.example.weanimals.user.adoption.listing.presenter.AdoptionPresenter
import com.example.weanimals.user.adoption.listing.repository.AnimalRepositoryImpl
import com.example.weanimals.user.adoption.questionnaire.interactor.BuildAdoptionProfileInteractor
import com.example.weanimals.user.adoption.questionnaire.domain.AdoptionQuestionnaireAnswers
import com.example.weanimals.user.adoption.questionnaire.interactor.GetAdoptionProfileInteractor
import com.example.weanimals.user.adoption.questionnaire.interactor.SaveAdoptionProfileInteractor
import com.example.weanimals.user.adoption.questionnaire.presenter.AdoptionQuestionnairePresenter
import com.example.weanimals.user.adoption.questionnaire.repository.FirebaseAdoptionProfileRepository
import com.example.weanimals.user.adoption.sent.domain.AdoptionSentSummary
import com.example.weanimals.user.adoption.sent.presenter.AdoptionSentPresenter
import com.example.weanimals.user.adoption.tracking.interactor.ObserveAdoptionApplicationInteractor
import com.example.weanimals.user.adoption.tracking.interactor.WithdrawAdoptionApplicationInteractor
import com.example.weanimals.user.adoption.tracking.presenter.AdoptionTrackingPresenter
import com.example.weanimals.user.adoption.tracking.repository.FirebaseAdoptionTrackingRepository
import com.example.weanimals.user.reporting.chat.interactor.ObserveCaseMessagesInteractor
import com.example.weanimals.user.reporting.chat.interactor.SendCaseMessageInteractor
import com.example.weanimals.user.reporting.chat.presenter.ChatPresenter
import com.example.weanimals.user.reporting.chat.repository.FirebaseChatRepository
import com.example.weanimals.user.reporting.details.interactor.ObserveReportDetailsInteractor
import com.example.weanimals.user.reporting.details.presenter.DetailsPresenter
import com.example.weanimals.user.reporting.details.repository.FirebaseDetailsRepository
import com.example.weanimals.user.home.interactor.ObserveUserReportsInteractor
import com.example.weanimals.user.home.repository.FirebaseHomeRepository
import com.example.weanimals.user.home.presenter.HomePresenter
import com.example.weanimals.user.profile.overview.repository.FirebaseProfileRepository
import com.example.weanimals.user.profile.overview.interactor.GetProfileIdentityInteractor
import com.example.weanimals.user.profile.overview.presenter.ProfilePresenter
import com.example.weanimals.user.profile.reports.interactor.ObserveMyReportsInteractor
import com.example.weanimals.user.profile.reports.presenter.MyReportsPresenter
import com.example.weanimals.user.profile.favorites.repository.FirebaseFavoriteRepository
import com.example.weanimals.user.profile.favorites.interactor.GetFavoriteAnimalsInteractor
import com.example.weanimals.user.profile.favorites.presenter.FavoritesPresenter
import com.example.weanimals.user.map.overview.repository.FirebasePublicOccurrenceRepository
import com.example.weanimals.user.map.overview.interactor.GetNearbyOccurrencesInteractor
import com.example.weanimals.user.map.overview.presenter.MapPresenter
import com.example.weanimals.user.reporting.locationsearch.interactor.GetCurrentLocationInteractor
import com.example.weanimals.user.reporting.locationsearch.interactor.SearchLocationsInteractor
import com.example.weanimals.user.reporting.locationsearch.repository.AndroidLocationRepository
import com.example.weanimals.user.reporting.locationsearch.repository.LocationRepository
import com.example.weanimals.user.reporting.locationsearch.presenter.LocationSearchPresenter
import com.example.weanimals.user.reporting.report.repository.FirebaseReportRepository
import com.example.weanimals.user.reporting.report.repository.ReportRepository
import com.example.weanimals.user.reporting.report.presenter.ReportPresenter
import com.example.weanimals.user.reporting.report.domain.ReportDraft
import com.example.weanimals.user.reporting.report.interactor.PrepareReportDraftInteractor
import com.example.weanimals.user.reporting.triage.interactor.CalculateTriageInteractor
import com.example.weanimals.user.reporting.triage.interactor.SubmitTriagedReportInteractor
import com.example.weanimals.user.reporting.triage.presenter.TriagePresenter
import com.example.weanimals.user.reporting.triage.repository.FirebaseTriageRepository
import com.example.weanimals.user.reporting.tracking.interactor.ObserveReportInteractor
import com.example.weanimals.user.reporting.tracking.presenter.TrackingPresenter
import com.example.weanimals.user.reporting.tracking.repository.FirebaseTrackingRepository
import com.example.weanimals.core.location.interactor.CalculateDistanceInteractor
import com.example.weanimals.core.location.interactor.GetUserLocationInteractor
import com.example.weanimals.core.location.repository.AndroidUserLocationRepository
import com.example.weanimals.user.community.feed.interactor.GetCommunityFeedInteractor
import com.example.weanimals.user.community.feed.presenter.CommunityPresenter
import com.example.weanimals.user.community.feed.repository.CommunityRepositoryFactory
import com.example.weanimals.user.community.create.interactor.PublishCommunityPostInteractor
import com.example.weanimals.user.community.create.presenter.CreateCommunityPostPresenter
import com.example.weanimals.user.community.create.repository.FirebaseCommunityPostRepository
import com.example.weanimals.user.community.campaign.repository.FirebaseCommunityCampaignRepository
import com.example.weanimals.user.community.detail.repository.CommunityPostEngagementRepository
import com.example.weanimals.user.community.detail.repository.FirebaseCommunityPostEngagementRepository
import com.example.weanimals.organization.dashboard.presenter.OrganizationReportDetailPresenter
import com.example.weanimals.organization.dashboard.presenter.OrganizationReportsPresenter
import com.example.weanimals.organization.dashboard.repository.FirebaseOrganizationReportsRepository
import com.example.weanimals.organization.dashboard.repository.OrganizationReportsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    private val reportRepository: ReportRepository by lazy {
        FirebaseReportRepository(
            context = applicationContext,
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }

    private val organizationReportsRepository: OrganizationReportsRepository by lazy {
        FirebaseOrganizationReportsRepository(
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

    private val adoptionChatRepository by lazy {
        FirebaseAdoptionChatRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }

    private val adoptionListingRepository by lazy {
        AnimalRepositoryImpl(
            FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance()
        )
    }

    private val profileRepository by lazy { FirebaseProfileRepository(FirebaseAuth.getInstance()) }

    private val favoriteRepository by lazy {
        FirebaseFavoriteRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
    }

    private val publicOccurrenceRepository by lazy {
        FirebasePublicOccurrenceRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
    }

    private val animalDetailsRepository by lazy {
        FirebaseAnimalDetailsRepository(
            FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance()
        )
    }

    private val animalShareRepository by lazy {
        AndroidAnimalShareRepository(applicationContext)
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

    private val adoptionApplicationRepository by lazy {
        FirebaseAdoptionApplicationRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }

    private val adoptionTrackingRepository by lazy {
        FirebaseAdoptionTrackingRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }

    private val adoptionFollowUpRepository by lazy {
        FirebaseAdoptionFollowUpRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
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
        com.example.weanimals.user.home.interactor.MarkReportAsViewedInteractor(homeRepository)
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

    private val getProfileIdentityInteractor by lazy {
        GetProfileIdentityInteractor(profileRepository)
    }

    fun createHomePresenter() = HomePresenter(
        observeUserReportsInteractor = observeUserReportsInteractor,
        markReportAsViewedInteractor = markReportAsViewedInteractor,
        getProfileIdentityInteractor = getProfileIdentityInteractor
    )

    fun createOrganizationReportsPresenter() = OrganizationReportsPresenter(
        repository = organizationReportsRepository
    )

    fun createOrganizationReportDetailPresenter(reportId: String) =
        OrganizationReportDetailPresenter(
            reportId = reportId,
            repository = organizationReportsRepository
        )

    fun createCommunityPresenter() = CommunityPresenter(
        GetCommunityFeedInteractor(CommunityRepositoryFactory.create()),
        getCurrentLocationInteractor
    )

    fun createCommunityPostPresenter() = CreateCommunityPostPresenter(
        getCurrentLocation = getCurrentLocationInteractor,
        publishPost = PublishCommunityPostInteractor(
            FirebaseCommunityPostRepository(
                applicationContext, FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()
            )
        )
    )

    fun createCommunityPostEngagementRepository(): CommunityPostEngagementRepository =
        FirebaseCommunityPostEngagementRepository(
            FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()
        )

    fun createCommunityCampaignRepository() = FirebaseCommunityCampaignRepository(
        applicationContext,
        FirebaseAuth.getInstance(),
        FirebaseFirestore.getInstance()
    )

    fun getCurrentLocationForCommunity() = getCurrentLocationInteractor

    fun createProfilePresenter() = ProfilePresenter(
        GetProfileIdentityInteractor(profileRepository), reportRepository
    )

    fun createMyReportsPresenter() = MyReportsPresenter(ObserveMyReportsInteractor(reportRepository))

    fun createFavoritesPresenter() = FavoritesPresenter(
        GetFavoriteAnimalsInteractor(favoriteRepository, adoptionListingRepository)
    )

    fun createMapPresenter() = MapPresenter(
        GetNearbyOccurrencesInteractor(
            publicOccurrenceRepository, getUserLocationInteractor, calculateDistanceInteractor
        )
    )

    fun createAdoptionPresenter() = AdoptionPresenter(
        getAvailableAnimals = GetAvailableAnimalsInteractor(adoptionListingRepository),
        getUserLocation = getUserLocationInteractor,
        getAdoptionProfile = getAdoptionProfileInteractor
    )

    fun createAnimalDetailsPresenter(animalId: String) =
        AnimalDetailsPresenter(
            animalId = animalId,
            getAnimalDetails = GetAnimalDetailsInteractor(animalDetailsRepository),
            getShelterDistance = GetShelterDistanceInteractor(
                getUserLocation = getUserLocationInteractor,
                calculateDistance = calculateDistanceInteractor
            ),
            getAdoptionProfile = getAdoptionProfileInteractor,
            createAnimalSharePdf = CreateAnimalSharePdfInteractor(animalShareRepository),
            favoriteRepository = favoriteRepository
        )

    fun createAdoptionQuestionnairePresenter(
        animalId: String,
        initialAnswers: AdoptionQuestionnaireAnswers? = null
    ) =
        AdoptionQuestionnairePresenter(
            animalId = animalId,
            buildProfile = BuildAdoptionProfileInteractor(),
            saveProfile = SaveAdoptionProfileInteractor(adoptionProfileRepository),
            initialAnswers = initialAnswers
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

    fun createAdoptionConfirmationPresenter(animalId: String) = AdoptionConfirmationPresenter(
        animalId = animalId,
        getCandidate = GetAdoptionCandidateInteractor(
            getProfile = getAdoptionProfileInteractor,
            animalRepository = adoptionListingRepository,
            getRecommendations = GetAnimalRecommendationsInteractor(
                repository = animalRecommendationRepository,
                getUserLocation = getUserLocationInteractor,
                calculateDistance = calculateDistanceInteractor
            )
        ),
        submitApplication = SubmitAdoptionApplicationInteractor(adoptionApplicationRepository)
    )

    fun createAdoptionSentPresenter(summary: AdoptionSentSummary) = AdoptionSentPresenter(summary)

    fun createAdoptionTrackingPresenter(summary: AdoptionSentSummary) = AdoptionTrackingPresenter(
        summary = summary,
        observeApplication = ObserveAdoptionApplicationInteractor(adoptionTrackingRepository),
        withdrawApplication = WithdrawAdoptionApplicationInteractor(adoptionTrackingRepository)
    )

    fun createAdoptionFollowUpPresenter(animalId: String): AdoptionFollowUpPresenter {
        return AdoptionFollowUpPresenter(
            animalId = animalId,
            getFollowUp = GetAdoptionFollowUpInteractor(adoptionFollowUpRepository),
            calculateMilestones = CalculateFollowUpMilestonesInteractor(),
            getQuestions = GetFollowUpQuestionsInteractor(),
            submitCheckIn = SubmitAdoptionCheckInInteractor(adoptionFollowUpRepository)
        )
    }

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

    fun createAdoptionChatPresenter(animalId: String) = ChatPresenter(
        reportId = animalId,
        observeCaseMessagesInteractor = ObserveCaseMessagesInteractor(adoptionChatRepository),
        sendCaseMessageInteractor = SendCaseMessageInteractor(adoptionChatRepository)
    )
}
