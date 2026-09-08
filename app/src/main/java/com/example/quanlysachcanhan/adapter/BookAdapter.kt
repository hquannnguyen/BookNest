package com.example.quanlysachcanhan.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.databinding.ItemBookBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.getCategoryLabel
import com.example.quanlysachcanhan.utils.getReadingStatusLabel
import java.io.File
import java.util.Locale

class BookAdapter(
    private val onBookClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private val items = mutableListOf<Book>()

    fun submitList(newItems: List<Book>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class BookViewHolder(
        private val binding: ItemBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) = with(binding) {
            val ctx = root.context

            tvTitle.text = book.title
            tvAuthor.text = book.author
            tvCategory.text = ctx.getCategoryLabel(book.category)
            tvRatingScore.text = String.format(Locale.getDefault(), "%.1f", book.rating)

            // Dùng mapper DB code → label theo ngôn ngữ hiện tại
            tvStatus.text = ctx.getReadingStatusLabel(book.readingStatus)

            // Badge styling theo trạng thái
            when (book.readingStatus) {
                Constants.ReadingStatus.READ -> {
                    tvStatus.backgroundTintList =
                        ContextCompat.getColorStateList(ctx, R.color.status_read_bg)
                    tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_read_text))
                }
                Constants.ReadingStatus.READING -> {
                    tvStatus.backgroundTintList =
                        ContextCompat.getColorStateList(ctx, R.color.status_reading_bg)
                    tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_reading_text))
                }
                else -> {
                    tvStatus.backgroundTintList =
                        ContextCompat.getColorStateList(ctx, R.color.status_unread_bg)
                    tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_unread_text))
                }
            }

            // Tiến độ đọc
            val (displayCurrent, total, percentage) = when (book.readingStatus) {
                Constants.ReadingStatus.READ -> {
                    val t = book.totalPages
                    val current = if (t > 0) {
                        if (book.currentPage > 0) maxOf(book.currentPage, t) else t
                    } else {
                        book.currentPage
                    }
                    Triple(current, t, 100)
                }
                Constants.ReadingStatus.UNREAD -> {
                    Triple(0, book.totalPages, 0)
                }
                else -> { // READING
                    if (book.totalPages > 0) {
                        val current = book.currentPage.coerceIn(0, book.totalPages)
                        val percent = (current * 100 / book.totalPages).coerceIn(0, 100)
                        Triple(current, book.totalPages, percent)
                    } else {
                        Triple(book.currentPage, 0, 60)
                    }
                }
            }

            progressReading.progress = percentage
            if (total > 0) {
                tvProgressLabel.text = "$displayCurrent/$total tr"
            } else {
                tvProgressLabel.text = when (book.readingStatus) {
                    Constants.ReadingStatus.READ    -> ctx.getString(R.string.status_read)
                    Constants.ReadingStatus.READING -> ctx.getString(R.string.status_reading)
                    else                            -> ctx.getString(R.string.status_unread)
                }
            }
            tvProgressPercent.text = "$percentage%"

            // Cover Image
            val coverPath = book.coverImagePath
            if (!coverPath.isNullOrBlank()) {
                val file = File(coverPath)
                if (file.exists()) {
                    imgCover.setImageURI(Uri.fromFile(file))
                } else {
                    imgCover.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                imgCover.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            root.setOnClickListener { onBookClick(book) }
        }
    }
}