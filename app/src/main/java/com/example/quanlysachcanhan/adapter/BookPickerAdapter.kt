package com.example.quanlysachcanhan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.databinding.ItemCollectionBookPickerBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.ImageStorageHelper

class BookPickerAdapter(
    private var allBooks: List<Book>,
    initialSelectedIds: Set<Long> = emptySet(),
    private val onSelectionChanged: (selectedCount: Int, selectedIds: Set<Long>) -> Unit
) : RecyclerView.Adapter<BookPickerAdapter.BookPickerViewHolder>() {

    private val selectedIds = initialSelectedIds.toMutableSet()
    private var filteredBooks: List<Book> = allBooks

    fun updateBooks(books: List<Book>) {
        allBooks = books
        filteredBooks = books
        notifyDataSetChanged()
    }

    fun filter(query: String, statusFilter: String? = null) {
        val q = query.trim().lowercase()
        filteredBooks = allBooks.filter { book ->
            val matchQuery = q.isEmpty() ||
                    book.title.lowercase().contains(q) ||
                    book.author.lowercase().contains(q)
            val matchStatus = statusFilter == null || book.readingStatus == statusFilter
            matchQuery && matchStatus
        }
        notifyDataSetChanged()
    }

    fun getSelectedBookIds(): List<Long> = selectedIds.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookPickerViewHolder {
        val binding = ItemCollectionBookPickerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BookPickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookPickerViewHolder, position: Int) {
        holder.bind(filteredBooks[position])
    }

    override fun getItemCount(): Int = filteredBooks.size

    inner class BookPickerViewHolder(
        private val binding: ItemCollectionBookPickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            val context = binding.root.context

            binding.tvBookTitle.text = book.title
            binding.tvBookAuthor.text = book.author

            // Status label & progress
            when (book.readingStatus) {
                Constants.ReadingStatus.READ -> {
                    binding.tvBookStatus.text = "Đã đọc 100%"
                    binding.tvBookStatus.setTextColor(ContextCompat.getColor(context, R.color.secondary_oak))
                }
                Constants.ReadingStatus.READING -> {
                    val pct = if (book.totalPages > 0) (book.currentPage * 100 / book.totalPages) else 0
                    binding.tvBookStatus.text = "Đang đọc $pct%"
                    binding.tvBookStatus.setTextColor(ContextCompat.getColor(context, R.color.primary_navy))
                }
                else -> {
                    binding.tvBookStatus.text = "Chưa đọc"
                    binding.tvBookStatus.setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                }
            }

            // Cover Image
            val coverPath = book.coverImagePath
            if (!coverPath.isNullOrBlank()) {
                val file = java.io.File(coverPath)
                if (file.exists()) {
                    binding.ivCover.setImageURI(android.net.Uri.fromFile(file))
                    binding.ivCover.imageTintList = null
                } else {
                    binding.ivCover.setImageResource(R.drawable.ic_auto_stories)
                    binding.ivCover.imageTintList = ContextCompat.getColorStateList(context, R.color.text_hint)
                }
            } else {
                binding.ivCover.setImageResource(R.drawable.ic_auto_stories)
                binding.ivCover.imageTintList = ContextCompat.getColorStateList(context, R.color.text_hint)
            }

            // Checked state
            val isChecked = selectedIds.contains(book.id)
            binding.cbSelect.isChecked = isChecked

            if (isChecked) {
                binding.cardBookPicker.strokeColor = ContextCompat.getColor(context, R.color.primary_navy)
                binding.cardBookPicker.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container))
            } else {
                binding.cardBookPicker.strokeColor = ContextCompat.getColor(context, R.color.outline_variant)
                binding.cardBookPicker.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_low))
            }

            binding.root.setOnClickListener {
                if (selectedIds.contains(book.id)) {
                    selectedIds.remove(book.id)
                } else {
                    selectedIds.add(book.id)
                }
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos)
                }
                onSelectionChanged(selectedIds.size, selectedIds)
            }
        }
    }
}
