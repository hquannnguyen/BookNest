package com.example.quanlysachcanhan.utils

import android.content.Context
import com.example.quanlysachcanhan.R

/**
 * Extension functions để map mã DB → chuỗi UI đã dịch.
 *
 * Quy tắc:
 *  - DB luôn lưu mã hằng số (UNREAD, SCIENCE, ...)
 *  - UI luôn hiển thị qua getXxxLabel() để i18n hoạt động đúng
 *  - KHÔNG đưa trực tiếp book.readingStatus hoặc book.category lên TextView
 *
 * Ví dụ:
 *   tvStatus.text = context.getReadingStatusLabel(book.readingStatus)
 *   tvCategory.text = context.getCategoryLabel(book.category)
 */

fun Context.getReadingStatusLabel(status: String): String {
    return when (status) {
        Constants.ReadingStatus.UNREAD  -> getString(R.string.status_unread)
        Constants.ReadingStatus.READING -> getString(R.string.status_reading)
        Constants.ReadingStatus.READ    -> getString(R.string.status_read)
        else -> status
    }
}

fun Context.getCategoryLabel(category: String): String {
    return when (category) {
        Constants.Category.SCIENCE              -> getString(R.string.category_science)
        Constants.Category.LITERATURE           -> getString(R.string.category_literature)
        Constants.Category.ECONOMICS            -> getString(R.string.category_economics)
        Constants.Category.CHILDREN             -> getString(R.string.category_children)
        Constants.Category.LIFE_SKILLS          -> getString(R.string.category_life_skills)
        Constants.Category.INFORMATION_TECHNOLOGY -> getString(R.string.category_it)
        Constants.Category.PSYCHOLOGY           -> getString(R.string.category_psychology)
        Constants.Category.HISTORY              -> getString(R.string.category_history)
        Constants.Category.OTHER                -> getString(R.string.category_other)
        else -> category
    }
}

/**
 * Ngược lại: từ label đã dịch (Spinner chọn) → mã DB để lưu.
 * Dùng khi save sách từ AddEditBookActivity.
 */
fun Context.getCategoryCode(label: String): String {
    return when (label) {
        getString(R.string.category_science)    -> Constants.Category.SCIENCE
        getString(R.string.category_literature) -> Constants.Category.LITERATURE
        getString(R.string.category_economics)  -> Constants.Category.ECONOMICS
        getString(R.string.category_children)   -> Constants.Category.CHILDREN
        getString(R.string.category_life_skills)-> Constants.Category.LIFE_SKILLS
        getString(R.string.category_it)         -> Constants.Category.INFORMATION_TECHNOLOGY
        getString(R.string.category_psychology) -> Constants.Category.PSYCHOLOGY
        getString(R.string.category_history)    -> Constants.Category.HISTORY
        getString(R.string.category_other)      -> Constants.Category.OTHER
        else -> label
    }
}

fun Context.getReadingStatusCode(label: String): String {
    return when (label) {
        getString(R.string.status_unread)  -> Constants.ReadingStatus.UNREAD
        getString(R.string.status_reading) -> Constants.ReadingStatus.READING
        getString(R.string.status_read)    -> Constants.ReadingStatus.READ
        else -> label
    }
}

/**
 * Lấy danh sách labels đã dịch theo thứ tự của Constants.Category.ALL.
 * Dùng cho Spinner category.
 */
fun Context.getCategoryLabels(): List<String> = listOf(
    getString(R.string.category_science),
    getString(R.string.category_literature),
    getString(R.string.category_economics),
    getString(R.string.category_children),
    getString(R.string.category_life_skills),
    getString(R.string.category_it),
    getString(R.string.category_psychology),
    getString(R.string.category_history),
    getString(R.string.category_other)
)

/**
 * Lấy danh sách labels đã dịch theo thứ tự của Constants.ReadingStatus.ALL.
 * Dùng cho Spinner status.
 */
fun Context.getReadingStatusLabels(): List<String> = listOf(
    getString(R.string.status_unread),
    getString(R.string.status_reading),
    getString(R.string.status_read)
)