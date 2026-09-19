package com.example.weanimals.community.campaign.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.campaign.domain.CommunityCampaignAnimal
import com.example.weanimals.databinding.ActivityCampaignAnimalsBinding
import kotlinx.coroutines.launch

class CampaignAnimalsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCampaignAnimalsBinding
    private val campaignId by lazy { intent.getStringExtra(EXTRA_CAMPAIGN_ID).orEmpty() }
    private val campaignTitle by lazy { intent.getStringExtra(EXTRA_CAMPAIGN_TITLE).orEmpty() }
    private val repository by lazy {
        (application as WeAnimalsApplication).appContainer.createCommunityCampaignRepository()
    }
    private val adapter = CampaignAnimalsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityCampaignAnimalsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.animalsHeader.headerTitle.setText(R.string.community_campaign_animals_title)
        binding.animalsEyebrow.text = campaignTitle
        binding.animalsHeader.backButton.setOnClickListener { finish() }
        binding.animalsRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.animalsRecycler.adapter = adapter
        loadAnimals()
    }

    private fun loadAnimals() {
        binding.animalsState.isVisible = true
        binding.animalsLoading.isVisible = true
        binding.animalsStateMessage.setText(R.string.community_campaign_animals_loading)
        lifecycleScope.launch {
            repository.getConfirmedAnimals(campaignId).fold(
                onSuccess = { animals ->
                    showAnimals(animals)
                },
                onFailure = { showErrorState() }
            )
        }
    }

    private fun showAnimals(animals: List<CommunityCampaignAnimal>) {
        binding.animalsState.isVisible = animals.isEmpty()
        binding.animalsLoading.isVisible = false
        if (animals.isEmpty()) binding.animalsStateMessage.setText(R.string.community_campaign_animals_empty)
        adapter.submitList(animals)
    }

    private fun showErrorState() {
        binding.animalsState.isVisible = true
        binding.animalsLoading.isVisible = false
        binding.animalsStateMessage.setText(R.string.community_campaign_animals_error)
    }

    companion object {
        private const val EXTRA_CAMPAIGN_ID = "campaign_id"
        private const val EXTRA_CAMPAIGN_TITLE = "campaign_title"

        fun newIntent(context: Context, campaignId: String, campaignTitle: String) =
            Intent(context, CampaignAnimalsActivity::class.java).apply {
                putExtra(EXTRA_CAMPAIGN_ID, campaignId)
                putExtra(EXTRA_CAMPAIGN_TITLE, campaignTitle)
            }
    }
}
