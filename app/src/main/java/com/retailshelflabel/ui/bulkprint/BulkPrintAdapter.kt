package com.retailshelflabel.ui.bulkprint

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.databinding.ItemBulkPrintRowBinding
import com.retailshelflabel.util.PreferencesHelper

/**
 * Adapter for the bulk-print confirmation list.
 * Each row shows item info and +/− controls for per-item copy count.
 */
class BulkPrintAdapter(
    private val onIncrease: (itemId: Long) -> Unit,
    private val onDecrease: (itemId: Long) -> Unit,
    private val onRemove: (itemId: Long) -> Unit,
    private var copiesMap: Map<Long, Int> = emptyMap()
) : ListAdapter<Item, BulkPrintAdapter.ViewHolder>(DIFF_CALLBACK) {

    fun updateCopies(newMap: Map<Long, Int>) {
        copiesMap = newMap
        notifyItemRangeChanged(0, itemCount)
    }

    inner class ViewHolder(private val binding: ItemBulkPrintRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            val currency = PreferencesHelper.getCurrencySymbol(binding.root.context)
            binding.tvDescription.text = item.description
            binding.tvBarcode.text = item.barcode
            binding.tvPrice.text = "$currency${String.format("%.2f", item.price)}"
            val copies = copiesMap.getOrDefault(item.itemId, 1)
            binding.tvCopies.text = copies.toString()

            binding.btnCopiesIncrease.setOnClickListener { onIncrease(item.itemId) }
            binding.btnCopiesDecrease.setOnClickListener { onDecrease(item.itemId) }
            binding.btnRemove.setOnClickListener { onRemove(item.itemId) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBulkPrintRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item) =
                oldItem.itemId == newItem.itemId
            override fun areContentsTheSame(oldItem: Item, newItem: Item) =
                oldItem == newItem
        }
    }
}
