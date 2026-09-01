package com.example.quanlysachcanhan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysachcanhan.databinding.ItemBookBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.utils.getCategoryLabel
import com.example.quanlysachcanhan.utils.getReadingStatusLabel
import java.io.File

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

            tvTitle.text  = book.title
            tvAuthor.text = book.author
            ratingBar.rating = book.rating

            // Dùng mapper: DB code → label đã dịch theo ngôn ngữ hiện tại
            tvStatus.text = ctx.getReadingStatusLabel(book.readingStatus)

            val coverPath = book.coverImagePath
            if (!coverPath.isNullOrBlank()) {
                val file = File(coverPath)
                if (file.exists()) {
                    imgCover.setImageURI(android.net.Uri.fromFile(file))
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