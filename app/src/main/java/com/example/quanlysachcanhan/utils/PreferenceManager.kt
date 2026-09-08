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

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Người yêu sách") ?: "Người yêu sách"
        set(value) { prefs.edit().putString(KEY_USER_NAME, value).apply() }

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "booklover@library.com") ?: "booklover@library.com"
        set(value) { prefs.edit().putString(KEY_USER_EMAIL, value).apply() }

    var userAvatarPath: String?
        get() = prefs.getString(KEY_USER_AVATAR, null)
        set(value) { prefs.edit().putString(KEY_USER_AVATAR, value).apply() }

    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC, System.currentTimeMillis())
        set(value) { prefs.edit().putLong(KEY_LAST_SYNC, value).apply() }

    var bannerQuoteContent: String?
        get() = prefs.getString(KEY_BANNER_QUOTE_CONTENT, null)
        set(value) { prefs.edit().putString(KEY_BANNER_QUOTE_CONTENT, value).apply() }

    var bannerQuoteBookTitle: String?
        get() = prefs.getString(KEY_BANNER_QUOTE_TITLE, null)
        set(value) { prefs.edit().putString(KEY_BANNER_QUOTE_TITLE, value).apply() }

    var bannerQuoteTime: Long
        get() = prefs.getLong(KEY_BANNER_QUOTE_TIME, 0L)
        set(value) { prefs.edit().putLong(KEY_BANNER_QUOTE_TIME, value).apply() }

    var hasSeededCollections: Boolean
        get() = prefs.getBoolean(KEY_HAS_SEEDED_COLLECTIONS, false)
        set(value) { prefs.edit().putBoolean(KEY_HAS_SEEDED_COLLECTIONS, value).apply() }

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
        private const val KEY_USER_NAME          = "user_name"
        private const val KEY_USER_EMAIL         = "user_email"
        private const val KEY_USER_AVATAR        = "user_avatar"
        private const val KEY_LAST_SYNC          = "last_sync"
        private const val KEY_BANNER_QUOTE_CONTENT = "banner_quote_content"
        private const val KEY_BANNER_QUOTE_TITLE   = "banner_quote_title"
        private const val KEY_BANNER_QUOTE_TIME    = "banner_quote_time"
        private const val KEY_HAS_SEEDED_COLLECTIONS = "has_seeded_collections"
    }
}
