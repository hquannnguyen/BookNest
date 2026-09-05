package com.example.quanlysachcanhan.ui.detail

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.adapter.QuoteAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.QuoteRepository
import com.example.quanlysachcanhan.databinding.ActivityBookDetailBinding
import com.example.quanlysachcanhan.databinding.DialogDeleteBookBinding
import com.example.quanlysachcanhan.ui.addedit.AddEditBookActivity
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.ShareUtils
import com.example.quanlysachcanhan.utils.getCategoryLabel
import com.example.quanlysachcanhan.utils.getReadingStatusLabel

class BookDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookDetailBinding
    private lateinit var bookRepository: BookRepository
    private lateinit var quoteRepository: QuoteRepository
    private lateinit var quoteAdapter: QuoteAdapter

    private var bookId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookRepository  = BookRepository(this)
        quoteRepository = QuoteRepository(this)

        bookId = intent.getLongExtra(Constants.Extras.BOOK_ID, 0L)
        if (bookId <= 0L) { finish(); return }

        setupQuoteList()
        setupEvents()
    }

    override fun onResume() {
        super.onResume()
        loadBook()
        loadQuotes()
    }

    private fun setupQuoteList() {
        quoteAdapter = QuoteAdapter { quote ->
            quoteRepository.delete(quote.id)
            loadQuotes()
        }
        binding.recyclerQuotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerQuotes.adapter = quoteAdapter
    }

    private fun setupEvents() {
        binding.btnEdit.setOnClickListener {
            startActivity(
                Intent(this, AddEditBookActivity::class.java)
                    .putExtra(Constants.Extras.BOOK_ID, bookId)
            )
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.btnShare.setOnClickListener {
            bookRepository.getById(bookId)?.let { ShareUtils.shareBook(this, it) }
        }

        binding.btnAddQuote.setOnClickListener {
            // TODO KienTT - QUOTE:
            // mo AlertDialog co EditText nhap noi dung
            // quoteRepository.insert(Quote(bookId=bookId, content=content))
            // loadQuotes()
        }
    }

    private fun loadBook() {
        val book = bookRepository.getById(bookId) ?: return

        binding.tvTitle.text  = book.title
        binding.tvAuthor.text = book.author
        binding.tvNote.text   = book.note
        binding.ratingBar.rating = book.rating

        // Dùng mapper để hiển thị đúng ngôn ngữ
        binding.tvCategory.text = getCategoryLabel(book.category)
        binding.tvStatus.text   = getReadingStatusLabel(book.readingStatus)
    }

    private fun showDeleteConfirmationDialog() {
        val dialogBinding = DialogDeleteBookBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDeleteConfirm.setOnClickListener {
            dialog.dismiss()
            bookRepository.delete(bookId)
            Toast.makeText(this, getString(R.string.msg_book_deleted), Toast.LENGTH_SHORT).show()
            finish()
        }

        dialog.show()
    }

    private fun loadQuotes() {
        quoteAdapter.submitList(quoteRepository.getByBookId(bookId))
    }
}