package com.example.weanimals.core.navigation

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.example.weanimals.R
import com.example.weanimals.adoption.listing.presentation.AdoptionActivity
import com.example.weanimals.databinding.ViewMainNavigationBinding
import com.example.weanimals.home.presentation.HomeActivity

object MainNavigation {
    enum class Destination { HOME, ADOPTION }

    fun bind(
        activity: Activity,
        binding: ViewMainNavigationBinding,
        selected: Destination? = null
    ) {
        val active = ContextCompat.getColor(activity, R.color.adoption_primary)
        val muted = ContextCompat.getColor(activity, R.color.muted)
        ImageViewCompat.setImageTintList(
            binding.navHomeIcon, ColorStateList.valueOf(if (selected == Destination.HOME) active else muted)
        )
        binding.navHomeLabel.setTextColor(if (selected == Destination.HOME) active else muted)
        ImageViewCompat.setImageTintList(
            binding.navAdoptionIcon, ColorStateList.valueOf(if (selected == Destination.ADOPTION) active else muted)
        )
        binding.navAdoptionLabel.setTextColor(if (selected == Destination.ADOPTION) active else muted)
        binding.navHome.isSelected = selected == Destination.HOME
        binding.navAdoption.isSelected = selected == Destination.ADOPTION
        // The navigation background consumes taps for destinations that are not
        // implemented yet, preventing them from reaching content behind the bar.
        binding.root.setOnClickListener { }
        binding.navHome.setOnClickListener {
            if (selected != Destination.HOME) activity.startActivity(
                Intent(activity, HomeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
        binding.navAdoption.setOnClickListener {
            if (selected != Destination.ADOPTION) activity.startActivity(
                Intent(activity, AdoptionActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }
}
