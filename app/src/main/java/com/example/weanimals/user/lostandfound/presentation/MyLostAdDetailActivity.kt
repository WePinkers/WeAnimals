package com.example.weanimals.user.lostandfound.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityMyLostAdDetailBinding
import com.example.weanimals.user.lostandfound.presentation.ReportLostPetActivity

class MyLostAdDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyLostAdDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityMyLostAdDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnViewMatches.setOnClickListener {
            startActivity(Intent(this, LostMatchesActivity::class.java))
        }

        binding.btnShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Ajude a encontrar o Pipoca, desaparecido na Vila Marlene! Veja no app WeAnimals.")
            }
            startActivity(Intent.createChooser(shareIntent, "Compartilhar anúncio"))
        }

        binding.btnCopyLink.setOnClickListener {
            Toast.makeText(this, "Link do anúncio copiado!", Toast.LENGTH_SHORT).show()
        }

        binding.btnPoster.setOnClickListener {
            Toast.makeText(this, "Gerando cartaz em PDF...", Toast.LENGTH_SHORT).show()
        }

        binding.btnEditAd.setOnClickListener {
            startActivity(Intent(this, ReportLostPetActivity::class.java))
        }

        binding.btnCloseAd.setOnClickListener {
            Toast.makeText(this, "Anúncio encerrado com sucesso.", Toast.LENGTH_SHORT).show()
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
