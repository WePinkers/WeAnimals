package com.example.weanimals.map.overview.presenter

import com.example.weanimals.map.overview.interactor.NearbyOccurrences

interface MapContract {
    interface View {
        fun showLoading()
        fun showOccurrences(result: NearbyOccurrences)
        fun showError()
    }
    interface Presenter { fun load() }
}
