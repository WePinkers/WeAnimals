package com.example.weanimals.user.ombudsman.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityNewManifestationBinding

class NewManifestationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewManifestationBinding
    private lateinit var viewModel: ManifestationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityNewManifestationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.screenHeader.headerTitle.text = "Nova manifestação"

        viewModel = ViewModelProvider(this)[ManifestationViewModel::class.java]

        setupViewPager()
        observeViewModel()
        setupHeaderInteractions()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = ManifestationPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false
    }

    private fun observeViewModel() {
        viewModel.currentStep.observe(this) { step ->
            binding.viewPager.setCurrentItem(step, true)
            updateStepIndicator(step)
        }
    }

    private fun updateStepIndicator(step: Int) {
        val activeBg = R.drawable.bg_step_segment_active
        val inactiveBg = R.drawable.bg_step_segment_inactive

        binding.stepIndicatorLabel.text = "ETAPA ${step + 1} DE 4"

        binding.segBar1.setBackgroundResource(if (step >= 0) activeBg else inactiveBg)
        binding.segBar2.setBackgroundResource(if (step >= 1) activeBg else inactiveBg)
        binding.segBar3.setBackgroundResource(if (step >= 2) activeBg else inactiveBg)
        binding.segBar4.setBackgroundResource(if (step >= 3) activeBg else inactiveBg)
    }

    private fun setupHeaderInteractions() {
        binding.screenHeader.backButton.setOnClickListener {
            val currentStep = viewModel.currentStep.value ?: 0
            if (currentStep > 0) {
                viewModel.previousStep()
            } else {
                finish()
            }
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private class ManifestationPagerAdapter(activity: FragmentActivity) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OmbudsmanStep1Fragment()
                1 -> OmbudsmanStep2Fragment()
                2 -> OmbudsmanStep3Fragment()
                else -> OmbudsmanStep4Fragment()
            }
        }
    }
}
