package com.example.weanimals.user.adoption.applications.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import com.example.weanimals.R
import com.example.weanimals.core.utils.DataUrlBitmapDecoder
import com.example.weanimals.databinding.ItemMyAdoptionApplicationBinding
import com.example.weanimals.user.adoption.applications.domain.MyAdoptionApplication
import com.example.weanimals.user.adoption.sent.domain.AdoptionApplicationStatus

class MyAdoptionApplicationsAdapter(
    private val onApplicationClicked: (MyAdoptionApplication) -> Unit,
) : ListAdapter<MyAdoptionApplication, MyAdoptionApplicationsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemMyAdoptionApplicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    inner class ViewHolder(
        private val binding: ItemMyAdoptionApplicationBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(application: MyAdoptionApplication) = with(binding) {
            val context = root.context
            val summary = application.summary
            applicationName.text = summary.animalName
            applicationShelter.text = summary.shelterName.ifBlank {
                context.getString(R.string.adoption_detail_shelter_fallback)
            }
            applicationMatch.text = context.getString(
                R.string.adoption_my_application_match,
                summary.matchPercentage,
            )
            applicationStatus.text = context.getString(statusLabel(application.status))
            val photoUrl = summary.photoUrl?.takeIf(String::isNotBlank)
            applicationPhotoPlaceholder.isVisible = true
            applicationPhoto.isVisible = photoUrl != null
            if (photoUrl == null) {
                applicationPhoto.dispose()
                applicationPhoto.setImageDrawable(null)
            } else {
                val dataUrlBitmap = DataUrlBitmapDecoder.decode(photoUrl)
                if (dataUrlBitmap != null) {
                    applicationPhoto.setImageBitmap(dataUrlBitmap)
                    applicationPhotoPlaceholder.isVisible = false
                } else {
                    applicationPhoto.load(photoUrl) {
                        placeholder(R.drawable.adoption_photo_placeholder)
                        error(R.drawable.adoption_photo_placeholder)
                        listener(
                            onSuccess = { _, _ -> applicationPhotoPlaceholder.isVisible = false },
                            onError = { _, _ -> applicationPhotoPlaceholder.isVisible = true },
                        )
                    }
                }
            }
            root.contentDescription = context.getString(
                R.string.adoption_my_application_accessibility,
                summary.animalName,
            )
            root.setOnClickListener { onApplicationClicked(application) }
        }

        fun recycle() {
            binding.applicationPhoto.dispose()
            binding.root.setOnClickListener(null)
        }
    }

    private fun statusLabel(status: AdoptionApplicationStatus): Int = when (status) {
        AdoptionApplicationStatus.PENDING -> R.string.adoption_my_application_status_pending
        AdoptionApplicationStatus.REVIEWING -> R.string.adoption_my_application_status_reviewing
        AdoptionApplicationStatus.APPROVED -> R.string.adoption_my_application_status_approved
        AdoptionApplicationStatus.REJECTED -> R.string.adoption_my_application_status_rejected
        AdoptionApplicationStatus.VISIT_SCHEDULED -> R.string.adoption_my_application_status_visit
        AdoptionApplicationStatus.ADOPTED -> R.string.adoption_my_application_status_adopted
        AdoptionApplicationStatus.CANCELLED -> R.string.adoption_my_application_status_cancelled
    }

    private object DiffCallback : DiffUtil.ItemCallback<MyAdoptionApplication>() {
        override fun areItemsTheSame(
            oldItem: MyAdoptionApplication,
            newItem: MyAdoptionApplication,
        ) = oldItem.summary.animalId == newItem.summary.animalId

        override fun areContentsTheSame(
            oldItem: MyAdoptionApplication,
            newItem: MyAdoptionApplication,
        ) = oldItem == newItem
    }
}
