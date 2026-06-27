package com.retailshelflabel.ui.sync

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.retailshelflabel.R
import com.retailshelflabel.data.db.SyncLog
import com.retailshelflabel.databinding.ItemSyncLogRowBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncLogAdapter : ListAdapter<SyncLog, SyncLogAdapter.ViewHolder>(DIFF) {

    private val dateFormat = SimpleDateFormat("MMM d HH:mm", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemSyncLogRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: SyncLog) {
            val ctx = binding.root.context

            // Mode chip
            binding.tvMode.text = log.mode.uppercase()

            // Status colour
            val (statusLabel, statusColor) = when (log.status) {
                "success" -> "Success" to R.color.primary
                "partial" -> "Partial" to R.color.secondary
                "failed"  -> "Failed"  to R.color.error
                else      -> "Running" to R.color.on_surface_variant
            }
            binding.tvStatus.text = statusLabel
            binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, statusColor))

            // Timestamp
            binding.tvTime.text = if (log.startedAt > 0)
                dateFormat.format(Date(log.startedAt)) else "—"

            // Summary
            if (log.status == "running") {
                binding.tvSummary.text = ctx.getString(R.string.sync_running)
            } else {
                binding.tvSummary.text = ctx.getString(
                    R.string.sync_summary,
                    log.itemsAdded, log.itemsUpdated, log.itemsUnchanged, log.errors
                )
            }

            // Error detail
            val err = log.errorMessage
            if (!err.isNullOrBlank() && log.status != "success") {
                binding.tvError.text = err.take(120)
                binding.tvError.visibility = android.view.View.VISIBLE
            } else {
                binding.tvError.visibility = android.view.View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSyncLogRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SyncLog>() {
            override fun areItemsTheSame(a: SyncLog, b: SyncLog) = a.syncLogId == b.syncLogId
            override fun areContentsTheSame(a: SyncLog, b: SyncLog) = a == b
        }
    }
}
