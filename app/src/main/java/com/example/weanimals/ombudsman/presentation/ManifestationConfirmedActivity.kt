package com.example.weanimals.ombudsman.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityManifestationConfirmedBinding
import com.example.weanimals.user.home.presentation.HomeActivity

class ManifestationConfirmedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManifestationConfirmedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityManifestationConfirmedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDataAndInteractions()
    }

    private fun setupDataAndInteractions() {
        val protocolNumber = intent.getStringExtra("extra_protocol_number") ?: "WA-2026-000123"
        binding.txtProtocolNumber.text = protocolNumber

        binding.btnCopyProtocol.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Protocolo Ouvidoria", protocolNumber)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Número do protocolo copiado!", Toast.LENGTH_SHORT).show()
        }

        binding.btnViewMyProtocols.setOnClickListener {
            startActivity(Intent(this, MyProtocolsActivity::class.java))
            finish()
        }

        binding.btnBackToHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
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
