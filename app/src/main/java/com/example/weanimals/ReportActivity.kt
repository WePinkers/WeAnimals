package com.example.weanimals

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.weanimals.databinding.ActivityReportBinding
import com.google.android.material.button.MaterialButton

class ReportActivity : AppCompatActivity() {

    private enum class Urgency(
        @param:androidx.annotation.ColorRes val selectedBackgroundRes: Int,
        @param:androidx.annotation.ColorRes val accentColorRes: Int
    ) {
        LOW(R.color.low_urgency_light, R.color.low_urgency),
        MEDIUM(R.color.medium_urgency_light, R.color.gold),
        HIGH(R.color.soft_red, R.color.red)
    }

    private lateinit var binding: ActivityReportBinding

    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { selectedPhoto: Uri? ->
        selectedPhoto ?: return@registerForActivityResult

        binding.photoPreview.setImageURI(selectedPhoto)
        binding.photoPreview.visibility = View.VISIBLE
        binding.photoIcon.visibility = View.GONE
        binding.photoPrompt.setText(R.string.photo_added)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInteractions()
    }

    private fun setupInteractions() {
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.photoDropzone.setOnClickListener {
            photoPicker.launch("image/*")
        }

        binding.dogButton.setOnClickListener {
            selectAnimalType(binding.dogButton, binding.catButton, binding.otherButton)
        }

        binding.catButton.setOnClickListener {
            selectAnimalType(binding.catButton, binding.dogButton, binding.otherButton)
        }

        binding.otherButton.setOnClickListener {
            selectAnimalType(binding.otherButton, binding.dogButton, binding.catButton)
        }

        binding.urgencyLowButton.setOnClickListener {
            selectUrgency(
                Urgency.LOW,
                binding.urgencyLowButton,
                binding.urgencyMediumButton to Urgency.MEDIUM,
                binding.urgencyHighButton to Urgency.HIGH
            )
        }

        binding.urgencyMediumButton.setOnClickListener {
            selectUrgency(
                Urgency.MEDIUM,
                binding.urgencyMediumButton,
                binding.urgencyLowButton to Urgency.LOW,
                binding.urgencyHighButton to Urgency.HIGH
            )
        }

        binding.urgencyHighButton.setOnClickListener {
            selectUrgency(
                Urgency.HIGH,
                binding.urgencyHighButton,
                binding.urgencyLowButton to Urgency.LOW,
                binding.urgencyMediumButton to Urgency.MEDIUM
            )
        }

        selectUrgency(
            Urgency.HIGH,
            binding.urgencyHighButton,
            binding.urgencyLowButton to Urgency.LOW,
            binding.urgencyMediumButton to Urgency.MEDIUM
        )

        binding.submitButton.setOnClickListener {
            continueToTriage()
        }
    }

    private fun selectAnimalType(
        selected: MaterialButton,
        vararg others: MaterialButton
    ) {
        styleAnimalButton(selected, selected = true)
        others.forEach { button -> styleAnimalButton(button, selected = false) }
    }

    private fun styleAnimalButton(button: MaterialButton, selected: Boolean) {
        val backgroundColor = if (selected) R.color.ink else R.color.surface
        val foregroundColor = if (selected) R.color.white else R.color.muted
        val strokeColor = if (selected) R.color.ink else R.color.border

        button.backgroundTintList = colorStateList(backgroundColor)
        button.setTextColor(ContextCompat.getColor(this, foregroundColor))
        button.strokeColor = colorStateList(strokeColor)
    }

    private fun selectUrgency(
        urgency: Urgency,
        selected: MaterialButton,
        vararg others: Pair<MaterialButton, Urgency>
    ) {
        styleUrgencyButton(selected, urgency, selected = true)
        others.forEach { (button, level) ->
            styleUrgencyButton(button, level, selected = false)
        }
    }

    private fun styleUrgencyButton(
        button: MaterialButton,
        urgency: Urgency,
        selected: Boolean
    ) {
        val backgroundColor = if (selected) urgency.selectedBackgroundRes else R.color.surface
        val textColor = if (selected) urgency.accentColorRes else R.color.muted
        val strokeColor = if (selected) urgency.accentColorRes else R.color.border

        button.backgroundTintList = colorStateList(backgroundColor)
        button.setTextColor(ContextCompat.getColor(this, textColor))
        button.strokeColor = colorStateList(strokeColor)
    }

    private fun continueToTriage() {
        if (binding.descriptionEdit.text?.toString()?.trim().isNullOrEmpty()) {
            binding.descriptionEdit.error = getString(R.string.description_required)
            return
        }

        Toast.makeText(
            this,
            R.string.triage_coming_soon,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun colorStateList(colorRes: Int): ColorStateList {
        return ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))
    }
}
