package com.example.quanlysachcanhan.ui.addedit

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.databinding.ActivityAddEditBookBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.utils.Constants

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
        binding.spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Constants.Category.ALL
        )

        binding.spinnerStatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            Constants.ReadingStatus.ALL
        )
    }

    private fun setupEvents() {
        binding.btnSave.setOnClickListener {
            saveBook()
        }

        binding.btnChooseCover.setOnClickListener {
            // TODO KienTT — IMAGE:
            // 1. Mở Photo Picker / Gallery
            // 2. Hoặc mở Camera
            // 3. Dùng ImageStorageHelper.copyImageToAppStorage(...)
            // 4. Gán currentCoverPath
        }
    }

    private fun loadBookForEdit(bookId: Long) {
        val book = repository.getById(bookId) ?: return

        binding.edtTitle.setText(book.title)
        binding.edtAuthor.setText(book.author)
        binding.edtNote.setText(book.note)
        binding.ratingBar.rating = book.rating
        currentCoverPath = book.coverImagePath

        binding.spinnerCategory.setSelection(
            Constants.Category.ALL.indexOf(book.category).coerceAtLeast(0)
        )

        binding.spinnerStatus.setSelection(
            Constants.ReadingStatus.ALL.indexOf(book.readingStatus).coerceAtLeast(0)
        )
    }

    private fun saveBook() {
        val title = binding.edtTitle.text.toString().trim()
        val author = binding.edtAuthor.text.toString().trim()

        if (title.isBlank()) {
            binding.edtTitle.error = "Vui lòng nhập tên sách"
            return
        }

        if (author.isBlank()) {
            binding.edtAuthor.error = "Vui lòng nhập tác giả"
            return
        }

        val status = binding.spinnerStatus.selectedItem.toString()

        val oldBook =
            if (editingBookId > 0) repository.getById(editingBookId)
            else null

        val finishedAt = when {
            status == Constants.ReadingStatus.READ &&
                oldBook?.readingStatus != Constants.ReadingStatus.READ ->
                System.currentTimeMillis()

            status != Constants.ReadingStatus.READ ->
                null

            else ->
                oldBook?.finishedAt
        }

        val book = Book(
            id = editingBookId,
            title = title,
            author = author,
            category = binding.spinnerCategory.selectedItem.toString(),
            rating = binding.ratingBar.rating,
            note = binding.edtNote.text.toString().trim(),
            readingStatus = status,
            coverImagePath = currentCoverPath,
            createdAt = oldBook?.createdAt ?: System.currentTimeMillis(),
            updatedAt = oldBook?.updatedAt,
            finishedAt = finishedAt
        )

        if (editingBookId > 0L) {
            repository.update(book)
            Toast.makeText(this, "Đã cập nhật sách", Toast.LENGTH_SHORT).show()
        } else {
            repository.insert(book)
            Toast.makeText(this, "Đã thêm sách", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}
