package com.example.weanimals.user.lostandfound.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityAdReportSubmittedBinding

class AdReportSubmittedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdReportSubmittedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdReportSubmittedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.screenHeader.headerTitle.text = "Denúncia enviada"
        binding.screenHeader.backButton.setOnClickListener { finish() }
        binding.btnBackToAd.setOnClickListener { finish() }
    }
}
