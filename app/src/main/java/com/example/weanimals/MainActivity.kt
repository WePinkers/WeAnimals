package com.example.weanimals

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        setupNavigation()
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
            binding.homeContent.smoothScrollTo(0, binding.mapPreview.mapRoot.top)
        }
    }

    private fun setupNavigation() {
        binding.navHomeButton.setOnClickListener {
            selectTab(isHome = true)
        }

        binding.navProfileButton.setOnClickListener {
            selectTab(isHome = false)
        }

        // Other buttons could be implemented similarly
        val notImplementedToast = {
            Toast.makeText(this, "Funcionalidade em desenvolvimento", Toast.LENGTH_SHORT).show()
        }
        binding.navMapButton.setOnClickListener { notImplementedToast() }
        binding.navCommunityButton.setOnClickListener { notImplementedToast() }
        binding.navAdoptionButton.setOnClickListener { notImplementedToast() }
    }

    private fun selectTab(isHome: Boolean) {
        // Toggle Visibility
        binding.homeContent.visibility = if (isHome) View.VISIBLE else View.GONE
        binding.profileContent.root.visibility = if (isHome) View.GONE else View.VISIBLE

        // Update Nav Colors
        updateNavItem(binding.navHomeIcon, binding.navHomeText, isHome)
        updateNavItem(binding.navProfileIcon, binding.navProfileText, !isHome)
    }

    private fun updateNavItem(icon: ImageView, text: TextView, isSelected: Boolean) {
        val color = if (isSelected) R.color.ink else R.color.muted
        val resolvedColor = ContextCompat.getColor(this, color)
        icon.setColorFilter(resolvedColor)
        text.setTextColor(resolvedColor)
        text.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
