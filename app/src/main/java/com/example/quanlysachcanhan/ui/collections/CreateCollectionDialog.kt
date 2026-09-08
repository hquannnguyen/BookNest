package com.example.quanlysachcanhan.ui.collections

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.adapter.BookPickerAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.CollectionRepository
import com.example.quanlysachcanhan.databinding.DialogCreateCollectionBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.model.BookCollection
import com.example.quanlysachcanhan.utils.Constants
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CreateCollectionDialog : BottomSheetDialogFragment() {

    private var _binding: DialogCreateCollectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookRepository: BookRepository
    private lateinit var collectionRepository: CollectionRepository
    private lateinit var bookPickerAdapter: BookPickerAdapter

    private var editingCollectionId: Long = 0L
    private var selectedColorHex: String = "#7D562D"
    private var selectedIconName: String = "auto_stories"
    private var onSavedListener: (() -> Unit)? = null

    private var currentSearchQuery: String = ""
    private var currentStatusFilter: String? = null // null = Tất cả

    fun setOnSavedListener(listener: () -> Unit) {
        this.onSavedListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingCollectionId = arguments?.getLong(ARG_COLLECTION_ID, 0L) ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val d = dialog as? BottomSheetDialog ?: return
        @Suppress("DEPRECATION")
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        bottomSheet.setBackgroundResource(android.R.color.transparent)

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        behavior.isFitToContents = true

        val displayMetrics = resources.displayMetrics
        val targetHeight = (displayMetrics.heightPixels * 0.90).toInt()
        bottomSheet.layoutParams.height = targetHeight
        bottomSheet.requestLayout()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val density = resources.displayMetrics.density
            val defaultBottomPad = (28 * density).toInt()
            val bottomPadding = if (imeHeight > 0) {
                imeHeight + (8 * density).toInt()
            } else {
                maxOf(navHeight, defaultBottomPad)
            }
            binding.layoutBottomActions.setPadding(
                binding.layoutBottomActions.paddingLeft,
                binding.layoutBottomActions.paddingTop,
                binding.layoutBottomActions.paddingRight,
                bottomPadding
            )
            insets
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bookRepository = BookRepository(requireContext())
        collectionRepository = CollectionRepository(requireContext())

        setupColorPicker()
        setupIconPicker()
        setupBookPicker()
        setupButtons()

        if (editingCollectionId > 0L) {
            loadExistingCollection()
        }
    }

    private fun setupColorPicker() {
        val colorNavy = binding.colorNavy
        val colorOak = binding.colorOak
        val colorAmber = binding.colorAmber
        val colorSlate = binding.colorSlate
        val colorGold = binding.colorGold
        val colorDark = binding.colorDark

        val allChecks = listOf(
            binding.checkNavy,
            binding.checkOak,
            binding.checkAmber,
            binding.checkSlate,
            binding.checkGold,
            binding.checkDark
        )

        fun selectColor(colorHex: String, checkToVisible: ImageView) {
            selectedColorHex = colorHex
            allChecks.forEach { it.visibility = View.GONE }
            checkToVisible.visibility = View.VISIBLE
        }

        colorNavy.setOnClickListener { selectColor("#031632", binding.checkNavy) }
        colorOak.setOnClickListener { selectColor("#7D562D", binding.checkOak) }
        colorAmber.setOnClickListener { selectColor("#FFCA98", binding.checkAmber) }
        colorSlate.setOnClickListener { selectColor("#4E5F7E", binding.checkSlate) }
        colorGold.setOnClickListener { selectColor("#D4A373", binding.checkGold) }
        colorDark.setOnClickListener { selectColor("#2C1600", binding.checkDark) }

        // Default: Oak
        selectColor("#7D562D", binding.checkOak)
    }

    private fun setupIconPicker() {
        val iconButtons = mapOf(
            "auto_stories" to Pair(binding.btnIconStories, binding.ivIconStories),
            "psychology" to Pair(binding.btnIconPsychology, binding.ivIconPsychology),
            "trending_up" to Pair(binding.btnIconTrending, binding.ivIconTrending),
            "account_balance" to Pair(binding.btnIconFinance, binding.ivIconFinance),
            "favorite" to Pair(binding.btnIconFavorite, binding.ivIconFavorite),
            "science" to Pair(binding.btnIconScience, binding.ivIconScience)
        )

        fun selectIcon(iconName: String) {
            selectedIconName = iconName
            iconButtons.forEach { (name, pair) ->
                val (btn, iv) = pair
                if (name == iconName) {
                    btn.setBackgroundResource(R.drawable.bg_chip_selected)
                    iv.setColorFilter(Color.WHITE)
                } else {
                    btn.setBackgroundResource(R.drawable.bg_chip_unselected)
                    iv.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_navy))
                }
            }
        }

        iconButtons.forEach { (name, pair) ->
            pair.first.setOnClickListener { selectIcon(name) }
        }

        selectIcon("auto_stories")
    }

    private fun setupBookPicker() {
        val allBooks = bookRepository.getAll()
        val initialIds = if (editingCollectionId > 0L) {
            collectionRepository.getBookIdsInCollection(editingCollectionId).toSet()
        } else {
            emptySet()
        }

        updateSelectionBadge(initialIds.size)

        // Book counts for chips
        val countAll = allBooks.size
        val countUnread = allBooks.count { it.readingStatus == Constants.ReadingStatus.UNREAD }
        val countReading = allBooks.count { it.readingStatus == Constants.ReadingStatus.READING }
        val countRead = allBooks.count { it.readingStatus == Constants.ReadingStatus.READ }

        binding.chipPickerAll.text = "Tất cả ($countAll)"
        binding.chipPickerUnread.text = "Chưa đọc ($countUnread)"
        binding.chipPickerReading.text = "Đang đọc ($countReading)"
        binding.chipPickerRead.text = "Đã đọc ($countRead)"

        fun updateChipStyles(selectedChip: TextView) {
            val chips = listOf(
                binding.chipPickerAll,
                binding.chipPickerUnread,
                binding.chipPickerReading,
                binding.chipPickerRead
            )
            chips.forEach { chip ->
                if (chip == selectedChip) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected)
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.surface_card))
                    chip.setTypeface(null, Typeface.BOLD)
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    chip.setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        fun applyFilter() {
            bookPickerAdapter.filter(currentSearchQuery, currentStatusFilter)
            val isEmpty = bookPickerAdapter.itemCount == 0
            binding.tvNoBooksHint.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvBookPicker.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        binding.chipPickerAll.setOnClickListener {
            currentStatusFilter = null
            updateChipStyles(binding.chipPickerAll)
            applyFilter()
        }

        binding.chipPickerUnread.setOnClickListener {
            currentStatusFilter = Constants.ReadingStatus.UNREAD
            updateChipStyles(binding.chipPickerUnread)
            applyFilter()
        }

        binding.chipPickerReading.setOnClickListener {
            currentStatusFilter = Constants.ReadingStatus.READING
            updateChipStyles(binding.chipPickerReading)
            applyFilter()
        }

        binding.chipPickerRead.setOnClickListener {
            currentStatusFilter = Constants.ReadingStatus.READ
            updateChipStyles(binding.chipPickerRead)
            applyFilter()
        }

        bookPickerAdapter = BookPickerAdapter(
            allBooks = allBooks,
            initialSelectedIds = initialIds,
            onSelectionChanged = { count, _ ->
                updateSelectionBadge(count)
            }
        )

        binding.rvBookPicker.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBookPicker.adapter = bookPickerAdapter

        // Initial filter & visibility check
        applyFilter()

        // Meta collapse/expand when searching
        fun showMetaSummary(collapsed: Boolean) {
            if (collapsed) {
                val name = binding.etCollectionName.text.toString().trim()
                binding.tvCollapsedMetaTitle.text = if (name.isNotEmpty()) "Bộ sưu tập: $name" else "Bộ sưu tập: Chưa đặt tên"
                binding.layoutCollectionMeta.visibility = View.GONE
                binding.layoutMetaCollapsedSummary.visibility = View.VISIBLE
            } else {
                binding.layoutCollectionMeta.visibility = View.VISIBLE
                binding.layoutMetaCollapsedSummary.visibility = View.GONE
            }
        }

        binding.btnToggleMeta.setOnClickListener {
            val isCurrentlyCollapsed = binding.layoutCollectionMeta.visibility == View.GONE
            showMetaSummary(!isCurrentlyCollapsed)
        }

        // Search in library
        binding.etSearchBooks.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString().orEmpty()
                if (currentSearchQuery.trim().isNotEmpty()) {
                    showMetaSummary(true)
                }
                applyFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateSelectionBadge(count: Int) {
        binding.tvSelectedBooksCount.text = "$count cuốn"
        val isEdit = editingCollectionId > 0L
        binding.btnSaveCollection.text = if (isEdit) {
            if (count > 0) "Lưu Thay Đổi ($count cuốn)" else "Lưu Thay Đổi"
        } else {
            if (count > 0) "Tạo Bộ Sưu Tập ($count cuốn)" else "Tạo Bộ Sưu Tập"
        }
    }

    private fun loadExistingCollection() {
        val col = collectionRepository.getById(editingCollectionId) ?: return
        binding.tvDialogTitle.text = "Chỉnh Sửa Bộ Sưu Tập"
        binding.etCollectionName.setText(col.name)
        binding.etCollectionDesc.setText(col.description)

        // Select color
        when (col.colorHex.uppercase()) {
            "#031632" -> binding.colorNavy.performClick()
            "#7D562D" -> binding.colorOak.performClick()
            "#FFCA98" -> binding.colorAmber.performClick()
            "#4E5F7E" -> binding.colorSlate.performClick()
            "#D4A373" -> binding.colorGold.performClick()
            "#2C1600" -> binding.colorDark.performClick()
            else -> binding.colorOak.performClick()
        }

        // Select icon
        when (col.iconName) {
            "psychology" -> binding.btnIconPsychology.performClick()
            "trending_up" -> binding.btnIconTrending.performClick()
            "account_balance" -> binding.btnIconFinance.performClick()
            "favorite" -> binding.btnIconFavorite.performClick()
            "science" -> binding.btnIconScience.performClick()
            else -> binding.btnIconStories.performClick()
        }
    }

    private fun setupButtons() {
        binding.btnClose.setOnClickListener { dismiss() }

        binding.btnSaveCollection.setOnClickListener {
            val name = binding.etCollectionName.text.toString().trim()
            if (name.isEmpty()) {
                binding.layoutCollectionMeta.visibility = View.VISIBLE
                binding.layoutMetaCollapsedSummary.visibility = View.GONE
                binding.etCollectionName.error = "Vui lòng nhập tên bộ sưu tập"
                binding.etCollectionName.requestFocus()
                return@setOnClickListener
            }

            val desc = binding.etCollectionDesc.text.toString().trim()
            val selectedBookIds = bookPickerAdapter.getSelectedBookIds()

            if (editingCollectionId > 0L) {
                val existing = collectionRepository.getById(editingCollectionId)
                if (existing != null) {
                    val updated = existing.copy(
                        name = name,
                        description = desc,
                        colorHex = selectedColorHex,
                        iconName = selectedIconName,
                        updatedAt = System.currentTimeMillis()
                    )
                    collectionRepository.update(updated, selectedBookIds)
                    Toast.makeText(requireContext(), "Đã cập nhật bộ sưu tập", Toast.LENGTH_SHORT).show()
                }
            } else {
                val newCol = BookCollection(
                    name = name,
                    description = desc,
                    colorHex = selectedColorHex,
                    iconName = selectedIconName
                )
                collectionRepository.insert(newCol, selectedBookIds)
                Toast.makeText(requireContext(), "Đã tạo bộ sưu tập mới", Toast.LENGTH_SHORT).show()
            }

            onSavedListener?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COLLECTION_ID = "arg_collection_id"

        fun newInstance(collectionId: Long = 0L): CreateCollectionDialog {
            return CreateCollectionDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_COLLECTION_ID, collectionId)
                }
            }
        }
    }
}
