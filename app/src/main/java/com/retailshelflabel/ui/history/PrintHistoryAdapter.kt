package com.retailshelflabel.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.retailshelflabel.data.db.BulkJobWithItems
import com.retailshelflabel.data.db.PrintJob
import com.retailshelflabel.databinding.ItemBulkHistoryRowBinding
import com.retailshelflabel.databinding.ItemPrintHistoryRowBinding
import com.retailshelflabel.util.PreferencesHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for the Print History screen.
 *
 * Renders two row types in a single chronologically-sorted list:
 *  - [HistoryEntry.VIEW_TYPE_SINGLE]: an individual label print with a one-tap Reprint button.
 *  - [HistoryEntry.VIEW_TYPE_BULK]: a completed bulk job with a "Reprint this job" button that
 *    navigates back to the bulk-print confirmation screen pre-populated with the saved items.
 */
class PrintHistoryAdapter(
    private val onReprint: (job: PrintJob) -> Unit,
    private val onReprintBulkJob: (jobWithItems: BulkJobWithItems) -> Unit
) : ListAdapter<HistoryEntry, RecyclerView.ViewHolder>(DIFF) {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is HistoryEntry.Single -> HistoryEntry.VIEW_TYPE_SINGLE
        is HistoryEntry.Bulk -> HistoryEntry.VIEW_TYPE_BULK
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HistoryEntry.VIEW_TYPE_BULK -> BulkViewHolder(
                ItemBulkHistoryRowBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            else -> SingleViewHolder(
                ItemPrintHistoryRowBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val entry = getItem(position)) {
            is HistoryEntry.Single -> (holder as SingleViewHolder).bind(entry.job)
            is HistoryEntry.Bulk -> (holder as BulkViewHolder).bind(entry.jobWithItems)
        }
    }

    inner class SingleViewHolder(private val binding: ItemPrintHistoryRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(job: PrintJob) {
            val currency = PreferencesHelper.getCurrencySymbol(binding.root.context)
            binding.tvDescription.text = job.description
            binding.tvBarcode.text = job.barcode
            binding.tvPrice.text = "$currency${String.format("%.2f", job.price)}"
            binding.tvCopies.text = binding.root.context
                .resources
                .getQuantityString(
                    com.retailshelflabel.R.plurals.history_copies_printed,
                    job.copies,
                    job.copies
                )
            binding.tvPrintedAt.text = dateFormat.format(Date(job.printedAt))
            binding.btnReprint.setOnClickListener { onReprint(job) }
        }
    }

    inner class BulkViewHolder(private val binding: ItemBulkHistoryRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(jobWithItems: BulkJobWithItems) {
            val ctx = binding.root.context
            val job = jobWithItems.bulkJob
            val items = jobWithItems.items

            binding.tvBulkStats.text = ctx.resources.getString(
                com.retailshelflabel.R.string.history_bulk_stats,
                job.totalItems,
                job.totalCopies
            )

            binding.tvBulkItemsPreview.text = when {
                items.isEmpty() -> ""
                items.size <= 2 -> items.joinToString(" · ") { it.description }
                else -> {
                    val visible = items.take(2).joinToString(" · ") { it.description }
                    val extra = items.size - 2
                    ctx.getString(com.retailshelflabel.R.string.history_bulk_preview_overflow, visible, extra)
                }
            }

            binding.tvBulkPrintedAt.text = dateFormat.format(Date(job.printedAt))
            binding.btnReprintJob.setOnClickListener { onReprintBulkJob(jobWithItems) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HistoryEntry>() {
            override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
                return when {
                    oldItem is HistoryEntry.Single && newItem is HistoryEntry.Single ->
                        oldItem.job.jobId == newItem.job.jobId
                    oldItem is HistoryEntry.Bulk && newItem is HistoryEntry.Bulk ->
                        oldItem.jobWithItems.bulkJob.bulkJobId == newItem.jobWithItems.bulkJob.bulkJobId
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean =
                oldItem == newItem
        }
    }
}
