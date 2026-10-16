package com.example.quanlysachcanhan.ui.addedit

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.QuoteRepository
import com.example.quanlysachcanhan.databinding.ActivityAddEditBookBinding
import com.example.quanlysachcanhan.databinding.DialogCustomSelectionBinding
import com.example.quanlysachcanhan.databinding.ItemDialogOptionBinding
import com.example.quanlysachcanhan.databinding.ItemQuoteEditorBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.model.Quote
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.ImageStorageHelper
import com.example.quanlysachcanhan.utils.NavigationHelper
import com.example.quanlysachcanhan.utils.PreferenceManager
import com.example.quanlysachcanhan.utils.getCategoryCode
import com.example.quanlysachcanhan.utils.getCategoryLabels
import com.example.quanlysachcanhan.utils.getReadingStatusCode
import com.example.quanlysachcanhan.utils.getReadingStatusLabels
import java.io.File

class AddEditBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBookBinding
    private lateinit var repository: BookRepository
    private lateinit var quoteRepository: QuoteRepository
    private lateinit var preferenceManager: PreferenceManager

    private var editingBookId: Long = 0L
    private var currentCoverPath: String? = null
    private var currentRating: Float = 0f

    private var tempCoverUri: Uri? = null
    private var tempCoverFile: File? = null

    // Chụp ảnh bìa từ máy ảnh hệ thống (ủy quyền qua FileProvider)
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        val uri = tempCoverUri
        if (isSuccess && uri != null) {
            val savedPath = ImageStorageHelper.copyImageToAppStorage(this, uri)
            if (savedPath != null) {
                currentCoverPath = savedPath
                displayCoverImage(savedPath)
            } else {
                Toast.makeText(this, "Không thể lưu ảnh bìa", Toast.LENGTH_SHORT).show()
            }
        }
        tempCoverFile?.let { ImageStorageHelper.deleteImage(it.absolutePath) }
        tempCoverUri = null
        tempCoverFile = null
    }

    // Image Picker mở thư viện ảnh và sao chép vào bộ nhớ ứng dụng
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = ImageStorageHelper.copyImageToAppStorage(this, it)
            if (savedPath != null) {
                currentCoverPath = savedPath
                displayCoverImage(savedPath)
            } else {
                Toast.makeText(this, "Không thể lưu ảnh bìa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddEditBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BookRepository(this)
        quoteRepository = QuoteRepository(this)
        preferenceManager = PreferenceManager(this)

        setupNavigationAndHeader()
        setupSpinners()
        setupHeartRating()
        setupEvents()

        editingBookId = intent.getLongExtra(Constants.Extras.BOOK_ID, 0L)
        val headerTitle = if (editingBookId > 0L) {
            getString(R.string.title_edit_book)
        } else {
            getString(R.string.title_add_book)
        }
        title = headerTitle
        binding.tvHeaderTitle.text = headerTitle

        if (editingBookId > 0L) {
            loadBookForEdit(editingBookId)
        } else {
            updateHeartRatingUI(0f)
            updateQuoteIndicesAndCount()
        }
    }

    private fun setupNavigationAndHeader() {
        NavigationHelper.setupHeader(
            this,
            binding.btnLanguage,
            binding.ivLangFlag,
            binding.tvLangCode,
            binding.btnReminder,
            binding.dotReminder,
            preferenceManager
        )

        NavigationHelper.setupBottomNavigation(
            this,
            NavigationHelper.Tab.SUB_PAGE,
            binding.navLibrary,
            binding.navCollections,
            binding.navStatistics,
            binding.navSettings
        )

        binding.btnBack.setOnClickListener {
            NavigationHelper.finishWithSlide(this)
        }

        onBackPressedDispatcher.addCallback(this) {
            NavigationHelper.finishWithSlide(this@AddEditBookActivity)
        }
    }

    private fun setupSpinners() {
        binding.spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            getCategoryLabels()
        )

        binding.spinnerStatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            getReadingStatusLabels()
        )
    }

    private fun setupHeartRating() {
        val hearts = listOf(
            binding.ivHeart1,
            binding.ivHeart2,
            binding.ivHeart3,
            binding.ivHeart4,
            binding.ivHeart5
        )

        hearts.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                val targetRating = (index + 1).toFloat()
                currentRating = if (currentRating == targetRating && index == 0) {
                    0f
                } else {
                    targetRating
                }
                updateHeartRatingUI(currentRating)
            }
        }
    }

    private fun updateHeartRatingUI(rating: Float) {
        val hearts = listOf(
            binding.ivHeart1,
            binding.ivHeart2,
            binding.ivHeart3,
            binding.ivHeart4,
            binding.ivHeart5
        )

        val goldColor = ContextCompat.getColor(this, R.color.star_gold)
        val outlineColor = ContextCompat.getColor(this, R.color.outline_variant)

        hearts.forEachIndexed { index, imageView ->
            if (rating >= index + 1) {
                imageView.setImageResource(R.drawable.ic_favorite)
                imageView.setColorFilter(goldColor)
            } else {
                imageView.setImageResource(R.drawable.ic_favorite_border)
                imageView.setColorFilter(outlineColor)
            }
        }

        updateRatingLabel(rating)
    }

    private fun updateRatingLabel(rating: Float) {
        val label = when {
            rating >= 4.5f -> getString(R.string.rating_5)
            rating >= 3.5f -> getString(R.string.rating_4)
            rating >= 2.5f -> getString(R.string.rating_3)
            rating >= 1.5f -> getString(R.string.rating_2)
            rating >= 0.5f -> getString(R.string.rating_1)
            else -> getString(R.string.rating_unrated)
        }
        binding.tvRatingLabel.text = label
    }

    private fun setupEvents() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveBook()
        }

        binding.edtTitle.doAfterTextChanged {
            val length = it?.length ?: 0
            binding.tvTitleCounter.text = "$length/120"
            if (!it.isNullOrBlank()) {
                binding.edtTitle.error = null
            }
        }

        binding.edtAuthor.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                binding.edtAuthor.error = null
            }
        }

        // Khung ảnh và nút camera: mở hộp thoại chọn chụp hoặc chọn từ thư viện
        binding.layoutCoverTrigger.setOnClickListener {
            showCoverOptionsDialog()
        }

        binding.btnChooseCover.setOnClickListener {
            showCoverOptionsDialog()
        }

        // Nút thêm trích dẫn mới
        binding.btnAddQuote.setOnClickListener {
            addQuoteItem(Quote(bookId = editingBookId, content = ""))
        }
    }

    private fun launchCameraForCover() {
        val pair = ImageStorageHelper.createTempCameraUri(this)
        if (pair != null) {
            tempCoverFile = pair.first
            tempCoverUri = pair.second
            takePhotoLauncher.launch(pair.second)
        } else {
            Toast.makeText(this, getString(R.string.msg_camera_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCoverOptionsDialog() {
        val dialogBinding = DialogCustomSelectionBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.tvDialogTitle.text = getString(R.string.dialog_choose_cover_title)

        val options = mutableListOf<Pair<String, () -> Unit>>()
        options.add(Pair(getString(R.string.btn_take_photo)) {
            launchCameraForCover()
        })
        options.add(Pair(getString(R.string.btn_choose_gallery)) {
            pickImageLauncher.launch("image/*")
        })

        if (!currentCoverPath.isNullOrBlank()) {
            options.add(Pair(getString(R.string.btn_remove_cover)) {
                currentCoverPath = null
                binding.imgCover.visibility = View.GONE
                binding.layoutCoverPlaceholder.visibility = View.VISIBLE
            })
        }

        options.forEach { (title, action) ->
            val itemBinding = ItemDialogOptionBinding.inflate(layoutInflater, dialogBinding.containerOptions, false)
            itemBinding.ivOptionIcon.visibility = View.GONE
            itemBinding.tvOptionIcon.visibility = View.GONE
            itemBinding.tvOptionText.text = title
            itemBinding.ivOptionCheck.visibility = View.GONE

            itemBinding.layoutOptionItem.setOnClickListener {
                dialog.dismiss()
                action()
            }
            dialogBinding.containerOptions.addView(itemBinding.root)
        }

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addQuoteItem(quote: Quote) {
        val itemBinding = ItemQuoteEditorBinding.inflate(
            layoutInflater,
            binding.containerQuotes,
            false
        )

        itemBinding.edtQuoteContent.setText(quote.content)
        itemBinding.edtQuotePage.setText(quote.page)
        itemBinding.edtQuoteChapter.setText(quote.chapter)
        itemBinding.edtQuoteTag.setText(quote.tag)

        itemBinding.btnDeleteQuote.setOnClickListener {
            binding.containerQuotes.removeView(itemBinding.root)
            updateQuoteIndicesAndCount()
        }

        binding.containerQuotes.addView(itemBinding.root)
        updateQuoteIndicesAndCount()

        if (quote.content.isEmpty()) {
            itemBinding.edtQuoteContent.requestFocus()
        }
    }

    private fun updateQuoteIndicesAndCount() {
        val count = binding.containerQuotes.childCount
        for (i in 0 until count) {
            val child = binding.containerQuotes.getChildAt(i)
            val tvIndex = child.findViewById<android.widget.TextView>(R.id.tvQuoteIndex)
            tvIndex?.text = getString(R.string.label_quote_item_title, i + 1)
        }
        binding.tvQuotesCount.text = getString(R.string.label_quote_count_format, count)
    }

    private fun displayCoverImage(path: String) {
        val file = File(path)
        if (file.exists()) {
            binding.imgCover.visibility = View.VISIBLE
            binding.layoutCoverPlaceholder.visibility = View.GONE
            binding.imgCover.setImageURI(Uri.fromFile(file))
        } else {
            binding.imgCover.visibility = View.GONE
            binding.layoutCoverPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun loadBookForEdit(bookId: Long) {
        val book = repository.getById(bookId) ?: return

        binding.edtTitle.setText(book.title)
        binding.edtAuthor.setText(book.author)
        binding.edtPublisher.setText(book.publisher)
        binding.edtNote.setText(book.note)
        currentRating = book.rating
        updateHeartRatingUI(book.rating)

        if (book.currentPage > 0) {
            binding.edtCurrentPage.setText(book.currentPage.toString())
        }
        if (book.totalPages > 0) {
            binding.edtTotalPages.setText(book.totalPages.toString())
        }

        currentCoverPath = book.coverImagePath
        if (!currentCoverPath.isNullOrBlank()) {
            displayCoverImage(currentCoverPath!!)
        } else {
            binding.imgCover.visibility = View.GONE
            binding.layoutCoverPlaceholder.visibility = View.VISIBLE
        }

        // Map mã DB sang index trong adapter
        val categoryIndex = Constants.Category.ALL.indexOf(book.category).coerceAtLeast(0)
        binding.spinnerCategory.setSelection(categoryIndex)

        val statusIndex = Constants.ReadingStatus.ALL.indexOf(book.readingStatus).coerceAtLeast(0)
        binding.spinnerStatus.setSelection(statusIndex)

        // Tải danh sách trích dẫn tâm đắc của sách
        binding.containerQuotes.removeAllViews()
        val quotes = quoteRepository.getByBookId(bookId)
        quotes.forEach { quote ->
            addQuoteItem(quote)
        }
        updateQuoteIndicesAndCount()
    }

    private fun saveBook() {
        val title     = binding.edtTitle.text.toString().trim()
        val author    = binding.edtAuthor.text.toString().trim()
        val publisher = binding.edtPublisher.text.toString().trim()

        if (title.isBlank()) {
            binding.edtTitle.error = getString(R.string.error_title_empty)
            binding.edtTitle.requestFocus()
            return
        }

        if (author.isBlank()) {
            binding.edtAuthor.error = getString(R.string.error_author_empty)
            binding.edtAuthor.requestFocus()
            return
        }

        var currentPage = binding.edtCurrentPage.text.toString().trim().toIntOrNull() ?: 0
        val totalPages  = binding.edtTotalPages.text.toString().trim().toIntOrNull() ?: 0

        val categoryLabel = binding.spinnerCategory.selectedItem.toString()
        val statusLabel   = binding.spinnerStatus.selectedItem.toString()
        val categoryCode  = getCategoryCode(categoryLabel)
        val statusCode    = getReadingStatusCode(statusLabel)

        if (statusCode == Constants.ReadingStatus.READ && totalPages > 0 && currentPage < totalPages) {
            currentPage = totalPages
        } else if (statusCode == Constants.ReadingStatus.UNREAD) {
            currentPage = 0
        }

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
            category       = categoryCode,
            rating         = currentRating,
            note           = binding.edtNote.text.toString().trim(),
            readingStatus  = statusCode,
            coverImagePath = currentCoverPath,
            currentPage    = currentPage,
            totalPages     = totalPages,
            publisher      = publisher,
            createdAt      = oldBook?.createdAt ?: System.currentTimeMillis(),
            updatedAt      = oldBook?.updatedAt,
            finishedAt     = finishedAt
        )

        // Thu thập danh sách trích dẫn đầy đủ các trường
        val quotesToSave = mutableListOf<Quote>()
        for (i in 0 until binding.containerQuotes.childCount) {
            val child = binding.containerQuotes.getChildAt(i)
            val edtContent = child.findViewById<android.widget.EditText>(R.id.edtQuoteContent)
            val edtPage    = child.findViewById<android.widget.EditText>(R.id.edtQuotePage)
            val edtChapter = child.findViewById<android.widget.EditText>(R.id.edtQuoteChapter)
            val edtTag     = child.findViewById<android.widget.EditText>(R.id.edtQuoteTag)

            val content = edtContent?.text?.toString()?.trim().orEmpty()
            val page    = edtPage?.text?.toString()?.trim().orEmpty()
            val chapter = edtChapter?.text?.toString()?.trim().orEmpty()
            val tag     = edtTag?.text?.toString()?.trim().orEmpty()

            if (content.isNotEmpty()) {
                quotesToSave.add(
                    Quote(
                        bookId  = if (editingBookId > 0L) editingBookId else 0L,
                        content = content,
                        page    = page,
                        chapter = chapter,
                        tag     = tag
                    )
                )
            }
        }

        if (editingBookId > 0L) {
            repository.update(book)

            // Cập nhật trích dẫn cho sách đang sửa
            val oldQuotes = quoteRepository.getByBookId(editingBookId)
            oldQuotes.forEach { quoteRepository.delete(it.id) }
            quotesToSave.forEach { quote ->
                quoteRepository.insert(quote.copy(bookId = editingBookId))
            }

            Toast.makeText(this, getString(R.string.msg_book_updated), Toast.LENGTH_SHORT).show()
        } else {
            val newBookId = repository.insert(book)

            // Lưu trích dẫn cho sách vừa thêm
            if (newBookId > 0L) {
                quotesToSave.forEach { quote ->
                    quoteRepository.insert(quote.copy(bookId = newBookId))
                }
            }

            Toast.makeText(this, getString(R.string.msg_book_added), Toast.LENGTH_SHORT).show()
        }

        NavigationHelper.finishWithSlide(this)
    }
}