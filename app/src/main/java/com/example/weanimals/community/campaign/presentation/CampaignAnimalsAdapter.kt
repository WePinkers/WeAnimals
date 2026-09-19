package com.example.weanimals.community.campaign.presentation

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.databinding.ItemCampaignAnimalBinding
import com.example.weanimals.community.campaign.domain.CommunityCampaignAnimal

class CampaignAnimalsAdapter : RecyclerView.Adapter<CampaignAnimalsAdapter.AnimalViewHolder>() {
    private var animals = emptyList<CommunityCampaignAnimal>()

    fun submitList(items: List<CommunityCampaignAnimal>) {
        animals = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder =
        AnimalViewHolder(
            ItemCampaignAnimalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
        holder.bind(animals[position])
    }

    override fun getItemCount(): Int = animals.size

    class AnimalViewHolder(
        private val binding: ItemCampaignAnimalBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(animal: CommunityCampaignAnimal) = with(binding) {
            val bitmap = animal.photoData?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            animalPhoto.setImageBitmap(bitmap)
            animalPhoto.isVisible = bitmap != null
            animalPlaceholder.isVisible = bitmap == null
            animalName.text = animal.name
            animalBreedBadge.text = animal.breed
            animalBreedBadge.isVisible = animal.breed.isNotBlank()
            animalSpeciesBadge.text = animal.ageText
            animalSpeciesBadge.isVisible = animal.ageText.isNotBlank()
            root.contentDescription = root.context.getString(
                com.example.weanimals.R.string.community_campaign_animal_accessibility,
                animal.name
            )
        }
    }
}
