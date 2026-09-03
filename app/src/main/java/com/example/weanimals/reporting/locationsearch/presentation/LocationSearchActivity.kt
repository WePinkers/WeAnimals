package com.example.weanimals.reporting.locationsearch.presentation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.databinding.ActivityLocationSearchBinding
import com.example.weanimals.databinding.ItemLocationResultBinding
import com.example.weanimals.reporting.locationsearch.domain.LocationDetails
import com.example.weanimals.reporting.locationsearch.presenter.LocationSearchContract
import com.example.weanimals.reporting.locationsearch.presenter.LocationSearchPresenter

class LocationSearchActivity : AppCompatActivity(), LocationSearchContract.View {

    private lateinit var binding: ActivityLocationSearchBinding
    private val presenter: LocationSearchPresenter by lazy {
        (application as WeAnimalsApplication).appContainer.createLocationSearchPresenter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityLocationSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.locationHeader.headerTitle.setText(R.string.location_search_title)

        binding.locationSearchEdit.setText(
            intent.getStringExtra(EXTRA_INITIAL_QUERY).orEmpty()
        )
        updateClearButtonVisibility()
        binding.locationSearchEdit.doAfterTextChanged { text ->
            updateClearButtonVisibility()
            presenter.search(text?.toString().orEmpty())
        }
        binding.clearSearchButton.setOnClickListener {
            binding.locationSearchEdit.text?.clear()
            binding.locationSearchEdit.requestFocus()
        }
        binding.locationHeader.backButton.setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.search(binding.locationSearchEdit.text?.toString().orEmpty())
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun showSearchLoading() {
        binding.resultsContainer.removeAllViews()
        binding.searchStatus.visibility = View.VISIBLE
        binding.searchStatus.setText(R.string.location_search_loading)
    }

    override fun showSearchResults(results: List<LocationDetails>) {
        binding.resultsContainer.removeAllViews()
        binding.searchStatus.visibility = View.GONE
        results.forEachIndexed { index, location ->
            val itemBinding = ItemLocationResultBinding.inflate(
                LayoutInflater.from(this),
                binding.resultsContainer,
                false
            )
            itemBinding.resultTitle.text = location.address
            itemBinding.resultSubtitle.text = location.secondaryAddress
            itemBinding.resultSubtitle.visibility =
                if (location.secondaryAddress.isBlank()) View.GONE else View.VISIBLE
            if (index == 0) {
                itemBinding.resultRoot.setBackgroundResource(R.drawable.bg_location_result_selected)
            }
            itemBinding.resultRoot.setOnClickListener { presenter.selectLocation(location) }
            binding.resultsContainer.addView(itemBinding.root)
        }
    }

    override fun showSearchEmpty() {
        binding.resultsContainer.removeAllViews()
        val hasQuery = !binding.locationSearchEdit.text.isNullOrBlank()
        binding.searchStatus.visibility = if (hasQuery) View.VISIBLE else View.GONE
        if (hasQuery) binding.searchStatus.setText(R.string.location_search_empty)
    }

    override fun showSearchError(error: Throwable) {
        Log.e(TAG, "Could not search locations", error)
        binding.resultsContainer.removeAllViews()
        binding.searchStatus.visibility = View.VISIBLE
        binding.searchStatus.setText(R.string.location_search_error)
    }

    override fun showLocationSelected(location: LocationDetails) {
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(EXTRA_ADDRESS, location.address)
                .putExtra(EXTRA_SECONDARY_ADDRESS, location.secondaryAddress)
                .putExtra(EXTRA_LATITUDE, location.latitude)
                .putExtra(EXTRA_LONGITUDE, location.longitude)
        )
        finish()
    }

    private fun updateClearButtonVisibility() {
        binding.clearSearchButton.visibility =
            if (binding.locationSearchEdit.text.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        const val EXTRA_INITIAL_QUERY = "extra_initial_query"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_SECONDARY_ADDRESS = "extra_secondary_address"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        private const val TAG = "LocationSearchActivity"
    }
}
