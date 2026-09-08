package com.example.quanlysachcanhan.ui.collections

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.adapter.BookPickerAdapter
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.data.CollectionRepository
import com.example.quanlysachcanhan.databinding.DialogColorPickerBinding
import com.example.quanlysachcanhan.databinding.DialogCreateCollectionBinding
import com.example.quanlysachcanhan.databinding.DialogIconPickerBinding
import com.example.quanlysachcanhan.databinding.ItemColorPaletteBinding
import com.example.quanlysachcanhan.databinding.ItemIconPickerBinding
import com.example.quanlysachcanhan.model.Book
import com.example.quanlysachcanhan.model.BookCollection
import com.example.quanlysachcanhan.utils.CollectionIconHelper
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

    private val presetColorHexes = listOf(
        "#031632", "#7D562D", "#FFCA98", "#4E5F7E",
        "#D4A373", "#2C1600", "#2E7D32", "#C2185B"
    )

    private val morePaletteColors = listOf(
        "#031632", "#7D562D", "#FFCA98", "#4E5F7E", "#D4A373", "#2C1600",
        "#2E7D32", "#C2185B", "#E53935", "#D81B60", "#8E24AA", "#5E35B1",
        "#3949AB", "#1E88E5", "#039BE5", "#00ACC1", "#00897B", "#43A047",
        "#7CB342", "#C0CA33", "#FDD835", "#FFB300", "#FB8C00", "#F4511E",
        "#6D4C41", "#757575", "#546E7A", "#1A237E", "#004D40", "#BF360C"
    )

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
        val allPresetChecks = listOf(
            binding.checkNavy,
            binding.checkOak,
            binding.checkAmber,
            binding.checkSlate,
            binding.checkGold,
            binding.checkDark,
            binding.checkGreen,
            binding.checkRuby
        )

        fun selectPresetColor(colorHex: String, checkToVisible: ImageView) {
            selectedColorHex = colorHex
            allPresetChecks.forEach { it.visibility = View.GONE }
            checkToVisible.visibility = View.VISIBLE

            // Reset btnColorMore to default unselected style
            binding.checkColorMore.visibility = View.GONE
            binding.ivColorMoreIcon.visibility = View.VISIBLE
            binding.ivColorMoreIcon.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.text_primary)
            binding.btnColorMore.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.outline_variant)
        }

        binding.colorNavy.setOnClickListener { selectPresetColor("#031632", binding.checkNavy) }
        binding.colorOak.setOnClickListener { selectPresetColor("#7D562D", binding.checkOak) }
        binding.colorAmber.setOnClickListener { selectPresetColor("#FFCA98", binding.checkAmber) }
        binding.colorSlate.setOnClickListener { selectPresetColor("#4E5F7E", binding.checkSlate) }
        binding.colorGold.setOnClickListener { selectPresetColor("#D4A373", binding.checkGold) }
        binding.colorDark.setOnClickListener { selectPresetColor("#2C1600", binding.checkDark) }
        binding.colorGreen.setOnClickListener { selectPresetColor("#2E7D32", binding.checkGreen) }
        binding.colorRuby.setOnClickListener { selectPresetColor("#C2185B", binding.checkRuby) }

        binding.btnColorMore.setOnClickListener {
            showColorPickerDialog { chosenColor ->
                selectCustomColor(chosenColor)
            }
        }

        // Default: Oak
        selectPresetColor("#7D562D", binding.checkOak)
    }

    private fun selectCustomColor(colorHex: String) {
        selectedColorHex = colorHex
        listOf(
            binding.checkNavy,
            binding.checkOak,
            binding.checkAmber,
            binding.checkSlate,
            binding.checkGold,
            binding.checkDark,
            binding.checkGreen,
            binding.checkRuby
        ).forEach { it.visibility = View.GONE }

        try {
            val parsed = Color.parseColor(colorHex)
            binding.btnColorMore.backgroundTintList = ColorStateList.valueOf(parsed)
            binding.ivColorMoreIcon.visibility = View.GONE
            binding.checkColorMore.visibility = View.VISIBLE
            val isLight = ColorUtils.calculateLuminance(parsed) > 0.6
            binding.checkColorMore.imageTintList = ColorStateList.valueOf(if (isLight) Color.BLACK else Color.WHITE)
        } catch (e: Exception) {
            binding.checkColorMore.visibility = View.VISIBLE
        }
    }

    private fun showColorPickerDialog(onColorPicked: (String) -> Unit) {
        val dialogBinding = DialogColorPickerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedInPicker = selectedColorHex

        fun updateHexPreview(hex: String) {
            try {
                val color = Color.parseColor(hex)
                dialogBinding.viewHexPreview.backgroundTintList = ColorStateList.valueOf(color)
            } catch (e: Exception) {
                dialogBinding.viewHexPreview.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.outline_variant)
            }
        }

        updateHexPreview(selectedInPicker)
        dialogBinding.etHexColor.setText(selectedInPicker)

        val adapter = ColorPaletteAdapter(morePaletteColors, selectedInPicker) { colorHex ->
            selectedInPicker = colorHex
            dialogBinding.etHexColor.setText(colorHex)
            updateHexPreview(colorHex)
            onColorPicked(colorHex)
            dialog.dismiss()
        }

        dialogBinding.rvColorPalette.layoutManager = GridLayoutManager(requireContext(), 6)
        dialogBinding.rvColorPalette.adapter = adapter

        dialogBinding.etHexColor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString()?.trim().orEmpty()
                if (text.length == 7 && text.startsWith("#")) {
                    updateHexPreview(text)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        dialogBinding.btnApplyHex.setOnClickListener {
            val hex = dialogBinding.etHexColor.text.toString().trim()
            try {
                Color.parseColor(hex)
                onColorPicked(hex)
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Mã màu không hợp lệ (#RRGGBB)", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnCancelColor.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupIconPicker() {
        val presetIconMap = mapOf(
            "auto_stories" to Pair(binding.btnIconStories, binding.ivIconStories),
            "psychology" to Pair(binding.btnIconPsychology, binding.ivIconPsychology),
            "trending_up" to Pair(binding.btnIconTrending, binding.ivIconTrending),
            "account_balance" to Pair(binding.btnIconFinance, binding.ivIconFinance),
            "favorite" to Pair(binding.btnIconFavorite, binding.ivIconFavorite),
            "science" to Pair(binding.btnIconScience, binding.ivIconScience)
        )

        fun selectPresetIcon(iconName: String) {
            selectedIconName = iconName
            presetIconMap.forEach { (name, pair) ->
                val (btn, iv) = pair
                if (name == iconName) {
                    btn.setBackgroundResource(R.drawable.bg_chip_selected)
                    iv.setColorFilter(Color.WHITE)
                } else {
                    btn.setBackgroundResource(R.drawable.bg_chip_unselected)
                    iv.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_navy))
                }
            }
            // Reset btnIconMore
            binding.btnIconMore.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.ivIconMore.setImageResource(R.drawable.ic_more_horiz)
            binding.ivIconMore.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_navy))
        }

        presetIconMap.forEach { (name, pair) ->
            pair.first.setOnClickListener { selectPresetIcon(name) }
        }

        binding.btnIconMore.setOnClickListener {
            showIconPickerDialog { iconItem ->
                if (presetIconMap.containsKey(iconItem.name)) {
                    selectPresetIcon(iconItem.name)
                } else {
                    selectCustomIcon(iconItem.name, iconItem.resId)
                }
            }
        }

        selectPresetIcon("auto_stories")
    }

    private fun selectCustomIcon(iconName: String, iconResId: Int) {
        selectedIconName = iconName
        val presetButtons = listOf(
            Pair(binding.btnIconStories, binding.ivIconStories),
            Pair(binding.btnIconPsychology, binding.ivIconPsychology),
            Pair(binding.btnIconTrending, binding.ivIconTrending),
            Pair(binding.btnIconFinance, binding.ivIconFinance),
            Pair(binding.btnIconFavorite, binding.ivIconFavorite),
            Pair(binding.btnIconScience, binding.ivIconScience)
        )
        presetButtons.forEach { (btn, iv) ->
            btn.setBackgroundResource(R.drawable.bg_chip_unselected)
            iv.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_navy))
        }

        // Highlight btnIconMore
        binding.btnIconMore.setBackgroundResource(R.drawable.bg_chip_selected)
        binding.ivIconMore.setImageResource(iconResId)
        binding.ivIconMore.setColorFilter(Color.WHITE)
    }

    private fun showIconPickerDialog(onIconPicked: (CollectionIconHelper.IconItem) -> Unit) {
        val dialogBinding = DialogIconPickerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val adapter = IconLibraryAdapter(CollectionIconHelper.availableIcons, selectedIconName) { iconItem ->
            onIconPicked(iconItem)
            dialog.dismiss()
        }

        dialogBinding.rvIconPicker.layoutManager = GridLayoutManager(requireContext(), 4)
        dialogBinding.rvIconPicker.adapter = adapter

        dialogBinding.btnCancelIcon.setOnClickListener {
            dialog.dismiss()
        }

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
            "#2E7D32" -> binding.colorGreen.performClick()
            "#C2185B" -> binding.colorRuby.performClick()
            else -> selectCustomColor(col.colorHex)
        }

        // Select icon
        when (col.iconName) {
            "auto_stories" -> binding.btnIconStories.performClick()
            "psychology" -> binding.btnIconPsychology.performClick()
            "trending_up" -> binding.btnIconTrending.performClick()
            "account_balance" -> binding.btnIconFinance.performClick()
            "favorite" -> binding.btnIconFavorite.performClick()
            "science" -> binding.btnIconScience.performClick()
            else -> {
                val iconRes = CollectionIconHelper.getIconResource(col.iconName)
                selectCustomIcon(col.iconName, iconRes)
            }
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

            if (selectedBookIds.isEmpty()) {
                if (editingCollectionId > 0L) {
                    // Nếu xóa hết sách trong bộ sưu tập đang sửa -> Xóa bộ sưu tập luôn
                    collectionRepository.delete(editingCollectionId)
                    Toast.makeText(requireContext(), "Bộ sưu tập đã bị xóa do không còn cuốn sách nào", Toast.LENGTH_SHORT).show()
                    onSavedListener?.invoke()
                    dismiss()
                } else {
                    // Tạo mới nhưng chưa chọn sách nào -> Bắt buộc chọn ít nhất 1 sách
                    Toast.makeText(requireContext(), "Vui lòng chọn ít nhất 1 cuốn sách cho bộ sưu tập", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

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

    // Inner Adapter for Color Palette Dialog
    private class ColorPaletteAdapter(
        private val colors: List<String>,
        private val currentColor: String,
        private val onColorSelected: (String) -> Unit
    ) : RecyclerView.Adapter<ColorPaletteAdapter.ColorViewHolder>() {

        inner class ColorViewHolder(val itemBinding: ItemColorPaletteBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val b = ItemColorPaletteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ColorViewHolder(b)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            val colorHex = colors[position]
            try {
                val colorInt = Color.parseColor(colorHex)
                holder.itemBinding.layoutColorCircle.backgroundTintList = ColorStateList.valueOf(colorInt)
                val isSelected = colorHex.equals(currentColor, ignoreCase = true)
                holder.itemBinding.ivColorCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
                val isLight = ColorUtils.calculateLuminance(colorInt) > 0.6
                holder.itemBinding.ivColorCheck.imageTintList = ColorStateList.valueOf(if (isLight) Color.BLACK else Color.WHITE)
            } catch (e: Exception) {}

            holder.itemBinding.layoutColorCircle.setOnClickListener {
                onColorSelected(colorHex)
            }
        }

        override fun getItemCount(): Int = colors.size
    }

    // Inner Adapter for Icon Library Dialog
    private class IconLibraryAdapter(
        private val icons: List<CollectionIconHelper.IconItem>,
        private val currentIconName: String,
        private val onIconSelected: (CollectionIconHelper.IconItem) -> Unit
    ) : RecyclerView.Adapter<IconLibraryAdapter.IconViewHolder>() {

        inner class IconViewHolder(val itemBinding: ItemIconPickerBinding) : RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
            val b = ItemIconPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return IconViewHolder(b)
        }

        override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
            val item = icons[position]
            holder.itemBinding.ivIconPicker.setImageResource(item.resId)
            holder.itemBinding.tvIconLabel.text = item.label

            val isSelected = item.name == currentIconName
            val context = holder.itemView.context
            if (isSelected) {
                holder.itemBinding.layoutIconItem.setBackgroundResource(R.drawable.bg_chip_selected)
                holder.itemBinding.ivIconPicker.setColorFilter(Color.WHITE)
                holder.itemBinding.tvIconLabel.setTextColor(Color.WHITE)
            } else {
                holder.itemBinding.layoutIconItem.setBackgroundResource(R.drawable.bg_dialog_option_item)
                holder.itemBinding.ivIconPicker.setColorFilter(ContextCompat.getColor(context, R.color.primary_navy))
                holder.itemBinding.tvIconLabel.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }

            holder.itemBinding.layoutIconItem.setOnClickListener {
                onIconSelected(item)
            }
        }

        override fun getItemCount(): Int = icons.size
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
