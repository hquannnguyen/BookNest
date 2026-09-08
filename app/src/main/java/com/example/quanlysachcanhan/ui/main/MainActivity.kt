package com.example.quanlysachcanhan.ui.main

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.adapter.BookAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.QuoteRepository
import com.example.quanlysachcanhan.databinding.ActivityMainBinding
import com.example.quanlysachcanhan.databinding.DialogCustomAlertBinding
import com.example.quanlysachcanhan.databinding.DialogCustomSelectionBinding
import com.example.quanlysachcanhan.databinding.ItemDialogOptionBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.ui.addedit.AddEditBookActivity
import com.example.quanlysachcanhan.ui.detail.BookDetailActivity
import com.example.quanlysachcanhan.ui.settings.SettingsActivity
import com.example.quanlysachcanhan.ui.stats.StatisticsActivity
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.LocaleHelper
import com.example.quanlysachcanhan.utils.NavigationHelper
import com.example.quanlysachcanhan.utils.PreferenceManager
import com.example.quanlysachcanhan.utils.ReadingReminderHelper
import com.example.quanlysachcanhan.utils.getCategoryLabels

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
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
            setupDailyQuoteBanner(forceRefresh = true, withAnimation = true)
            bannerQuoteHandler.postDelayed(this, BANNER_AUTO_TRANSITION_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferenceManager = PreferenceManager(this)
        AppCompatDelegate.setDefaultNightMode(preferenceManager.nightMode)

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BookRepository(this)
        quoteRepository = QuoteRepository(this)

        setupRecyclerView()
        setupStatusChips()
        setupLanguageBadge()
        setupReminderDot()
        setupDailyQuoteBanner()
        updateFilterLabel()
        updateSortLabel()
        setupEvents()

        if (intent.getBooleanExtra(NavigationHelper.EXTRA_OPEN_FILTER, false)) {
            showFilterDialog()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(NavigationHelper.EXTRA_OPEN_FILTER, false)) {
            showFilterDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        setupLanguageBadge()
        setupReminderDot()
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

    override fun onDestroy() {
        super.onDestroy()
        stopBannerAutoTransition()
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
            val intent = Intent(this, BookDetailActivity::class.java).apply {
                putExtra(Constants.Extras.BOOK_ID, book.id)
            }
            NavigationHelper.startActivitySlide(this, intent)
        }

        // Cấu hình danh sách dạng lưới 2 cột như thiết kế Stitch
        binding.recyclerBooks.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerBooks.adapter = adapter
    }

    private fun setupDailyQuoteBanner(forceRefresh: Boolean = false, withAnimation: Boolean = false) {
        val now = System.currentTimeMillis()
        val cachedContent = preferenceManager.bannerQuoteContent
        val cachedTitle = preferenceManager.bannerQuoteBookTitle

        // Nếu không yêu cầu làm mới và đã có câu trích dẫn lưu trước đó, hiển thị ngay
        if (!forceRefresh && !cachedContent.isNullOrBlank()) {
            binding.tvDailyQuote.alpha = 1f
            binding.tvDailyQuote.translationY = 0f
            binding.tvDailyQuote.text = cachedContent
            binding.tvBannerTime.alpha = 0.75f
            binding.tvBannerTime.text = cachedTitle ?: getString(R.string.banner_quote_time)
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
            binding.tvDailyQuote.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(fadeOut)
                .withEndAction {
                    binding.tvDailyQuote.text = newContent
                    binding.tvDailyQuote.translationY = 8f
                    binding.tvDailyQuote.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(fadeIn)
                        .start()
                }.start()

            binding.tvBannerTime.animate()
                .alpha(0f)
                .setDuration(fadeOut)
                .withEndAction {
                    binding.tvBannerTime.text = newTitle
                    binding.tvBannerTime.animate()
                        .alpha(0.75f)
                        .setDuration(fadeIn)
                        .start()
                }.start()
        } else {
            binding.tvDailyQuote.alpha = 1f
            binding.tvDailyQuote.translationY = 0f
            binding.tvDailyQuote.text = newContent
            binding.tvBannerTime.alpha = 0.75f
            binding.tvBannerTime.text = newTitle
        }
    }

    private fun setupReminderDot() {
        binding.viewReminderDot.visibility =
            if (preferenceManager.reminderEnabled) View.VISIBLE else View.GONE
    }

    private fun updateChipCounts() {
        val allBooks = repository.getBooks()
        val totalCount = allBooks.size
        val unreadCount = allBooks.count { it.readingStatus == Constants.ReadingStatus.UNREAD }
        val readingCount = allBooks.count { it.readingStatus == Constants.ReadingStatus.READING }
        val readCount = allBooks.count { it.readingStatus == Constants.ReadingStatus.READ }

        binding.chipAll.text = "${getString(R.string.status_all)} ($totalCount)"
        binding.chipUnread.text = "${getString(R.string.status_unread)} ($unreadCount)"
        binding.chipReading.text = "${getString(R.string.status_reading)} ($readingCount)"
        binding.chipRead.text = "${getString(R.string.status_read)} ($readCount)"
    }

    private fun setupStatusChips() {
        val statusViews = listOf(
            Pair(binding.chipAll, null),
            Pair(binding.chipUnread, Constants.ReadingStatus.UNREAD),
            Pair(binding.chipReading, Constants.ReadingStatus.READING),
            Pair(binding.chipRead, Constants.ReadingStatus.READ)
        )

        fun resetStatusChipsUI() {
            statusViews.forEach { (view, _) ->
                view.setBackgroundResource(R.drawable.bg_chip_unselected)
                view.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
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

    private fun setupLanguageBadge() {
        if (LocaleHelper.isVietnamese()) {
            binding.ivLangFlag.setImageResource(R.drawable.ic_flag_vn)
            binding.tvLangCode.text = "VI"
        } else {
            binding.ivLangFlag.setImageResource(R.drawable.ic_flag_en)
            binding.tvLangCode.text = "EN"
        }
    }

    private fun updateFilterLabel() {
        val categoryIndex = Constants.Category.ALL.indexOf(selectedCategory)
        val label = if (categoryIndex >= 0) {
            getCategoryLabels().getOrNull(categoryIndex) ?: getString(R.string.btn_filter)
        } else {
            getString(R.string.filter_all)
        }
        binding.tvFilterLabel.text = "${getString(R.string.btn_filter)}: $label"
    }

    private fun updateSortLabel() {
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
        binding.tvSortLabel.text = "${getString(R.string.btn_sort)}: ${sortLabels[index]}"
    }

    private fun setupEvents() {
        // Ô tìm kiếm sách trong tủ sách
        binding.edtSearch.addTextChangedListener { editable ->
            keyword = editable?.toString()?.trim() ?: ""
            loadBooks()
        }

        // Chạm vào banner để đổi trích dẫn khác theo nhu cầu
        binding.cardBannerQuote.setOnClickListener {
            setupDailyQuoteBanner(forceRefresh = true, withAnimation = true)
            startBannerAutoTransition()
        }

        // Top Header: Nút ngôn ngữ
        binding.btnLanguage.setOnClickListener {
            showLanguageDialog()
        }

        // Top Header: Chuông thông báo
        binding.btnReminder.setOnClickListener {
            showNotificationCenterDialog()
        }

        // Dải nút Lọc thể loại & Sắp xếp
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        binding.btnSort.setOnClickListener {
            showSortDialog()
        }

        // Nút FAB thêm sách
        binding.fabAddBook.setOnClickListener {
            NavigationHelper.startActivitySlide(this, Intent(this, AddEditBookActivity::class.java))
        }

        // Bottom Navigation Events
        NavigationHelper.setupBottomNavigation(
            this,
            NavigationHelper.Tab.LIBRARY,
            binding.navLibrary,
            binding.navCollections,
            binding.navStatistics,
            binding.navSettings,
            onLibraryReselect = {
                binding.recyclerBooks.smoothScrollToPosition(0)
            }
        )
    }

    private fun showLanguageDialog() {
        val dialogBinding = DialogCustomSelectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvDialogTitle.text = getString(R.string.dialog_language_title)

        val currentLang = LocaleHelper.getCurrentLanguage()
        val options = listOf(
            Triple(LocaleHelper.VI, getString(R.string.lang_vietnamese) + " (VI)", R.drawable.ic_flag_vn),
            Triple(LocaleHelper.EN, getString(R.string.lang_english) + " (EN)", R.drawable.ic_flag_en)
        )

        options.forEach { (code, title, flagRes) ->
            val isSelected = (code == LocaleHelper.VI && currentLang.startsWith(LocaleHelper.VI)) ||
                    (code == LocaleHelper.EN && currentLang.startsWith(LocaleHelper.EN))

            val itemBinding = ItemDialogOptionBinding.inflate(layoutInflater, dialogBinding.containerOptions, false)
            itemBinding.ivOptionIcon.setImageResource(flagRes)
            itemBinding.ivOptionIcon.visibility = View.VISIBLE
            itemBinding.tvOptionText.text = title

            if (isSelected) {
                itemBinding.layoutOptionItem.setBackgroundResource(R.drawable.bg_dialog_option_item_selected)
                itemBinding.ivOptionCheck.visibility = View.VISIBLE
            } else {
                itemBinding.layoutOptionItem.setBackgroundResource(R.drawable.bg_dialog_option_item)
                itemBinding.ivOptionCheck.visibility = View.GONE
            }

            itemBinding.layoutOptionItem.setOnClickListener {
                dialog.dismiss()
                if (code != currentLang) {
                    LocaleHelper.setLocale(code)
                }
            }
            dialogBinding.containerOptions.addView(itemBinding.root)
        }

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showNotificationCenterDialog() {
        val dialogBinding = DialogCustomAlertBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvAlertTitle.text = getString(R.string.title_notifications)

        val isEnabled = preferenceManager.reminderEnabled
        val hour = preferenceManager.reminderHour
        val minute = preferenceManager.reminderMinute

        val message = if (isEnabled) {
            getString(R.string.notification_bell_active_msg, hour, minute)
        } else {
            getString(R.string.notification_bell_inactive_msg)
        }

        dialogBinding.tvAlertMessage.text = message
        dialogBinding.btnAlertPositive.text = getString(R.string.btn_understand)
        dialogBinding.btnAlertPositive.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSettingsMenu() {
        val dialogBinding = DialogCustomSelectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvDialogTitle.text = getString(R.string.title_settings_menu)

        val options = listOf(
            Triple(getString(R.string.menu_item_theme), "🌓", 0),
            Triple(getString(R.string.menu_item_reminder), "⏰", 1)
        )

        options.forEach { (title, emoji, actionId) ->
            val itemBinding = ItemDialogOptionBinding.inflate(layoutInflater, dialogBinding.containerOptions, false)
            itemBinding.tvOptionIcon.text = emoji
            itemBinding.tvOptionIcon.visibility = View.VISIBLE
            itemBinding.tvOptionText.text = title

            itemBinding.layoutOptionItem.setOnClickListener {
                dialog.dismiss()
                if (actionId == 0) {
                    showThemeDialog()
                } else {
                    showReminderScheduleDialog()
                }
            }
            dialogBinding.containerOptions.addView(itemBinding.root)
        }

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showReminderScheduleDialog() {
        val dialogBinding = DialogCustomAlertBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvAlertTitle.text = getString(R.string.dialog_reminder_title)

        val isEnabled = preferenceManager.reminderEnabled
        val hour = preferenceManager.reminderHour
        val minute = preferenceManager.reminderMinute

        val message = if (isEnabled) {
            getString(R.string.dialog_reminder_status_on, hour, minute)
        } else {
            getString(R.string.dialog_reminder_status_off)
        }

        dialogBinding.tvAlertMessage.text = message
        dialogBinding.btnAlertPositive.text = getString(R.string.btn_set_reminder_time)
        dialogBinding.btnAlertPositive.setOnClickListener {
            dialog.dismiss()
            showTimePicker()
        }

        if (isEnabled) {
            dialogBinding.btnAlertNegative.visibility = View.VISIBLE
            dialogBinding.spaceButtons.visibility = View.VISIBLE
            dialogBinding.btnAlertNegative.text = getString(R.string.btn_turn_off_reminder)
            dialogBinding.btnAlertNegative.setOnClickListener {
                dialog.dismiss()
                preferenceManager.reminderEnabled = false
                ReadingReminderHelper.cancelReminder(this)
                setupReminderDot()
                Toast.makeText(this, R.string.msg_reminder_disabled, Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showTimePicker() {
        val currentHour = preferenceManager.reminderHour
        val currentMinute = preferenceManager.reminderMinute

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                preferenceManager.reminderEnabled = true
                preferenceManager.reminderHour = selectedHour
                preferenceManager.reminderMinute = selectedMinute

                ReadingReminderHelper.scheduleReminder(this, selectedHour, selectedMinute)
                setupReminderDot()

                Toast.makeText(
                    this,
                    getString(R.string.msg_reminder_set, selectedHour, selectedMinute),
                    Toast.LENGTH_LONG
                ).show()
            },
            currentHour,
            currentMinute,
            true
        ).show()
    }

    private fun showThemeDialog() {
        val dialogBinding = DialogCustomSelectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvDialogTitle.text = getString(R.string.dialog_theme_title)

        val options = listOf(
            Triple(AppCompatDelegate.MODE_NIGHT_YES, getString(R.string.theme_dark), "🌙"),
            Triple(AppCompatDelegate.MODE_NIGHT_NO, getString(R.string.theme_light), "☀️"),
            Triple(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, getString(R.string.theme_system), "⚙️")
        )

        val currentNightMode = preferenceManager.nightMode

        options.forEach { (mode, title, emoji) ->
            val isSelected = mode == currentNightMode

            val itemBinding = ItemDialogOptionBinding.inflate(layoutInflater, dialogBinding.containerOptions, false)
            itemBinding.tvOptionIcon.text = emoji
            itemBinding.tvOptionIcon.visibility = View.VISIBLE
            itemBinding.tvOptionText.text = title

            if (isSelected) {
                itemBinding.layoutOptionItem.setBackgroundResource(R.drawable.bg_dialog_option_item_selected)
                itemBinding.ivOptionCheck.visibility = View.VISIBLE
            } else {
                itemBinding.layoutOptionItem.setBackgroundResource(R.drawable.bg_dialog_option_item)
                itemBinding.ivOptionCheck.visibility = View.GONE
            }

            itemBinding.layoutOptionItem.setOnClickListener {
                dialog.dismiss()
                if (mode != currentNightMode) {
                    preferenceManager.nightMode = mode
                    AppCompatDelegate.setDefaultNightMode(mode)
                    Toast.makeText(this, R.string.msg_theme_applied, Toast.LENGTH_SHORT).show()
                }
            }
            dialogBinding.containerOptions.addView(itemBinding.root)
        }

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showFilterDialog() {
        val dialogBinding = DialogCustomSelectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvDialogTitle.text = getString(R.string.dialog_filter_title)

        val categories = listOf(Pair(null, getString(R.string.filter_all))) +
                Constants.Category.ALL.mapIndexed { idx, cat ->
                    Pair(cat, getCategoryLabels().getOrNull(idx) ?: cat)
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
        val dialog = AlertDialog.Builder(this)
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

    private fun loadBooks() {
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
        binding.layoutEmpty.visibility =
            if (books.isEmpty()) View.VISIBLE
            else View.GONE
    }

    companion object {
        // Chu kỳ tự động chuyển câu trích dẫn: 15 giây (thời gian lý tưởng để đọc và cảm nhận)
        private const val BANNER_AUTO_TRANSITION_INTERVAL_MS = 15_000L
    }
}