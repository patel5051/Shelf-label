package com.retailshelflabel.ui.history

import com.retailshelflabel.data.db.BulkJobWithItems
import com.retailshelflabel.data.db.PrintJob

/**
 * Unified history list item.
 *
 * The Print History screen shows both single-item reprints ([Single]) and
 * completed bulk print jobs ([Bulk]) in one chronologically-sorted list.
 * [PrintHistoryAdapter] uses [viewType] to select the correct row layout.
 */
sealed class HistoryEntry {

    /** Timestamp used for chronological sorting across both entry types. */
    abstract val printedAt: Long

    /** A single-item print job recorded when one label was sent to the printer. */
    data class Single(val job: PrintJob) : HistoryEntry() {
        override val printedAt: Long get() = job.printedAt
    }

    /** A completed bulk print job containing multiple item snapshots. */
    data class Bulk(val jobWithItems: BulkJobWithItems) : HistoryEntry() {
        override val printedAt: Long get() = jobWithItems.bulkJob.printedAt
    }

    companion object {
        const val VIEW_TYPE_SINGLE = 0
        const val VIEW_TYPE_BULK = 1
    }
}
