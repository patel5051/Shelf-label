package com.retailshelflabel.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SyncLogDao {

    @Insert
    suspend fun insert(log: SyncLog): Long

    @Update
    suspend fun update(log: SyncLog)

    /** Most-recent 50 sync logs, newest first */
    @Query("SELECT * FROM sync_logs ORDER BY startedAt DESC LIMIT 50")
    fun getRecentLogs(): LiveData<List<SyncLog>>

    @Query("SELECT * FROM sync_logs WHERE syncLogId = :id")
    suspend fun getById(id: Long): SyncLog?

    /** Delete all logs older than [cutoffMillis] epoch ms */
    @Query("DELETE FROM sync_logs WHERE startedAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}
