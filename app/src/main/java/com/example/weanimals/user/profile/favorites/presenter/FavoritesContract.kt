package com.example.weanimals.user.profile.favorites.presenter

import com.example.weanimals.user.adoption.listing.domain.Animal

interface FavoritesContract {
    interface View {
        fun showAnimals(animals: List<Animal>)
        fun showError()
    }
    interface Presenter { fun load() }
}
