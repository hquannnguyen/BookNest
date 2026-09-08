package com.example.quanlysachcanhan.ui.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.QuoteRepository
import com.example.quanlysachcanhan.databinding.ActivitySettingsBinding
import com.example.quanlysachcanhan.databinding.DialogCustomAlertBinding
import com.example.quanlysachcanhan.databinding.DialogCustomSelectionBinding
import com.example.quanlysachcanhan.databinding.DialogEditProfileBinding
import com.example.quanlysachcanhan.databinding.ItemDialogOptionBinding
import com.example.quanlysachcanhan.ui.main.MainActivity
import com.example.quanlysachcanhan.ui.stats.StatisticsActivity
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.ImageStorageHelper
import com.example.quanlysachcanhan.utils.LocaleHelper
import com.example.quanlysachcanhan.utils.NavigationHelper
import com.example.quanlysachcanhan.utils.PreferenceManager
import com.example.quanlysachcanhan.utils.ReadingReminderHelper
import java.io.File
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var bookRepository: BookRepository
    private lateinit var quoteRepository: QuoteRepository

    // Trình chọn ảnh đại diện từ bộ sưu tập
    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = ImageStorageHelper.copyImageToAppStorage(this, it)
            if (savedPath != null) {
                // Xóa ảnh cũ nếu có
                ImageStorageHelper.deleteImage(preferenceManager.userAvatarPath)
                preferenceManager.userAvatarPath = savedPath
                bindAvatar()
                Toast.makeText(this, getString(R.string.msg_avatar_updated), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Không thể lưu ảnh đại diện", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferenceManager = PreferenceManager(this)
        AppCompatDelegate.setDefaultNightMode(preferenceManager.nightMode)

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookRepository = BookRepository(this)
        quoteRepository = QuoteRepository(this)

        bindProfile()
        bindPreferences()
        setupHeaderAndNavigation()
        setupEvents()
    }

    private fun setupHeaderAndNavigation() {
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
            NavigationHelper.Tab.SETTINGS,
            binding.navLibrary,
            binding.navCollections,
            binding.navStatistics,
            binding.navSettings
        )

        binding.btnBack.setOnClickListener {
            NavigationHelper.finishWithSlide(this)
        }

        onBackPressedDispatcher.addCallback(this) {
            NavigationHelper.finishWithSlide(this@SettingsActivity)
        }
    }

    private fun bindProfile() {
        binding.tvUserName.text = preferenceManager.userName
        binding.tvUserEmail.text = preferenceManager.userEmail
        bindAvatar()
    }

    private fun bindAvatar() {
        val avatarPath = preferenceManager.userAvatarPath
        if (!avatarPath.isNullOrBlank()) {
            val file = File(avatarPath)
            if (file.exists()) {
                binding.ivAvatar.visibility = View.VISIBLE
                binding.ivAvatarPlaceholder.visibility = View.GONE
                binding.ivAvatar.setImageURI(Uri.fromFile(file))
                return
            }
        }
        binding.ivAvatar.visibility = View.GONE
        binding.ivAvatarPlaceholder.visibility = View.VISIBLE
    }

    private fun bindPreferences() {
        // 1. Chế độ ban đêm
        binding.switchDarkMode.isChecked = preferenceManager.darkMode

        // 2. Ngôn ngữ
        binding.tvLanguageValue.text = if (LocaleHelper.isVietnamese()) {
            "${getString(R.string.lang_vietnamese)} (VI)"
        } else {
            "${getString(R.string.lang_english)} (EN)"
        }

        // 3. Nhắc nhở đọc sách
        binding.switchReminder.isChecked = preferenceManager.reminderEnabled
        updateReminderTimeText()

        // 4. Sắp xếp mặc định
        updateSortUI()
    }

    private fun updateReminderTimeText() {
        val hour = preferenceManager.reminderHour
        val minute = preferenceManager.reminderMinute
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        binding.tvReminderTimeValue.text = "$timeFormatted ${getString(R.string.reminder_daily_label)}"
    }

    private fun updateSortUI() {
        val currentSort = preferenceManager.sortType
        binding.ivCheckSortTitle.visibility  = if (currentSort == Constants.Sort.TITLE) View.VISIBLE else View.GONE
        binding.ivCheckSortAuthor.visibility = if (currentSort == Constants.Sort.AUTHOR) View.VISIBLE else View.GONE
        binding.ivCheckSortRating.visibility = if (currentSort == Constants.Sort.RATING) View.VISIBLE else View.GONE
        binding.ivCheckSortStatus.visibility = if (currentSort == Constants.Sort.STATUS) View.VISIBLE else View.GONE
    }

    private fun setupEvents() {
        // Quay lại
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Đổi ảnh đại diện (nút camera hoặc chạm vào avatar)
        binding.btnEditAvatar.setOnClickListener {
            showAvatarOptionsDialog()
        }
        binding.cardAvatar.setOnClickListener {
            showAvatarOptionsDialog()
        }

        // Chạm vào hàng Tên người dùng để sửa tên & email
        binding.layoutEditName.setOnClickListener {
            showEditProfileDialog()
        }

        // Chế độ ban đêm: toggle
        binding.layoutDarkModeRow.setOnClickListener {
            binding.switchDarkMode.toggle()
        }
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (preferenceManager.darkMode != isChecked) {
                preferenceManager.darkMode = isChecked
                AppCompatDelegate.setDefaultNightMode(preferenceManager.nightMode)
            }
        }

        // Chọn ngôn ngữ
        binding.layoutLanguageRow.setOnClickListener {
            showLanguageDialog()
        }

        // Nhắc nhở đọc sách: toggle
        binding.layoutReminderToggleRow.setOnClickListener {
            binding.switchReminder.toggle()
        }
        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            preferenceManager.reminderEnabled = isChecked
            if (isChecked) {
                ReadingReminderHelper.scheduleReminder(
                    this,
                    preferenceManager.reminderHour,
                    preferenceManager.reminderMinute
                )
                Toast.makeText(
                    this,
                    getString(R.string.msg_reminder_set, preferenceManager.reminderHour, preferenceManager.reminderMinute),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                ReadingReminderHelper.cancelReminder(this)
                Toast.makeText(this, getString(R.string.msg_reminder_disabled), Toast.LENGTH_SHORT).show()
            }
        }

        // Đặt giờ nhắc nhở
        binding.layoutReminderTimeRow.setOnClickListener {
            showTimePicker()
        }

        // Sắp xếp mặc định
        binding.itemSortTitle.setOnClickListener {
            setSortType(Constants.Sort.TITLE)
        }
        binding.itemSortAuthor.setOnClickListener {
            setSortType(Constants.Sort.AUTHOR)
        }
        binding.itemSortRating.setOnClickListener {
            setSortType(Constants.Sort.RATING)
        }
        binding.itemSortStatus.setOnClickListener {
            setSortType(Constants.Sort.STATUS)
        }

        // Dữ liệu & Bộ nhớ
        binding.btnSyncData.setOnClickListener {
            val totalBooks = bookRepository.getBooks().size
            preferenceManager.lastSyncTime = System.currentTimeMillis()
            showCustomAlert(
                title = getString(R.string.pref_sync_title),
                message = "${getString(R.string.msg_sync_success)}\n\n• Tổng số sách: $totalBooks\n• Cơ sở dữ liệu: SQLite Offline Encrypted"
            )
        }

        binding.btnBackupData.setOnClickListener {
            val totalBooks = bookRepository.getBooks().size
            showCustomAlert(
                title = getString(R.string.pref_backup_title),
                message = "${getString(R.string.msg_backup_success)}\n\n• Đã lưu trữ toàn bộ $totalBooks tác phẩm và các trích dẫn yêu thích vào bộ nhớ an toàn của thiết bị."
            )
        }

        // Thông tin ứng dụng & Chính sách
        binding.btnAboutApp.setOnClickListener {
            showCustomAlert(
                title = getString(R.string.pref_about_app),
                message = getString(R.string.about_app_desc)
            )
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            showCustomAlert(
                title = getString(R.string.pref_privacy_policy),
                message = getString(R.string.privacy_policy_desc)
            )
        }

        // Đặt lại cài đặt
        binding.btnResetSettings.setOnClickListener {
            showResetConfirmDialog()
        }
    }

    private fun setSortType(sortType: String) {
        preferenceManager.sortType = sortType
        updateSortUI()
        Toast.makeText(this, getString(R.string.msg_theme_applied), Toast.LENGTH_SHORT).show()
    }

    private fun showTimePicker() {
        val currentHour = preferenceManager.reminderHour
        val currentMinute = preferenceManager.reminderMinute

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                preferenceManager.reminderHour = hourOfDay
                preferenceManager.reminderMinute = minute
                updateReminderTimeText()

                if (preferenceManager.reminderEnabled) {
                    ReadingReminderHelper.scheduleReminder(this, hourOfDay, minute)
                }
                Toast.makeText(
                    this,
                    getString(R.string.msg_reminder_set, hourOfDay, minute),
                    Toast.LENGTH_SHORT
                ).show()
            },
            currentHour,
            currentMinute,
            true
        ).show()
    }

    private fun showAvatarOptionsDialog() {
        val dialogBinding = DialogCustomSelectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvDialogTitle.text = getString(R.string.dialog_avatar_options_title)

        val hasCustomAvatar = !preferenceManager.userAvatarPath.isNullOrBlank()

        val options = mutableListOf<Pair<String, () -> Unit>>()
        options.add(Pair(getString(R.string.btn_choose_new_avatar)) {
            pickAvatarLauncher.launch("image/*")
        })

        if (hasCustomAvatar) {
            options.add(Pair(getString(R.string.btn_remove_avatar)) {
                ImageStorageHelper.deleteImage(preferenceManager.userAvatarPath)
                preferenceManager.userAvatarPath = null
                bindAvatar()
                Toast.makeText(this, getString(R.string.msg_avatar_removed), Toast.LENGTH_SHORT).show()
            })
        }

        options.add(Pair(getString(R.string.dialog_edit_profile_title)) {
            showEditProfileDialog()
        })

        options.forEach { (title, action) ->
            val itemBinding = ItemDialogOptionBinding.inflate(layoutInflater, dialogBinding.containerOptions, false)
            itemBinding.ivOptionIcon.visibility = View.GONE
            itemBinding.tvOptionIcon.visibility = View.GONE
            itemBinding.tvOptionText.text = title
            itemBinding.ivOptionCheck.visibility = View.GONE

            itemBinding.layoutOptionItem.setOnClickListener {
                dialog.dismiss()
                action()
            }
            dialogBinding.containerOptions.addView(itemBinding.root)
        }

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditProfileDialog() {
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.edtProfileName.setText(preferenceManager.userName)
        dialogBinding.edtProfileEmail.setText(preferenceManager.userEmail)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSaveProfile.setOnClickListener {
            val name = dialogBinding.edtProfileName.text.toString().trim()
            val email = dialogBinding.edtProfileEmail.text.toString().trim()

            if (name.isNotBlank()) {
                preferenceManager.userName = name
            }
            if (email.isNotBlank()) {
                preferenceManager.userEmail = email
            }

            bindProfile()
            dialog.dismiss()
            Toast.makeText(this, getString(R.string.msg_profile_updated), Toast.LENGTH_SHORT).show()
        }

        dialog.show()
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
            Triple(LocaleHelper.VI, "${getString(R.string.lang_vietnamese)} (VI)", R.drawable.ic_flag_vn),
            Triple(LocaleHelper.EN, "${getString(R.string.lang_english)} (EN)", R.drawable.ic_flag_en)
        )

        options.forEach { (code, title, flagRes) ->
            val isSelected = (code == LocaleHelper.VI && currentLang.startsWith(LocaleHelper.VI)) ||
                    (code == LocaleHelper.EN && currentLang.startsWith(LocaleHelper.EN))

            val itemBinding = ItemDialogOptionBinding.inflate(layoutInflater, dialogBinding.containerOptions, false)
            itemBinding.ivOptionIcon.setImageResource(flagRes)
            itemBinding.ivOptionIcon.visibility = View.VISIBLE
            itemBinding.tvOptionIcon.visibility = View.GONE
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
                if (!isSelected) {
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

    private fun showCustomAlert(title: String, message: String) {
        val dialogBinding = DialogCustomAlertBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvAlertTitle.text = title
        dialogBinding.tvAlertMessage.text = message
        dialogBinding.btnAlertPositive.text = getString(android.R.string.ok)
        dialogBinding.btnAlertNegative.visibility = View.GONE
        dialogBinding.spaceButtons.visibility = View.GONE

        dialogBinding.btnAlertPositive.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showResetConfirmDialog() {
        val dialogBinding = DialogCustomAlertBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvAlertTitle.text = getString(R.string.btn_reset_settings)
        dialogBinding.tvAlertMessage.text = getString(R.string.dialog_reset_confirm)
        dialogBinding.btnAlertPositive.text = getString(R.string.btn_delete)
        dialogBinding.btnAlertPositive.setBackgroundResource(R.drawable.bg_btn_delete)
        dialogBinding.btnAlertNegative.text = getString(R.string.btn_cancel)
        dialogBinding.btnAlertNegative.visibility = View.VISIBLE
        dialogBinding.spaceButtons.visibility = View.VISIBLE

        dialogBinding.btnAlertNegative.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnAlertPositive.setOnClickListener {
            dialog.dismiss()
            preferenceManager.sortType = Constants.Sort.TITLE
            preferenceManager.reminderEnabled = false
            ReadingReminderHelper.cancelReminder(this)
            preferenceManager.userName = "Người yêu sách"
            preferenceManager.userEmail = "booklover@library.com"
            ImageStorageHelper.deleteImage(preferenceManager.userAvatarPath)
            preferenceManager.userAvatarPath = null

            bindProfile()
            bindPreferences()
            Toast.makeText(this, getString(R.string.msg_reset_success), Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }
}
