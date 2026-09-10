package com.example.weanimals.adoption.detail.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import com.example.weanimals.R
import com.example.weanimals.databinding.ItemAnimalPhotoBinding

class AnimalPhotoAdapter : RecyclerView.Adapter<AnimalPhotoAdapter.PhotoViewHolder>() {

    private var photoUrls = emptyList<String>()
    private var animalName = ""

    val photoCount: Int
        get() = photoUrls.size

    fun submitPhotos(urls: List<String>, name: String) {
        photoUrls = urls.filter(String::isNotBlank).distinct()
        animalName = name
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = photoUrls.size.coerceAtLeast(1)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemAnimalPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(
            photoUrl = photoUrls.getOrNull(position),
            animalName = animalName,
            position = position,
            total = photoUrls.size
        )
    }

    override fun onViewRecycled(holder: PhotoViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    class PhotoViewHolder(
        private val binding: ItemAnimalPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photoUrl: String?, animalName: String, position: Int, total: Int) = with(binding) {
            val hasPhoto = photoUrl != null
            photoImage.contentDescription = if (total > 1) {
                root.context.getString(
                    R.string.adoption_detail_photo_position,
                    position + 1,
                    total,
                    animalName
                )
            } else {
                root.context.getString(R.string.adoption_photo_accessibility, animalName)
            }
            photoImage.isVisible = hasPhoto
            photoPlaceholder.isVisible = !hasPhoto
            if (hasPhoto) {
                photoImage.load(photoUrl) {
                    placeholder(R.drawable.bg_adoption_photo)
                    error(R.drawable.bg_adoption_photo)
                    crossfade(true)
                    listener(
                        onSuccess = { _, _ -> photoPlaceholder.isVisible = false },
                        onError = { _, _ -> photoPlaceholder.isVisible = true }
                    )
                }
            } else {
                photoImage.dispose()
                photoImage.setImageDrawable(null)
            }
        }

        fun recycle() {
            binding.photoImage.dispose()
        }
    }
}
