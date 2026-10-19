package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityVerificationSentBinding

class VerificationSentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationSentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityVerificationSentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDataAndInteractions()
    }

    private fun setupDataAndInteractions() {
        val ownerName = intent.getStringExtra("extra_owner_name") ?: "Pipoca"
        val secretMark = intent.getStringExtra("extra_secret_mark") ?: "mancha em formato de coração"

        binding.txtSummaryName.text = "✓  Nome: $ownerName"
        binding.txtSummaryMark.text = "✓  Marca: $secretMark"

        binding.btnBackToAds.setOnClickListener {
            val intent = Intent(this, LostAndFoundActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            finish()
        }

        binding.btnTestReviewScreen.setOnClickListener {
            val intent = Intent(this, OwnerRequestReviewActivity::class.java).apply {
                putExtra("extra_owner_name", ownerName)
                putExtra("extra_secret_mark", secretMark)
            }
            startActivity(intent)
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
