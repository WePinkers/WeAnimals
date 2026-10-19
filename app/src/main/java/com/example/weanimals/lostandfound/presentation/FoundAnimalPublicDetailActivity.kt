package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityFoundAnimalPublicDetailBinding

class FoundAnimalPublicDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoundAnimalPublicDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityFoundAnimalPublicDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnBackFooter.setOnClickListener { finish() }

        binding.btnReportAd.setOnClickListener {
            Toast.makeText(this, "Anúncio denunciado para análise.", Toast.LENGTH_SHORT).show()
        }

        binding.btnClaimPet.setOnClickListener {
            startActivity(Intent(this, OwnerVerificationActivity::class.java))
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
