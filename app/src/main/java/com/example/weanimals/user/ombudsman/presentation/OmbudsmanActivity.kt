package com.example.weanimals.user.ombudsman.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityOmbudsmanBinding

class OmbudsmanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOmbudsmanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityOmbudsmanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.screenHeader.headerTitle.text = "Ouvidoria"
        binding.screenHeader.backButton.setOnClickListener { finish() }

        binding.cardNewManifestation.setOnClickListener {
            startActivity(Intent(this, NewManifestationActivity::class.java))
        }

        binding.cardMyProtocols.setOnClickListener {
            startActivity(Intent(this, MyProtocolsActivity::class.java))
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
