package com.retailshelflabel.ui.queue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.retailshelflabel.databinding.ItemQueueRowBinding
import com.retailshelflabel.util.PreferencesHelper

/**
 * RecyclerView adapter for the print queue.
 * Each row shows item info, a copy count stepper, and a Remove button.
 */
class PrintQueueAdapter(
    private val onCopiesChanged: (itemId: Long, copies: Int) -> Unit,
    private val onRemove: (itemId: Long) -> Unit
) : ListAdapter<PrintQueueManager.QueueEntry, PrintQueueAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemQueueRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: PrintQueueManager.QueueEntry) {
            val item = entry.item
            val currency = PreferencesHelper.getCurrencySymbol(binding.root.context)

            binding.tvDescription.text = item.description
            binding.tvPrice.text = "$currency${String.format("%.2f", item.price)}"
            binding.tvBarcode.text = item.barcode
            binding.tvCopies.text = entry.copies.toString()

            binding.btnIncrease.setOnClickListener {
                val newCopies = entry.copies + 1
                binding.tvCopies.text = newCopies.toString()
                onCopiesChanged(item.itemId, newCopies)
            }

            binding.btnDecrease.setOnClickListener {
                if (entry.copies > 1) {
                    val newCopies = entry.copies - 1
                    binding.tvCopies.text = newCopies.toString()
                    onCopiesChanged(item.itemId, newCopies)
                }
            }

            binding.btnRemove.setOnClickListener {
                onRemove(item.itemId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQueueRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PrintQueueManager.QueueEntry>() {
            override fun areItemsTheSame(
                oldItem: PrintQueueManager.QueueEntry,
                newItem: PrintQueueManager.QueueEntry
            ) = oldItem.item.itemId == newItem.item.itemId

            override fun areContentsTheSame(
                oldItem: PrintQueueManager.QueueEntry,
                newItem: PrintQueueManager.QueueEntry
            ) = oldItem == newItem
        }
    }
}
