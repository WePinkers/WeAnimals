package com.example.weanimals

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.weanimals.databinding.ActivityMainBinding
import com.example.weanimals.screens.AdoptionFragment
import com.example.weanimals.screens.CommunityFragment
import com.example.weanimals.screens.HomeFragment
import com.example.weanimals.screens.MapFragment
import com.example.weanimals.screens.ProfileFragment

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply System WindowInsets safely to avoid cutting off bottom navigation or status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply status bar / notch inset to top of fragment container
            binding.fragmentContainer.updatePadding(
                top = systemBars.top
            )

            // Apply bottom gesture bar inset to bottom navigation view padding
            binding.bottomNavigation.updatePadding(
                bottom = systemBars.bottom
            )

            insets
        }

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_map -> {
                    replaceFragment(MapFragment())
                    true
                }
                R.id.nav_community -> {
                    replaceFragment(CommunityFragment())
                    true
                }
                R.id.nav_adoption -> {
                    replaceFragment(AdoptionFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun selectBottomNavItem(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }
}
