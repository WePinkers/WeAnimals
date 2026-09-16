package com.example.weanimals.profile.overview.presenter

interface ProfileContract {
    interface View {
        fun showIdentity(name: String?)
        fun showReportCount(count: Int)
        fun showReportsError()
    }

    interface Presenter {
        fun start()
    }
}
