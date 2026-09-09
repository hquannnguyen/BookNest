package com.example.quanlysachcanhan.utils

import com.example.quanlysachcanhan.R

data class CollectionIconItem(
    val key: String,
    val iconRes: Int,
    val labelVi: String,
    val labelEn: String
)

object CollectionIconHelper {

    val ALL_ICONS: List<CollectionIconItem> = listOf(
        CollectionIconItem("auto_stories", R.drawable.ic_auto_stories, "Sách mở", "Open Book"),
        CollectionIconItem("psychology", R.drawable.ic_psychology, "Tư duy", "Mindset"),
        CollectionIconItem("auto_awesome", R.drawable.ic_auto_awesome, "Cảm hứng", "Inspiration"),
        CollectionIconItem("trending_up", R.drawable.ic_trending_up, "Kỷ luật", "Discipline"),
        CollectionIconItem("bar_chart", R.drawable.ic_bar_chart, "Thống kê", "Statistics"),
        CollectionIconItem("edit_note", R.drawable.ic_edit_note, "Ghi chép", "Notes"),
        CollectionIconItem("account_balance", R.drawable.ic_account_balance, "Kinh tế", "Economics"),
        CollectionIconItem("science", R.drawable.ic_science, "Khoa học", "Science"),
        CollectionIconItem("favorite", R.drawable.ic_favorite, "Yêu thích", "Favorites"),
        CollectionIconItem("star", R.drawable.ic_star, "Kinh điển", "Classics"),
        CollectionIconItem("schedule", R.drawable.ic_schedule, "Thời gian", "Time"),
        CollectionIconItem("person", R.drawable.ic_person, "Nhân vật", "Biography"),
        CollectionIconItem("language", R.drawable.ic_language, "Ngoại ngữ", "Language"),
        CollectionIconItem("pie_chart", R.drawable.ic_pie_chart, "Phân tích", "Analytics"),
        CollectionIconItem("lightbulb", R.drawable.ic_lightbulb, "Sáng tạo", "Creativity"),
        CollectionIconItem("explore", R.drawable.ic_explore, "Khám phá", "Explore"),
        CollectionIconItem("spa", R.drawable.ic_spa, "Chữa lành", "Wellness"),
        CollectionIconItem("computer", R.drawable.ic_computer, "Công nghệ", "Technology"),
        CollectionIconItem("palette", R.drawable.ic_palette, "Nghệ thuật", "Art"),
        CollectionIconItem("flight", R.drawable.ic_flight, "Du lịch", "Travel")
    )

    fun getIconRes(key: String): Int {
        return ALL_ICONS.find { it.key == key }?.iconRes ?: R.drawable.ic_auto_stories
    }

    fun getIconLabel(key: String, isVietnamese: Boolean = true): String {
        val item = ALL_ICONS.find { it.key == key } ?: return "Sách"
        return if (isVietnamese) item.labelVi else item.labelEn
    }
}
