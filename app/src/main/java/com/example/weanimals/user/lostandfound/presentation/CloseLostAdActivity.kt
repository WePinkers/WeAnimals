package com.example.weanimals.user.lostandfound.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityCloseLostAdBinding

class CloseLostAdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCloseLostAdBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityCloseLostAdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.screenHeader.headerTitle.text = "Encerrar e registrar reencontro"

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.screenHeader.backButton.setOnClickListener { finish() }

        binding.btnCloseAndRegister.setOnClickListener {
            Toast.makeText(this, "Anúncio encerrado! Reencontro registrado com sucesso.", Toast.LENGTH_LONG).show()
            finish()
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
