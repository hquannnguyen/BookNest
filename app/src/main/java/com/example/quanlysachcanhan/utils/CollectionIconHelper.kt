package com.example.quanlysachcanhan.utils

import com.example.quanlysachcanhan.R

object CollectionIconHelper {
    data class IconItem(val name: String, val label: String, val resId: Int)

    val availableIcons = listOf(
        IconItem("auto_stories", "Sách mở", R.drawable.ic_auto_stories),
        IconItem("psychology", "Tư duy", R.drawable.ic_psychology),
        IconItem("trending_up", "Phát triển", R.drawable.ic_trending_up),
        IconItem("account_balance", "Tài chính", R.drawable.ic_account_balance),
        IconItem("favorite", "Yêu thích", R.drawable.ic_favorite),
        IconItem("science", "Khoa học", R.drawable.ic_science),
        IconItem("book_open", "Đọc sách", R.drawable.ic_book_open),
        IconItem("collections_bookmark", "Bộ sưu tập", R.drawable.ic_collections_bookmark),
        IconItem("star", "Đặc sắc", R.drawable.ic_star),
        IconItem("category", "Thể loại", R.drawable.ic_category),
        IconItem("folder_special", "Thư mục", R.drawable.ic_folder_special),
        IconItem("schedule", "Thời gian", R.drawable.ic_schedule),
        IconItem("auto_awesome", "Cảm hứng", R.drawable.ic_auto_awesome),
        IconItem("bar_chart", "Thống kê", R.drawable.ic_bar_chart),
        IconItem("pie_chart", "Biểu đồ", R.drawable.ic_pie_chart),
        IconItem("bookmark", "Dấu trang", R.drawable.ic_bookmark_border),
        IconItem("security", "Kỷ luật", R.drawable.ic_security),
        IconItem("notifications", "Nhắc nhở", R.drawable.ic_notifications),
        IconItem("quote", "Trích dẫn", R.drawable.ic_format_quote),
        IconItem("edit_note", "Ghi chép", R.drawable.ic_edit_note)
    )

    fun getIconResource(iconName: String): Int {
        return availableIcons.find { it.name == iconName }?.resId ?: when (iconName) {
            "book_open" -> R.drawable.ic_book_open
            "collections_bookmark" -> R.drawable.ic_collections_bookmark
            "star" -> R.drawable.ic_star
            "category" -> R.drawable.ic_category
            "folder_special" -> R.drawable.ic_folder_special
            "schedule" -> R.drawable.ic_schedule
            "auto_awesome" -> R.drawable.ic_auto_awesome
            "bar_chart" -> R.drawable.ic_bar_chart
            "pie_chart" -> R.drawable.ic_pie_chart
            "bookmark" -> R.drawable.ic_bookmark_border
            "security" -> R.drawable.ic_security
            "notifications" -> R.drawable.ic_notifications
            "quote" -> R.drawable.ic_format_quote
            "edit_note" -> R.drawable.ic_edit_note
            "psychology" -> R.drawable.ic_psychology
            "trending_up" -> R.drawable.ic_trending_up
            "account_balance" -> R.drawable.ic_account_balance
            "favorite" -> R.drawable.ic_favorite
            "science" -> R.drawable.ic_science
            else -> R.drawable.ic_auto_stories
        }
    }
}
