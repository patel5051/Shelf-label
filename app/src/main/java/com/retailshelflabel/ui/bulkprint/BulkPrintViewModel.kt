package com.retailshelflabel.ui.bulkprint

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.retailshelflabel.ShelfLabelApplication
import com.retailshelflabel.data.db.BulkJobItem
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.data.repository.BulkJobRepository
import com.retailshelflabel.data.repository.ItemRepository
import com.retailshelflabel.sdk.PrinterManager
import kotlinx.coroutines.launch

/** Snapshot of a removed item, used to restore it on undo. */
data class RemovedItemSnapshot(val item: Item, val position: Int, val copies: Int)

/**
 * ViewModel for the bulk-print confirmation screen.
 *
 * Holds the list of selected items and per-item copy counts.
 * A global copy count setter updates all rows simultaneously.
 *
 * Items can be loaded two ways:
 *  1. [loadItems] — from a list of item IDs in the catalogue (normal bulk-print flow).
 *  2. [loadFromBulkJob] — from a saved [BulkJobWithItems] record (reprint-from-history flow).
 *     In this case synthetic [Item] objects are constructed from the stored snapshots so
 *     the rest of the UI works identically.
 */
class BulkPrintViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ShelfLabelApplication
    private val itemRepository = ItemRepository(app.database.itemDao())
    val bulkJobRepository = BulkJobRepository(app.database.bulkJobDao())

    private val _items = MutableLiveData<List<Item>>(emptyList())
    val items: LiveData<List<Item>> = _items

    /** True once the initial item load has completed; prevents spurious back-navigation on first empty emission. */
    var hasLoadedOnce: Boolean = false
        private set

    /** Per-item copy counts keyed by itemId */
    private val _copiesMap = MutableLiveData<Map<Long, Int>>(emptyMap())
    val copiesMap: LiveData<Map<Long, Int>> = _copiesMap

    private val _globalCopies = MutableLiveData(1)
    val globalCopies: LiveData<Int> = _globalCopies

    /**
     * Current printer status — one of the PrinterManager.STATUS_* constants.
     * Updated by the Fragment whenever [checkPrinterStatus] is called.
     */
    private val _printerStatus = MutableLiveData(PrinterManager.STATUS_NORMAL)
    val printerStatus: LiveData<Int> = _printerStatus

    /**
     * True while a removal Snackbar is visible. Prevents auto-back-navigation so the
     * user has a chance to tap Undo before the screen closes on an empty list.
     */
    private val _hasPendingRemoval = MutableLiveData(false)
    val hasPendingRemoval: LiveData<Boolean> = _hasPendingRemoval

    /** Called by the Fragment after querying [PrinterManager.getPrinterStatus]. */
    fun updatePrinterStatus(statusCode: Int) {
        _printerStatus.value = statusCode
    }

    /** Returns true when the printer is in a state that allows printing. */
    fun isPrinterReady(): Boolean = _printerStatus.value == PrinterManager.STATUS_NORMAL

    fun loadItems(ids: LongArray) {
        viewModelScope.launch {
            val loaded = itemRepository.getItemsByIds(ids.toList())
            _items.value = loaded
            _copiesMap.value = loaded.associate { it.itemId to (_globalCopies.value ?: 1) }
            hasLoadedOnce = true
        }
    }

    /**
     * Pre-populate the screen from a saved bulk job for reprint.
     *
     * Constructs temporary [Item] objects from the stored snapshots, keyed
     * by [BulkJobItem.id] (which serves as the synthetic itemId). The saved
     * per-item copy counts are restored so staff can adjust before reprinting.
     */
    fun loadFromBulkJob(bulkJobId: Long) {
        viewModelScope.launch {
            val jobWithItems = bulkJobRepository.getJobWithItems(bulkJobId) ?: return@launch
            val syntheticItems = jobWithItems.items.map { jobItem ->
                Item(
                    itemId = jobItem.id,
                    barcode = jobItem.barcode,
                    description = jobItem.description,
                    price = jobItem.price
                )
            }
            _items.value = syntheticItems
            _copiesMap.value = jobWithItems.items.associate { it.id to it.copies }
            hasLoadedOnce = true
        }
    }

    fun setGlobalCopies(copies: Int) {
        val safe = copies.coerceAtLeast(1)
        _globalCopies.value = safe
        val currentItems = _items.value ?: return
        _copiesMap.value = currentItems.associate { it.itemId to safe }
    }

    fun incrementGlobalCopies() = setGlobalCopies((_globalCopies.value ?: 1) + 1)
    fun decrementGlobalCopies() = setGlobalCopies((_globalCopies.value ?: 1) - 1)

    fun setCopiesForItem(itemId: Long, copies: Int) {
        val safe = copies.coerceAtLeast(1)
        val current = _copiesMap.value?.toMutableMap() ?: mutableMapOf()
        current[itemId] = safe
        _copiesMap.value = current
    }

    fun incrementCopiesForItem(itemId: Long) {
        val current = _copiesMap.value?.getOrDefault(itemId, 1) ?: 1
        setCopiesForItem(itemId, current + 1)
    }

    fun decrementCopiesForItem(itemId: Long) {
        val current = _copiesMap.value?.getOrDefault(itemId, 1) ?: 1
        setCopiesForItem(itemId, current - 1)
    }

    /**
     * Removes the item with [itemId] from the list and returns a [RemovedItemSnapshot]
     * so the Fragment can offer an Undo action. Sets [hasPendingRemoval] to true so
     * the items observer defers auto-back-navigation while the Snackbar is visible.
     * Returns null if the item is not found.
     */
    fun removeItem(itemId: Long): RemovedItemSnapshot? {
        val currentItems = _items.value?.toMutableList() ?: return null
        val position = currentItems.indexOfFirst { it.itemId == itemId }
        if (position < 0) return null
        val item = currentItems[position]
        val copies = getCopiesForItem(itemId)
        currentItems.removeAt(position)
        _items.value = currentItems
        val currentCopies = _copiesMap.value?.toMutableMap() ?: mutableMapOf()
        currentCopies.remove(itemId)
        _copiesMap.value = currentCopies
        _hasPendingRemoval.value = true
        return RemovedItemSnapshot(item, position, copies)
    }

    /**
     * Re-inserts a previously removed item at its original position, restoring its
     * copy count. Clears [hasPendingRemoval] so the items observer resumes normal
     * empty-list navigation behaviour.
     */
    fun undoRemove(snapshot: RemovedItemSnapshot) {
        val currentItems = _items.value?.toMutableList() ?: return
        val insertAt = snapshot.position.coerceAtMost(currentItems.size)
        currentItems.add(insertAt, snapshot.item)
        _items.value = currentItems
        val currentCopies = _copiesMap.value?.toMutableMap() ?: mutableMapOf()
        currentCopies[snapshot.item.itemId] = snapshot.copies
        _copiesMap.value = currentCopies
        _hasPendingRemoval.value = false
    }

    /**
     * Called when the removal Snackbar is dismissed without the user tapping Undo.
     * Clears [hasPendingRemoval]; if the list is now empty the Fragment will then
     * navigate back as normal.
     */
    fun confirmRemoval() {
        _hasPendingRemoval.value = false
    }

    /**
     * Removes every item from the bulk print at once. Intended to be called only
     * after the user confirms the destructive action in a dialog. Clears any
     * pending single-item removal so the empty list triggers the Fragment's
     * normal back-navigation.
     */
    fun clearAll() {
        _hasPendingRemoval.value = false
        _copiesMap.value = emptyMap()
        _items.value = emptyList()
    }

    fun getCopiesForItem(itemId: Long): Int =
        _copiesMap.value?.getOrDefault(itemId, _globalCopies.value ?: 1) ?: 1

    fun totalLabelCount(): Int =
        _copiesMap.value?.values?.sum() ?: 0
}
