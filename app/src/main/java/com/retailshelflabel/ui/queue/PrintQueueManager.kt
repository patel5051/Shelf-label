package com.retailshelflabel.ui.queue

import com.retailshelflabel.data.db.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory print queue — process-scoped singleton.
 *
 * Items are keyed by [Item.itemId] so the same item is never queued twice.
 * Each entry also carries a copy count that the user can adjust in the queue screen.
 */
object PrintQueueManager {

    data class QueueEntry(val item: Item, val copies: Int = 1)

    /** Snapshot of a removed entry, used to restore it on undo. */
    data class RemovedEntrySnapshot(val entry: QueueEntry, val position: Int)

    private val _entries = MutableStateFlow<List<QueueEntry>>(emptyList())
    val entries: StateFlow<List<QueueEntry>> = _entries.asStateFlow()

    val size: Int get() = _entries.value.size

    /** Add [item] to the queue (or update copies if already present). */
    fun add(item: Item, copies: Int = 1) {
        val current = _entries.value.toMutableList()
        val idx = current.indexOfFirst { it.item.itemId == item.itemId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(copies = copies)
        } else {
            current.add(QueueEntry(item, copies))
        }
        _entries.value = current
    }

    /** Update the copy count for an existing queue entry. */
    fun setCopies(itemId: Long, copies: Int) {
        _entries.value = _entries.value.map { entry ->
            if (entry.item.itemId == itemId) entry.copy(copies = copies.coerceAtLeast(1))
            else entry
        }
    }

    /**
     * Remove a single item from the queue, returning a [RemovedEntrySnapshot] so the
     * caller can offer an Undo action. Returns null if the item is not found.
     */
    fun remove(itemId: Long): RemovedEntrySnapshot? {
        val current = _entries.value
        val position = current.indexOfFirst { it.item.itemId == itemId }
        if (position < 0) return null
        val entry = current[position]
        _entries.value = current.filter { it.item.itemId != itemId }
        return RemovedEntrySnapshot(entry, position)
    }

    /** Re-insert a previously removed entry at its original [position]. */
    fun insert(entry: QueueEntry, position: Int) {
        val current = _entries.value.toMutableList()
        val insertAt = position.coerceIn(0, current.size)
        current.add(insertAt, entry)
        _entries.value = current
    }

    /** Clear the entire queue (call after a successful bulk print). */
    fun clear() {
        _entries.value = emptyList()
    }

    /** Returns true if [item] is already in the queue. */
    fun contains(itemId: Long): Boolean =
        _entries.value.any { it.item.itemId == itemId }
}
