package com.example.weanimals.core.di

import android.content.Context
import com.example.weanimals.home.interactor.ObserveUserReportsInteractor
import com.example.weanimals.home.repository.FirebaseHomeRepository
import com.example.weanimals.home.presenter.HomePresenter
import com.example.weanimals.locationsearch.interactor.GetCurrentLocationInteractor
import com.example.weanimals.locationsearch.interactor.SearchLocationsInteractor
import com.example.weanimals.locationsearch.repository.AndroidLocationRepository
import com.example.weanimals.locationsearch.repository.LocationRepository
import com.example.weanimals.locationsearch.presenter.LocationSearchPresenter
import com.example.weanimals.report.repository.FirebaseReportRepository
import com.example.weanimals.report.repository.ReportRepository
import com.example.weanimals.report.presenter.ReportPresenter
import com.example.weanimals.report.domain.ReportDraft
import com.example.weanimals.report.interactor.PrepareReportDraftInteractor
import com.example.weanimals.triage.interactor.CalculateTriageInteractor
import com.example.weanimals.triage.interactor.SubmitTriagedReportInteractor
import com.example.weanimals.triage.presenter.TriagePresenter
import com.example.weanimals.triage.repository.FirebaseTriageRepository
import com.example.weanimals.tracking.interactor.ObserveReportInteractor
import com.example.weanimals.tracking.presenter.TrackingPresenter
import com.example.weanimals.tracking.repository.FirebaseTrackingRepository
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

    private val observeUserReportsInteractor by lazy {
        ObserveUserReportsInteractor(homeRepository)
    }

    private val markReportAsViewedInteractor by lazy {
        com.example.weanimals.home.interactor.MarkReportAsViewedInteractor(homeRepository)
    }

    private val observeReportInteractor by lazy {
        ObserveReportInteractor(trackingRepository)
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
}
