package com.example.weanimals.ombudsman.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.weanimals.databinding.FragmentOmbudsmanStep3Binding

class OmbudsmanStep3Fragment : Fragment() {

    private var _binding: FragmentOmbudsmanStep3Binding? = null
    private val binding get() = _binding!!
    private val viewModel: ManifestationViewModel by lazy {
        ViewModelProvider(requireActivity())[ManifestationViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOmbudsmanStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSelection()

        binding.btnNextStep3.setOnClickListener {
            viewModel.nextStep()
        }
    }

    private fun setupSelection() {
        fun selectAnonymous(isAnonymous: Boolean) {
            viewModel.formData.isAnonymous = isAnonymous
            binding.radioIdentified.isChecked = !isAnonymous
            binding.radioAnonymous.isChecked = isAnonymous
        }

        binding.cardIdIdentified.setOnClickListener { selectAnonymous(false) }
        binding.cardIdAnonymous.setOnClickListener { selectAnonymous(true) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
