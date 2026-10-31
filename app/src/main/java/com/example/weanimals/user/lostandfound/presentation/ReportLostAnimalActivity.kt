package com.example.weanimals.user.lostandfound.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.weanimals.R
import com.example.weanimals.databinding.ActivityReportLostAnimalBinding

class ReportLostAnimalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportLostAnimalBinding
    private lateinit var viewModel: LostAnimalViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityReportLostAnimalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.screenHeader.headerTitle.text = "Perdi meu animal"

        viewModel = ViewModelProvider(this)[LostAnimalViewModel::class.java]

        setupViewPager()
        observeViewModel()
        setupHeaderInteractions()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = LostAnimalPagerAdapter(this)
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

        when (step) {
            0 -> {
                binding.stepBar1.setBackgroundResource(activeBg)
                binding.stepBar2.setBackgroundResource(inactiveBg)
                binding.stepBar3.setBackgroundResource(inactiveBg)
            }
            1 -> {
                binding.stepBar1.setBackgroundResource(activeBg)
                binding.stepBar2.setBackgroundResource(activeBg)
                binding.stepBar3.setBackgroundResource(inactiveBg)
            }
            2 -> {
                binding.stepBar1.setBackgroundResource(activeBg)
                binding.stepBar2.setBackgroundResource(activeBg)
                binding.stepBar3.setBackgroundResource(activeBg)
            }
        }
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

    private inner class LostAnimalPagerAdapter(activity: FragmentActivity) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> LostStep1Fragment()
                1 -> LostStep2Fragment()
                else -> LostStep3Fragment()
            }
        }
    }
}
