package com.example.weanimals.lostandfound.presentation

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityOwnerRequestReviewBinding
import com.example.weanimals.user.reporting.chat.presentation.CaseChatActivity

class OwnerRequestReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerRequestReviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityOwnerRequestReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDataAndInteractions()
    }

    private fun setupDataAndInteractions() {
        val ownerName = intent.getStringExtra("extra_owner_name") ?: "Pipoca"
        val secretMark = intent.getStringExtra("extra_secret_mark") ?: "mancha em formato de coração no peito"

        binding.btnBack.setOnClickListener { finish() }

        binding.reviewName.text = "✓  Nome informado: $ownerName"
        binding.reviewMark.text = "✓  Marca informada: $secretMark"

        binding.cardProofPhoto.setOnClickListener {
            Toast.makeText(this, "Exibindo foto de comprovação enviada", Toast.LENGTH_SHORT).show()
        }

        binding.btnReportRequest.setOnClickListener {
            Toast.makeText(this, "Pedido denunciado para análise.", Toast.LENGTH_SHORT).show()
        }

        binding.btnReject.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Recusar solicitação")
                .setMessage("Deseja indicar que as respostas não conferem com o animal encontrado?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar") { _, _ ->
                    Toast.makeText(this, "Solicitação recusada", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .show()
        }

        binding.btnAcceptChat.setOnClickListener {
            Toast.makeText(this, "Verificação aprovada! Abrindo chat...", Toast.LENGTH_SHORT).show()
            val intent = CaseChatActivity.newIntent(this, "case_owner_verified", 4487)
            startActivity(intent)
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
