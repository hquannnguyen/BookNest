package com.example.quanlysachcanhan.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

class PreferenceManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var sortType: String
        get() = prefs.getString(KEY_SORT_TYPE, Constants.Sort.TITLE) ?: Constants.Sort.TITLE
        set(value) { prefs.edit().putString(KEY_SORT_TYPE, value).apply() }

    var nightMode: Int
        get() = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES)
        set(value) { prefs.edit().putInt(KEY_NIGHT_MODE, value).apply() }

    var darkMode: Boolean
        get() = nightMode == AppCompatDelegate.MODE_NIGHT_YES
        set(value) {
            nightMode = if (value) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        }

    var reminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply() }

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 20)
        set(value) { prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply() }

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        set(value) { prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply() }

    // language: KHONG luu o day.
    // AppCompatDelegate.getApplicationLocales() la source of truth duy nhat.
    // Dung LocaleHelper.setLocale() / LocaleHelper.getCurrentLanguage() de doc/ghi.

    companion object {
        private const val PREF_NAME              = "book_app_preferences"
        private const val KEY_SORT_TYPE          = "sort_type"
        private const val KEY_NIGHT_MODE         = "night_mode"
        private const val KEY_REMINDER_ENABLED   = "reminder_enabled"
        private const val KEY_REMINDER_HOUR      = "reminder_hour"
        private const val KEY_REMINDER_MINUTE    = "reminder_minute"
    }
}
