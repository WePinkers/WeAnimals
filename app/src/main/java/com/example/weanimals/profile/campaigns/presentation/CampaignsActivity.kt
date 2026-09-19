package com.example.weanimals.profile.campaigns.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weanimals.R
import com.example.weanimals.community.campaign.presentation.CampaignDetailActivity
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.community.feed.repository.CommunityRepositoryFactory
import com.example.weanimals.community.feed.presentation.CommunityFeedAdapter
import com.example.weanimals.databinding.ActivityCampaignsBinding
import kotlinx.coroutines.launch

class CampaignsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCampaignsBinding
    private val feedRepository by lazy { CommunityRepositoryFactory.create() }
    private val campaignsAdapter = CommunityFeedAdapter(
        onCampaignClick = { campaign ->
            startActivity(CampaignDetailActivity.newIntent(this, campaign))
        },
        onCampaignActionClick = {},
        onPostCommentClick = {},
        onPostLikeClick = {}
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCampaignsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.screenHeader.headerTitle.setText(R.string.title_campaigns)
        binding.screenHeader.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.campaignsList.layoutManager = LinearLayoutManager(this)
        binding.campaignsList.adapter = campaignsAdapter
    }

    override fun onStart() {
        super.onStart()
        loadConfirmedCampaigns()
    }

    private fun loadConfirmedCampaigns() {
        binding.campaignsProgress.isVisible = true
        binding.campaignsList.isVisible = false
        binding.campaignsEmpty.isVisible = false
        lifecycleScope.launch {
            feedRepository.getFeed().fold(
                onSuccess = { items ->
                    val confirmedCampaigns = items
                        .filterIsInstance<CommunityFeedItem.Campaign>()
                        .filter { it.participatingByCurrentUser }
                    campaignsAdapter.submitList(confirmedCampaigns)
                    binding.campaignsProgress.isVisible = false
                    binding.campaignsList.isVisible = confirmedCampaigns.isNotEmpty()
                    binding.campaignsEmpty.isVisible = confirmedCampaigns.isEmpty()
                },
                onFailure = {
                    campaignsAdapter.submitList(emptyList())
                    binding.campaignsProgress.isVisible = false
                    binding.campaignsList.isVisible = false
                    binding.campaignsEmpty.isVisible = true
                    binding.campaignsEmpty.setText(R.string.profile_campaigns_error)
                }
            )
        }
    }
}
