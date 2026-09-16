package com.example.weanimals.profile.favorites.presenter

import com.example.weanimals.adoption.listing.domain.Animal

interface FavoritesContract {
    interface View {
        fun showAnimals(animals: List<Animal>)
        fun showError()
    }
    interface Presenter { fun load() }
}
