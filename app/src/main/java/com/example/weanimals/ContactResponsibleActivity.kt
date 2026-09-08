package com.example.weanimals

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weanimals.databinding.ActivityContactResponsibleBinding

class ContactResponsibleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactResponsibleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactResponsibleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnChat.setOnClickListener {
            Toast.makeText(this, "Abrindo chat com o responsável...", Toast.LENGTH_SHORT).show()
        }
    }
}
