package com.example.weanimals.community.campaign.presentation

import android.os.Bundle
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
import com.example.weanimals.community.campaign.domain.CampaignDonationInfo
import com.example.weanimals.databinding.ActivityDonationHelpBinding

class DonationHelpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDonationHelpBinding
    private val donationInfo by lazy { CampaignDonationInfo.fromIntent(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonationHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.helpHeader.headerTitle.setText(R.string.community_donation_help_title)
        binding.helpHeader.backButton.setOnClickListener { finish() }
        binding.helpEyebrow.text = donationInfo.campaignTitle.uppercase()
        binding.donationMoneyCard.setOnClickListener {
            startActivity(DonationPixActivity.newIntent(this, donationInfo))
        }
        binding.donationItemsCard.setOnClickListener {
            startActivity(DonationItemsActivity.newIntent(this, donationInfo))
        }
    }

    companion object {
        fun newIntent(context: Context, info: CampaignDonationInfo) =
            info.toIntent(context, DonationHelpActivity::class.java)
    }
}
