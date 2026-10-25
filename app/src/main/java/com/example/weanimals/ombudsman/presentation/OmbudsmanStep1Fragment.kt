package com.example.weanimals.ombudsman.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.weanimals.databinding.FragmentOmbudsmanStep1Binding
import com.example.weanimals.ombudsman.domain.ManifestationType

class OmbudsmanStep1Fragment : Fragment() {

    private var _binding: FragmentOmbudsmanStep1Binding? = null
    private val binding get() = _binding!!
    private val viewModel: ManifestationViewModel by lazy {
        ViewModelProvider(requireActivity())[ManifestationViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOmbudsmanStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSelection()

        binding.btnNextStep1.setOnClickListener {
            viewModel.nextStep()
        }
    }

    private fun setupSelection() {
        fun selectType(type: ManifestationType) {
            viewModel.formData.type = type
            binding.radioReclamacao.isChecked = (type == ManifestationType.RECLAMACAO)
            binding.radioDenuncia.isChecked = (type == ManifestationType.DENUNCIA)
            binding.radioSolicitacao.isChecked = (type == ManifestationType.SOLICITACAO)
        }

        binding.cardTypeReclamacao.setOnClickListener { selectType(ManifestationType.RECLAMACAO) }
        binding.cardTypeDenuncia.setOnClickListener { selectType(ManifestationType.DENUNCIA) }
        binding.cardTypeSolicitacao.setOnClickListener { selectType(ManifestationType.SOLICITACAO) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
