package com.example.quanlysachcanhan.utils

import android.content.Context
import android.content.Intent
import com.example.quanlysachcanhan.model.Book

object ShareUtils {

    fun shareBook(context: Context, book: Book) {
        val text = buildString {
            appendLine("\uD83D\uDCD6 ${book.title}")
            appendLine("\u270D\uFE0F ${book.author}")
            append("\u2B50 ${book.rating}/5")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        context.startActivity(
            Intent.createChooser(intent, "Chia s\u1EBB s\u00E1ch")
        )
    }
}
