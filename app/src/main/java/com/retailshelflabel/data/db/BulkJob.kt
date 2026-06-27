package com.retailshelflabel.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing the header of a completed bulk print job.
 *
 * One [BulkJob] is created each time the staff taps "Print All" on the bulk
 * confirmation screen and the job completes. The individual items are stored
 * as [BulkJobItem] rows referencing this record, so the entire job can be
 * replayed from the Print History screen with a single tap.
 */
@Entity(tableName = "bulk_jobs")
data class BulkJob(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "bulk_job_id")
    val bulkJobId: Long = 0,

    /** Total number of distinct items in this job */
    @ColumnInfo(name = "total_items")
    val totalItems: Int,

    /** Sum of all per-item copy counts */
    @ColumnInfo(name = "total_copies")
    val totalCopies: Int,

    /** Unix epoch milliseconds when the job completed */
    @ColumnInfo(name = "printed_at")
    val printedAt: Long = System.currentTimeMillis()
)
