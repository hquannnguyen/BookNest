package com.example.quanlysachcanhan.utils

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.databinding.DialogCustomAlertBinding
import com.example.quanlysachcanhan.databinding.DialogCustomSelectionBinding
import com.example.quanlysachcanhan.databinding.ItemDialogOptionBinding
import com.example.quanlysachcanhan.ui.collections.CollectionsActivity
import com.example.quanlysachcanhan.ui.main.MainActivity
import com.example.quanlysachcanhan.ui.settings.SettingsActivity
import com.example.quanlysachcanhan.ui.stats.StatisticsActivity

object NavigationHelper {

    enum class Tab {
        LIBRARY,
        COLLECTIONS,
        STATISTICS,
        SETTINGS,
        SUB_PAGE
    }

    const val EXTRA_OPEN_FILTER = "EXTRA_OPEN_FILTER"

    fun startActivitySlide(activity: Activity, intent: Intent) {
        activity.startActivity(intent)
        applySlideEnter(activity)
    }

    fun finishWithSlide(activity: Activity) {
        activity.finish()
        applySlideExit(activity)
    }

    fun applySlideEnter(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 34) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    fun applySlideExit(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 34) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    fun setupHeader(
        activity: Activity,
        btnLanguage: View?,
        ivLangFlag: ImageView?,
        tvLangCode: TextView?,
        btnReminder: View?,
        dotReminder: View?,
        preferenceManager: PreferenceManager
    ) {
        val currentLang = LocaleHelper.getCurrentLanguage()
        if (currentLang.startsWith(LocaleHelper.VI)) {
            ivLangFlag?.setImageResource(R.drawable.ic_flag_vn)
            tvLangCode?.text = "VI"
        } else {
            ivLangFlag?.setImageResource(R.drawable.ic_flag_en)
            tvLangCode?.text = "EN"
        }

        dotReminder?.visibility = if (preferenceManager.reminderEnabled) View.VISIBLE else View.GONE

        btnLanguage?.setOnClickListener {
            showLanguageDialog(activity)
        }

        btnReminder?.setOnClickListener {
            showNotificationCenterDialog(activity, preferenceManager)
        }
    }

    fun showLanguageDialog(activity: Activity) {
        val dialogBinding = DialogCustomSelectionBinding.inflate(activity.layoutInflater)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvDialogTitle.text = activity.getString(R.string.dialog_language_title)

        val currentLang = LocaleHelper.getCurrentLanguage()
        val options = listOf(
            Triple(LocaleHelper.VI, activity.getString(R.string.lang_vietnamese) + " (VI)", R.drawable.ic_flag_vn),
            Triple(LocaleHelper.EN, activity.getString(R.string.lang_english) + " (EN)", R.drawable.ic_flag_en)
        )

        options.forEach { (code, title, flagRes) ->
            val isSelected = (code == LocaleHelper.VI && currentLang.startsWith(LocaleHelper.VI)) ||
                    (code == LocaleHelper.EN && currentLang.startsWith(LocaleHelper.EN))

            val itemBinding = ItemDialogOptionBinding.inflate(activity.layoutInflater, dialogBinding.containerOptions, false)
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

    fun showNotificationCenterDialog(activity: Activity, preferenceManager: PreferenceManager) {
        val dialogBinding = DialogCustomAlertBinding.inflate(activity.layoutInflater)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvAlertTitle.text = activity.getString(R.string.title_notifications)

        val isEnabled = preferenceManager.reminderEnabled
        val hour = preferenceManager.reminderHour
        val minute = preferenceManager.reminderMinute

        val message = if (isEnabled) {
            activity.getString(R.string.notification_bell_active_msg, hour, minute)
        } else {
            activity.getString(R.string.notification_bell_inactive_msg)
        }

        dialogBinding.tvAlertMessage.text = message
        dialogBinding.btnAlertPositive.text = activity.getString(R.string.btn_understand)
        dialogBinding.btnAlertPositive.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun setupBottomNavigation(
        activity: Activity,
        currentTab: Tab,
        navLibrary: View,
        navCollections: View,
        navStatistics: View,
        navSettings: View,
        onLibraryReselect: (() -> Unit)? = null
    ) {
        navLibrary.setOnClickListener {
            if (currentTab == Tab.LIBRARY) {
                onLibraryReselect?.invoke()
            } else {
                val intent = Intent(activity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                activity.startActivity(intent)
                finishWithSlide(activity)
            }
        }

        navCollections.setOnClickListener {
            if (currentTab != Tab.COLLECTIONS) {
                val intent = Intent(activity, CollectionsActivity::class.java)
                startActivitySlide(activity, intent)
                if (currentTab != Tab.LIBRARY) {
                    activity.finish()
                }
            }
        }

        navStatistics.setOnClickListener {
            if (currentTab != Tab.STATISTICS) {
                val intent = Intent(activity, StatisticsActivity::class.java)
                startActivitySlide(activity, intent)
                if (currentTab != Tab.LIBRARY) {
                    activity.finish()
                }
            }
        }

        navSettings.setOnClickListener {
            if (currentTab != Tab.SETTINGS) {
                val intent = Intent(activity, SettingsActivity::class.java)
                startActivitySlide(activity, intent)
                if (currentTab != Tab.LIBRARY) {
                    activity.finish()
                }
            }
        }
    }
}
