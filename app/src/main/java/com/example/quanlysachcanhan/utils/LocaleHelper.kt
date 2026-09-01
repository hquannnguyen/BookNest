package com.example.quanlysachcanhan.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Single source of truth cho ngon ngu: AppCompatDelegate.
 *
 * - setLocale()          -> ghi vao AppCompatDelegate (Android tu persist)
 * - getCurrentLanguage() -> doc tu AppCompatDelegate
 *
 * KHONG luu language vao SharedPreferences de tranh 2 nguon trang thai lech nhau.
 */
object LocaleHelper {

    const val VI = "vi"
    const val EN = "en"

    /**
     * Doi ngon ngu toan app.
     * Android tu recreate Activity lien quan, khong can goi recreate() thu cong.
     */
    fun setLocale(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageCode)
        )
    }

    /**
     * Lay language code hien tai tu AppCompatDelegate.
     * Tra ve "vi" neu chua set (mac dinh).
     */
    fun getCurrentLanguage(): String {
        return AppCompatDelegate
            .getApplicationLocales()
            .toLanguageTags()
            .ifBlank { VI }
    }

    /** Tien ich: tra ve true neu dang dung tieng Viet. */
    fun isVietnamese(): Boolean = getCurrentLanguage().startsWith(VI)
}
