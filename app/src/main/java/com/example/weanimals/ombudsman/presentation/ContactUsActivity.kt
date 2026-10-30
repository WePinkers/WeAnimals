package com.example.weanimals.ombudsman.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityContactUsBinding

class ContactUsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactUsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityContactUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.cardSupportEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:suporte@weanimals.org")
                putExtra(Intent.EXTRA_SUBJECT, "Suporte WeAnimals")
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "E-mail: suporte@weanimals.org", Toast.LENGTH_LONG).show()
            }
        }

        binding.cardLiveChat.setOnClickListener {
            Toast.makeText(this, "Horário de atendimento: Seg a Sex, 9h às 18h", Toast.LENGTH_SHORT).show()
        }

        binding.btnSendQuickMessage.setOnClickListener {
            val msg = binding.inputQuickMessage.text.toString().trim()
            if (msg.isBlank()) {
                binding.inputQuickMessage.error = "Escreva sua mensagem"
                return@setOnClickListener
            }
            Toast.makeText(this, "Mensagem enviada com sucesso! Responderemos em breve.", Toast.LENGTH_LONG).show()
            binding.inputQuickMessage.setText("")
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
