package com.example.quanlysachcanhan.model

data class Quote(
    val id: Long = 0L,
    val bookId: Long,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
