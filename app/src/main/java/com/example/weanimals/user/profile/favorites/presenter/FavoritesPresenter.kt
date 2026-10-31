package com.example.weanimals.user.profile.favorites.presenter

import com.example.weanimals.core.base.BasePresenter
import com.example.weanimals.user.profile.favorites.interactor.GetFavoriteAnimalsInteractor
import kotlinx.coroutines.launch

class FavoritesPresenter(private val getFavorites: GetFavoriteAnimalsInteractor) :
    BasePresenter<FavoritesContract.View>(), FavoritesContract.Presenter {
    override fun load() {
        presenterScope.launch {
            getFavorites().onSuccess { animals -> withView { it.showAnimals(animals) } }
                .onFailure { withView(FavoritesContract.View::showError) }
        }
    }
}
