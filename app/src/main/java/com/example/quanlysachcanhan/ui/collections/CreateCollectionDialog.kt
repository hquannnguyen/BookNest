package com.example.quanlysachcanhan.ui.collections

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.adapter.BookPickerAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.CollectionRepository
import com.example.quanlysachcanhan.databinding.DialogCreateCollectionBinding
import com.example.quanlysachcanhan.databinding.DialogExtendedColorPickerBinding
import com.example.quanlysachcanhan.databinding.DialogExtendedIconPickerBinding
import com.example.quanlysachcanhan.databinding.ItemExtendedIconBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.model.BookCollection
import com.example.quanlysachcanhan.utils.CollectionIconHelper
import com.example.quanlysachcanhan.utils.CollectionIconItem
import com.example.quanlysachcanhan.utils.Constants
import com.example.quanlysachcanhan.utils.LocaleHelper
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
        } else {
            applySelectedColor("#7D562D")
            applySelectedIcon("auto_stories")
        }
    }

    private fun setupColorPicker() {
        binding.colorNavy.setOnClickListener { applySelectedColor("#031632") }
        binding.colorOak.setOnClickListener { applySelectedColor("#7D562D") }
        binding.colorAmber.setOnClickListener { applySelectedColor("#B45309") }
        binding.colorWine.setOnClickListener { applySelectedColor("#991B1B") }
        binding.colorEmerald.setOnClickListener { applySelectedColor("#065F46") }
        binding.colorCyan.setOnClickListener { applySelectedColor("#0284C7") }
        binding.colorPurple.setOnClickListener { applySelectedColor("#6B21A8") }
        binding.colorCharcoal.setOnClickListener { applySelectedColor("#1F2937") }

        binding.colorMore.setOnClickListener {
            showExtendedColorPickerDialog()
        }
    }

    private fun applySelectedColor(colorHex: String) {
        selectedColorHex = colorHex

        val allChecks = listOf(
            binding.checkNavy,
            binding.checkOak,
            binding.checkAmber,
            binding.checkWine,
            binding.checkEmerald,
            binding.checkCyan,
            binding.checkPurple,
            binding.checkCharcoal,
            binding.checkColorMore
        )
        allChecks.forEach { it.visibility = View.GONE }
        binding.ivColorMore.visibility = View.VISIBLE
        binding.colorMore.setBackgroundResource(R.drawable.bg_chip_unselected)

        when (colorHex.uppercase()) {
            "#031632" -> binding.checkNavy.visibility = View.VISIBLE
            "#7D562D" -> binding.checkOak.visibility = View.VISIBLE
            "#B45309", "#FFCA98" -> binding.checkAmber.visibility = View.VISIBLE
            "#991B1B" -> binding.checkWine.visibility = View.VISIBLE
            "#065F46" -> binding.checkEmerald.visibility = View.VISIBLE
            "#0284C7", "#4E5F7E" -> binding.checkCyan.visibility = View.VISIBLE
            "#6B21A8" -> binding.checkPurple.visibility = View.VISIBLE
            "#1F2937", "#2C1600" -> binding.checkCharcoal.visibility = View.VISIBLE
            else -> {
                try {
                    val parsed = Color.parseColor(colorHex)
                    binding.colorMore.background.mutate().setTint(parsed)
                    binding.ivColorMore.visibility = View.GONE
                    binding.checkColorMore.visibility = View.VISIBLE
                } catch (e: Exception) {
                    binding.checkOak.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showExtendedColorPickerDialog() {
        val ctx = requireContext()
        val dialogBinding = DialogExtendedColorPickerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(ctx)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        var currentColor = selectedColorHex
        dialogBinding.etHexCode.setText(currentColor)

        fun updatePreview(hexStr: String) {
            try {
                val parsed = Color.parseColor(hexStr)
                dialogBinding.viewColorPreview.background.mutate().setTint(parsed)
                dialogBinding.ivPreviewIcon.setImageResource(CollectionIconHelper.getIconRes(selectedIconName))
                currentColor = hexStr
            } catch (e: Exception) {
                // Ignore while typing
            }
        }
        updatePreview(currentColor)

        dialogBinding.etHexCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val raw = s?.toString()?.trim().orEmpty()
                val hex = if (!raw.startsWith("#")) "#$raw" else raw
                updatePreview(hex)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 36 Material Palette Swatches
        val materialPalette = listOf(
            "#EF4444", "#DC2626", "#B91C1C", "#991B1B", "#F43F5E", "#E11D48",
            "#F97316", "#EA580C", "#C2410C", "#F59E0B", "#D97706", "#B45309",
            "#10B981", "#059669", "#047857", "#065F46", "#84CC16", "#15803D",
            "#06B6D4", "#0891B2", "#0284C7", "#0369A1", "#3B82F6", "#1D4ED8",
            "#6366F1", "#4F46E5", "#4338CA", "#8B5CF6", "#7C3AED", "#6B21A8",
            "#031632", "#7D562D", "#D4A373", "#4E5F7E", "#1F2937", "#2C1600"
        )

        dialogBinding.gridMaterialColors.removeAllViews()
        val density = resources.displayMetrics.density
        val size = (38 * density).toInt()
        val margin = (4 * density).toInt()

        materialPalette.forEach { hex ->
            val swatch = FrameLayout(ctx).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                }
                setBackgroundResource(R.drawable.bg_status_badge)
                background.mutate().setTint(Color.parseColor(hex))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    dialogBinding.etHexCode.setText(hex)
                    updatePreview(hex)
                }
            }
            dialogBinding.gridMaterialColors.addView(swatch)
        }

        dialogBinding.btnApplyColor.setOnClickListener {
            val raw = dialogBinding.etHexCode.text.toString().trim()
            val hex = if (!raw.startsWith("#")) "#$raw" else raw
            try {
                Color.parseColor(hex)
                applySelectedColor(hex)
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(ctx, R.string.error_invalid_hex, Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnColorPickerClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupIconPicker() {
        binding.btnIconStories.setOnClickListener { applySelectedIcon("auto_stories") }
        binding.btnIconPsychology.setOnClickListener { applySelectedIcon("psychology") }
        binding.btnIconTrending.setOnClickListener { applySelectedIcon("trending_up") }
        binding.btnIconFinance.setOnClickListener { applySelectedIcon("account_balance") }
        binding.btnIconFavorite.setOnClickListener { applySelectedIcon("favorite") }
        binding.btnIconScience.setOnClickListener { applySelectedIcon("science") }

        binding.btnIconMore.setOnClickListener {
            showExtendedIconPickerDialog()
        }
    }

    private fun applySelectedIcon(iconName: String) {
        selectedIconName = iconName
        val primaryMap = mapOf(
            "auto_stories" to (binding.btnIconStories to binding.ivIconStories),
            "psychology" to (binding.btnIconPsychology to binding.ivIconPsychology),
            "trending_up" to (binding.btnIconTrending to binding.ivIconTrending),
            "account_balance" to (binding.btnIconFinance to binding.ivIconFinance),
            "favorite" to (binding.btnIconFavorite to binding.ivIconFavorite),
            "science" to (binding.btnIconScience to binding.ivIconScience)
        )

        primaryMap.forEach { (_, pair) ->
            pair.first.setBackgroundResource(R.drawable.bg_chip_unselected)
            pair.second.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_navy))
        }

        binding.btnIconMore.setBackgroundResource(R.drawable.bg_chip_unselected)
        binding.ivIconMore.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_navy))
        binding.ivIconMore.setImageResource(R.drawable.ic_more_horiz)

        if (primaryMap.containsKey(iconName)) {
            val (btn, iv) = primaryMap[iconName]!!
            btn.setBackgroundResource(R.drawable.bg_chip_selected)
            iv.setColorFilter(Color.WHITE)
        } else {
            binding.btnIconMore.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.ivIconMore.setImageResource(CollectionIconHelper.getIconRes(iconName))
            binding.ivIconMore.setColorFilter(Color.WHITE)
        }
    }

    private fun showExtendedIconPickerDialog() {
        val ctx = requireContext()
        val dialogBinding = DialogExtendedIconPickerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(ctx)
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val icons = CollectionIconHelper.ALL_ICONS
        val isVietnamese = LocaleHelper.isVietnamese()

        class ExtendedIconViewHolder(val itemBinding: ItemExtendedIconBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        val adapter = object : RecyclerView.Adapter<ExtendedIconViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExtendedIconViewHolder {
                val itemBinding = ItemExtendedIconBinding.inflate(layoutInflater, parent, false)
                return ExtendedIconViewHolder(itemBinding)
            }

            override fun onBindViewHolder(holder: ExtendedIconViewHolder, position: Int) {
                val item = icons[position]
                holder.itemBinding.ivItemIcon.setImageResource(item.iconRes)
                holder.itemBinding.tvItemIconLabel.text = if (isVietnamese) item.labelVi else item.labelEn

                val isSelected = item.key == selectedIconName
                if (isSelected) {
                    holder.itemBinding.containerIconBg.setBackgroundResource(R.drawable.bg_chip_selected)
                    holder.itemBinding.ivItemIcon.setColorFilter(Color.WHITE)
                    holder.itemBinding.ivSelectedCheck.visibility = View.VISIBLE
                } else {
                    holder.itemBinding.containerIconBg.setBackgroundResource(R.drawable.bg_chip_unselected)
                    holder.itemBinding.ivItemIcon.setColorFilter(ContextCompat.getColor(ctx, R.color.primary_navy))
                    holder.itemBinding.ivSelectedCheck.visibility = View.GONE
                }

                holder.itemBinding.layoutIconItem.setOnClickListener {
                    applySelectedIcon(item.key)
                    dialog.dismiss()
                }
            }

            override fun getItemCount(): Int = icons.size
        }

        dialogBinding.rvExtendedIcons.layoutManager = GridLayoutManager(ctx, 4)
        dialogBinding.rvExtendedIcons.adapter = adapter

        dialogBinding.btnIconPickerClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupBookPicker() {
        val allBooks = bookRepository.getAll()
        val initialIds = if (editingCollectionId > 0L) {
            collectionRepository.getBookIdsInCollection(editingCollectionId).toSet()
        } else {
            emptySet()
        }

        updateSelectionBadge(initialIds.size)

        val countAll = allBooks.size
        val countUnread = allBooks.count { it.readingStatus == Constants.ReadingStatus.UNREAD }
        val countReading = allBooks.count { it.readingStatus == Constants.ReadingStatus.READING }
        val countRead = allBooks.count { it.readingStatus == Constants.ReadingStatus.READ }

        binding.chipPickerAll.text = getString(R.string.picker_chip_all, countAll)
        binding.chipPickerUnread.text = getString(R.string.picker_chip_unread, countUnread)
        binding.chipPickerReading.text = getString(R.string.picker_chip_reading, countReading)
        binding.chipPickerRead.text = getString(R.string.picker_chip_read, countRead)

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

        applyFilter()

        fun showMetaSummary(collapsed: Boolean) {
            if (collapsed) {
                val name = binding.etCollectionName.text.toString().trim()
                val displayTitle = name.ifEmpty { getString(R.string.collection_meta_untitled) }
                binding.tvCollapsedMetaTitle.text = getString(R.string.collection_meta_prefix, displayTitle)
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
        binding.tvSelectedBooksCount.text = getString(R.string.count_books_format, count)
        val isEdit = editingCollectionId > 0L
        binding.btnSaveCollection.text = if (isEdit) {
            if (count > 0) getString(R.string.btn_save_collection_with_count, count) else getString(R.string.btn_save_collection)
        } else {
            if (count > 0) getString(R.string.btn_create_collection_with_count, count) else getString(R.string.btn_create_collection)
        }
    }

    private fun loadExistingCollection() {
        val col = collectionRepository.getById(editingCollectionId) ?: return
        binding.tvDialogTitle.text = getString(R.string.dialog_edit_collection_title)
        binding.etCollectionName.setText(col.name)
        binding.etCollectionDesc.setText(col.description)

        applySelectedColor(col.colorHex)
        applySelectedIcon(col.iconName)
    }

    private fun setupButtons() {
        binding.btnClose.setOnClickListener { dismiss() }

        binding.btnSaveCollection.setOnClickListener {
            val name = binding.etCollectionName.text.toString().trim()
            if (name.isEmpty()) {
                binding.layoutCollectionMeta.visibility = View.VISIBLE
                binding.layoutMetaCollapsedSummary.visibility = View.GONE
                binding.etCollectionName.error = getString(R.string.error_collection_name_empty)
                binding.etCollectionName.requestFocus()
                return@setOnClickListener
            }

            val desc = binding.etCollectionDesc.text.toString().trim()
            val selectedBookIds = bookPickerAdapter.getSelectedBookIds()

            if (editingCollectionId > 0L) {
                // Tự động xóa bộ sưu tập khi rỗng
                if (selectedBookIds.isEmpty()) {
                    collectionRepository.delete(editingCollectionId)
                    Toast.makeText(requireContext(), R.string.msg_collection_deleted_empty, Toast.LENGTH_LONG).show()
                } else {
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
                        Toast.makeText(requireContext(), R.string.msg_collection_updated, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Bắt buộc có sách khi tạo mới
                if (selectedBookIds.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.error_collection_must_have_books, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val newCol = BookCollection(
                    name = name,
                    description = desc,
                    colorHex = selectedColorHex,
                    iconName = selectedIconName
                )
                collectionRepository.insert(newCol, selectedBookIds)
                Toast.makeText(requireContext(), R.string.msg_collection_created, Toast.LENGTH_SHORT).show()
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
