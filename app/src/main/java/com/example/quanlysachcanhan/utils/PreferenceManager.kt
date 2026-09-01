package com.example.quanlysachcanhan.utils

import android.content.Context

class PreferenceManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    var sortType: String
        get() = prefs.getString(KEY_SORT_TYPE, Constants.Sort.TITLE)
            ?: Constants.Sort.TITLE
        set(value) {
            prefs.edit().putString(KEY_SORT_TYPE, value).apply()
        }

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
        }

    companion object {
        private const val PREF_NAME = "book_app_preferences"
        private const val KEY_SORT_TYPE = "sort_type"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
