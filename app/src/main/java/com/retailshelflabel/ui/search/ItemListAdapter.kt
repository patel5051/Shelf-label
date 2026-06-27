package com.retailshelflabel.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.databinding.ItemListRowBinding
import com.retailshelflabel.util.PreferencesHelper

/**
 * RecyclerView adapter for the item search results list.
 *
 * Supports two modes:
 *  - Normal mode: single tap navigates to item detail.
 *  - Multi-select mode: checkboxes are shown; tapping toggles selection;
 *    long-pressing in normal mode triggers entry into multi-select mode.
 */
class ItemListAdapter(
    private val onClick: (Item) -> Unit,
    private val onLongClick: (Item) -> Unit = {},
    private val onSelectionChanged: (selectedIds: Set<Long>) -> Unit = {}
) : ListAdapter<Item, ItemListAdapter.ViewHolder>(DIFF_CALLBACK) {

    var isMultiSelectMode: Boolean = false
        private set

    private val selectedIds = mutableSetOf<Long>()

    fun enterMultiSelectMode(item: Item) {
        isMultiSelectMode = true
        selectedIds.clear()
        selectedIds.add(item.itemId)
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.toSet())
    }

    /**
     * Re-enters multi-select mode with an existing set of IDs already selected.
     * Used when returning from the bulk-print screen to restore the previous selection.
     */
    fun restoreMultiSelectMode(ids: Set<Long>) {
        isMultiSelectMode = true
        selectedIds.clear()
        selectedIds.addAll(ids)
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.toSet())
    }

    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.toSet())
    }

    fun getSelectedItems(): List<Item> {
        return currentList.filter { it.itemId in selectedIds }
    }

    fun getSelectedIds(): Set<Long> = selectedIds.toSet()

    inner class ViewHolder(private val binding: ItemListRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.tvDescription.text = item.description
            val currency = PreferencesHelper.getCurrencySymbol(binding.root.context)
            binding.tvPrice.text = "$currency${String.format("%.2f", item.price)}"
            binding.tvBarcode.text = item.barcode
            binding.tvDepartment.text = item.department.ifBlank { "" }

            if (isMultiSelectMode) {
                binding.cbSelect.visibility = View.VISIBLE
                val checked = item.itemId in selectedIds
                binding.cbSelect.isChecked = checked
                binding.cardRoot.isChecked = checked
                binding.root.setOnClickListener {
                    if (item.itemId in selectedIds) {
                        selectedIds.remove(item.itemId)
                    } else {
                        selectedIds.add(item.itemId)
                    }
                    binding.cbSelect.isChecked = item.itemId in selectedIds
                    binding.cardRoot.isChecked = item.itemId in selectedIds
                    onSelectionChanged(selectedIds.toSet())
                }
                binding.root.setOnLongClickListener(null)
            } else {
                binding.cbSelect.visibility = View.GONE
                binding.cardRoot.isChecked = false
                binding.root.setOnClickListener { onClick(item) }
                binding.root.setOnLongClickListener {
                    onLongClick(item)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListRowBinding.inflate(
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
