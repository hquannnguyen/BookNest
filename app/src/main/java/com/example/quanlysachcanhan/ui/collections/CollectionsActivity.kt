package com.example.quanlysachcanhan.ui.collections

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.adapter.CollectionAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.CollectionRepository
import com.example.quanlysachcanhan.databinding.ActivityCollectionsBinding
import com.example.quanlysachcanhan.model.CollectionWithStats
import com.example.quanlysachcanhan.ui.main.MainActivity
import com.example.quanlysachcanhan.utils.NavigationHelper
import com.example.quanlysachcanhan.utils.PreferenceManager

class CollectionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectionsBinding
    private lateinit var collectionRepository: CollectionRepository
    private lateinit var bookRepository: BookRepository
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: CollectionAdapter

    private var allCollections: List<CollectionWithStats> = emptyList()
    private var currentFilterMode: FilterMode = FilterMode.ALL

    enum class FilterMode {
        ALL, READING, COMPLETED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        collectionRepository = CollectionRepository(this)
        bookRepository = BookRepository(this)
        preferenceManager = PreferenceManager(this)

        // Seed sample collections if empty so the screen looks vibrant on first launch
        collectionRepository.seedSampleCollectionsIfEmpty(bookRepository.getAll())

        setupTopHeader()
        setupBottomNavigation()
        setupRecyclerView()
        setupFilterChips()
        setupActions()

        loadCollections()
    }

    override fun onResume() {
        super.onResume()
        loadCollections()
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
            NavigationHelper.finishWithSlide(this@CollectionsActivity)
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
        adapter = CollectionAdapter(
            onItemClick = { item ->
                val intent = Intent(this, CollectionDetailActivity::class.java).apply {
                    putExtra(CollectionDetailActivity.EXTRA_COLLECTION_ID, item.collection.id)
                }
                NavigationHelper.startActivitySlide(this, intent)
            },
            onEditClick = { item ->
                openCreateOrEditDialog(item.collection.id)
            },
            onDeleteClick = { item ->
                confirmDeleteCollection(item)
            }
        )

        binding.rvCollections.layoutManager = LinearLayoutManager(this)
        binding.rvCollections.adapter = adapter
    }

    private fun setupFilterChips() {
        binding.chipFilterAll.setOnClickListener {
            setFilter(FilterMode.ALL)
        }
        binding.chipFilterReading.setOnClickListener {
            setFilter(FilterMode.READING)
        }
        binding.chipFilterCompleted.setOnClickListener {
            setFilter(FilterMode.COMPLETED)
        }
    }

    private fun setFilter(mode: FilterMode) {
        currentFilterMode = mode

        // Update styling of chips
        val selectedBg = R.drawable.bg_chip_selected
        val unselectedBg = R.drawable.bg_chip_unselected
        val selectedTextColor = ContextCompat.getColor(this, R.color.surface_card)
        val unselectedTextColor = ContextCompat.getColor(this, R.color.text_primary)

        binding.chipFilterAll.setBackgroundResource(if (mode == FilterMode.ALL) selectedBg else unselectedBg)
        binding.chipFilterAll.setTextColor(if (mode == FilterMode.ALL) selectedTextColor else unselectedTextColor)

        binding.chipFilterReading.setBackgroundResource(if (mode == FilterMode.READING) selectedBg else unselectedBg)
        binding.chipFilterReading.setTextColor(if (mode == FilterMode.READING) selectedTextColor else unselectedTextColor)

        binding.chipFilterCompleted.setBackgroundResource(if (mode == FilterMode.COMPLETED) selectedBg else unselectedBg)
        binding.chipFilterCompleted.setTextColor(if (mode == FilterMode.COMPLETED) selectedTextColor else unselectedTextColor)

        applyFilterToAdapter()
    }

    private fun setupActions() {
        binding.btnOverviewCreate.setOnClickListener {
            openCreateOrEditDialog(0L)
        }

        binding.cardAddCollectionDashed.setOnClickListener {
            openCreateOrEditDialog(0L)
        }

        binding.fabCreateCollection.setOnClickListener {
            openCreateOrEditDialog(0L)
        }
    }

    private fun openCreateOrEditDialog(collectionId: Long = 0L) {
        val dialog = CreateCollectionDialog.newInstance(collectionId)
        dialog.setOnSavedListener {
            loadCollections()
        }
        dialog.show(supportFragmentManager, "CreateCollectionDialog")
    }

    private fun loadCollections() {
        allCollections = collectionRepository.getAllWithStats()

        // 1. Overview card stats
        val overview = collectionRepository.getOverviewStats()
        binding.tvOverviewTitle.text = "${overview.totalCollections} Bộ sưu tập • ${overview.totalBooksInCollections} Sách"
        binding.tvOverviewSub.text = "${overview.readBooksInCollections}/${overview.totalBooksInCollections} cuốn đã hoàn thành (${overview.percent}%)"
        binding.progressBarOverview.progress = overview.percent

        // 2. Filter chip counters
        val readingCount = allCollections.count { it.readingBooks > 0 }
        val completedCount = allCollections.count { it.totalBooks > 0 && it.readBooks == it.totalBooks }

        binding.chipFilterAll.text = "Tất cả (${allCollections.size})"
        binding.chipFilterReading.text = "Đang đọc dở ($readingCount)"
        binding.chipFilterCompleted.text = "Đã xong ($completedCount)"

        applyFilterToAdapter()
    }

    private fun applyFilterToAdapter() {
        val filteredList = when (currentFilterMode) {
            FilterMode.ALL -> allCollections
            FilterMode.READING -> allCollections.filter { it.readingBooks > 0 }
            FilterMode.COMPLETED -> allCollections.filter { it.totalBooks > 0 && it.readBooks == it.totalBooks }
        }

        adapter.submitList(filteredList)

        if (filteredList.isEmpty()) {
            binding.layoutEmptyCollections.visibility = View.VISIBLE
            binding.rvCollections.visibility = View.GONE
        } else {
            binding.layoutEmptyCollections.visibility = View.GONE
            binding.rvCollections.visibility = View.VISIBLE
        }
    }

    private fun confirmDeleteCollection(item: CollectionWithStats) {
        AlertDialog.Builder(this)
            .setTitle("Xóa bộ sưu tập")
            .setMessage("Bạn có chắc chắn muốn xóa bộ sưu tập \"${item.collection.name}\"?\n(Các cuốn sách bên trong vẫn được giữ nguyên trong thư viện của bạn).")
            .setPositiveButton("Xóa") { _, _ ->
                collectionRepository.delete(item.collection.id)
                Toast.makeText(this, "Đã xóa bộ sưu tập", Toast.LENGTH_SHORT).show()
                loadCollections()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
