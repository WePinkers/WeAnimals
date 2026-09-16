package com.example.weanimals.profile.campaigns.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityCampaignsBinding

class CampaignsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCampaignsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.screenHeader.headerTitle.setText(R.string.title_campaigns)
        binding.screenHeader.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}
