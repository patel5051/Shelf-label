package com.retailshelflabel.data.db

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation that pairs a [BulkJob] header with all of its [BulkJobItem] lines.
 *
 * Used by [BulkJobDao.getRecentJobsWithItems] to return fully-populated bulk
 * jobs in a single query, and by [BulkJobDao.getJobWithItems] when loading a
 * specific job for reprint pre-population.
 */
data class BulkJobWithItems(
    @Embedded val bulkJob: BulkJob,
    @Relation(
        parentColumn = "bulk_job_id",
        entityColumn = "bulk_job_id"
    )
    val items: List<BulkJobItem>
)
