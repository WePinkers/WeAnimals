package com.example.weanimals.ombudsman.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityHelpAndSupportBinding

class HelpAndSupportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpAndSupportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityHelpAndSupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.cardFaq.setOnClickListener {
            startActivity(Intent(this, FaqActivity::class.java))
        }

        binding.cardOmbudsman.setOnClickListener {
            startActivity(Intent(this, OmbudsmanActivity::class.java))
        }

        binding.cardAccountIssues.setOnClickListener {
            startActivity(Intent(this, AccountIssuesActivity::class.java))
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
