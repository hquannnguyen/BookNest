package com.example.quanlysachcanhan.model

data class BookCollection(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val colorHex: String = "#7D562D",
    val iconName: String = "auto_stories",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class CollectionWithStats(
    val collection: BookCollection,
    val totalBooks: Int = 0,
    val readBooks: Int = 0,
    val readingBooks: Int = 0,
    val unreadBooks: Int = 0,
    val bookCovers: List<String?> = emptyList(),
    val bookIds: List<Long> = emptyList()
) {
    val progressPercent: Int
        get() = if (totalBooks > 0) (readBooks * 100 / totalBooks) else 0
}
