package com.example.weanimals.ombudsman.presentation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.weanimals.databinding.FragmentOmbudsmanStep4Binding
import com.example.weanimals.ombudsman.repository.OmbudsmanRepository

class OmbudsmanStep4Fragment : Fragment() {

    private var _binding: FragmentOmbudsmanStep4Binding? = null
    private val binding get() = _binding!!
    private val viewModel: ManifestationViewModel by lazy {
        ViewModelProvider(requireActivity())[ManifestationViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOmbudsmanStep4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        renderReviewSummary()

        binding.btnEditPreviousStep.setOnClickListener {
            viewModel.previousStep()
        }

        binding.btnSubmitManifestation.setOnClickListener {
            val form = viewModel.formData
            val protocol = OmbudsmanRepository.addProtocol(
                type = form.type,
                subject = form.subject.ifBlank { "Assunto geral" },
                description = form.description.ifBlank { "Sem descrição." },
                isAnonymous = form.isAnonymous,
                attachmentsCount = if (form.attachedFileName != null) 1 else 0
            )

            val intent = Intent(requireContext(), ManifestationConfirmedActivity::class.java).apply {
                putExtra("extra_protocol_number", protocol.protocolNumber)
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun renderReviewSummary() {
        val form = viewModel.formData
        binding.revTypeBadge.text = form.type.displayName
        binding.revSubject.text = form.subject.ifBlank { "Geral" }
        binding.revDescription.text = form.description
        binding.revIdentification.text = if (form.isAnonymous) "Manifestação anônima" else "Manifestação identificada"
        binding.revAttachments.text = if (form.attachedFileName != null) "1 arquivo (${form.attachedFileName})" else "Nenhum arquivo"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
