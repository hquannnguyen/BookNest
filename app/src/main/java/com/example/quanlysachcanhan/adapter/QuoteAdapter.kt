package com.example.quanlysachcanhan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysachcanhan.databinding.ItemQuoteBinding
import com.example.quanlysachcanhan.model.Quote

class QuoteAdapter(
    private val onDeleteClick: (Quote) -> Unit
) : RecyclerView.Adapter<QuoteAdapter.QuoteViewHolder>() {

    private val items = mutableListOf<Quote>()

    fun submitList(newItems: List<Quote>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QuoteViewHolder {
        val binding = ItemQuoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuoteViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: QuoteViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class QuoteViewHolder(
        private val binding: ItemQuoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(quote: Quote) = with(binding) {
            tvQuote.text = quote.content
            btnDeleteQuote.setOnClickListener {
                onDeleteClick(quote)
            }
        }
    }
}
