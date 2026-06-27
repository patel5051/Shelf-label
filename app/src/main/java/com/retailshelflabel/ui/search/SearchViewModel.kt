package com.retailshelflabel.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.retailshelflabel.ShelfLabelApplication
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.data.repository.ItemRepository

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ItemRepository(
        (application as ShelfLabelApplication).database.itemDao()
    )

    private val _query = MutableLiveData("")

    /**
     * Items matching the current [query].
     * When query is blank, returns all items.
     */
    val items: LiveData<List<Item>> = _query.switchMap { q ->
        if (q.isNullOrBlank()) repository.allItems else repository.searchItems(q)
    }

    fun setQuery(query: String) {
        if (_query.value != query) _query.value = query
    }

    /**
     * Selected item IDs to restore when returning from the bulk-print screen.
     * Empty set means there is nothing to restore.
     */
    var pendingMultiSelectIds: Set<Long> = emptySet()
        private set

    /**
     * First-visible-item position to restore when returning from the bulk-print screen.
     */
    var savedScrollPosition: Int = 0
        private set

    /**
     * Called just before navigating to [BulkPrintFragment] so the selection
     * and scroll position can survive the view being destroyed.
     */
    fun saveMultiSelectState(selectedIds: Set<Long>, firstVisiblePosition: Int) {
        pendingMultiSelectIds = selectedIds.toSet()
        savedScrollPosition = firstVisiblePosition
    }

    /**
     * Returns the saved multi-select state (selected IDs and scroll position) and
     * clears it so it is only consumed once. Returns null if there is nothing to restore.
     */
    fun consumeMultiSelectState(): Pair<Set<Long>, Int>? {
        if (pendingMultiSelectIds.isEmpty()) return null
        val result = pendingMultiSelectIds to savedScrollPosition
        pendingMultiSelectIds = emptySet()
        savedScrollPosition = 0
        return result
    }
}
