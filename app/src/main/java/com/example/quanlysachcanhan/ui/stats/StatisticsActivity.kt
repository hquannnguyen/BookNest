package com.example.quanlysachcanhan.ui.stats

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.QuoteRepository
import com.example.quanlysachcanhan.databinding.ActivityStatisticsBinding
import com.example.quanlysachcanhan.databinding.ItemCategoryStatBinding
import com.example.quanlysachcanhan.ui.main.MainActivity
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.NavigationHelper
import com.example.quanlysachcanhan.utils.PreferenceManager
import com.example.quanlysachcanhan.utils.getCategoryLabel
import kotlin.math.roundToInt

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var repository: BookRepository
    private lateinit var quoteRepository: QuoteRepository
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BookRepository(this)
        quoteRepository = QuoteRepository(this)
        preferenceManager = PreferenceManager(this)

        setupNavigationAndHeader()
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
    }

    private fun setupNavigationAndHeader() {
        NavigationHelper.setupHeader(
            this,
            binding.btnLanguage,
            binding.ivLangFlag,
            binding.tvLangCode,
            binding.btnReminder,
            binding.dotReminder,
            preferenceManager
        )

        NavigationHelper.setupBottomNavigation(
            this,
            NavigationHelper.Tab.STATISTICS,
            binding.navLibrary,
            binding.navCollections,
            binding.navStatistics,
            binding.navSettings
        )

        binding.btnBack.setOnClickListener {
            NavigationHelper.finishWithSlide(this)
        }

        onBackPressedDispatcher.addCallback(this) {
            NavigationHelper.finishWithSlide(this@StatisticsActivity)
        }

        binding.btnViewLibrary.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            NavigationHelper.finishWithSlide(this)
        }
    }

    private fun loadStatistics() {
        val total = repository.countAll()
        val unread = repository.countByStatus(Constants.ReadingStatus.UNREAD)
        val reading = repository.countByStatus(Constants.ReadingStatus.READING)
        val read = repository.countByStatus(Constants.ReadingStatus.READ)

        // 1. Key Metrics 3-Card Row
        binding.tvTotal.text = total.toString()
        binding.tvReadHighlight.text = read.toString()
        binding.tvReading.text = reading.toString()

        // 2. Visual Card 1: Donut Chart & Category Breakdown
        setupCategoryDistribution(total)

        // 3. Visual Card 2: Reading Status Distribution
        setupReadingStatusBars(total, read, reading, unread)

        // 4. Visual Card 3: Quote & Inspiration Bento
        setupInspirationBento()
    }

    private fun setupCategoryDistribution(totalBooks: Int) {
        val categoryCounts = repository.getCategoryCounts()
        binding.containerCategoryStats.removeAllViews()

        if (totalBooks <= 0 || categoryCounts.isEmpty()) {
            binding.donutView.setSlices(emptyList())
            binding.tvDonutTotal.text = "0"
            binding.tvFavoriteCategory.text = getString(R.string.status_all)
            binding.tvFavoriteCategoryDesc.text = "Tủ sách của bạn đang chờ những cuốn sách đầu tiên."
            return
        }

        binding.tvDonutTotal.text = totalBooks.toString()

        // Colors palette according to Literary Sanctuary theme
        val palette = listOf(
            ContextCompat.getColor(this, R.color.primary_navy),
            ContextCompat.getColor(this, R.color.secondary_oak),
            Color.parseColor("#4E5F7E"),
            ContextCompat.getColor(this, R.color.secondary_container),
            ContextCompat.getColor(this, R.color.primary_fixed),
            ContextCompat.getColor(this, R.color.secondary_fixed)
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
            val itemBinding = ItemCategoryStatBinding.inflate(layoutInflater, binding.containerCategoryStats, false)
            itemBinding.viewColorDot.background.mutate().setTint(color)
            itemBinding.tvCategoryName.text = getCategoryLabel(entry.key)
            itemBinding.tvCategoryCount.text = "${entry.value} cuốn"
            itemBinding.tvCategoryPercentBadge.text = "$pct%"
            binding.containerCategoryStats.addView(itemBinding.root)
        }

        binding.donutView.setSlices(slices)

        // Highlight top favorite category
        val topEntry = categoryCounts.maxByOrNull { it.value }
        if (topEntry != null) {
            val topPercent = (topEntry.value.toFloat() * 100f / totalBooks).roundToInt()
            binding.tvFavoriteCategory.text = getCategoryLabel(topEntry.key)
            binding.tvFavoriteCategoryDesc.text = "Chiếm $topPercent% tủ sách, phản ánh khẩu vị và sở thích chiêm nghiệm của bạn."
        }
    }

    private fun setupReadingStatusBars(
        total: Int,
        read: Int,
        reading: Int,
        unread: Int
    ) {
        val readPct = if (total > 0) (read * 100 / total) else 0
        val readingPct = if (total > 0) (reading * 100 / total) else 0
        val unreadPct = if (total > 0) (unread * 100 / total) else 0

        // Status 1: Đã đọc
        binding.tvReadCount.text = read.toString()
        binding.tvReadPercent.text = "($readPct%)"
        binding.progressStatRead.progress = readPct
        binding.tvReadAvg.text = "$read tác phẩm hoàn thành"

        // Status 2: Đang đọc
        binding.tvReadingCount.text = reading.toString()
        binding.tvReadingPercent.text = "($readingPct%)"
        binding.progressStatReading.progress = readingPct
        binding.tvReadingSub.text = "$reading tác phẩm gối đầu giường"

        // Status 3: Chưa đọc
        binding.tvUnreadCount.text = unread.toString()
        binding.tvUnreadPercent.text = "($unreadPct%)"
        binding.progressStatUnread.progress = unreadPct
    }

    private fun setupInspirationBento() {
        val totalQuotes = quoteRepository.countAll()
        binding.tvQuoteStatsCount.text = "Tổng cộng: $totalQuotes trích dẫn đã lưu"

        val randomQuote = quoteRepository.getRandomQuote()
        if (randomQuote != null && randomQuote.content.isNotBlank()) {
            binding.tvInspirationQuote.text = "“${randomQuote.content}”"
            val book = repository.getById(randomQuote.bookId)
            val sourceText = buildString {
                if (book != null) append("— ${book.title}")
                if (randomQuote.page.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append("Trang ${randomQuote.page}")
                }
                if (randomQuote.chapter.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(randomQuote.chapter)
                }
            }
            binding.tvInspirationSource.text = sourceText.ifEmpty { "— Trích dẫn tâm đắc" }
        } else {
            binding.tvInspirationQuote.text = "“Mỗi trang sách khép lại là một thế giới mới bắt đầu mở ra trong tâm thức.”"
            binding.tvInspirationSource.text = "— Thói quen đọc sách mỗi ngày"
        }
    }
}