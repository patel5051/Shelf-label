package com.retailshelflabel.data.repository

import androidx.lifecycle.LiveData
import com.retailshelflabel.data.db.BulkJob
import com.retailshelflabel.data.db.BulkJobDao
import com.retailshelflabel.data.db.BulkJobItem
import com.retailshelflabel.data.db.BulkJobWithItems

/**
 * Repository that abstracts access to [BulkJobDao].
 *
 * Callers pass a list of (barcode, description, price, copies) snapshots;
 * this class creates the header record and all item rows atomically.
 */
class BulkJobRepository(private val dao: BulkJobDao) {

    val recentJobsWithItems: LiveData<List<BulkJobWithItems>> = dao.getRecentJobsWithItems()

    /**
     * Persist a completed bulk print job.
     *
     * @param items  Snapshot data for each item that was in the job.
     *               Each element is a (barcode, description, price, copies) tuple.
     * @return The [BulkJobWithItems] that was saved.
     */
    suspend fun saveJob(items: List<BulkJobItem>): BulkJobWithItems {
        val header = BulkJob(
            totalItems = items.size,
            totalCopies = items.sumOf { it.copies }
        )
        val jobId = dao.insertJob(header)
        val itemsWithJobId = items.map { it.copy(bulkJobId = jobId) }
        dao.insertItems(itemsWithJobId)
        return BulkJobWithItems(header.copy(bulkJobId = jobId), itemsWithJobId)
    }

    /** Returns every bulk job with all items, uncapped. For CSV export only. */
    suspend fun getAllJobsWithItems(): List<BulkJobWithItems> = dao.getAllJobsWithItems()

    /** Fetch a single bulk job with all its items, for reprint pre-population. */
    suspend fun getJobWithItems(bulkJobId: Long): BulkJobWithItems? =
        dao.getJobWithItems(bulkJobId)

    /** Delete every bulk job (and all associated item rows). */
    suspend fun deleteAll() {
        dao.deleteAllItems()
        dao.deleteAllJobs()
    }

    /**
     * Remove bulk jobs older than [retentionDays] days, together with their item rows.
     *
     * Call this after every [saveJob] so the table never grows unbounded. Item rows
     * are deleted first to handle SQLite configurations where FK cascades are not
     * enforced at the driver level.
     */
    suspend fun pruneByAge(retentionDays: Int) {
        val cutoffMs = System.currentTimeMillis() - retentionDays.toLong() * 24L * 60L * 60L * 1000L
        dao.deleteItemsOlderThan(cutoffMs)
        dao.deleteJobsOlderThan(cutoffMs)
    }
}
