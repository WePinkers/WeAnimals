package com.example.weanimals.community.campaign.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
import com.example.weanimals.community.campaign.domain.CampaignDonationInfo
import com.example.weanimals.community.feed.presentation.CommunityActivity
import com.example.weanimals.databinding.ActivityDonationConfirmationBinding
import java.text.NumberFormat
import java.util.Locale

class DonationConfirmationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDonationConfirmationBinding
    private val donationInfo by lazy { CampaignDonationInfo.fromIntent(intent) }
    private val amountCents by lazy { intent.getLongExtra(EXTRA_AMOUNT_CENTS, 0L) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonationConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.confirmationEyebrow.text = getString(
            R.string.community_donation_registered_eyebrow,
            formatAmount(amountCents)
        )
        binding.confirmationCampaign.text = getString(
            R.string.community_donation_registered_campaign,
            donationInfo.campaignTitle.ifBlank { getString(R.string.community_campaign_adoption) }
        )
        binding.confirmationTitle.setText(R.string.community_donation_registered_title)
        binding.confirmationBody.text = getString(
            R.string.community_donation_registered_body,
            donationInfo.organization.ifBlank {
                getString(R.string.community_organization_default)
            }
        )

        binding.viewCampaignButton.setOnClickListener {
            val updatedRaisedCents = donationInfo.donationRaisedCents
                ?.plus(amountCents)
                ?.let { raised ->
                    donationInfo.donationGoalCents?.let { goal -> raised.coerceAtMost(goal) }
                        ?: raised
                }
            startActivity(
                CampaignDetailActivity.newIntent(
                    this,
                    donationInfo.toCampaign().copy(donationRaisedCents = updatedRaisedCents)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
            finish()
        }
        binding.backCommunityButton.setOnClickListener {
            startActivity(
                Intent(this, CommunityActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
            finish()
        }
    }

    private fun formatAmount(cents: Long): String = NumberFormat.getCurrencyInstance(
        Locale("pt", "BR")
    ).format(cents.coerceAtLeast(0L) / 100.0)

    companion object {
        private const val EXTRA_AMOUNT_CENTS = "donation_confirmation_amount_cents"

        fun newIntent(
            context: Context,
            info: CampaignDonationInfo,
            amountCents: Long
        ): Intent = info.toIntent(context, DonationConfirmationActivity::class.java).apply {
            putExtra(EXTRA_AMOUNT_CENTS, amountCents)
        }
    }
}
