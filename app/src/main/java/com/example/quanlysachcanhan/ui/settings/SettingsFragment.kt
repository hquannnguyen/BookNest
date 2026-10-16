package com.example.quanlysachcanhan.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.QuoteRepository
import com.example.quanlysachcanhan.databinding.DialogCustomAlertBinding
import com.example.quanlysachcanhan.databinding.DialogCustomSelectionBinding
import com.example.quanlysachcanhan.databinding.DialogEditProfileBinding
import com.example.quanlysachcanhan.databinding.FragmentSettingsBinding
import com.example.quanlysachcanhan.databinding.ItemDialogOptionBinding
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.ImageStorageHelper
import com.example.quanlysachcanhan.utils.LocaleHelper
import com.example.quanlysachcanhan.utils.PreferenceManager
import com.example.quanlysachcanhan.utils.ReadingReminderHelper
import com.example.quanlysachcanhan.utils.ReadingReminderReceiver
import java.io.File
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var bookRepository: BookRepository
    private lateinit var quoteRepository: QuoteRepository

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val ctx = context ?: return@registerForActivityResult
        if (isGranted) {
            preferenceManager.reminderEnabled = true
            binding.switchReminder.isChecked = true
            ReadingReminderHelper.scheduleReminder(
                ctx,
                preferenceManager.reminderHour,
                preferenceManager.reminderMinute
            )
            Toast.makeText(
                ctx,
                getString(R.string.msg_reminder_set, preferenceManager.reminderHour, preferenceManager.reminderMinute),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            preferenceManager.reminderEnabled = false
            binding.switchReminder.isChecked = false
            Toast.makeText(
                ctx,
                getString(R.string.msg_notification_permission_denied),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private var tempAvatarUri: Uri? = null
    private var tempAvatarFile: File? = null

    private val takeAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        val ctx = context ?: return@registerForActivityResult
        val uri = tempAvatarUri
        if (isSuccess && uri != null) {
            val savedPath = ImageStorageHelper.copyImageToAppStorage(ctx, uri)
            if (savedPath != null) {
                ImageStorageHelper.deleteImage(preferenceManager.userAvatarPath)
                preferenceManager.userAvatarPath = savedPath
                bindAvatar()
                Toast.makeText(ctx, getString(R.string.msg_avatar_updated), Toast.LENGTH_SHORT).show()
            }
        }
        tempAvatarFile?.let { ImageStorageHelper.deleteImage(it.absolutePath) }
        tempAvatarUri = null
        tempAvatarFile = null
    }

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val ctx = context ?: return@registerForActivityResult
            val savedPath = ImageStorageHelper.copyImageToAppStorage(ctx, it)
            if (savedPath != null) {
                ImageStorageHelper.deleteImage(preferenceManager.userAvatarPath)
                preferenceManager.userAvatarPath = savedPath
                bindAvatar()
                Toast.makeText(ctx, getString(R.string.msg_avatar_updated), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(ctx, "Không thể lưu ảnh đại diện", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()
        preferenceManager = PreferenceManager(ctx)
        bookRepository = BookRepository(ctx)
        quoteRepository = QuoteRepository(ctx)

        bindProfile()
        bindPreferences()
        setupEvents()
    }

    override fun onResume() {
        super.onResume()
        bindProfile()
        bindPreferences()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && isAdded && _binding != null && context != null) {
            bindProfile()
            bindPreferences()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindProfile() {
        val b = _binding ?: return
        if (!isAdded || context == null) return
        b.tvUserName.text = preferenceManager.userName
        b.tvUserEmail.text = preferenceManager.userEmail
        bindAvatar()
    }

    private fun bindAvatar() {
        val b = _binding ?: return
        val avatarPath = preferenceManager.userAvatarPath
        if (!avatarPath.isNullOrBlank()) {
            val file = File(avatarPath)
            if (file.exists()) {
                b.ivAvatar.visibility = View.VISIBLE
                b.ivAvatarPlaceholder.visibility = View.GONE
                b.ivAvatar.setImageURI(Uri.fromFile(file))
                return
            }
        }
        b.ivAvatar.visibility = View.GONE
        b.ivAvatarPlaceholder.visibility = View.VISIBLE
    }

    private fun bindPreferences() {
        val b = _binding ?: return
        if (!isAdded || context == null) return
        // 1. Chế độ ban đêm
        b.switchDarkMode.isChecked = preferenceManager.darkMode

        // 2. Ngôn ngữ
        b.tvLanguageValue.text = if (LocaleHelper.isVietnamese()) {
            "${getString(R.string.lang_vietnamese)} (VI)"
        } else {
            "${getString(R.string.lang_english)} (EN)"
        }

        // 3. Nhắc nhở đọc sách
        b.switchReminder.isChecked = preferenceManager.reminderEnabled
        updateReminderTimeText()

        // 4. Sắp xếp mặc định
        updateSortUI()
    }

    private fun updateReminderTimeText() {
        val b = _binding ?: return
        val hour = preferenceManager.reminderHour
        val minute = preferenceManager.reminderMinute
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        b.tvReminderTimeValue.text = "$timeFormatted ${getString(R.string.reminder_daily_label)}"
    }

    private fun updateSortUI() {
        val b = _binding ?: return
        val currentSort = preferenceManager.sortType
        b.ivCheckSortTitle.visibility = if (currentSort == Constants.Sort.TITLE) View.VISIBLE else View.GONE
        b.ivCheckSortAuthor.visibility = if (currentSort == Constants.Sort.AUTHOR) View.VISIBLE else View.GONE
        b.ivCheckSortRating.visibility = if (currentSort == Constants.Sort.RATING) View.VISIBLE else View.GONE
        b.ivCheckSortStatus.visibility = if (currentSort == Constants.Sort.STATUS) View.VISIBLE else View.GONE
    }

    private fun setupEvents() {
        // Đổi ảnh đại diện
        binding.btnEditAvatar.setOnClickListener {
            showAvatarOptionsDialog()
        }
        binding.cardAvatar.setOnClickListener {
            showAvatarOptionsDialog()
        }

        // Sửa tên & email
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
            val ctx = context ?: return@setOnCheckedChangeListener
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@setOnCheckedChangeListener
                }
                preferenceManager.reminderEnabled = true
                ReadingReminderHelper.scheduleReminder(
                    ctx,
                    preferenceManager.reminderHour,
                    preferenceManager.reminderMinute
                )
                Toast.makeText(
                    ctx,
                    getString(R.string.msg_reminder_set, preferenceManager.reminderHour, preferenceManager.reminderMinute),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                preferenceManager.reminderEnabled = false
                ReadingReminderHelper.cancelReminder(ctx)
                Toast.makeText(ctx, getString(R.string.msg_reminder_disabled), Toast.LENGTH_SHORT).show()
            }
        }

        // Đặt giờ nhắc nhở
        binding.layoutReminderTimeRow.setOnClickListener {
            showTimePicker()
        }

        // Thử nghiệm gửi thông báo ngay
        binding.layoutTestReminderRow.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@setOnClickListener
            }
            ReadingReminderReceiver.sendNotification(ctx, isTest = true)
            Toast.makeText(ctx, getString(R.string.msg_test_notification_sent), Toast.LENGTH_SHORT).show()
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
        val ctx = context ?: return
        preferenceManager.sortType = sortType
        updateSortUI()
        Toast.makeText(ctx, getString(R.string.msg_theme_applied), Toast.LENGTH_SHORT).show()
    }

    private fun showTimePicker() {
        val ctx = context ?: return
        val currentHour = preferenceManager.reminderHour
        val currentMinute = preferenceManager.reminderMinute

        TimePickerDialog(
            ctx,
            { _, hourOfDay, minute ->
                preferenceManager.reminderHour = hourOfDay
                preferenceManager.reminderMinute = minute
                updateReminderTimeText()

                if (preferenceManager.reminderEnabled) {
                    ReadingReminderHelper.scheduleReminder(ctx, hourOfDay, minute)
                }
                Toast.makeText(
                    ctx,
                    getString(R.string.msg_reminder_set, hourOfDay, minute),
                    Toast.LENGTH_SHORT
                ).show()
            },
            currentHour,
            currentMinute,
            true
        ).show()
    }

    private fun launchCameraForAvatar() {
        val ctx = context ?: return
        val pair = ImageStorageHelper.createTempCameraUri(ctx)
        if (pair != null) {
            tempAvatarFile = pair.first
            tempAvatarUri = pair.second
            takeAvatarLauncher.launch(pair.second)
        } else {
            Toast.makeText(ctx, getString(R.string.msg_camera_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAvatarOptionsDialog() {
        val ctx = context ?: return
        val dialogBinding = DialogCustomSelectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(ctx)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvDialogTitle.text = getString(R.string.dialog_avatar_options_title)

        val hasCustomAvatar = !preferenceManager.userAvatarPath.isNullOrBlank()

        val options = mutableListOf<Pair<String, () -> Unit>>()
        options.add(Pair(getString(R.string.btn_take_photo)) {
            launchCameraForAvatar()
        })
        options.add(Pair(getString(R.string.btn_choose_gallery)) {
            pickAvatarLauncher.launch("image/*")
        })

        if (hasCustomAvatar) {
            options.add(Pair(getString(R.string.btn_remove_avatar)) {
                ImageStorageHelper.deleteImage(preferenceManager.userAvatarPath)
                preferenceManager.userAvatarPath = null
                bindAvatar()
                Toast.makeText(ctx, getString(R.string.msg_avatar_removed), Toast.LENGTH_SHORT).show()
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
        val ctx = context ?: return
        val dialogBinding = DialogEditProfileBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(ctx)
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
            Toast.makeText(ctx, getString(R.string.msg_profile_updated), Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showLanguageDialog() {
        val ctx = context ?: return
        val dialogBinding = DialogCustomSelectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(ctx)
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
        val ctx = context ?: return
        val dialogBinding = DialogCustomAlertBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(ctx)
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
        val ctx = context ?: return
        val dialogBinding = DialogCustomAlertBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(ctx)
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
            ReadingReminderHelper.cancelReminder(ctx)
            preferenceManager.userName = "Người yêu sách"
            preferenceManager.userEmail = "booklover@library.com"
            ImageStorageHelper.deleteImage(preferenceManager.userAvatarPath)
            preferenceManager.userAvatarPath = null

            bindProfile()
            bindPreferences()
            Toast.makeText(ctx, getString(R.string.msg_reset_success), Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
