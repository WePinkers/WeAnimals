package com.example.weanimals

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInteractions()
    }

    private fun setupInteractions() {
        binding.reportNowButton.setOnClickListener {
            Toast.makeText(
                this,
                R.string.report_action_feedback,
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.viewMapButton.setOnClickListener {
            binding.contentScroll.smoothScrollTo(0, binding.mapPreview.mapRoot.top)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
