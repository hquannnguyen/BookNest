package com.example.quanlysachcanhan.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.databinding.ItemCollectionBinding
import com.example.quanlysachcanhan.model.CollectionWithStats
import com.example.quanlysachcanhan.utils.ImageStorageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CollectionAdapter(
    private var items: List<CollectionWithStats> = emptyList(),
    private val onItemClick: (CollectionWithStats) -> Unit,
    private val onEditClick: (CollectionWithStats) -> Unit,
    private val onDeleteClick: (CollectionWithStats) -> Unit
) : RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder>() {

    fun submitList(newItems: List<CollectionWithStats>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val binding = ItemCollectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CollectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CollectionViewHolder(
        private val binding: ItemCollectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CollectionWithStats) {
            val context = binding.root.context
            val col = item.collection

            // 1. Title & Subtitle
            binding.tvCollectionName.text = col.name

            val timeStr = formatUpdateTime(col.updatedAt)
            binding.tvCollectionSub.text = "${item.totalBooks} cuốn sách • $timeStr"

            // 2. Icon & Color
            val iconRes = com.example.quanlysachcanhan.utils.CollectionIconHelper.getIconResource(col.iconName)
            binding.ivCollectionIcon.setImageResource(iconRes)

            try {
                val parsedColor = Color.parseColor(col.colorHex)
                binding.ivCollectionIcon.setColorFilter(parsedColor)
                binding.containerIcon.background.mutate().setTint(parsedColor)
                binding.containerIcon.alpha = 0.85f
                binding.ivCollectionIcon.setColorFilter(Color.WHITE)
            } catch (e: Exception) {
                binding.containerIcon.background.mutate().setTint(
                    ContextCompat.getColor(context, R.color.secondary_oak)
                )
                binding.ivCollectionIcon.setColorFilter(Color.WHITE)
            }

            // 3. Overlapping Book Covers
            val covers = item.bookCovers
            val total = item.totalBooks

            if (total <= 0) {
                binding.cardCover1.visibility = View.VISIBLE
                binding.cardCover2.visibility = View.GONE
                binding.cardCover3.visibility = View.GONE
                binding.cardCoverMore.visibility = View.GONE
                binding.ivCover1.setImageResource(R.drawable.ic_auto_stories)
                binding.ivCover1.imageTintList = ContextCompat.getColorStateList(context, R.color.text_hint)
            } else {
                // Cover 1
                binding.cardCover1.visibility = View.VISIBLE
                bindCoverImage(binding.ivCover1, covers.getOrNull(0))

                // Cover 2
                if (total >= 2) {
                    binding.cardCover2.visibility = View.VISIBLE
                    bindCoverImage(binding.ivCover2, covers.getOrNull(1))
                } else {
                    binding.cardCover2.visibility = View.GONE
                }

                // Cover 3
                if (total >= 3) {
                    binding.cardCover3.visibility = View.VISIBLE
                    bindCoverImage(binding.ivCover3, covers.getOrNull(2))
                } else {
                    binding.cardCover3.visibility = View.GONE
                }

                // More badge
                if (total > 3) {
                    binding.cardCoverMore.visibility = View.VISIBLE
                    binding.tvCoverMore.text = "+${total - 3}"
                } else {
                    binding.cardCoverMore.visibility = View.GONE
                }
            }

            // 4. Progress Counts
            binding.tvProgressFraction.text = "${item.readBooks} / ${item.totalBooks}"
            if (item.totalBooks > 0 && item.readBooks == item.totalBooks) {
                binding.tvProgressStatus.text = "Hoàn thành ✓"
                binding.tvProgressStatus.setTextColor(ContextCompat.getColor(context, R.color.secondary_oak))
            } else {
                binding.tvProgressStatus.text = "cuốn đã đọc"
                binding.tvProgressStatus.setTextColor(ContextCompat.getColor(context, R.color.text_hint))
            }

            val pct = item.progressPercent
            binding.tvProgressPercent.text = "$pct%"
            binding.progressBarCollection.progress = pct

            // 5. Options menu
            binding.btnMore.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                popup.menu.add(0, 1, 0, "Chỉnh sửa bộ sưu tập")
                popup.menu.add(0, 2, 1, "Xóa bộ sưu tập")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> onEditClick(item)
                        2 -> onDeleteClick(item)
                    }
                    true
                }
                popup.show()
            }

            // 6. Card Click
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }

        private fun bindCoverImage(iv: ImageView, path: String?) {
            val context = iv.context
            if (!path.isNullOrBlank()) {
                val file = java.io.File(path)
                if (file.exists()) {
                    iv.setImageURI(android.net.Uri.fromFile(file))
                    iv.imageTintList = null
                    return
                }
            }
            iv.setImageResource(R.drawable.ic_auto_stories)
            iv.imageTintList = ContextCompat.getColorStateList(context, R.color.text_hint)
        }

        private fun formatUpdateTime(timeMs: Long): String {
            val diff = System.currentTimeMillis() - timeMs
            val hours = diff / (1000 * 60 * 60)
            val days = diff / (1000 * 60 * 60 * 24)

            return when {
                hours < 1 -> "Vừa cập nhật"
                hours < 24 -> "$hours giờ trước"
                days == 1L -> "Hôm qua"
                days < 7 -> "$days ngày trước"
                days < 30 -> "${days / 7} tuần trước"
                else -> {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.format(Date(timeMs))
                }
            }
        }
    }
}
