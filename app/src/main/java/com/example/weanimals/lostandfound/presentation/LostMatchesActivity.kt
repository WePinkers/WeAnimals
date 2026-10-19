package com.example.weanimals.lostandfound.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityLostMatchesBinding

class LostMatchesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLostMatchesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityLostMatchesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnNotSame1.setOnClickListener {
            Toast.makeText(this, "Match removido da lista", Toast.LENGTH_SHORT).show()
        }

        binding.btnMatch1Details.setOnClickListener {
            startActivity(Intent(this, FoundAnimalPublicDetailActivity::class.java))
        }

        binding.btnNotSame2.setOnClickListener {
            Toast.makeText(this, "Match removido da lista", Toast.LENGTH_SHORT).show()
        }

        binding.btnMatch2Details.setOnClickListener {
            startActivity(Intent(this, FoundAnimalPublicDetailActivity::class.java))
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
