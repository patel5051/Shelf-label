package com.retailshelflabel.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for [PrintJob].
 *
 * Returns the most-recent 100 jobs newest-first so the History screen
 * always shows a manageable, relevant list without unbounded growth.
 */
@Dao
interface PrintJobDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: PrintJob): Long

    /**
     * Live list of the last 100 print jobs, newest-first.
     * The UI automatically updates whenever a new job is inserted.
     */
    @Query("SELECT * FROM print_jobs ORDER BY printed_at DESC LIMIT 100")
    fun getRecentJobs(): LiveData<List<PrintJob>>

    /**
     * Returns every print job ever recorded, newest-first.
     * Used for CSV export only — has no row cap so the audit trail is complete.
     */
    @Query("SELECT * FROM print_jobs ORDER BY printed_at DESC")
    suspend fun getAllJobs(): List<PrintJob>

    /** Delete every print job record. */
    @Query("DELETE FROM print_jobs")
    suspend fun deleteAll()
}
