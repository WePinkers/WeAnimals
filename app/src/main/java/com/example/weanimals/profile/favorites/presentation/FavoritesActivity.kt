package com.example.weanimals.profile.favorites.presentation

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.adoption.detail.presentation.AnimalDetailsActivity
import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.adoption.listing.presentation.AnimalAdapter
import com.example.weanimals.databinding.ActivityFavoritesBinding
import com.example.weanimals.profile.favorites.presenter.FavoritesContract

class FavoritesActivity : AppCompatActivity(), FavoritesContract.View {
    private lateinit var binding: ActivityFavoritesBinding
    private val adapter = AnimalAdapter { id -> startActivity(AnimalDetailsActivity.newIntent(this, id)) }
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createFavoritesPresenter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.favoritesList.layoutManager = LinearLayoutManager(this)
        binding.favoritesList.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        binding.emptyState.setText(R.string.profile_favorites_loading)
        presenter.load()
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun showAnimals(animals: List<Animal>) {
        adapter.showAnimals(animals) { }
        binding.emptyState.visibility = if (animals.isEmpty()) View.VISIBLE else View.GONE
        binding.emptyState.setText(R.string.profile_favorites_empty)
    }

    override fun showError() {
        binding.emptyState.visibility = View.VISIBLE
        binding.emptyState.setText(R.string.profile_favorites_error)
    }
}
