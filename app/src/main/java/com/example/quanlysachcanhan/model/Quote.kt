package com.example.quanlysachcanhan.model

data class Quote(
    val id: Long = 0L,
    val bookId: Long,
    val content: String,
    val page: String = "",
    val chapter: String = "",
    val tag: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
