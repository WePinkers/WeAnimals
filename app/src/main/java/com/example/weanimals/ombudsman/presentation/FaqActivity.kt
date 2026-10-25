package com.example.weanimals.ombudsman.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.databinding.ActivityFaqBinding
import com.example.weanimals.databinding.ItemFaqAccordionBinding

data class FaqItem(
    val id: String,
    val category: String,
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false
)

class FaqActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaqBinding
    private val allFaqs = listOf(
        FaqItem(
            id = "1",
            category = "Adoção",
            question = "Como funciona o processo de adoção responsável?",
            answer = "O processo envolve a escolha do animal no app, preenchimento do questionário de compatibilidade e alinhamento direto com a ONG ou protetor responsável."
        ),
        FaqItem(
            id = "2",
            category = "Denúncias",
            question = "O que acontece após eu registrar uma denúncia?",
            answer = "A denúncia é triada pela nossa equipe e enviada para fiscalização e atendimento prioritário da autoridade responsável no seu município."
        ),
        FaqItem(
            id = "3",
            category = "Achados e Perdidos",
            question = "Como encontrar o tutor de um animal perdido?",
            answer = "Ao cadastrar o animal em Achados e Perdidos, nosso sistema calcula a compatibilidade cruzando marcas, local e horário com os alertas de tutores em até 91% de match."
        ),
        FaqItem(
            id = "4",
            category = "ONGs",
            question = "As ONGs cadastradas são verificadas pela plataforma?",
            answer = "Sim. Todas as instituições parceiras passam por verificação documental e credenciamento de segurança no ecossistema WeAnimals."
        )
    )

    private var currentFilterCategory = "Todas"
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInteractions()
        renderFaqList()
    }

    private fun setupInteractions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.inputSearchFaq.doAfterTextChanged { text ->
            searchQuery = text?.toString().orEmpty().lowercase()
            renderFaqList()
        }

        binding.chipCatAll.setOnClickListener { selectCategory("Todas") }
        binding.chipCatAdoption.setOnClickListener { selectCategory("Adoção") }
        binding.chipCatReports.setOnClickListener { selectCategory("Denúncias") }
        binding.chipCatLostFound.setOnClickListener { selectCategory("Achados e Perdidos") }
        binding.chipCatOngs.setOnClickListener { selectCategory("ONGs") }
    }

    private fun selectCategory(cat: String) {
        currentFilterCategory = cat
        renderFaqList()
    }

    private fun renderFaqList() {
        val filtered = allFaqs.filter { faq ->
            val matchesCategory = (currentFilterCategory == "Todas" || faq.category.equals(currentFilterCategory, true))
            val matchesQuery = searchQuery.isBlank() ||
                    faq.question.lowercase().contains(searchQuery) ||
                    faq.answer.lowercase().contains(searchQuery)
            matchesCategory && matchesQuery
        }

        binding.rvFaqItems.layoutManager = LinearLayoutManager(this)
        binding.rvFaqItems.adapter = FaqAccordionAdapter(filtered)
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}

class FaqAccordionAdapter(
    private val items: List<FaqItem>
) : RecyclerView.Adapter<FaqAccordionAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemFaqAccordionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFaqAccordionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            faqQuestion.text = item.question
            faqAnswer.text = item.answer
            faqAnswer.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            faqChevron.rotation = if (item.isExpanded) 90f else 0f

            cardFaqRoot.setOnClickListener {
                item.isExpanded = !item.isExpanded
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount() = items.size
}
