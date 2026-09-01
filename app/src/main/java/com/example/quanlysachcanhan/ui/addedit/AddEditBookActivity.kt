package com.example.quanlysachcanhan.ui.addedit

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.databinding.ActivityAddEditBookBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.getCategoryCode
import com.example.quanlysachcanhan.utils.getCategoryLabels
import com.example.quanlysachcanhan.utils.getReadingStatusCode
import com.example.quanlysachcanhan.utils.getReadingStatusLabels

class AddEditBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBookBinding
    private lateinit var repository: BookRepository

    private var editingBookId: Long = 0L
    private var currentCoverPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddEditBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BookRepository(this)

        setupSpinners()
        setupEvents()

        editingBookId = intent.getLongExtra(Constants.Extras.BOOK_ID, 0L)
        if (editingBookId > 0L) {
            loadBookForEdit(editingBookId)
        }
    }

    private fun setupSpinners() {
        // Spinner hiển thị labels đã dịch, KHÔNG phải mã DB
        binding.spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            getCategoryLabels()          // ["Khoa học", "Văn học", ...] hoặc ["Science", ...]
        )

        binding.spinnerStatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            getReadingStatusLabels()     // ["Chưa đọc", "Đang đọc", "Đã đọc"]
        )
    }

    private fun setupEvents() {
        binding.btnSave.setOnClickListener {
            saveBook()
        }

        binding.btnChooseCover.setOnClickListener {
            // TODO KienTT - IMAGE:
            // 1. Mo Photo Picker / Gallery
            // 2. Hoac mo Camera
            // 3. Dung ImageStorageHelper.copyImageToAppStorage(...)
            // 4. Gan duong dan vao currentCoverPath
        }
    }

    private fun loadBookForEdit(bookId: Long) {
        val book = repository.getById(bookId) ?: return

        binding.edtTitle.setText(book.title)
        binding.edtAuthor.setText(book.author)
        binding.edtNote.setText(book.note)
        binding.ratingBar.rating = book.rating
        currentCoverPath = book.coverImagePath

        // Tìm index theo mã DB → map sang index label
        val categoryIndex = Constants.Category.ALL.indexOf(book.category).coerceAtLeast(0)
        binding.spinnerCategory.setSelection(categoryIndex)

        val statusIndex = Constants.ReadingStatus.ALL.indexOf(book.readingStatus).coerceAtLeast(0)
        binding.spinnerStatus.setSelection(statusIndex)
    }

    private fun saveBook() {
        val title  = binding.edtTitle.text.toString().trim()
        val author = binding.edtAuthor.text.toString().trim()

        if (title.isBlank()) {
            binding.edtTitle.error = getString(R.string.error_title_empty)
            return
        }

        if (author.isBlank()) {
            binding.edtAuthor.error = getString(R.string.error_author_empty)
            return
        }

        // Label người dùng chọn → convert về mã DB trước khi lưu
        val categoryLabel = binding.spinnerCategory.selectedItem.toString()
        val statusLabel   = binding.spinnerStatus.selectedItem.toString()
        val categoryCode  = getCategoryCode(categoryLabel)      // "Khoa học" → "SCIENCE"
        val statusCode    = getReadingStatusCode(statusLabel)    // "Đã đọc"   → "READ"

        val oldBook =
            if (editingBookId > 0) repository.getById(editingBookId)
            else null

        val finishedAt = when {
            statusCode == Constants.ReadingStatus.READ &&
                oldBook?.readingStatus != Constants.ReadingStatus.READ ->
                System.currentTimeMillis()

            statusCode != Constants.ReadingStatus.READ ->
                null

            else -> oldBook?.finishedAt
        }

        val book = Book(
            id             = editingBookId,
            title          = title,
            author         = author,
            category       = categoryCode,      // lưu mã DB, KHÔNG lưu label
            rating         = binding.ratingBar.rating,
            note           = binding.edtNote.text.toString().trim(),
            readingStatus  = statusCode,         // lưu mã DB, KHÔNG lưu label
            coverImagePath = currentCoverPath,
            createdAt      = oldBook?.createdAt ?: System.currentTimeMillis(),
            updatedAt      = oldBook?.updatedAt,
            finishedAt     = finishedAt
        )

        if (editingBookId > 0L) {
            repository.update(book)
            Toast.makeText(this, getString(R.string.msg_book_updated), Toast.LENGTH_SHORT).show()
        } else {
            repository.insert(book)
            Toast.makeText(this, getString(R.string.msg_book_added), Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}