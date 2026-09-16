package com.example.weanimals.profile.reports.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.profile.reports.interactor.ObserveMyReportsInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MyReportsPresenter(private val observeReports: ObserveMyReportsInteractor) :
    BasePresenter<MyReportsContract.View>(), MyReportsContract.Presenter {
    private var job: Job? = null

    override fun start() {
        job?.cancel()
        job = presenterScope.launch {
            observeReports().collect { result ->
                result.onSuccess { reports -> withView { it.showReports(reports) } }
                    .onFailure { withView(MyReportsContract.View::showError) }
            }
        }
    }

    override fun detachView() {
        job?.cancel()
        super.detachView()
    }
}
