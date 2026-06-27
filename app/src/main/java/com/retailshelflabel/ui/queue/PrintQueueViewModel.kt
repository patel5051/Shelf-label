package com.retailshelflabel.ui.queue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.retailshelflabel.ShelfLabelApplication
import com.retailshelflabel.data.repository.PrintJobRepository
import com.retailshelflabel.sdk.PrinterManager

class PrintQueueViewModel(application: Application) : AndroidViewModel(application) {

    private val printJobRepository = PrintJobRepository(
        (application as ShelfLabelApplication).database.printJobDao()
    )
    private val printerManager = PrinterManager(application, printJobRepository)

    /** Live list of queue entries — reflects PrintQueueManager state. */
    val entries: LiveData<List<PrintQueueManager.QueueEntry>> =
        PrintQueueManager.entries.asLiveData()

    private val _printResult = MutableLiveData<PrintResult?>()
    val printResult: LiveData<PrintResult?> = _printResult

    sealed class PrintResult {
        data class Success(val totalLabels: Int) : PrintResult()
        data class Partial(val printed: Int, val failed: Int, val lastError: String) : PrintResult()
        data class Error(val message: String) : PrintResult()
    }

    fun setCopies(itemId: Long, copies: Int) {
        PrintQueueManager.setCopies(itemId, copies)
    }

    /**
     * Remove the item with [itemId], returning a snapshot so the Fragment can offer
     * an Undo action. Returns null if the item is not found.
     */
    fun remove(itemId: Long): PrintQueueManager.RemovedEntrySnapshot? =
        PrintQueueManager.remove(itemId)

    /** Re-insert a previously removed entry at its original position. */
    fun undoRemove(snapshot: PrintQueueManager.RemovedEntrySnapshot) {
        PrintQueueManager.insert(snapshot.entry, snapshot.position)
    }

    fun clearQueue() {
        PrintQueueManager.clear()
    }

    /**
     * Print all items in the queue sequentially.
     *
     * Continues on individual failures so a single problematic item does not
     * block the rest of the batch. Reports a [PrintResult.Partial] if some
     * jobs failed, or [PrintResult.Success] if all succeeded.
     */
    fun printAll() {
        val queue = PrintQueueManager.entries.value
        if (queue.isEmpty()) {
            _printResult.value = PrintResult.Error("Queue is empty")
            return
        }

        printerManager.initPrinter(
            onReady = {
                var printed = 0
                var failed = 0
                var lastError = ""

                for (entry in queue) {
                    printerManager.printShelfLabel(entry.item, entry.copies) { success, message ->
                        if (success) {
                            printed += entry.copies
                        } else {
                            failed++
                            lastError = message ?: "Unknown error"
                        }
                    }
                }

                when {
                    failed == 0 -> {
                        PrintQueueManager.clear()
                        _printResult.value = PrintResult.Success(printed)
                    }
                    printed > 0 -> _printResult.value = PrintResult.Partial(printed, failed, lastError)
                    else -> _printResult.value = PrintResult.Error(lastError)
                }
            },
            onError = { err -> _printResult.value = PrintResult.Error(err) }
        )
    }

    fun clearResult() { _printResult.value = null }
}
