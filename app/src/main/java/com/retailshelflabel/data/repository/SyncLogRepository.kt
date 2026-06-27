package com.retailshelflabel.data.repository

import androidx.lifecycle.LiveData
import com.retailshelflabel.data.db.SyncLog
import com.retailshelflabel.data.db.SyncLogDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncLogRepository(private val dao: SyncLogDao) {

    val recentLogs: LiveData<List<SyncLog>> = dao.getRecentLogs()

    /** Insert a new "running" log and return its generated ID. */
    suspend fun startLog(mode: String): Long = withContext(Dispatchers.IO) {
        dao.insert(SyncLog(mode = mode, startedAt = System.currentTimeMillis()))
    }

    /** Finalise an in-progress log with results. */
    suspend fun finishLog(
        logId: Long,
        added: Int,
        updated: Int,
        unchanged: Int,
        errors: Int,
        errorMessage: String? = null
    ) = withContext(Dispatchers.IO) {
        val log = dao.getById(logId) ?: return@withContext
        val status = when {
            errors > 0 && (added + updated) == 0 -> "failed"
            errors > 0 -> "partial"
            else -> "success"
        }
        dao.update(
            log.copy(
                finishedAt = System.currentTimeMillis(),
                itemsAdded = added,
                itemsUpdated = updated,
                itemsUnchanged = unchanged,
                errors = errors,
                status = status,
                errorMessage = errorMessage
            )
        )
    }

    /** Mark a log as failed (e.g. network error before any rows processed). */
    suspend fun failLog(logId: Long, message: String) = withContext(Dispatchers.IO) {
        val log = dao.getById(logId) ?: return@withContext
        dao.update(
            log.copy(
                finishedAt = System.currentTimeMillis(),
                status = "failed",
                errorMessage = message
            )
        )
    }

    /** Prune logs older than 30 days to keep storage tidy. */
    suspend fun pruneOldLogs() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        dao.deleteOlderThan(cutoff)
    }
}
