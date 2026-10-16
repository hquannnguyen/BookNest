package com.example.quanlysachcanhan.ui.detail

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.adapter.QuoteAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.QuoteRepository
import com.example.quanlysachcanhan.databinding.ActivityBookDetailBinding
import com.example.quanlysachcanhan.databinding.DialogAddQuoteBinding
import com.example.quanlysachcanhan.databinding.DialogDeleteBookBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.model.Quote
import com.example.quanlysachcanhan.ui.addedit.AddEditBookActivity
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.NavigationHelper
import com.example.quanlysachcanhan.utils.PreferenceManager
import com.example.quanlysachcanhan.utils.ShareUtils
import com.example.quanlysachcanhan.utils.getCategoryLabel
import com.example.quanlysachcanhan.utils.getReadingStatusLabel
import java.io.File

class BookDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookDetailBinding
    private lateinit var bookRepository: BookRepository
    private lateinit var quoteRepository: QuoteRepository
    private lateinit var quoteAdapter: QuoteAdapter

    private var bookId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookRepository  = BookRepository(this)
        quoteRepository = QuoteRepository(this)

        bookId = intent.getLongExtra(Constants.Extras.BOOK_ID, 0L)
        if (bookId <= 0L) {
            NavigationHelper.finishWithSlide(this)
            return
        }

        setupNavigationAndHeader()
        setupQuoteList()
        setupTabs()
        setupEvents()
    }

    private fun setupNavigationAndHeader() {
        NavigationHelper.setupHeader(
            this,
            binding.btnLanguage,
            binding.ivLangFlag,
            binding.tvLangCode,
            binding.btnReminder,
            binding.dotReminder,
            PreferenceManager(this)
        )

        NavigationHelper.setupBottomNavigation(
            this,
            NavigationHelper.Tab.SUB_PAGE,
            binding.navLibrary,
            binding.navCollections,
            binding.navStatistics,
            binding.navSettings
        )

        onBackPressedDispatcher.addCallback(this) {
            NavigationHelper.finishWithSlide(this@BookDetailActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        loadBook()
        loadQuotes()
    }

    private fun setupQuoteList() {
        quoteAdapter = QuoteAdapter { quote ->
            quoteRepository.delete(quote.id)
            loadQuotes()
            Toast.makeText(this, getString(R.string.msg_quote_deleted), Toast.LENGTH_SHORT).show()
        }

        quoteAdapter.onShareClick = { quote ->
            shareIndividualQuote(quote)
        }

        binding.recyclerQuotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerQuotes.adapter = quoteAdapter
    }

    private fun setupTabs() {
        binding.tabQuotes.setOnClickListener {
            switchTab(isQuotesTab = true)
        }

        binding.tabNotes.setOnClickListener {
            switchTab(isQuotesTab = false)
        }
    }

    private fun switchTab(isQuotesTab: Boolean) {
        val primaryNavy = ContextCompat.getColor(this, R.color.primary_navy)
        val textHint    = ContextCompat.getColor(this, R.color.text_hint)

        if (isQuotesTab) {
            binding.tabQuotes.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.ivTabQuotesIcon.setColorFilter(primaryNavy)
            binding.tvTabQuotesTitle.setTextColor(primaryNavy)

            binding.tabNotes.setBackgroundResource(R.drawable.bg_tab_unselected)
            binding.ivTabNotesIcon.setColorFilter(textHint)
            binding.tvTabNotesTitle.setTextColor(textHint)

            binding.layoutQuotesTabContent.visibility = View.VISIBLE
            binding.layoutNotesTabContent.visibility  = View.GONE
        } else {
            binding.tabQuotes.setBackgroundResource(R.drawable.bg_tab_unselected)
            binding.ivTabQuotesIcon.setColorFilter(textHint)
            binding.tvTabQuotesTitle.setTextColor(textHint)

            binding.tabNotes.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.ivTabNotesIcon.setColorFilter(primaryNavy)
            binding.tvTabNotesTitle.setTextColor(primaryNavy)

            binding.layoutQuotesTabContent.visibility = View.GONE
            binding.layoutNotesTabContent.visibility  = View.VISIBLE
        }
    }

    private fun setupEvents() {
        binding.btnBack.setOnClickListener {
            NavigationHelper.finishWithSlide(this)
        }

        binding.btnEdit.setOnClickListener {
            openEditScreen()
        }

        binding.btnQuickEditCover.setOnClickListener {
            openEditScreen()
        }

        binding.btnEditNote.setOnClickListener {
            openEditScreen()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.btnShare.setOnClickListener {
            bookRepository.getById(bookId)?.let { ShareUtils.shareBook(this, it) }
        }

        binding.btnAddQuote.setOnClickListener {
            showAddQuoteDialog()
        }
    }

    private fun openEditScreen() {
        val intent = Intent(this, AddEditBookActivity::class.java).apply {
            putExtra(Constants.Extras.BOOK_ID, bookId)
        }
        NavigationHelper.startActivitySlide(this, intent)
    }

    private fun loadBook() {
        val book = bookRepository.getById(bookId) ?: return

        // 1. Tên sách, tác giả, thể loại, nhà xuất bản
        binding.tvTitle.text        = book.title
        binding.tvAuthor.text       = book.author
        binding.tvCategory.text     = getCategoryLabel(book.category)
        binding.tvInfoCategory.text = getCategoryLabel(book.category)

        // Tag Nhà xuất bản (thay cho SQLite Offline cũ, hiện nếu có thông tin)
        if (book.publisher.isNotBlank()) {
            binding.tvPublisher.visibility = View.VISIBLE
            binding.tvPublisher.text = book.publisher
        } else {
            binding.tvPublisher.visibility = View.GONE
        }

        // Thông tin NXB trong bảng xuất bản
        binding.tvInfoPublisher.text = book.publisher.ifBlank {
            getString(R.string.label_publisher_none)
        }

        // 2. Ghi chú cá nhân
        binding.tvNote.text = if (book.note.isNotBlank()) {
            book.note
        } else {
            getString(R.string.empty_notes_hint)
        }

        // 3. Ảnh bìa
        val coverPath = book.coverImagePath
        if (!coverPath.isNullOrBlank() && File(coverPath).exists()) {
            binding.imgCover.visibility = View.VISIBLE
            binding.layoutCoverPlaceholder.visibility = View.GONE
            binding.imgCover.setImageURI(Uri.fromFile(File(coverPath)))
        } else {
            binding.imgCover.visibility = View.GONE
            binding.layoutCoverPlaceholder.visibility = View.VISIBLE
        }

        // 4. Trạng thái đọc
        bindReadingStatus(book.readingStatus)

        // 5. Đánh giá dải 5 trái tim
        bindRatingHearts(book.rating)

        // 6. Thanh tiến độ đọc sách
        bindReadingProgress(book)
    }

    private fun bindReadingStatus(status: String) {
        val label = getReadingStatusLabel(status)
        binding.tvStatus.text = label

        val bgDrawable = GradientDrawable()
        bgDrawable.cornerRadius = 32f

        when (status) {
            Constants.ReadingStatus.READ -> {
                bgDrawable.setColor(ContextCompat.getColor(this, R.color.status_read_bg))
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_read_text))
                binding.viewStatusDot.setBackgroundColor(Color.WHITE)
            }
            Constants.ReadingStatus.READING -> {
                bgDrawable.setColor(ContextCompat.getColor(this, R.color.status_reading_bg))
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_reading_text))
                binding.viewStatusDot.setBackgroundColor(ContextCompat.getColor(this, R.color.secondary_oak))
            }
            else -> {
                bgDrawable.setColor(ContextCompat.getColor(this, R.color.status_unread_bg))
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_unread_text))
                binding.viewStatusDot.setBackgroundColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
        binding.layoutStatusBadge.background = bgDrawable
    }

    private fun bindRatingHearts(rating: Float) {
        val hearts = listOf<ImageView>(
            binding.ivHeart1,
            binding.ivHeart2,
            binding.ivHeart3,
            binding.ivHeart4,
            binding.ivHeart5
        )

        val goldColor    = ContextCompat.getColor(this, R.color.star_gold)
        val outlineColor = ContextCompat.getColor(this, R.color.outline_variant)

        hearts.forEachIndexed { index, iv ->
            if (rating >= index + 1) {
                iv.setImageResource(R.drawable.ic_favorite)
                iv.setColorFilter(goldColor)
            } else {
                iv.setImageResource(R.drawable.ic_favorite_border)
                iv.setColorFilter(outlineColor)
            }
        }

        binding.tvRatingScore.text = if (rating > 0f) {
            String.format("%.1f", rating)
        } else {
            "0.0"
        }
    }

    private fun bindReadingProgress(book: Book) {
        val (displayCurrent, total, percentage) = when (book.readingStatus) {
            Constants.ReadingStatus.READ -> {
                val total = book.totalPages
                val current = if (total > 0) {
                    if (book.currentPage > 0) maxOf(book.currentPage, total) else total
                } else {
                    book.currentPage
                }
                Triple(current, total, 100)
            }
            Constants.ReadingStatus.UNREAD -> {
                Triple(0, book.totalPages, 0)
            }
            else -> { // Constants.ReadingStatus.READING
                if (book.totalPages > 0) {
                    val current = book.currentPage.coerceIn(0, book.totalPages)
                    val percent = (current * 100 / book.totalPages).coerceIn(0, 100)
                    Triple(current, book.totalPages, percent)
                } else {
                    Triple(book.currentPage, 0, 60)
                }
            }
        }

        if (total > 0) {
            binding.tvProgressPages.text = getString(
                R.string.label_progress_detail_format,
                displayCurrent,
                total,
                percentage
            )
        } else {
            binding.tvProgressPages.text = "${getReadingStatusLabel(book.readingStatus)} ($percentage%)"
        }

        binding.progressBarReading.progress = percentage
    }

    private fun loadQuotes() {
        val quotes = quoteRepository.getByBookId(bookId)
        quoteAdapter.submitList(quotes)

        binding.tvTabQuotesTitle.text = getString(R.string.label_quotes_tab, quotes.size)
        binding.tvQuotesSummary.text  = "(${quotes.size} trích dẫn)"
        binding.tvEmptyQuotes.visibility = if (quotes.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddQuoteDialog() {
        val dialogBinding = DialogAddQuoteBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSaveQuote.setOnClickListener {
            val content = dialogBinding.edtQuoteContent.text.toString().trim()
            val page    = dialogBinding.edtQuotePage.text.toString().trim()
            val chapter = dialogBinding.edtQuoteChapter.text.toString().trim()
            val tag     = dialogBinding.edtQuoteTag.text.toString().trim()

            if (content.isEmpty()) {
                dialogBinding.edtQuoteContent.error = getString(R.string.hint_quote_content)
                dialogBinding.edtQuoteContent.requestFocus()
                return@setOnClickListener
            }

            val newQuote = Quote(
                bookId  = bookId,
                content = content,
                page    = page,
                chapter = chapter,
                tag     = tag
            )

            quoteRepository.insert(newQuote)
            dialog.dismiss()
            loadQuotes()
            Toast.makeText(this, getString(R.string.msg_quote_added), Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun shareIndividualQuote(quote: Quote) {
        val book = bookRepository.getById(bookId)
        val sourceInfo = buildString {
            if (quote.page.isNotBlank()) append(quote.page)
            if (quote.chapter.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(quote.chapter)
            }
            if (isEmpty()) {
                append(book?.author.orEmpty())
            }
        }

        val textToShare = getString(
            R.string.share_quote_format,
            quote.content,
            book?.title.orEmpty(),
            sourceInfo
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textToShare)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.btn_share_quote)))
    }

    private fun showDeleteConfirmationDialog() {
        val dialogBinding = DialogDeleteBookBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDeleteConfirm.setOnClickListener {
            dialog.dismiss()
            bookRepository.delete(bookId)
            Toast.makeText(this, getString(R.string.msg_book_deleted), Toast.LENGTH_SHORT).show()
            NavigationHelper.finishWithSlide(this)
        }

        dialog.show()
    }
}