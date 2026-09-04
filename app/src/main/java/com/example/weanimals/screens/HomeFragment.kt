package com.example.weanimals.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.weanimals.MainActivity
import com.example.weanimals.R
import com.example.weanimals.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.reportNowButton.setOnClickListener {
            Toast.makeText(
                requireContext(),
                R.string.report_action_feedback,
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.viewMapButton.setOnClickListener {
            navigateToMap()
        }

        binding.mapPreview.mapRoot.setOnClickListener {
            navigateToMap()
        }
    }

    private fun navigateToMap() {
        (activity as? MainActivity)?.selectBottomNavItem(R.id.nav_map)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
