package com.example.quanlysachcanhan.model

data class Book(
    val id: Long = 0L,
    val title: String,
    val author: String,
    val category: String,
    val rating: Float = 0f,
    val note: String = "",
    val readingStatus: String,
    val coverImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val finishedAt: Long? = null
)
