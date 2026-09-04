package com.example.weanimals.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.R
import com.example.weanimals.databinding.FragmentMapBinding
import com.example.weanimals.databinding.ItemOccurrenceBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior

data class OccurrenceItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val urgencyLevel: String,
    val badgeText: String,
    val badgeBgColorRes: Int,
    val badgeTextColorRes: Int,
    val indicatorColorRes: Int,
    val pinId: Int
)

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding: FragmentMapBinding get() = _binding!!

    private lateinit var adapter: OccurrenceAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private val allOccurrences = listOf(
        OccurrenceItem(
            id = "1",
            title = "Filhote — Rua Aimberê",
            subtitle = "420 m · protocolo #4471",
            urgencyLevel = "Emergência",
            badgeText = "Novo",
            badgeBgColorRes = R.drawable.bg_new_badge,
            badgeTextColorRes = R.color.alert600,
            indicatorColorRes = R.color.alert600,
            pinId = R.id.pin_1
        ),
        OccurrenceItem(
            id = "2",
            title = "Cão idoso — Vila Marlene",
            subtitle = "1,1 km · equipe a caminho",
            urgencyLevel = "Urgente",
            badgeText = "Em curso",
            badgeBgColorRes = R.drawable.bg_progress_badge,
            badgeTextColorRes = R.color.brass500,
            indicatorColorRes = R.color.brass500,
            pinId = R.id.pin_2
        ),
        OccurrenceItem(
            id = "3",
            title = "Gato preso em terreno",
            subtitle = "850 m · protocolo #4473",
            urgencyLevel = "Muito urgente",
            badgeText = "Novo",
            badgeBgColorRes = R.drawable.bg_new_badge,
            badgeTextColorRes = R.color.alert600,
            indicatorColorRes = R.color.clay600,
            pinId = R.id.pin_3
        ),
        OccurrenceItem(
            id = "4",
            title = "Cão Comunitário",
            subtitle = "1,8 km · em acompanhamento",
            urgencyLevel = "Pouco urgente",
            badgeText = "Em curso",
            badgeBgColorRes = R.drawable.bg_progress_badge,
            badgeTextColorRes = R.color.brass500,
            indicatorColorRes = R.color.pine700,
            pinId = R.id.pin_4
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mapBinding = FragmentMapBinding.inflate(inflater, container, false)
        _binding = mapBinding
        return mapBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheet()
        setupRecyclerView()
        setupFilterChips()
        setupPins()

        adapter.submitList(allOccurrences)
        updateCountText(allOccurrences.size)
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        val density = resources.displayMetrics.density
        bottomSheetBehavior.peekHeight = (260 * density).toInt()
        bottomSheetBehavior.isHideable = false
    }

    private fun setupRecyclerView() {
        adapter = OccurrenceAdapter { item ->
            highlightPin(item.pinId)
        }
        binding.recyclerOccurrences.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerOccurrences.adapter = adapter
    }

    private fun setupFilterChips() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedChipId = checkedIds.firstOrNull() ?: R.id.chip_all

            val filteredList = when (selectedChipId) {
                R.id.chip_urgente -> allOccurrences.filter { it.urgencyLevel == "Urgente" }
                R.id.chip_em_curso -> allOccurrences.filter { it.badgeText == "Em curso" }
                R.id.chip_emergencia -> allOccurrences.filter { it.urgencyLevel == "Emergência" }
                else -> allOccurrences
            }

            adapter.submitList(filteredList)
            updateCountText(filteredList.size)
            updatePinsVisibility(filteredList)
        }
    }

    private fun setupPins() {
        binding.pin1.setOnClickListener { selectOccurrenceByPin(R.id.pin_1) }
        binding.pin2.setOnClickListener { selectOccurrenceByPin(R.id.pin_2) }
        binding.pin3.setOnClickListener { selectOccurrenceByPin(R.id.pin_3) }
        binding.pin4.setOnClickListener { selectOccurrenceByPin(R.id.pin_4) }
    }

    private fun selectOccurrenceByPin(pinId: Int) {
        val index = adapter.currentList.indexOfFirst { it.pinId == pinId }
        if (index != -1) {
            adapter.selectedPosition = index
            adapter.notifyItemChanged(index)
            binding.recyclerOccurrences.smoothScrollToPosition(index)
            highlightPin(pinId)

            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun highlightPin(pinId: Int) {
        val pins: List<ImageView> = listOf(binding.pin1, binding.pin2, binding.pin3, binding.pin4)
        pins.forEach { pin ->
            if (pin.id == pinId) {
                pin.animate().scaleX(1.3f).scaleY(1.3f).setDuration(200).start()
                pin.alpha = 1.0f
            } else {
                pin.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                pin.alpha = 0.6f
            }
        }
    }

    private fun updatePinsVisibility(visibleItems: List<OccurrenceItem>) {
        val visiblePinIds = visibleItems.map { it.pinId }
        val pinMap: Map<Int, ImageView> = mapOf(
            R.id.pin_1 to binding.pin1,
            R.id.pin_2 to binding.pin2,
            R.id.pin_3 to binding.pin3,
            R.id.pin_4 to binding.pin4
        )

        pinMap.forEach { (id, view) ->
            if (visiblePinIds.contains(id)) {
                view.visibility = View.VISIBLE
                view.alpha = 1.0f
                view.scaleX = 1.0f
                view.scaleY = 1.0f
            } else {
                view.visibility = View.GONE
            }
        }
    }

    private fun updateCountText(count: Int) {
        binding.occurrenceCountText.text = "$count ocorrências em um raio de 2 km"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class OccurrenceAdapter(
    private val onItemClick: (OccurrenceItem) -> Unit
) : ListAdapter<OccurrenceItem, OccurrenceAdapter.OccurrenceViewHolder>(DiffCallback) {

    var selectedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OccurrenceViewHolder {
        val binding = ItemOccurrenceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OccurrenceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OccurrenceViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    inner class OccurrenceViewHolder(
        private val binding: ItemOccurrenceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OccurrenceItem, isSelected: Boolean) {
            binding.occurrenceTitle.text = item.title
            binding.occurrenceSubtitle.text = item.subtitle
            binding.occurrenceBadge.text = item.badgeText
            binding.occurrenceBadge.setBackgroundResource(item.badgeBgColorRes)
            binding.occurrenceBadge.setTextColor(
                ContextCompat.getColor(binding.root.context, item.badgeTextColorRes)
            )
            binding.urgencyIndicator.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, item.indicatorColorRes)
            )

            binding.cardRoot.alpha = if (isSelected) 1.0f else 0.95f

            binding.root.setOnClickListener {
                val prev = selectedPosition
                selectedPosition = bindingAdapterPosition
                if (prev != -1) notifyItemChanged(prev)
                notifyItemChanged(selectedPosition)
                onItemClick(item)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<OccurrenceItem>() {
        override fun areItemsTheSame(oldItem: OccurrenceItem, newItem: OccurrenceItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: OccurrenceItem, newItem: OccurrenceItem): Boolean {
            return oldItem == newItem
        }
    }
}
