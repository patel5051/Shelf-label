package com.retailshelflabel.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted record of every Modisoft sync attempt.
 *
 * One row is inserted when a sync starts (status = "running") and then
 * updated in-place when it finishes.  The UI shows the last N entries
 * so staff can see what changed and whether any rows failed to import.
 */
@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true) val syncLogId: Long = 0,

    /** "api" | "csv" | "manual" */
    val mode: String,

    /** Epoch-millis when sync started */
    val startedAt: Long,

    /** Epoch-millis when sync finished (0 while running) */
    val finishedAt: Long = 0L,

    val itemsAdded: Int = 0,
    val itemsUpdated: Int = 0,
    val itemsUnchanged: Int = 0,
    val errors: Int = 0,

    /** "running" | "success" | "partial" | "failed" */
    val status: String = "running",

    /** Human-readable error summary (null on full success) */
    val errorMessage: String? = null
)
