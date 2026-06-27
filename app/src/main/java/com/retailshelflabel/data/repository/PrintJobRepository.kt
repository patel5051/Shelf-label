package com.retailshelflabel.data.repository

import androidx.lifecycle.LiveData
import com.retailshelflabel.data.db.PrintJob
import com.retailshelflabel.data.db.PrintJobDao

/**
 * Repository that abstracts access to [PrintJobDao].
 *
 * ViewModels and [com.retailshelflabel.sdk.PrinterManager] interact only with
 * this class — they never touch the DAO directly.
 */
class PrintJobRepository(private val dao: PrintJobDao) {

    val recentJobs: LiveData<List<PrintJob>> = dao.getRecentJobs()

    suspend fun insert(job: PrintJob): Long = dao.insert(job)

    /** Returns every print job ever recorded, uncapped. For CSV export only. */
    suspend fun getAllJobs(): List<PrintJob> = dao.getAllJobs()

    suspend fun deleteAll() = dao.deleteAll()
}
