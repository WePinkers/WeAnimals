package com.example.weanimals.user.lostandfound.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.weanimals.R
import com.example.weanimals.databinding.FragmentLostStep2Binding

class LostStep2Fragment : Fragment() {

    private var _binding: FragmentLostStep2Binding? = null
    private val binding get() = _binding!!
    private val viewModel: LostAnimalViewModel by lazy {
        ViewModelProvider(requireActivity())[LostAnimalViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLostStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupIdentificationChips()
        setupBehaviorChips()

        binding.btnBackStep2.setOnClickListener {
            viewModel.previousStep()
        }

        binding.btnContinueStep2.setOnClickListener {
            val marks = binding.inputDistinctiveMarks.text.toString().trim()
            if (marks.isNotBlank()) {
                viewModel.formData.distinctiveMarks = marks
            }
            val secret = binding.inputSecretDetail.text.toString().trim()
            if (secret.isNotBlank()) {
                viewModel.formData.secretDetail = secret
            }
            viewModel.nextStep()
        }
    }

    private fun setupIdentificationChips() {
        val pineBg = R.drawable.bg_chip_selected_pine
        val unselectedBg = R.drawable.bg_chip_unselected
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inkColor = ContextCompat.getColor(requireContext(), R.color.ink)

        fun toggleChip(chipName: String, textView: android.widget.TextView) {
            if (viewModel.formData.identification.contains(chipName)) {
                viewModel.formData.identification.remove(chipName)
                textView.setBackgroundResource(unselectedBg)
                textView.setTextColor(inkColor)
            } else {
                viewModel.formData.identification.add(chipName)
                textView.setBackgroundResource(pineBg)
                textView.setTextColor(whiteColor)
            }
        }

        binding.chipIdCollar.setOnClickListener { toggleChip("Coleira", binding.chipIdCollar) }
        binding.chipIdTag.setOnClickListener { toggleChip("Plaquinha", binding.chipIdTag) }
        binding.chipIdMicrochip.setOnClickListener { toggleChip("Microchip", binding.chipIdMicrochip) }
        binding.chipIdNone.setOnClickListener {
            viewModel.formData.identification.clear()
            viewModel.formData.identification.add("Nenhuma")
            binding.chipIdCollar.setBackgroundResource(unselectedBg)
            binding.chipIdCollar.setTextColor(inkColor)
            binding.chipIdTag.setBackgroundResource(unselectedBg)
            binding.chipIdTag.setTextColor(inkColor)
            binding.chipIdMicrochip.setBackgroundResource(unselectedBg)
            binding.chipIdMicrochip.setTextColor(inkColor)
            binding.chipIdNone.setBackgroundResource(pineBg)
            binding.chipIdNone.setTextColor(whiteColor)
        }
    }

    private fun setupBehaviorChips() {
        val pineBg = R.drawable.bg_chip_selected_pine
        val unselectedBg = R.drawable.bg_chip_unselected
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inkColor = ContextCompat.getColor(requireContext(), R.color.ink)

        binding.chipBehaviorDocile.setOnClickListener {
            viewModel.formData.behavior = "Dócil com estranhos"
            binding.chipBehaviorDocile.setBackgroundResource(pineBg)
            binding.chipBehaviorDocile.setTextColor(whiteColor)
            binding.chipBehaviorTimid.setBackgroundResource(unselectedBg)
            binding.chipBehaviorTimid.setTextColor(inkColor)
            binding.chipBehaviorReactive.setBackgroundResource(unselectedBg)
            binding.chipBehaviorReactive.setTextColor(inkColor)
        }

        binding.chipBehaviorTimid.setOnClickListener {
            viewModel.formData.behavior = "Arisco ou medroso"
            binding.chipBehaviorDocile.setBackgroundResource(unselectedBg)
            binding.chipBehaviorDocile.setTextColor(inkColor)
            binding.chipBehaviorTimid.setBackgroundResource(pineBg)
            binding.chipBehaviorTimid.setTextColor(whiteColor)
            binding.chipBehaviorReactive.setBackgroundResource(unselectedBg)
            binding.chipBehaviorReactive.setTextColor(inkColor)
        }

        binding.chipBehaviorReactive.setOnClickListener {
            viewModel.formData.behavior = "Pode reagir mal a estranhos"
            binding.chipBehaviorDocile.setBackgroundResource(unselectedBg)
            binding.chipBehaviorDocile.setTextColor(inkColor)
            binding.chipBehaviorTimid.setBackgroundResource(unselectedBg)
            binding.chipBehaviorTimid.setTextColor(inkColor)
            binding.chipBehaviorReactive.setBackgroundResource(pineBg)
            binding.chipBehaviorReactive.setTextColor(whiteColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
