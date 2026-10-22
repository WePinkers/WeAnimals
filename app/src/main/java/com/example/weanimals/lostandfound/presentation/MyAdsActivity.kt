package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityMyAdsBinding

class MyAdsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyAdsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityMyAdsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.cardMyAdItem.setOnClickListener {
            startActivity(Intent(this, MyLostAdDetailActivity::class.java))
        }

        binding.btnMyAdMatches.setOnClickListener {
            startActivity(Intent(this, LostMatchesActivity::class.java))
        }

        binding.btnMyAdEdit.setOnClickListener {
            startActivity(Intent(this, MyLostAdDetailActivity::class.java))
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
