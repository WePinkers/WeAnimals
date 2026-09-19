package com.example.weanimals.profile.overview.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.weanimals.R
import com.example.weanimals.WeAnimalsApplication
import com.example.weanimals.community.feed.domain.CommunityFeedItem
import com.example.weanimals.community.feed.repository.CommunityRepositoryFactory
import com.example.weanimals.core.navigation.MainNavigation
import com.example.weanimals.databinding.ActivityProfileBinding
import com.example.weanimals.profile.campaigns.presentation.CampaignsActivity
import com.example.weanimals.profile.favorites.presentation.FavoritesActivity
import com.example.weanimals.profile.overview.presenter.ProfileContract
import com.example.weanimals.profile.reports.presentation.MyReportsActivity
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity(), ProfileContract.View {
    private lateinit var binding: ActivityProfileBinding
    private val presenter by lazy {
        (application as WeAnimalsApplication).appContainer.createProfilePresenter()
    }
    private val communityRepository by lazy { CommunityRepositoryFactory.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MainNavigation.bind(this, binding.mainNavigation, MainNavigation.Destination.PROFILE)
        binding.profileContent.itemMyReports.root.setOnClickListener {
            startActivity(Intent(this, MyReportsActivity::class.java))
        }
        binding.profileContent.itemCampaigns.root.setOnClickListener {
            startActivity(Intent(this, CampaignsActivity::class.java))
        }
        binding.profileContent.itemFavorites.root.setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.attachView(this)
        presenter.start()
        loadCampaignSummary()
    }

    override fun onStop() {
        presenter.detachView()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.destroy()
        super.onDestroy()
    }

    override fun showIdentity(name: String?) {
        val displayName = name ?: getString(R.string.profile_default_name)
        binding.profileContent.userName.text = displayName
        binding.profileContent.avatarText.text =
            name?.firstOrNull()?.uppercase() ?: getString(R.string.profile_default_avatar)
        binding.profileContent.userInfo.text = getString(R.string.profile_account_info)
    }

    override fun showReportCount(count: Int) {
        binding.profileContent.reportCount.text = count.toString()
    }

    override fun showReportsError() {
        binding.profileContent.reportCount.text = "—"
    }

    private fun loadCampaignSummary() {
        lifecycleScope.launch {
            communityRepository.getFeed().fold(
                onSuccess = { items ->
                    val count = items
                        .filterIsInstance<CommunityFeedItem.Campaign>()
                        .count { it.participatingByCurrentUser }
                    binding.profileContent.campaignCount.text = count.toString()
                    binding.profileContent.itemCampaigns.campaignsDescription.text = if (count == 0) {
                        getString(R.string.profile_activity_campaigns_desc)
                    } else {
                        resources.getQuantityString(
                            R.plurals.profile_campaigns_participation_count,
                            count,
                            count
                        )
                    }
                },
                onFailure = {
                    binding.profileContent.campaignCount.text = "—"
                    binding.profileContent.itemCampaigns.campaignsDescription.setText(
                        R.string.profile_campaigns_count_error
                    )
                }
            )
        }
    }
}
