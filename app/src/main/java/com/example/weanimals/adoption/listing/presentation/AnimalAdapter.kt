package com.example.weanimals.adoption.listing.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import com.example.weanimals.R
import com.example.weanimals.adoption.listing.domain.Animal
import com.example.weanimals.databinding.ItemAnimalBinding
import java.text.NumberFormat
import java.util.Locale

class AnimalAdapter(
    private val onPetClicked: (String) -> Unit
) : ListAdapter<Animal, AnimalAdapter.AnimalViewHolder>(DiffCallback) {
    // Includes submissions still being diffed, so fast consecutive pages cannot lose rows.
    private var submittedAnimals = emptyList<Animal>()

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    fun showAnimals(animals: List<Animal>, onCommitted: () -> Unit) {
        submittedAnimals = animals.toList()
        submitList(submittedAnimals, onCommitted)
    }

    fun appendAnimals(animals: List<Animal>, onCommitted: () -> Unit) {
        submittedAnimals = (submittedAnimals + animals).distinctBy { it.id }
        submitList(submittedAnimals, onCommitted)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AnimalViewHolder(
        ItemAnimalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: AnimalViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    inner class AnimalViewHolder(private val binding: ItemAnimalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.clipToOutline = true
        }

        private val distanceFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }

        fun bind(animal: Animal) = with(binding) {
            val context = root.context
            animalName.text = animal.name
            animalTraits.text = animal.characteristics.joinToString(" · ")
            animalTraits.isVisible = animal.characteristics.isNotEmpty()
            ageSizeBadge.text = listOf(animal.size, animal.ageText)
                .filter { it.isNotBlank() }.joinToString(" · ")
            ageSizeBadge.isVisible = ageSizeBadge.text.isNotEmpty()
            distanceBadge.isVisible = animal.distanceKm != null
            distanceBadge.text = animal.distanceKm?.let { distance ->
                if (distance < 0.1) context.getString(R.string.adoption_distance_near)
                else context.getString(R.string.adoption_distance, distanceFormat.format(distance))
            }
            animalPhoto.contentDescription = context.getString(R.string.adoption_photo_accessibility, animal.name)
            animalPhoto.load(animal.photoUrl) {
                placeholder(R.drawable.adoption_photo_placeholder)
                error(R.drawable.adoption_photo_placeholder)
                fallback(R.drawable.adoption_photo_placeholder)
                crossfade(true)
            }
            root.contentDescription = context.getString(R.string.adoption_meet_accessibility, animal.name)
            root.setOnClickListener { onPetClicked(animal.id) }
            meetButton.contentDescription = context.getString(R.string.adoption_meet_accessibility, animal.name)
            meetButton.setOnClickListener { onPetClicked(animal.id) }
        }

        fun recycle() {
            binding.animalPhoto.dispose()
            binding.root.setOnClickListener(null)
            binding.meetButton.setOnClickListener(null)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Animal>() {
        override fun areItemsTheSame(oldItem: Animal, newItem: Animal) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Animal, newItem: Animal) = oldItem == newItem
    }
}
