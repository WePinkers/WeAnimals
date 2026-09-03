package com.example.weanimals.core.di

import android.content.Context
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
