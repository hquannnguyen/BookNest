package com.example.quanlysachcanhan.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.databinding.ItemCollectionDetailBookBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.utils.Constants
import java.io.File
import java.util.Locale

class CollectionDetailBookAdapter(
    private val onBookClick: (Book) -> Unit,
    private val onOptionsClick: (Book, View) -> Unit
) : RecyclerView.Adapter<CollectionDetailBookAdapter.BookViewHolder>() {

    private val books = mutableListOf<Book>()
    private var quoteCounts: Map<Long, Int> = emptyMap()

    fun submitData(newBooks: List<Book>, quotesMap: Map<Long, Int>) {
        books.clear()
        books.addAll(newBooks)
        quoteCounts = quotesMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemCollectionDetailBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount(): Int = books.size

    inner class BookViewHolder(
        private val binding: ItemCollectionDetailBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) = with(binding) {
            val ctx = root.context

            // Title and Author
            tvBookTitle.text = book.title
            tvBookAuthor.text = book.author

            // Book Cover
            val coverPath = book.coverImagePath
            if (!coverPath.isNullOrBlank()) {
                val file = File(coverPath)
                if (file.exists()) {
                    ivBookCover.setImageURI(Uri.fromFile(file))
                    ivBookCover.visibility = View.VISIBLE
                    ivCoverPlaceholder.visibility = View.GONE
                } else {
                    ivBookCover.visibility = View.GONE
                    ivCoverPlaceholder.visibility = View.VISIBLE
                }
            } else {
                ivBookCover.visibility = View.GONE
                ivCoverPlaceholder.visibility = View.VISIBLE
            }

            // Rating Stars
            val rating = book.rating
            tvRatingValue.text = String.format(Locale.getDefault(), "%.1f", rating)

            val stars = listOf(ivStar1, ivStar2, ivStar3, ivStar4, ivStar5)
            for (i in 1..5) {
                val star = stars[i - 1]
                when {
                    rating >= i -> {
                        star.setImageResource(R.drawable.ic_star)
                        star.alpha = 1.0f
                    }
                    rating >= i - 0.5f -> {
                        star.setImageResource(R.drawable.ic_star_half)
                        star.alpha = 1.0f
                    }
                    else -> {
                        star.setImageResource(R.drawable.ic_star)
                        star.alpha = 0.22f
                    }
                }
            }

            // Status and Progress Calculation
            val totalPages = book.totalPages
            val currentPage = book.currentPage
            val percent = if (totalPages > 0) {
                ((currentPage.toFloat() / totalPages) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            when (book.readingStatus) {
                Constants.ReadingStatus.READ -> {
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
                    ivStatusIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.secondary_oak))
                    tvStatusLabel.text = "Đã hoàn thành"
                    tvStatusLabel.setTextColor(ContextCompat.getColor(ctx, R.color.secondary_oak))

                    tvPageProgress.text = "$totalPages / $totalPages tr"
                    pbBookReading.visibility = View.GONE

                    ivDoneIcon.setImageResource(R.drawable.ic_done_all)
                    ivDoneIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.secondary_oak))
                    tvPercentText.text = "100%"
                    tvPercentText.setTextColor(ContextCompat.getColor(ctx, R.color.secondary_oak))
                }
                Constants.ReadingStatus.READING -> {
                    ivStatusIcon.setImageResource(R.drawable.ic_auto_stories)
                    ivStatusIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.primary_navy))
                    tvStatusLabel.text = "Đang đọc"
                    tvStatusLabel.setTextColor(ContextCompat.getColor(ctx, R.color.primary_navy))

                    tvPageProgress.text = "$currentPage / $totalPages tr ($percent%)"
                    pbBookReading.visibility = View.VISIBLE
                    pbBookReading.progress = percent

                    ivDoneIcon.setImageResource(R.drawable.ic_schedule)
                    ivDoneIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.text_hint))
                    tvPercentText.text = "$percent%"
                    tvPercentText.setTextColor(ContextCompat.getColor(ctx, R.color.secondary_oak))
                }
                else -> { // UNREAD
                    ivStatusIcon.setImageResource(R.drawable.ic_bookmark_border)
                    ivStatusIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.text_hint))
                    tvStatusLabel.text = "Chưa bắt đầu"
                    tvStatusLabel.setTextColor(ContextCompat.getColor(ctx, R.color.text_hint))

                    tvPageProgress.text = "0 / $totalPages tr"
                    pbBookReading.visibility = View.GONE

                    ivDoneIcon.setImageResource(R.drawable.ic_bookmark_add)
                    ivDoneIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.text_hint))
                    tvPercentText.text = "0%"
                    tvPercentText.setTextColor(ContextCompat.getColor(ctx, R.color.text_hint))
                }
            }

            // Quotes Count
            val count = quoteCounts[book.id] ?: 0
            tvQuoteCount.text = "$count trích dẫn"

            // Click Handlers
            root.setOnClickListener {
                onBookClick(book)
            }

            btnBookOptions.setOnClickListener { v ->
                onOptionsClick(book, v)
            }
        }
    }
}
