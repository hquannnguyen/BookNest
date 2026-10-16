package com.example.quanlysachcanhan.ui.collections

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.adapter.CollectionDetailBookAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.CollectionRepository
import com.example.quanlysachcanhan.data.QuoteRepository
import com.example.quanlysachcanhan.databinding.ActivityCollectionDetailBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.model.BookCollection
import com.example.quanlysachcanhan.model.CollectionWithStats
import com.example.quanlysachcanhan.ui.detail.BookDetailActivity
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.NavigationHelper
import com.example.quanlysachcanhan.utils.PreferenceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CollectionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectionDetailBinding
    private lateinit var collectionRepository: CollectionRepository
    private lateinit var bookRepository: BookRepository
    private lateinit var quoteRepository: QuoteRepository
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: CollectionDetailBookAdapter

    private var collectionId: Long = 0L
    private var currentCollection: BookCollection? = null
    private var allBooksInCollection: List<Book> = emptyList()
    private var currentFilter: StatusFilter = StatusFilter.ALL
    private var currentSort: SortOption = SortOption.PROGRESS

    enum class StatusFilter {
        ALL, READING, COMPLETED, UNREAD
    }

    enum class SortOption(val label: String) {
        PROGRESS("Tiến độ"),
        TITLE("Tên sách"),
        RATING("Đánh giá"),
        PAGES("Số trang")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        collectionId = intent.getLongExtra(EXTRA_COLLECTION_ID, 0L)
        if (collectionId == 0L) {
            Toast.makeText(this, "Không tìm thấy bộ sưu tập", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        collectionRepository = CollectionRepository(this)
        bookRepository = BookRepository(this)
        quoteRepository = QuoteRepository(this)
        preferenceManager = PreferenceManager(this)

        setupTopHeader()
        setupBottomNavigation()
        setupRecyclerView()
        setupFilterPills()
        setupActions()
        binding.btnBookshelf3d.setOnClickListener {
            NavigationHelper.startActivitySlide(this,
                Intent(this, Bookshelf3DActivity::class.java).apply {
                    putExtra(EXTRA_COLLECTION_ID, collectionId)
                })
        }

        loadCollectionDetails()
    }

    override fun onResume() {
        super.onResume()
        loadCollectionDetails()
    }

    private fun setupTopHeader() {
        NavigationHelper.setupHeader(
            this,
            binding.btnLanguage,
            binding.ivFlag,
            binding.tvLangCode,
            binding.btnReminder,
            binding.dotReminder,
            preferenceManager
        )

        binding.btnBack.setOnClickListener {
            NavigationHelper.finishWithSlide(this)
        }

        onBackPressedDispatcher.addCallback(this) {
            NavigationHelper.finishWithSlide(this@CollectionDetailActivity)
        }
    }

    private fun setupBottomNavigation() {
        NavigationHelper.setupBottomNavigation(
            this,
            NavigationHelper.Tab.COLLECTIONS,
            binding.navLibrary,
            binding.navCollections,
            binding.navStatistics,
            binding.navSettings
        )
    }

    private fun setupRecyclerView() {
        adapter = CollectionDetailBookAdapter(
            onBookClick = { book ->
                val intent = Intent(this, BookDetailActivity::class.java).apply {
                    putExtra(Constants.Extras.BOOK_ID, book.id)
                }
                NavigationHelper.startActivitySlide(this, intent)
            },
            onOptionsClick = { book, anchorView ->
                showBookOptionsPopup(book, anchorView)
            }
        )

        binding.recyclerCollectionBooks.layoutManager = LinearLayoutManager(this)
        binding.recyclerCollectionBooks.adapter = adapter
    }

    private fun setupFilterPills() {
        binding.pillFilterAll.setOnClickListener {
            setFilter(StatusFilter.ALL)
        }
        binding.pillFilterReading.setOnClickListener {
            setFilter(StatusFilter.READING)
        }
        binding.pillFilterCompleted.setOnClickListener {
            setFilter(StatusFilter.COMPLETED)
        }
        binding.pillFilterUnread.setOnClickListener {
            setFilter(StatusFilter.UNREAD)
        }
    }

    private fun setFilter(filter: StatusFilter) {
        currentFilter = filter
        updateFilterPillsUI()
        applyFilterAndSort()
    }

    private fun updateFilterPillsUI() {
        val activeBg = R.drawable.bg_pill_active
        val inactiveBg = R.drawable.bg_pill_inactive
        val whiteColor = Color.WHITE
        val secondaryColor = ContextCompat.getColor(this, R.color.text_secondary)

        fun stylePill(tv: TextView, isActive: Boolean) {
            tv.setBackgroundResource(if (isActive) activeBg else inactiveBg)
            tv.setTextColor(if (isActive) whiteColor else secondaryColor)
        }

        stylePill(binding.pillFilterAll, currentFilter == StatusFilter.ALL)
        stylePill(binding.pillFilterReading, currentFilter == StatusFilter.READING)
        stylePill(binding.pillFilterCompleted, currentFilter == StatusFilter.COMPLETED)
        stylePill(binding.pillFilterUnread, currentFilter == StatusFilter.UNREAD)
    }

    private fun setupActions() {
        // "+ Thêm sách vào bộ"
        val openAddBooksDialog = {
            val dialog = CreateCollectionDialog.newInstance(collectionId)
            dialog.setOnSavedListener {
                loadCollectionDetails()
            }
            dialog.show(supportFragmentManager, "EditCollectionDialog")
        }

        binding.btnAddBooks.setOnClickListener { openAddBooksDialog() }
        binding.btnEmptyAddBooks.setOnClickListener { openAddBooksDialog() }

        // Sort Button
        binding.btnSort.setOnClickListener { anchor ->
            showSortPopupMenu(anchor)
        }

        // Share button
        binding.btnShare.setOnClickListener {
            shareCollection()
        }

        // More options button (Edit, Share, Delete)
        binding.btnMoreOptions.setOnClickListener { anchor ->
            showCollectionOptionsPopup(anchor)
        }
    }

    private fun loadCollectionDetails() {
        val col = collectionRepository.getById(collectionId)
        if (col == null) {
            Toast.makeText(this, "Bộ sưu tập không còn tồn tại", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        allBooksInCollection = collectionRepository.getBooksInCollection(collectionId)
        if (allBooksInCollection.isEmpty()) {
            collectionRepository.delete(collectionId)
            Toast.makeText(this, R.string.msg_collection_deleted_empty, Toast.LENGTH_SHORT).show()
            NavigationHelper.finishWithSlide(this)
            return
        }

        currentCollection = col
        val stats = collectionRepository.getCollectionWithStats(collectionId)

        bindHeroCard(col, stats)
        bindFeaturedQuote(allBooksInCollection)
        updatePillLabels()
        updateFilterPillsUI()
        applyFilterAndSort()
    }

    private fun bindHeroCard(col: BookCollection, stats: CollectionWithStats?) {
        // Icon & Colors
        val iconRes = com.example.quanlysachcanhan.utils.CollectionIconHelper.getIconRes(col.iconName)
        binding.ivCollectionIcon.setImageResource(iconRes)

        try {
            val parsedColor = Color.parseColor(col.colorHex)
            binding.layoutIconContainer.background.mutate().setTint(parsedColor)
            binding.layoutIconContainer.alpha = 0.88f
            binding.ivCollectionIcon.setColorFilter(Color.WHITE)
        } catch (e: Exception) {
            binding.layoutIconContainer.background.mutate().setTint(
                ContextCompat.getColor(this, R.color.secondary_container)
            )
            binding.ivCollectionIcon.setColorFilter(
                ContextCompat.getColor(this, R.color.on_secondary_container)
            )
        }

        // Title & Description
        binding.tvCollectionTitle.text = col.name
        if (col.description.isNotBlank()) {
            binding.tvCollectionDescription.text = col.description
            binding.tvCollectionDescription.visibility = View.VISIBLE
        } else {
            binding.tvCollectionDescription.visibility = View.GONE
        }

        // Key Metrics
        val totalCount = allBooksInCollection.size
        val readCount = stats?.readBooks ?: allBooksInCollection.count { it.readingStatus == Constants.ReadingStatus.READ }
        val totalPages = allBooksInCollection.sumOf { it.totalPages }

        binding.tvTotalBooksMetric.text = "$totalCount cuốn"
        binding.tvCompletedBooksMetric.text = "$readCount / $totalCount"

        val pagesFormatted = if (totalPages >= 1000) {
            String.format(Locale.getDefault(), "%,d tr", totalPages).replace(',', '.')
        } else {
            "$totalPages tr"
        }
        binding.tvTotalPagesMetric.text = pagesFormatted

        // Progress
        val progressPercent = if (totalCount > 0) (readCount * 100 / totalCount) else 0
        binding.tvProgressPercentage.text = "$progressPercent%"
        binding.pbCollectionProgress.progress = progressPercent

        // Last updated time
        val updateTimeStr = formatUpdateTime(col.updatedAt)
        binding.tvLastUpdated.text = updateTimeStr
    }

    private fun bindFeaturedQuote(books: List<Book>) {
        val bookIds = books.map { it.id }
        val quote = quoteRepository.getRandomQuoteForBookIds(bookIds)

        if (quote != null) {
            val sourceBook = books.firstOrNull { it.id == quote.bookId }
            binding.tvFeaturedQuoteContent.text = "“${quote.content.trim()}”"
            val title = sourceBook?.title ?: "Tác phẩm"
            val author = sourceBook?.author ?: ""
            binding.tvFeaturedQuoteSource.text = if (author.isNotBlank()) "$title — $author" else title
            binding.tvFeaturedQuotePage.text = if (quote.page.isNotBlank()) "Trang ${quote.page}" else ""
            binding.cardFeaturedQuote.visibility = View.VISIBLE
        } else if (books.isNotEmpty()) {
            // Hiển thị trích dẫn gợi mở văn học mặc định theo phong cách Stitch
            val firstBook = books.first()
            binding.tvFeaturedQuoteContent.text = "“Người có một lý do để sống có thể chịu đựng được hầu như mọi nghịch cảnh.”"
            binding.tvFeaturedQuoteSource.text = "${firstBook.title} — ${firstBook.author}"
            binding.tvFeaturedQuotePage.text = "Trích đoạn"
            binding.cardFeaturedQuote.visibility = View.VISIBLE
        } else {
            binding.cardFeaturedQuote.visibility = View.GONE
        }
    }

    private fun updatePillLabels() {
        val total = allBooksInCollection.size
        val reading = allBooksInCollection.count { it.readingStatus == Constants.ReadingStatus.READING }
        val completed = allBooksInCollection.count { it.readingStatus == Constants.ReadingStatus.READ }
        val unread = allBooksInCollection.count { it.readingStatus == Constants.ReadingStatus.UNREAD }

        binding.pillFilterAll.text = "Tất cả ($total)"
        binding.pillFilterReading.text = "Đang đọc ($reading)"
        binding.pillFilterCompleted.text = "Đã xong ($completed)"
        binding.pillFilterUnread.text = "Chưa đọc ($unread)"
    }

    private fun applyFilterAndSort() {
        var filtered = when (currentFilter) {
            StatusFilter.ALL -> allBooksInCollection
            StatusFilter.READING -> allBooksInCollection.filter { it.readingStatus == Constants.ReadingStatus.READING }
            StatusFilter.COMPLETED -> allBooksInCollection.filter { it.readingStatus == Constants.ReadingStatus.READ }
            StatusFilter.UNREAD -> allBooksInCollection.filter { it.readingStatus == Constants.ReadingStatus.UNREAD }
        }

        filtered = when (currentSort) {
            SortOption.PROGRESS -> filtered.sortedWith(
                compareByDescending<Book> {
                    if (it.readingStatus == Constants.ReadingStatus.READ) 100
                    else if (it.totalPages > 0) (it.currentPage * 100 / it.totalPages)
                    else 0
                }.thenBy { it.title }
            )
            SortOption.TITLE -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SortOption.RATING -> filtered.sortedWith(
                compareByDescending<Book> { it.rating }.thenBy { it.title }
            )
            SortOption.PAGES -> filtered.sortedWith(
                compareByDescending<Book> { it.totalPages }.thenBy { it.title }
            )
        }

        binding.tvSortLabel.text = currentSort.label

        val quotesMap = quoteRepository.getQuoteCountPerBook()
        adapter.submitData(filtered, quotesMap)

        if (filtered.isEmpty()) {
            binding.recyclerCollectionBooks.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE

            if (allBooksInCollection.isEmpty()) {
                binding.tvEmptyTitle.text = "Chưa có sách trong bộ sưu tập này"
                binding.tvEmptySubtitle.text = "Nhấn nút bên dưới để chọn các tác phẩm phù hợp"
                binding.btnEmptyAddBooks.visibility = View.VISIBLE
            } else {
                binding.tvEmptyTitle.text = "Không có sách ở mục này"
                binding.tvEmptySubtitle.text = "Thử chọn trạng thái lọc khác hoặc thêm sách mới"
                binding.btnEmptyAddBooks.visibility = View.GONE
            }
        } else {
            binding.recyclerCollectionBooks.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun showSortPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Tiến độ đọc")
        popup.menu.add(0, 2, 1, "Tựa đề (A-Z)")
        popup.menu.add(0, 3, 2, "Đánh giá sao")
        popup.menu.add(0, 4, 3, "Khối lượng trang")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> currentSort = SortOption.PROGRESS
                2 -> currentSort = SortOption.TITLE
                3 -> currentSort = SortOption.RATING
                4 -> currentSort = SortOption.PAGES
            }
            applyFilterAndSort()
            true
        }
        popup.show()
    }

    private fun showCollectionOptionsPopup(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Chỉnh sửa bộ sưu tập")
        popup.menu.add(0, 2, 1, "Chia sẻ bộ sưu tập")
        popup.menu.add(0, 3, 2, "Xóa bộ sưu tập này")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    val dialog = CreateCollectionDialog.newInstance(collectionId)
                    dialog.setOnSavedListener {
                        loadCollectionDetails()
                    }
                    dialog.show(supportFragmentManager, "EditCollectionDialog")
                    true
                }
                2 -> {
                    shareCollection()
                    true
                }
                3 -> {
                    confirmDeleteCollection()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showBookOptionsPopup(book: Book, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Xem chi tiết sách")
        popup.menu.add(0, 2, 1, "Bỏ khỏi bộ sưu tập")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    val intent = Intent(this, BookDetailActivity::class.java).apply {
                        putExtra(Constants.Extras.BOOK_ID, book.id)
                    }
                    NavigationHelper.startActivitySlide(this, intent)
                    true
                }
                2 -> {
                    confirmRemoveBook(book)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun confirmRemoveBook(book: Book) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_remove_book_title)
            .setMessage(getString(R.string.dialog_remove_book_message, book.title))
            .setPositiveButton(R.string.dialog_remove_book_confirm) { _, _ ->
                val success = collectionRepository.removeBookFromCollection(collectionId, book.id)
                if (success) {
                    val remaining = collectionRepository.getBooksInCollection(collectionId)
                    if (remaining.isEmpty()) {
                        collectionRepository.delete(collectionId)
                        Toast.makeText(this, R.string.msg_collection_deleted_empty, Toast.LENGTH_SHORT).show()
                        NavigationHelper.finishWithSlide(this)
                    } else {
                        Toast.makeText(this, getString(R.string.msg_book_removed_from_collection, book.title), Toast.LENGTH_SHORT).show()
                        loadCollectionDetails()
                    }
                } else {
                    Toast.makeText(this, "Có lỗi xảy ra", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.dialog_delete_cancel, null)
            .show()
    }

    private fun confirmDeleteCollection() {
        val colName = currentCollection?.name ?: "bộ sưu tập"
        AlertDialog.Builder(this)
            .setTitle("Xóa bộ sưu tập")
            .setMessage("Bạn có chắc chắn muốn xóa \"$colName\"? Sách trong tủ sách sẽ không bị xóa.")
            .setPositiveButton("Xóa") { _, _ ->
                val success = collectionRepository.delete(collectionId)
                if (success) {
                    Toast.makeText(this, "Đã xóa bộ sưu tập", Toast.LENGTH_SHORT).show()
                    NavigationHelper.finishWithSlide(this)
                } else {
                    Toast.makeText(this, "Không thể xóa bộ sưu tập", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun shareCollection() {
        val col = currentCollection ?: return
        val builder = StringBuilder()
        builder.append("📚 Bộ sưu tập: ${col.name}\n")
        if (col.description.isNotBlank()) {
            builder.append("${col.description}\n\n")
        }
        builder.append("Danh sách tác phẩm (${allBooksInCollection.size} cuốn):\n")
        allBooksInCollection.forEachIndexed { index, book ->
            val status = when (book.readingStatus) {
                Constants.ReadingStatus.READ -> "✓ Đã xong"
                Constants.ReadingStatus.READING -> "⏳ Đang đọc"
                else -> "○ Chưa đọc"
            }
            builder.append("${index + 1}. ${book.title} — ${book.author} [$status]\n")
        }
        builder.append("\nChia sẻ từ ứng dụng BookNest.")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, builder.toString())
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Chia sẻ bộ sưu tập"))
    }

    private fun formatUpdateTime(timeMs: Long): String {
        val diff = System.currentTimeMillis() - timeMs
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)

        return when {
            hours < 1 -> "Cập nhật gần đây"
            hours < 24 -> "Cập nhật $hours giờ trước"
            days == 1L -> "Cập nhật hôm qua"
            days < 7 -> "Cập nhật $days ngày trước"
            days < 30 -> "Cập nhật ${days / 7} tuần trước"
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                "Cập nhật ngày " + sdf.format(Date(timeMs))
            }
        }
    }

    companion object {
        const val EXTRA_COLLECTION_ID = "extra_collection_id"
    }
}
