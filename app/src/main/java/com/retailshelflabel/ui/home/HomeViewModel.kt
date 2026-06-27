package com.retailshelflabel.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.retailshelflabel.ShelfLabelApplication
import com.retailshelflabel.data.db.BulkJobWithItems
import com.retailshelflabel.data.repository.BulkJobRepository

/**
 * ViewModel for the Home screen.
 *
 * Exposes [recentBulkJobs], the last 2 completed bulk print jobs, so the
 * Home screen can show a compact "Recent bulk jobs" summary card without
 * requiring the user to navigate to the full History screen.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val bulkJobRepository = BulkJobRepository(
        (application as ShelfLabelApplication).database.bulkJobDao()
    )

    /**
     * The two most-recent bulk jobs, newest-first.
     * Emits an empty list when no jobs have been run yet, which causes
     * the card to be hidden on the Home screen.
     */
    val recentBulkJobs: LiveData<List<BulkJobWithItems>> =
        MediatorLiveData<List<BulkJobWithItems>>().apply {
            addSource(bulkJobRepository.recentJobsWithItems) { all ->
                value = all.take(2)
            }
        }
}
