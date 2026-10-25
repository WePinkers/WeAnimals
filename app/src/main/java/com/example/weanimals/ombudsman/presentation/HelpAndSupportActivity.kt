package com.example.weanimals.ombudsman.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
            Toast.makeText(this, "Exibindo Perguntas Frequentes", Toast.LENGTH_SHORT).show()
        }

        binding.cardContact.setOnClickListener {
            Toast.makeText(this, "Fale Conosco: suporte@weanimals.com.br", Toast.LENGTH_SHORT).show()
        }

        binding.cardOmbudsman.setOnClickListener {
            startActivity(Intent(this, OmbudsmanActivity::class.java))
        }

        binding.cardAccountIssues.setOnClickListener {
            Toast.makeText(this, "Acessando suporte de conta", Toast.LENGTH_SHORT).show()
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
