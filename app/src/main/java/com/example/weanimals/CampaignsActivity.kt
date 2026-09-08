package com.example.weanimals

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityCampaignsBinding

class CampaignsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCampaignsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCampaignsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        setupCampaigns()
    }

    private fun setupCampaigns() {
        binding.campaign1.apply {
            campaignTitle.text = getString(R.string.campaign_adoption_title)
            campaignDesc.text = "Participação confirmada para 12/09"
            campaignAddress.text = getString(R.string.campaign_adoption_address)
            campaignContact.text = getString(R.string.campaign_adoption_contact)
        }
        
        binding.campaign2.apply {
            campaignTitle.text = getString(R.string.campaign_items_title)
            campaignDesc.text = "Coleta ativa em pontos parceiros"
            campaignAddress.text = getString(R.string.campaign_items_address)
            campaignContact.text = getString(R.string.campaign_items_contact)
        }
    }
}
