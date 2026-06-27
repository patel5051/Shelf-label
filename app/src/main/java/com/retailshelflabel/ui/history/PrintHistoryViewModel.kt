package com.retailshelflabel.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.retailshelflabel.ShelfLabelApplication
import com.retailshelflabel.data.db.BulkJobWithItems
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.data.db.PrintJob
import com.retailshelflabel.data.repository.BulkJobRepository
import com.retailshelflabel.data.repository.PrintJobRepository
import com.retailshelflabel.sdk.PrinterManager
import kotlinx.coroutines.launch

class PrintHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ShelfLabelApplication
    private val printJobRepository = PrintJobRepository(app.database.printJobDao())
    private val bulkJobRepository = BulkJobRepository(app.database.bulkJobDao())
    private val printerManager = PrinterManager(application, printJobRepository)

    private val singleJobs: LiveData<List<PrintJob>> = printJobRepository.recentJobs
    private val bulkJobs: LiveData<List<BulkJobWithItems>> = bulkJobRepository.recentJobsWithItems

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _dateRangeStart = MutableLiveData<Long?>(null)
    val dateRangeStart: LiveData<Long?> = _dateRangeStart

    private val _dateRangeEnd = MutableLiveData<Long?>(null)
    val dateRangeEnd: LiveData<Long?> = _dateRangeEnd

    /**
     * Unified, chronologically-sorted list of all print history entries
     * (single-item prints and bulk jobs together), newest-first.
     *
     * Filtered in real time by [searchQuery] (matches description or barcode)
     * and optionally by a date range ([dateRangeStart]..[dateRangeEnd]).
     *
     * Uses a [MediatorLiveData] so the list updates whenever either source or
     * filter changes.
     */
    val historyEntries: LiveData<List<HistoryEntry>> = MediatorLiveData<List<HistoryEntry>>().apply {
        fun merge() {
            val query = _searchQuery.value.orEmpty().trim().lowercase()
            val start = _dateRangeStart.value
            val end = _dateRangeEnd.value

            val singles = singleJobs.value.orEmpty().map { HistoryEntry.Single(it) }
            val bulks = bulkJobs.value.orEmpty().map { HistoryEntry.Bulk(it) }
            val all = (singles + bulks).sortedByDescending { it.printedAt }

            value = all.filter { entry ->
                val matchesQuery = if (query.isEmpty()) {
                    true
                } else {
                    when (entry) {
                        is HistoryEntry.Single ->
                            entry.job.description.lowercase().contains(query) ||
                                entry.job.barcode.lowercase().contains(query)

                        is HistoryEntry.Bulk ->
                            entry.jobWithItems.items.any { item ->
                                item.description.lowercase().contains(query) ||
                                    item.barcode.lowercase().contains(query)
                            }
                    }
                }

                val matchesDate = if (start == null || end == null) {
                    true
                } else {
                    entry.printedAt in start..end
                }

                matchesQuery && matchesDate
            }
        }
        addSource(singleJobs) { merge() }
        addSource(bulkJobs) { merge() }
        addSource(_searchQuery) { merge() }
        addSource(_dateRangeStart) { merge() }
        addSource(_dateRangeEnd) { merge() }
    }

    private val _reprintResult = MutableLiveData<ReprintResult?>()
    val reprintResult: LiveData<ReprintResult?> = _reprintResult

    sealed class ReprintResult {
        data class Success(val copies: Int) : ReprintResult()
        data class Error(val message: String) : ReprintResult()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Apply a date range filter.
     * [start] is the first millisecond of the start day; [end] is the last
     * millisecond of the end day (inclusive).
     */
    fun setDateRange(start: Long, end: Long) {
        _dateRangeStart.value = start
        _dateRangeEnd.value = end
    }

    fun clearDateRange() {
        _dateRangeStart.value = null
        _dateRangeEnd.value = null
    }

    val isDateFilterActive: Boolean
        get() = _dateRangeStart.value != null && _dateRangeEnd.value != null

    /**
     * Reprint a single-item job by constructing a temporary [Item] from the
     * stored snapshot and sending it straight to the printer. Uses the copy
     * count from the original job so staff get an identical batch without
     * extra taps.
     */
    fun reprint(job: PrintJob) {
        val item = Item(
            barcode = job.barcode,
            description = job.description,
            price = job.price
        )
        printerManager.initPrinter(
            onReady = {
                printerManager.printShelfLabel(item, job.copies) { success, message ->
                    if (success) {
                        _reprintResult.value = ReprintResult.Success(job.copies)
                    } else {
                        _reprintResult.value = ReprintResult.Error(message ?: "Unknown error")
                    }
                }
            },
            onError = { err -> _reprintResult.value = ReprintResult.Error(err) }
        )
    }

    fun clearResult() { _reprintResult.value = null }

    /** States for the CSV export flow. Idle is the resting state; the observer ignores it. */
    sealed class CsvExportState {
        object Idle : CsvExportState()
        object NoData : CsvExportState()
        data class Ready(val csv: String) : CsvExportState()
    }

    private val _csvExportState = MutableLiveData<CsvExportState>(CsvExportState.Idle)
    val csvExportState: LiveData<CsvExportState> = _csvExportState

    /**
     * Fetches the **complete, uncapped** print history from the database and
     * posts the result to [csvExportState].
     *
     * Uses [PrintJobRepository.getAllJobs] and [BulkJobRepository.getAllJobsWithItems]
     * — both of which have no row limit — so the export always contains every record
     * regardless of any active search/date filter on-screen or the UI caps used to
     * keep the history list fast (100 single / 50 bulk).
     *
     * Transitions:
     *  - [CsvExportState.Idle]   → starting state / after consumption
     *  - [CsvExportState.NoData] → no history exists
     *  - [CsvExportState.Ready]  → CSV string is ready to share
     *
     * Columns: Date, Type, Item(s), Copies, Price
     * Rows are sorted newest-first.
     */
    fun exportCsv() {
        viewModelScope.launch {
            val singles = printJobRepository.getAllJobs()
            val bulks = bulkJobRepository.getAllJobsWithItems()

            if (singles.isEmpty() && bulks.isEmpty()) {
                _csvExportState.value = CsvExportState.NoData
                return@launch
            }

            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val sb = StringBuilder()
            sb.appendLine("Date,Type,Item(s),Copies,Price")

            val allEntries: List<HistoryEntry> =
                (singles.map { HistoryEntry.Single(it) } + bulks.map { HistoryEntry.Bulk(it) })
                    .sortedByDescending { it.printedAt }

            for (entry in allEntries) {
                when (entry) {
                    is HistoryEntry.Single -> {
                        val job = entry.job
                        sb.appendLine(
                            "${dateFormat.format(java.util.Date(job.printedAt))},single," +
                                "${csvEscape(job.description)}," +
                                "${job.copies},${String.format(java.util.Locale.US, "%.2f", job.price)}"
                        )
                    }
                    is HistoryEntry.Bulk -> {
                        val date = dateFormat.format(java.util.Date(entry.jobWithItems.bulkJob.printedAt))
                        for (item in entry.jobWithItems.items) {
                            sb.appendLine(
                                "$date,bulk," +
                                    "${csvEscape(item.description)}," +
                                    "${item.copies},${String.format(java.util.Locale.US, "%.2f", item.price)}"
                            )
                        }
                    }
                }
            }
            _csvExportState.value = CsvExportState.Ready(sb.toString())
        }
    }

    /** Return to the resting [CsvExportState.Idle] state after the Fragment has consumed the event. */
    fun clearCsvExportState() { _csvExportState.value = CsvExportState.Idle }

    private fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Permanently delete all print history (single jobs and bulk jobs).
     * Called after the user confirms the "Clear History" dialog.
     */
    fun clearHistory() {
        viewModelScope.launch {
            printJobRepository.deleteAll()
            bulkJobRepository.deleteAll()
        }
    }
}
