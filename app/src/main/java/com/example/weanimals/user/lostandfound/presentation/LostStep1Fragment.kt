package com.example.weanimals.user.lostandfound.presentation

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.weanimals.R
import com.example.weanimals.databinding.FragmentLostStep1Binding

class LostStep1Fragment : Fragment() {

    private var _binding: FragmentLostStep1Binding? = null
    private val binding get() = _binding!!
    private val viewModel: LostAnimalViewModel by lazy {
        ViewModelProvider(requireActivity())[LostAnimalViewModel::class.java]
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.formData.photoUri = uri.toString()
            binding.photoPreview.setImageURI(uri)
            binding.photoPreview.visibility = View.VISIBLE
            binding.iconCamera.visibility = View.GONE
            binding.uploadLabel.text = "Foto adicionada (toque para alterar)"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLostStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.containerUploadPhoto.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        setupChips()

        binding.btnContinueStep1.setOnClickListener {
            val name = binding.inputPetName.text.toString().trim()
            if (name.isBlank()) {
                binding.inputPetName.error = "Digite o nome do animal"
                Toast.makeText(requireContext(), "Por favor, digite o nome do animal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.formData.name = name
            val breed = binding.inputBreed.text.toString().trim()
            if (breed.isNotBlank()) {
                viewModel.formData.breed = breed
            }
            viewModel.nextStep()
        }
    }

    private fun setupChips() {
        val pineBg = R.drawable.bg_chip_selected_pine
        val unselectedBg = R.drawable.bg_chip_unselected
        val whiteColor = ContextCompat.getColor(requireContext(), R.color.white)
        val inkColor = ContextCompat.getColor(requireContext(), R.color.ink)

        // Species Chips
        binding.chipSpeciesDog.setOnClickListener {
            viewModel.formData.species = "dog"
            binding.chipSpeciesDog.setBackgroundResource(pineBg)
            binding.chipSpeciesDog.setTextColor(whiteColor)
            binding.chipSpeciesCat.setBackgroundResource(unselectedBg)
            binding.chipSpeciesCat.setTextColor(inkColor)
            binding.chipSpeciesOther.setBackgroundResource(unselectedBg)
            binding.chipSpeciesOther.setTextColor(inkColor)
        }
        binding.chipSpeciesCat.setOnClickListener {
            viewModel.formData.species = "cat"
            binding.chipSpeciesDog.setBackgroundResource(unselectedBg)
            binding.chipSpeciesDog.setTextColor(inkColor)
            binding.chipSpeciesCat.setBackgroundResource(pineBg)
            binding.chipSpeciesCat.setTextColor(whiteColor)
            binding.chipSpeciesOther.setBackgroundResource(unselectedBg)
            binding.chipSpeciesOther.setTextColor(inkColor)
        }
        binding.chipSpeciesOther.setOnClickListener {
            viewModel.formData.species = "other"
            binding.chipSpeciesDog.setBackgroundResource(unselectedBg)
            binding.chipSpeciesDog.setTextColor(inkColor)
            binding.chipSpeciesCat.setBackgroundResource(unselectedBg)
            binding.chipSpeciesCat.setTextColor(inkColor)
            binding.chipSpeciesOther.setBackgroundResource(pineBg)
            binding.chipSpeciesOther.setTextColor(whiteColor)
        }

        // Size Chips
        binding.chipSizeSmall.setOnClickListener {
            viewModel.formData.size = "small"
            binding.chipSizeSmall.setBackgroundResource(pineBg)
            binding.chipSizeSmall.setTextColor(whiteColor)
            binding.chipSizeMedium.setBackgroundResource(unselectedBg)
            binding.chipSizeMedium.setTextColor(inkColor)
            binding.chipSizeLarge.setBackgroundResource(unselectedBg)
            binding.chipSizeLarge.setTextColor(inkColor)
        }
        binding.chipSizeMedium.setOnClickListener {
            viewModel.formData.size = "medium"
            binding.chipSizeSmall.setBackgroundResource(unselectedBg)
            binding.chipSizeSmall.setTextColor(inkColor)
            binding.chipSizeMedium.setBackgroundResource(pineBg)
            binding.chipSizeMedium.setTextColor(whiteColor)
            binding.chipSizeLarge.setBackgroundResource(unselectedBg)
            binding.chipSizeLarge.setTextColor(inkColor)
        }
        binding.chipSizeLarge.setOnClickListener {
            viewModel.formData.size = "large"
            binding.chipSizeSmall.setBackgroundResource(unselectedBg)
            binding.chipSizeSmall.setTextColor(inkColor)
            binding.chipSizeMedium.setBackgroundResource(unselectedBg)
            binding.chipSizeMedium.setTextColor(inkColor)
            binding.chipSizeLarge.setBackgroundResource(pineBg)
            binding.chipSizeLarge.setTextColor(whiteColor)
        }

        // Age Chips
        binding.chipAgePuppy.setOnClickListener {
            viewModel.formData.age = "puppy"
            binding.chipAgePuppy.setBackgroundResource(pineBg)
            binding.chipAgePuppy.setTextColor(whiteColor)
            binding.chipAgeAdult.setBackgroundResource(unselectedBg)
            binding.chipAgeAdult.setTextColor(inkColor)
            binding.chipAgeSenior.setBackgroundResource(unselectedBg)
            binding.chipAgeSenior.setTextColor(inkColor)
        }
        binding.chipAgeAdult.setOnClickListener {
            viewModel.formData.age = "adult"
            binding.chipAgePuppy.setBackgroundResource(unselectedBg)
            binding.chipAgePuppy.setTextColor(inkColor)
            binding.chipAgeAdult.setBackgroundResource(pineBg)
            binding.chipAgeAdult.setTextColor(whiteColor)
            binding.chipAgeSenior.setBackgroundResource(unselectedBg)
            binding.chipAgeSenior.setTextColor(inkColor)
        }
        binding.chipAgeSenior.setOnClickListener {
            viewModel.formData.age = "senior"
            binding.chipAgePuppy.setBackgroundResource(unselectedBg)
            binding.chipAgePuppy.setTextColor(inkColor)
            binding.chipAgeAdult.setBackgroundResource(unselectedBg)
            binding.chipAgeAdult.setTextColor(inkColor)
            binding.chipAgeSenior.setBackgroundResource(pineBg)
            binding.chipAgeSenior.setTextColor(whiteColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
