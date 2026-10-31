package com.example.weanimals.user.reporting.details.presenter

import com.example.weanimals.user.reporting.details.domain.ReportDetails

interface DetailsContract {
    interface View {
        fun showLoading()
        fun showDetails(details: ReportDetails)
        fun showDetailsError(error: Throwable)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun destroy()
        fun loadDetails()
    }
}
