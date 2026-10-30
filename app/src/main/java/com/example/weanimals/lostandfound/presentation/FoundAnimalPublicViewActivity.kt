package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityFoundAnimalPublicViewBinding
import com.example.weanimals.user.reporting.tracking.presentation.TrackingActivity

class FoundAnimalPublicViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoundAnimalPublicViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityFoundAnimalPublicViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnReportAd.setOnClickListener {
            Toast.makeText(this, "Anúncio denunciado para análise.", Toast.LENGTH_SHORT).show()
        }

        binding.btnBackFooter.setOnClickListener { finish() }

        binding.btnClaimPet.setOnClickListener {
            val reportId = intent.getStringExtra("extra_report_id") ?: "report_4487"
            startActivity(TrackingActivity.newIntent(this, reportId))
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
