package com.example.quanlysachcanhan.utils

import android.content.Context
import android.content.Intent
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.model.Book

object ShareUtils {

    /**
     * Chia sẻ thông tin sách qua Intent.
     * Nội dung dùng R.string.share_book_format để hỗ trợ đa ngôn ngữ.
     *
     * Format: share_book_format = "📖 %1$s\n✍️ %2$s\n⭐ %3$.1f/5\n%4$s"
     *   %1$s = title
     *   %2$s = author
     *   %3$.1f = rating
     *   %4$s = status label đã dịch
     */
    fun shareBook(context: Context, book: Book) {
        val statusLabel = context.getReadingStatusLabel(book.readingStatus)

        val text = context.getString(
            R.string.share_book_format,
            book.title,
            book.author,
            book.rating,
            statusLabel
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_chooser_title))
        )
    }
}