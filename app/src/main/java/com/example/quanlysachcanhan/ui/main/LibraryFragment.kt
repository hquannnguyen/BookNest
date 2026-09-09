package com.example.quanlysachcanhan.ui.main

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.adapter.BookAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.QuoteRepository
import com.example.quanlysachcanhan.databinding.DialogCustomSelectionBinding
import com.example.quanlysachcanhan.databinding.FragmentLibraryBinding
import com.example.quanlysachcanhan.databinding.ItemDialogOptionBinding
import com.example.quanlysachcanhan.ui.detail.BookDetailActivity
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.NavigationHelper
import com.example.quanlysachcanhan.utils.PreferenceManager
import com.example.quanlysachcanhan.utils.getCategoryLabels

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: BookRepository
    private lateinit var quoteRepository: QuoteRepository
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: BookAdapter

    private var keyword: String = ""
    private var selectedCategory: String? = null
    private var selectedStatus: String? = null
    private var isFavoriteFilter: Boolean = false

    private val bannerQuoteHandler = Handler(Looper.getMainLooper())
    private val bannerQuoteRunnable = object : Runnable {
        override fun run() {
            if (isAdded && _binding != null && context != null && !isHidden) {
                setupDailyQuoteBanner(forceRefresh = true, withAnimation = true)
                bannerQuoteHandler.postDelayed(this, BANNER_AUTO_TRANSITION_INTERVAL_MS)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()
        repository = BookRepository(ctx)
        quoteRepository = QuoteRepository(ctx)
        preferenceManager = PreferenceManager(ctx)

        setupRecyclerView()
        setupStatusChips()
        setupDailyQuoteBanner()
        updateFilterLabel()
        updateSortLabel()
        setupEvents()
    }

    override fun onResume() {
        super.onResume()
        setupDailyQuoteBanner(forceRefresh = false, withAnimation = false)
        startBannerAutoTransition()
        updateFilterLabel()
        updateSortLabel()
        updateChipCounts()
        loadBooks()
    }

    override fun onPause() {
        super.onPause()
        stopBannerAutoTransition()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            stopBannerAutoTransition()
        } else {
            if (isAdded && _binding != null && context != null) {
                setupDailyQuoteBanner(forceRefresh = false, withAnimation = false)
                startBannerAutoTransition()
                updateFilterLabel()
                updateSortLabel()
                updateChipCounts()
                loadBooks()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopBannerAutoTransition()
        _binding = null
    }

    fun scrollToTop() {
        _binding?.nestedScrollLibrary?.smoothScrollTo(0, 0)
    }

    fun openFilterDialog() {
        showFilterDialog()
    }

    private fun startBannerAutoTransition() {
        bannerQuoteHandler.removeCallbacks(bannerQuoteRunnable)
        bannerQuoteHandler.postDelayed(bannerQuoteRunnable, BANNER_AUTO_TRANSITION_INTERVAL_MS)
    }

    private fun stopBannerAutoTransition() {
        bannerQuoteHandler.removeCallbacks(bannerQuoteRunnable)
    }

    private fun setupRecyclerView() {
        adapter = BookAdapter { book ->
            val intent = Intent(requireContext(), BookDetailActivity::class.java).apply {
                putExtra(Constants.Extras.BOOK_ID, book.id)
            }
            NavigationHelper.startActivitySlide(requireActivity(), intent)
        }

        binding.recyclerBooks.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerBooks.adapter = adapter
    }

    private fun setupDailyQuoteBanner(forceRefresh: Boolean = false, withAnimation: Boolean = false) {
        val b = _binding ?: return
        if (!isAdded || context == null) return
        val now = System.currentTimeMillis()
        val cachedContent = preferenceManager.bannerQuoteContent
        val cachedTitle = preferenceManager.bannerQuoteBookTitle

        if (!forceRefresh && !cachedContent.isNullOrBlank()) {
            b.tvDailyQuote.alpha = 1f
            b.tvDailyQuote.translationY = 0f
            b.tvDailyQuote.text = cachedContent
            b.tvBannerTime.alpha = 0.75f
            b.tvBannerTime.text = cachedTitle ?: getString(R.string.banner_quote_time)
            return
        }

        val allBooks = repository.getBooks()
        val allQuotes = allBooks.flatMap { quoteRepository.getByBookId(it.id) }

        val newContent: String
        val newTitle: String

        if (allQuotes.isNotEmpty()) {
            val candidateQuotes = if (allQuotes.size > 1 && !cachedContent.isNullOrBlank()) {
                allQuotes.filter { "“${it.content.trim()}”" != cachedContent }
            } else {
                allQuotes
            }.ifEmpty { allQuotes }

            val chosen = candidateQuotes.random()
            val book = allBooks.find { it.id == chosen.bookId }
            newContent = "“${chosen.content.trim()}”"
            newTitle = book?.title ?: getString(R.string.banner_quote_time)
        } else {
            val fallbackQuotes = listOf(
                Pair(
                    "“Khi bạn thực sự mong muốn một điều gì, cả vũ trụ sẽ hợp sức giúp bạn đạt được.”",
                    "Nhà Giả Kim"
                ),
                Pair(
                    "“Người có một lý do để sống có thể chịu đựng được hầu như mọi nghịch cảnh.”",
                    "Đi Tìm Lẽ Sống"
                ),
                Pair(
                    "“Một cuốn sách hay trên giá là một người bạn sẵn sàng trò chuyện bất cứ lúc nào.”",
                    "Gợi ý hôm nay"
                ),
                Pair(
                    "“Đọc sách không chỉ để biết nhiều hơn, mà để thấu hiểu sâu hơn.”",
                    "Gợi ý hôm nay"
                ),
                Pair(
                    "“Hôm nay là một người đọc sách, ngày mai sẽ là một người dẫn đường.”",
                    "Margaret Fuller"
                )
            )

            val candidateFallbacks = if (fallbackQuotes.size > 1 && !cachedContent.isNullOrBlank()) {
                fallbackQuotes.filter { it.first != cachedContent }
            } else {
                fallbackQuotes
            }.ifEmpty { fallbackQuotes }

            val chosen = candidateFallbacks.random()
            newContent = chosen.first
            newTitle = chosen.second
        }

        preferenceManager.bannerQuoteContent = newContent
        preferenceManager.bannerQuoteBookTitle = newTitle
        preferenceManager.bannerQuoteTime = now

        if (withAnimation) {
            val fadeOut = 220L
            val fadeIn = 320L
            b.tvDailyQuote.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(fadeOut)
                .withEndAction {
                    _binding?.let { validBinding ->
                        if (!isAdded || context == null) return@let
                        validBinding.tvDailyQuote.text = newContent
                        validBinding.tvDailyQuote.translationY = 8f
                        validBinding.tvDailyQuote.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(fadeIn)
                            .start()
                    }
                }.start()

            b.tvBannerTime.animate()
                .alpha(0f)
                .setDuration(fadeOut)
                .withEndAction {
                    _binding?.let { validBinding ->
                        if (!isAdded || context == null) return@let
                        validBinding.tvBannerTime.text = newTitle
                        validBinding.tvBannerTime.animate()
                            .alpha(0.75f)
                            .setDuration(fadeIn)
                            .start()
                    }
                }.start()
        } else {
            b.tvDailyQuote.alpha = 1f
            b.tvDailyQuote.translationY = 0f
            b.tvDailyQuote.text = newContent
            b.tvBannerTime.alpha = 0.75f
            b.tvBannerTime.text = newTitle
        }
    }

    private fun updateChipCounts() {
        val b = _binding ?: return
        if (!isAdded || context == null) return
        val allBooks = repository.getBooks()
        val totalCount = allBooks.size
        val unreadCount = allBooks.count { it.readingStatus == Constants.ReadingStatus.UNREAD }
        val readingCount = allBooks.count { it.readingStatus == Constants.ReadingStatus.READING }
        val readCount = allBooks.count { it.readingStatus == Constants.ReadingStatus.READ }

        b.chipAll.text = "${getString(R.string.status_all)} ($totalCount)"
        b.chipUnread.text = "${getString(R.string.status_unread)} ($unreadCount)"
        b.chipReading.text = "${getString(R.string.status_reading)} ($readingCount)"
        b.chipRead.text = "${getString(R.string.status_read)} ($readCount)"
    }

    private fun setupStatusChips() {
        val b = _binding ?: return
        val statusViews = listOf(
            Pair(b.chipAll, null),
            Pair(b.chipUnread, Constants.ReadingStatus.UNREAD),
            Pair(b.chipReading, Constants.ReadingStatus.READING),
            Pair(b.chipRead, Constants.ReadingStatus.READ)
        )

        fun resetStatusChipsUI() {
            statusViews.forEach { (view, _) ->
                view.setBackgroundResource(R.drawable.bg_chip_unselected)
                view.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }

        statusViews.forEach { (view, status) ->
            view.setOnClickListener {
                isFavoriteFilter = false
                selectedStatus = status
                resetStatusChipsUI()
                view.setBackgroundResource(R.drawable.bg_chip_selected)
                view.setTextColor(Color.WHITE)
                loadBooks()
            }
        }
    }

    private fun updateFilterLabel() {
        val b = _binding ?: return
        if (!isAdded || context == null) return
        val categoryIndex = Constants.Category.ALL.indexOf(selectedCategory)
        val label = if (categoryIndex >= 0) {
            requireContext().getCategoryLabels().getOrNull(categoryIndex) ?: getString(R.string.btn_filter)
        } else {
            getString(R.string.filter_all)
        }
        b.tvFilterLabel.text = "${getString(R.string.btn_filter)}: $label"
    }

    private fun updateSortLabel() {
        val b = _binding ?: return
        if (!isAdded || context == null) return
        val sortCodes = arrayOf(
            Constants.Sort.TITLE,
            Constants.Sort.AUTHOR,
            Constants.Sort.RATING,
            Constants.Sort.STATUS
        )
        val sortLabels = arrayOf(
            getString(R.string.sort_title),
            getString(R.string.sort_author),
            getString(R.string.sort_rating),
            getString(R.string.sort_status)
        )
        val index = sortCodes.indexOf(preferenceManager.sortType).coerceAtLeast(0)
        b.tvSortLabel.text = "${getString(R.string.btn_sort)}: ${sortLabels[index]}"
    }

    private fun setupEvents() {
        binding.edtSearch.addTextChangedListener { editable ->
            keyword = editable?.toString()?.trim() ?: ""
            loadBooks()
        }

        binding.cardBannerQuote.setOnClickListener {
            setupDailyQuoteBanner(forceRefresh = true, withAnimation = true)
            startBannerAutoTransition()
        }

        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        binding.btnSort.setOnClickListener {
            showSortDialog()
        }
    }

    private fun showFilterDialog() {
        val dialogBinding = DialogCustomSelectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvDialogTitle.text = getString(R.string.dialog_filter_title)

        val categories = listOf(Pair(null, getString(R.string.filter_all))) +
                Constants.Category.ALL.mapIndexed { idx, cat ->
                    Pair(cat, requireContext().getCategoryLabels().getOrNull(idx) ?: cat)
                }

        categories.forEach { (catKey, catLabel) ->
            val isSelected = catKey == selectedCategory

            val itemBinding = ItemDialogOptionBinding.inflate(layoutInflater, dialogBinding.containerOptions, false)
            itemBinding.tvOptionText.text = catLabel

            if (isSelected) {
                itemBinding.layoutOptionItem.setBackgroundResource(R.drawable.bg_dialog_option_item_selected)
                itemBinding.ivOptionCheck.visibility = View.VISIBLE
            } else {
                itemBinding.layoutOptionItem.setBackgroundResource(R.drawable.bg_dialog_option_item)
                itemBinding.ivOptionCheck.visibility = View.GONE
            }

            itemBinding.layoutOptionItem.setOnClickListener {
                dialog.dismiss()
                selectedCategory = catKey
                updateFilterLabel()
                loadBooks()
            }
            dialogBinding.containerOptions.addView(itemBinding.root)
        }

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSortDialog() {
        val dialogBinding = DialogCustomSelectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvDialogTitle.text = getString(R.string.dialog_sort_title)

        val sortOptions = listOf(
            Pair(Constants.Sort.TITLE, getString(R.string.sort_title)),
            Pair(Constants.Sort.AUTHOR, getString(R.string.sort_author)),
            Pair(Constants.Sort.RATING, getString(R.string.sort_rating)),
            Pair(Constants.Sort.STATUS, getString(R.string.sort_status))
        )

        val currentSort = preferenceManager.sortType

        sortOptions.forEach { (code, label) ->
            val isSelected = code == currentSort

            val itemBinding = ItemDialogOptionBinding.inflate(layoutInflater, dialogBinding.containerOptions, false)
            itemBinding.tvOptionText.text = label

            if (isSelected) {
                itemBinding.layoutOptionItem.setBackgroundResource(R.drawable.bg_dialog_option_item_selected)
                itemBinding.ivOptionCheck.visibility = View.VISIBLE
            } else {
                itemBinding.layoutOptionItem.setBackgroundResource(R.drawable.bg_dialog_option_item)
                itemBinding.ivOptionCheck.visibility = View.GONE
            }

            itemBinding.layoutOptionItem.setOnClickListener {
                dialog.dismiss()
                preferenceManager.sortType = code
                updateSortLabel()
                loadBooks()
            }
            dialogBinding.containerOptions.addView(itemBinding.root)
        }

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun loadBooks() {
        val b = _binding ?: return
        if (!isAdded || context == null) return
        var books = repository.getBooks(
            keyword = keyword,
            category = selectedCategory,
            status = selectedStatus,
            sortBy = preferenceManager.sortType
        )

        if (isFavoriteFilter) {
            books = books.filter { it.rating >= 4.0f }
        }

        adapter.submitList(books)
        b.layoutEmpty.visibility =
            if (books.isEmpty()) View.VISIBLE
            else View.GONE
    }

    companion object {
        private const val BANNER_AUTO_TRANSITION_INTERVAL_MS = 15_000L

        fun newInstance() = LibraryFragment()
    }
}
