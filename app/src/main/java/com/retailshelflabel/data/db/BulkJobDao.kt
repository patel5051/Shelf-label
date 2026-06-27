package com.retailshelflabel.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * Data Access Object for [BulkJob] and [BulkJobItem].
 *
 * Bulk jobs are returned newest-first, capped at 50 entries to keep the
 * History screen fast on modest hardware.
 */
@Dao
interface BulkJobDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: BulkJob): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<BulkJobItem>)

    /**
     * Live list of the last 50 bulk print jobs with all their items, newest-first.
     * The UI automatically updates whenever a new job is inserted.
     */
    @Transaction
    @Query("SELECT * FROM bulk_jobs ORDER BY printed_at DESC LIMIT 50")
    fun getRecentJobsWithItems(): LiveData<List<BulkJobWithItems>>

    /**
     * Returns every bulk job with all their items, newest-first.
     * Used for CSV export only — has no row cap so the audit trail is complete.
     */
    @Transaction
    @Query("SELECT * FROM bulk_jobs ORDER BY printed_at DESC")
    suspend fun getAllJobsWithItems(): List<BulkJobWithItems>

    /**
     * Single bulk job with items — used to pre-populate the bulk print screen
     * when staff tap "Reprint this job".
     */
    @Transaction
    @Query("SELECT * FROM bulk_jobs WHERE bulk_job_id = :bulkJobId LIMIT 1")
    suspend fun getJobWithItems(bulkJobId: Long): BulkJobWithItems?

    /** Delete every bulk job header (cascade removes items via FK). */
    @Query("DELETE FROM bulk_jobs")
    suspend fun deleteAllJobs()

    /** Delete every bulk job item row explicitly (for databases without FK cascade). */
    @Query("DELETE FROM bulk_job_items")
    suspend fun deleteAllItems()

    /**
     * Delete all bulk jobs whose [BulkJob.printedAt] timestamp is older than [cutoffMs].
     * The FK cascade (or a prior [deleteItemsOlderThan] call) removes the child rows.
     */
    @Query("DELETE FROM bulk_jobs WHERE printed_at < :cutoffMs")
    suspend fun deleteJobsOlderThan(cutoffMs: Long)

    /**
     * Delete all bulk job item rows whose parent job pre-dates [cutoffMs].
     * Call this before [deleteJobsOlderThan] on databases that do not enforce
     * FK cascades at the SQLite level.
     */
    @Query("DELETE FROM bulk_job_items WHERE bulk_job_id IN (SELECT bulk_job_id FROM bulk_jobs WHERE printed_at < :cutoffMs)")
    suspend fun deleteItemsOlderThan(cutoffMs: Long)
}
