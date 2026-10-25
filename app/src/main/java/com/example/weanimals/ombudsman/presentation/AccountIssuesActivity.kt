package com.example.weanimals.ombudsman.presentation

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityAccountIssuesBinding
import com.example.weanimals.profile.overview.presentation.ProfileActivity

class AccountIssuesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountIssuesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityAccountIssuesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.cardResetPassword.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Redefinição de Senha")
                .setMessage("Deseja receber as instruções de redefinição no seu e-mail de cadastro?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Enviar e-mail") { _, _ ->
                    Toast.makeText(this, "E-mail de redefinição enviado com sucesso!", Toast.LENGTH_LONG).show()
                }
                .show()
        }

        binding.cardUpdateData.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.cardVerifiedSeal.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Selo Comunitário Verificado")
                .setMessage("Para obter o selo verificado no WeAnimals:\n\n1. Mantenha seu cadastro completo com e-mail confirmado.\n2. Participe de ações comunitárias ou campanhas.\n3. Mantenha histórico positivo de relatórios válidos.")
                .setPositiveButton("Entendido", null)
                .show()
        }

        binding.cardDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Exclusão de Conta (LGPD)")
                .setMessage("Conforme os termos da LGPD, a exclusão da conta apaga seus dados cadastrais permanentemente. Deseja prosseguir com a solicitação?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Solicitar Exclusão") { _, _ ->
                    Toast.makeText(this, "Solicitação de exclusão recebida. Nossa equipe entrará em contato em até 48h.", Toast.LENGTH_LONG).show()
                }
                .show()
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
