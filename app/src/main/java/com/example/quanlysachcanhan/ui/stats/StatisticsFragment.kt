package com.example.quanlysachcanhan.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.QuoteRepository
import com.example.quanlysachcanhan.databinding.FragmentStatisticsBinding
import com.example.quanlysachcanhan.databinding.ItemCategoryStatBinding
import com.example.quanlysachcanhan.ui.main.MainActivity
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.PreferenceManager
import com.example.quanlysachcanhan.utils.getCategoryLabel
import kotlin.math.roundToInt

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: BookRepository
    private lateinit var quoteRepository: QuoteRepository
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()
        repository = BookRepository(ctx)
        quoteRepository = QuoteRepository(ctx)
        preferenceManager = PreferenceManager(ctx)

        binding.btnViewLibrary.setOnClickListener {
            (activity as? MainActivity)?.selectTab(MainActivity.TAB_LIBRARY)
        }
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && isAdded && _binding != null && context != null) {
            loadStatistics()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun loadStatistics() {
        val b = _binding ?: return
        if (!isAdded || context == null) return
        val total = repository.countAll()
        val unread = repository.countByStatus(Constants.ReadingStatus.UNREAD)
        val reading = repository.countByStatus(Constants.ReadingStatus.READING)
        val read = repository.countByStatus(Constants.ReadingStatus.READ)

        // 1. Key Metrics 3-Card Row
        b.tvTotal.text = total.toString()
        b.tvReadHighlight.text = read.toString()
        b.tvReading.text = reading.toString()

        // 2. Visual Card 1: Donut Chart & Category Breakdown
        setupCategoryDistribution(total)

        // 3. Visual Card 2: Reading Status Distribution
        setupReadingStatusBars(total, read, reading, unread)

        // 4. Visual Card 3: Quote & Inspiration Bento
        setupInspirationBento()
    }

    private fun setupCategoryDistribution(totalBooks: Int) {
        val b = _binding ?: return
        val ctx = context ?: return
        val categoryCounts = repository.getCategoryCounts()
        b.containerCategoryStats.removeAllViews()

        if (totalBooks <= 0 || categoryCounts.isEmpty()) {
            b.donutView.setSlices(emptyList())
            b.tvDonutTotal.text = "0"
            b.tvFavoriteCategory.text = getString(R.string.status_all)
            b.tvFavoriteCategoryDesc.text = getString(R.string.stats_category_desc_empty)
            return
        }

        b.tvDonutTotal.text = totalBooks.toString()

        // Colors palette according to Literary Sanctuary theme
        val palette = listOf(
            ContextCompat.getColor(ctx, R.color.primary_navy),
            ContextCompat.getColor(ctx, R.color.secondary_oak),
            Color.parseColor("#4E5F7E"),
            ContextCompat.getColor(ctx, R.color.secondary_container),
            ContextCompat.getColor(ctx, R.color.primary_fixed),
            ContextCompat.getColor(ctx, R.color.secondary_fixed)
        )

        val slices = mutableListOf<CategoryDonutView.Slice>()
        categoryCounts.entries.forEachIndexed { index, entry ->
            val color = palette[index % palette.size]
            val pct = (entry.value.toFloat() * 100f / totalBooks).roundToInt()

            slices.add(
                CategoryDonutView.Slice(
                    percentage = entry.value.toFloat() * 100f / totalBooks,
                    color = color
                )
            )

            // Inflate dynamic legend item
            val itemBinding = ItemCategoryStatBinding.inflate(layoutInflater, b.containerCategoryStats, false)
            itemBinding.viewColorDot.background.mutate().setTint(color)
            itemBinding.tvCategoryName.text = ctx.getCategoryLabel(entry.key)
            itemBinding.tvCategoryCount.text = getString(R.string.count_books_format, entry.value)
            itemBinding.tvCategoryPercentBadge.text = "$pct%"
            b.containerCategoryStats.addView(itemBinding.root)
        }

        b.donutView.setSlices(slices)

        // Highlight top favorite category
        val topEntry = categoryCounts.maxByOrNull { it.value }
        if (topEntry != null) {
            val topPercent = (topEntry.value.toFloat() * 100f / totalBooks).roundToInt()
            b.tvFavoriteCategory.text = ctx.getCategoryLabel(topEntry.key)
            b.tvFavoriteCategoryDesc.text = getString(R.string.stats_category_desc_format, topPercent)
        }
    }

    private fun setupReadingStatusBars(
        total: Int,
        read: Int,
        reading: Int,
        unread: Int
    ) {
        val b = _binding ?: return
        val readPct = if (total > 0) (read * 100 / total) else 0
        val readingPct = if (total > 0) (reading * 100 / total) else 0
        val unreadPct = if (total > 0) (unread * 100 / total) else 0

        // Status 1: Đã đọc
        b.tvReadCount.text = read.toString()
        b.tvReadPercent.text = "($readPct%)"
        b.progressStatRead.progress = readPct
        b.tvReadAvg.text = getString(R.string.stats_status_completed_sub, read)

        // Status 2: Đang đọc
        b.tvReadingCount.text = reading.toString()
        b.tvReadingPercent.text = "($readingPct%)"
        b.progressStatReading.progress = readingPct
        b.tvReadingSub.text = getString(R.string.stats_status_reading_sub, reading)

        // Status 3: Chưa đọc
        b.tvUnreadCount.text = unread.toString()
        b.tvUnreadPercent.text = "($unreadPct%)"
        b.progressStatUnread.progress = unreadPct
    }

    private fun setupInspirationBento() {
        val b = _binding ?: return
        val totalQuotes = quoteRepository.countAll()
        b.tvQuoteStatsCount.text = getString(R.string.stats_quote_total_count, totalQuotes)

        val randomQuote = quoteRepository.getRandomQuote()
        if (randomQuote != null && randomQuote.content.isNotBlank()) {
            b.tvInspirationQuote.text = "“${randomQuote.content}”"
            val book = repository.getById(randomQuote.bookId)
            val sourceText = buildString {
                if (book != null) append("— ${book.title}")
                if (randomQuote.page.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(getString(R.string.stats_quote_source_page, randomQuote.page))
                }
                if (randomQuote.chapter.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(randomQuote.chapter)
                }
            }
            b.tvInspirationSource.text = sourceText.ifEmpty { getString(R.string.stats_quote_source_fallback) }
        } else {
            b.tvInspirationQuote.text = getString(R.string.stats_quote_default)
            b.tvInspirationSource.text = getString(R.string.stats_quote_default_source)
        }
    }

    companion object {
        fun newInstance() = StatisticsFragment()
    }
}
