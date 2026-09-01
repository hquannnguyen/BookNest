package com.example.quanlysachcanhan.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysachcanhan.adapter.BookAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.databinding.ActivityMainBinding
import com.example.quanlysachcanhan.ui.addedit.AddEditBookActivity
import com.example.quanlysachcanhan.ui.detail.BookDetailActivity
import com.example.quanlysachcanhan.ui.stats.StatisticsActivity
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: BookRepository
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: BookAdapter

    private var keyword: String = ""
    private var selectedCategory: String? = null
    private var selectedStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BookRepository(this)
        preferenceManager = PreferenceManager(this)

        setupRecyclerView()
        setupEvents()
    }

    override fun onResume() {
        super.onResume()
        loadBooks()
    }

    private fun setupRecyclerView() {
        adapter = BookAdapter { book ->
            startActivity(
                Intent(this, BookDetailActivity::class.java)
                    .putExtra(Constants.Extras.BOOK_ID, book.id)
            )
        }

        binding.recyclerBooks.layoutManager = LinearLayoutManager(this)
        binding.recyclerBooks.adapter = adapter
    }

    private fun setupEvents() {
        binding.edtSearch.addTextChangedListener {
            keyword = it?.toString().orEmpty()
            loadBooks()
        }

        binding.fabAddBook.setOnClickListener {
            startActivity(Intent(this, AddEditBookActivity::class.java))
        }

        binding.btnStatistics.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        // TODO QuanNH — BOOK + SEARCH/FILTER/SORT:
        // btnFilter -> dialog/category + status
        // btnSort -> dialog chọn Constants.Sort.*
        // sau khi chọn thì gọi loadBooks()
    }

    private fun loadBooks() {
        val books = repository.getBooks(
            keyword = keyword,
            category = selectedCategory,
            status = selectedStatus,
            sortBy = preferenceManager.sortType
        )
        adapter.submitList(books)
        binding.tvEmpty.visibility =
            if (books.isEmpty()) android.view.View.VISIBLE
            else android.view.View.GONE
    }
}
