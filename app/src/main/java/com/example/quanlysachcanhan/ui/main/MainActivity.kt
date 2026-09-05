package com.example.quanlysachcanhan.ui.main

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.adapter.BookAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.databinding.ActivityMainBinding
import com.example.quanlysachcanhan.databinding.DialogCustomAlertBinding
import com.example.quanlysachcanhan.databinding.DialogCustomSelectionBinding
import com.example.quanlysachcanhan.databinding.ItemDialogOptionBinding
import com.example.quanlysachcanhan.ui.addedit.AddEditBookActivity
import com.example.quanlysachcanhan.ui.detail.BookDetailActivity
import com.example.quanlysachcanhan.ui.stats.StatisticsActivity
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.LocaleHelper
import com.example.quanlysachcanhan.utils.PreferenceManager
import com.example.quanlysachcanhan.utils.ReadingReminderHelper
import com.example.quanlysachcanhan.utils.getCategoryLabels

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: BookRepository
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: BookAdapter

    private var keyword: String = ""
    private var selectedCategory: String? = null
    private var selectedStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        preferenceManager = PreferenceManager(this)
        AppCompatDelegate.setDefaultNightMode(preferenceManager.nightMode)

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BookRepository(this)

        setupRecyclerView()
        setupStatusChips()
        setupLanguageBadge()
        updateFilterLabel()
        updateSortLabel()
        setupEvents()
    }

    override fun onResume() {
        super.onResume()
        setupLanguageBadge()
        updateFilterLabel()
        updateSortLabel()
        loadBooks()
    }

    private fun setupRecyclerView() {
        adapter = BookAdapter { book ->
            startActivity(
                Intent(this, BookDetailActivity::class.java)
                    .putExtra(Constants.Extras.BOOK_ID, book.id)
            )
        }

        binding.recyclerBooks.layoutManager = LinearLayoutManager(this)
        binding.recyclerBooks.adapter = adapter
    }

    private fun setupStatusChips() {
        val chips = listOf(
            Triple(binding.chipAll, null, true),
            Triple(binding.chipReading, Constants.ReadingStatus.READING, false),
            Triple(binding.chipRead, Constants.ReadingStatus.READ, false),
            Triple(binding.chipUnread, Constants.ReadingStatus.UNREAD, false)
        )

        fun updateChipsUI(selectedView: TextView) {
            chips.forEach { (view, _, _) ->
                if (view == selectedView) {
                    view.setBackgroundResource(R.drawable.bg_chip_selected)
                    view.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                } else {
                    view.setBackgroundResource(R.drawable.bg_chip_unselected)
                    view.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
            }
        }

        chips.forEach { (view, status, _) ->
            view.setOnClickListener {
                selectedStatus = status
                updateChipsUI(view)
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
        binding.edtSearch.addTextChangedListener {
            keyword = it?.toString().orEmpty()
            loadBooks()
        }

        // Top Header: Nút ngôn ngữ mở dialog chọn ngôn ngữ
        binding.btnLanguage.setOnClickListener {
            showLanguageDialog()
        }

        // Top Header: Chuông chỉ để xem thông báo
        binding.btnReminder.setOnClickListener {
            showNotificationCenterDialog()
        }

        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        binding.btnSort.setOnClickListener {
            showSortDialog()
        }

        binding.fabAddBook.setOnClickListener {
            startActivity(Intent(this, AddEditBookActivity::class.java))
        }

        // Bottom Navigation Events
        binding.navLibrary.setOnClickListener {
            binding.recyclerBooks.smoothScrollToPosition(0)
        }

        binding.navSearch.setOnClickListener {
            binding.edtSearch.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(binding.edtSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        binding.navStatistics.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        // Bottom Navigation: Cài đặt mở Menu gồm Dark/Light mode & Lập lịch thông báo
        binding.navSettings.setOnClickListener {
            showSettingsMenu()
        }
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
        val books = repository.getBooks(
            keyword = keyword,
            category = selectedCategory,
            status = selectedStatus,
            sortBy = preferenceManager.sortType
        )
        adapter.submitList(books)
        binding.layoutEmpty.visibility =
            if (books.isEmpty()) View.VISIBLE
            else View.GONE
    }
}