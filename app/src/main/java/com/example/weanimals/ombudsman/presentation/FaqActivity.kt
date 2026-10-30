package com.example.weanimals.ombudsman.presentation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weanimals.R
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
            category = "Denúncias",
            question = "Como faço uma denúncia de animal em risco?",
            answer = "Acesse a aba Início ou Denúncias, toque no botão Registre uma Ocorrência e siga as etapas descrevendo o local e a situação."
        ),
        FaqItem(
            id = "2",
            category = "Denúncias",
            question = "Como acompanho o andamento da minha denúncia?",
            answer = "Abra Perfil › Minhas denúncias e toque no protocolo. Lá você vê cada atualização e pode conversar com a equipe."
        ),
        FaqItem(
            id = "3",
            category = "Adoção",
            question = "Como funciona a adoção pelo app?",
            answer = "Você escolhe o animal, preenche o formulário de compatibilidade e conversa diretamente com a ONG ou protetor responsável."
        ),
        FaqItem(
            id = "4",
            category = "Conta",
            question = "O que é o Nível de Confiabilidade?",
            answer = "É um selo que indica o histórico positivo e a verificação do perfil para garantir a segurança da comunidade."
        ),
        FaqItem(
            id = "5",
            category = "Golpes",
            question = "Pediram dinheiro para devolver um animal. E agora?",
            answer = "Não faça nenhuma transferência adiantada. Registre uma denúncia de suspeita de golpe na nossa Ouvidoria imediatamente."
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

        binding.btnSendQuestion.setOnClickListener {
            startActivity(Intent(this, SendQuestionActivity::class.java))
        }

        val pineBg = R.drawable.bg_chip_selected_pine
        val unselectedBg = R.drawable.bg_chip_unselected
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val inkColor = ContextCompat.getColor(this, R.color.ink)

        fun updateChips(activeChip: android.widget.TextView, categoryName: String) {
            currentFilterCategory = categoryName
            listOf(binding.chipCatAll, binding.chipCatReports, binding.chipCatAdoption, binding.chipCatAccount, binding.chipCatScams).forEach { chip ->
                chip.setBackgroundResource(unselectedBg)
                chip.setTextColor(inkColor)
            }
            activeChip.setBackgroundResource(pineBg)
            activeChip.setTextColor(whiteColor)
            renderFaqList()
        }

        binding.chipCatAll.setOnClickListener { updateChips(binding.chipCatAll, "Todas") }
        binding.chipCatReports.setOnClickListener { updateChips(binding.chipCatReports, "Denúncias") }
        binding.chipCatAdoption.setOnClickListener { updateChips(binding.chipCatAdoption, "Adoção") }
        binding.chipCatAccount.setOnClickListener { updateChips(binding.chipCatAccount, "Conta") }
        binding.chipCatScams.setOnClickListener { updateChips(binding.chipCatScams, "Golpes") }
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
            expandedContainer.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            faqChevron.rotation = if (item.isExpanded) 90f else 0f

            val context = root.context
            if (item.isExpanded) {
                cardFaqRoot.setBackgroundResource(R.drawable.bg_card_press_selector)
            } else {
                cardFaqRoot.setBackgroundResource(R.drawable.bg_stat_card)
            }

            cardFaqRoot.setOnClickListener {
                item.isExpanded = !item.isExpanded
                notifyItemChanged(position)
            }

            btnHelpfulYes.setOnClickListener {
                Toast.makeText(context, "Obrigado pelo feedback!", Toast.LENGTH_SHORT).show()
            }

            btnHelpfulNo.setOnClickListener {
                Toast.makeText(context, "Obrigado pelo feedback! Vamos melhorar a resposta.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = items.size
}
