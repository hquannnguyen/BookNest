package com.example.quanlysachcanhan.ui.collections

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.adapter.CollectionAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.CollectionRepository
import com.example.quanlysachcanhan.databinding.FragmentCollectionsBinding
import com.example.quanlysachcanhan.model.CollectionWithStats
import com.example.quanlysachcanhan.utils.NavigationHelper
import com.example.quanlysachcanhan.utils.PreferenceManager

class CollectionsFragment : Fragment() {

    private var _binding: FragmentCollectionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var collectionRepository: CollectionRepository
    private lateinit var bookRepository: BookRepository
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var adapter: CollectionAdapter

    private var allCollections: List<CollectionWithStats> = emptyList()
    private var currentFilterMode: FilterMode = FilterMode.ALL

    enum class FilterMode {
        ALL, UNREAD, READING, COMPLETED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext()
        collectionRepository = CollectionRepository(ctx)
        bookRepository = BookRepository(ctx)
        preferenceManager = PreferenceManager(ctx)

        // Không tự động tạo mẫu bộ sưu tập. Tự động dọn dẹp các bộ sưu tập mẫu rỗng cũ nếu có
        collectionRepository.cleanUpEmptySampleCollections()
        preferenceManager.collectionsSampleSeeded = true

        setupRecyclerView()
        setupFilterChips()
        setupActions()

        loadCollections()
    }

    override fun onResume() {
        super.onResume()
        loadCollections()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && isAdded && _binding != null && context != null) {
            loadCollections()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        adapter = CollectionAdapter(
            onItemClick = { item ->
                val intent = Intent(requireContext(), CollectionDetailActivity::class.java).apply {
                    putExtra(CollectionDetailActivity.EXTRA_COLLECTION_ID, item.collection.id)
                }
                NavigationHelper.startActivitySlide(requireActivity(), intent)
            },
            onEditClick = { item ->
                openCreateOrEditDialog(item.collection.id)
            },
            onDeleteClick = { item ->
                confirmDeleteCollection(item)
            }
        )

        binding.rvCollections.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCollections.adapter = adapter
    }

    private fun setupFilterChips() {
        binding.chipFilterAll.setOnClickListener {
            setFilter(FilterMode.ALL)
        }
        binding.chipFilterUnread.setOnClickListener {
            setFilter(FilterMode.UNREAD)
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

        val ctx = context ?: return
        val selectedBg = R.drawable.bg_chip_selected
        val unselectedBg = R.drawable.bg_chip_unselected
        val selectedTextColor = ContextCompat.getColor(ctx, R.color.surface_card)
        val unselectedTextColor = ContextCompat.getColor(ctx, R.color.text_primary)

        binding.chipFilterAll.setBackgroundResource(if (mode == FilterMode.ALL) selectedBg else unselectedBg)
        binding.chipFilterAll.setTextColor(if (mode == FilterMode.ALL) selectedTextColor else unselectedTextColor)

        binding.chipFilterUnread.setBackgroundResource(if (mode == FilterMode.UNREAD) selectedBg else unselectedBg)
        binding.chipFilterUnread.setTextColor(if (mode == FilterMode.UNREAD) selectedTextColor else unselectedTextColor)

        binding.chipFilterReading.setBackgroundResource(if (mode == FilterMode.READING) selectedBg else unselectedBg)
        binding.chipFilterReading.setTextColor(if (mode == FilterMode.READING) selectedTextColor else unselectedTextColor)

        binding.chipFilterCompleted.setBackgroundResource(if (mode == FilterMode.COMPLETED) selectedBg else unselectedBg)
        binding.chipFilterCompleted.setTextColor(if (mode == FilterMode.COMPLETED) selectedTextColor else unselectedTextColor)

        applyFilterToAdapter()
    }

    private fun setupActions() {
        binding.fabCreateCollection.setOnClickListener {
            openCreateOrEditDialog(0L)
        }
        binding.layoutEmptyCollections.setOnClickListener {
            openCreateOrEditDialog(0L)
        }
    }

    private fun openCreateOrEditDialog(collectionId: Long = 0L) {
        val dialog = CreateCollectionDialog.newInstance(collectionId)
        dialog.setOnSavedListener {
            loadCollections()
        }
        dialog.show(childFragmentManager, "CreateCollectionDialog")
    }

    fun loadCollections() {
        val b = _binding ?: return
        if (!isAdded || context == null) return
        allCollections = collectionRepository.getAllWithStats()

        // 1. Overview card stats
        val overview = collectionRepository.getOverviewStats()
        if (overview.totalCollections == 0) {
            b.tvOverviewTitle.text = getString(R.string.collections_overview_empty_title)
            b.tvOverviewSub.text = getString(R.string.collections_overview_empty_sub)
            b.progressBarOverview.progress = 0
        } else {
            b.tvOverviewTitle.text = getString(
                R.string.collections_overview_title,
                overview.totalCollections,
                overview.totalBooksInCollections
            )
            b.tvOverviewSub.text = getString(
                R.string.collections_overview_sub,
                overview.readBooksInCollections,
                overview.totalBooksInCollections,
                overview.percent
            )
            b.progressBarOverview.progress = overview.percent
        }

        // 2. Filter chip counters
        val unreadCount = allCollections.count { it.unreadBooks > 0 }
        val readingCount = allCollections.count { it.readingBooks > 0 }
        val completedCount = allCollections.count { it.totalBooks > 0 && it.readBooks == it.totalBooks }

        b.chipFilterAll.text = getString(R.string.collections_filter_all, allCollections.size)
        b.chipFilterUnread.text = getString(R.string.collections_filter_unread, unreadCount)
        b.chipFilterReading.text = getString(R.string.collections_filter_reading, readingCount)
        b.chipFilterCompleted.text = getString(R.string.collections_filter_completed, completedCount)

        applyFilterToAdapter()
    }

    private fun applyFilterToAdapter() {
        val b = _binding ?: return
        val filteredList = when (currentFilterMode) {
            FilterMode.ALL -> allCollections
            FilterMode.UNREAD -> allCollections.filter { it.unreadBooks > 0 }
            FilterMode.READING -> allCollections.filter { it.readingBooks > 0 }
            FilterMode.COMPLETED -> allCollections.filter { it.totalBooks > 0 && it.readBooks == it.totalBooks }
        }

        adapter.submitList(filteredList)

        if (filteredList.isEmpty()) {
            b.layoutEmptyCollections.visibility = View.VISIBLE
            b.rvCollections.visibility = View.GONE
        } else {
            b.layoutEmptyCollections.visibility = View.GONE
            b.rvCollections.visibility = View.VISIBLE
        }
    }

    private fun confirmDeleteCollection(item: CollectionWithStats) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_collection_title)
            .setMessage(getString(R.string.dialog_delete_collection_message, item.collection.name))
            .setPositiveButton(R.string.dialog_delete_confirm) { _, _ ->
                collectionRepository.delete(item.collection.id)
                Toast.makeText(requireContext(), R.string.msg_collection_deleted, Toast.LENGTH_SHORT).show()
                loadCollections()
            }
            .setNegativeButton(R.string.dialog_delete_cancel, null)
            .show()
    }

    companion object {
        fun newInstance() = CollectionsFragment()
    }
}
