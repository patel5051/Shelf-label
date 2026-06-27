package com.retailshelflabel.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.data.repository.ItemRepository
import com.retailshelflabel.data.repository.SyncLogRepository
import com.retailshelflabel.util.PreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates all three Modisoft sync modes.
 *
 * Mode routing:
 *  "api"    → [syncViaApi]  — uses [ModisoftApiClient] (requires Modisoft API credentials)
 *  "csv"    → [syncViaCsv]  — uses [CsvPricebookImporter] with a user-selected file URI
 *  "manual" → [syncManual]  — no-op placeholder that returns a helpful message
 *
 * All modes upsert into the local Room database via [ItemRepository].
 * The app remains fully functional offline regardless of sync mode.
 */
class ModisoftSyncManager(
    private val itemRepository: ItemRepository,
    private val syncLogRepository: SyncLogRepository
) {

    companion object {
        private const val TAG = "ModisoftSyncManager"

        const val MODE_API    = "api"
        const val MODE_CSV    = "csv"
        const val MODE_MANUAL = "manual"
    }

    data class SyncResult(
        val mode: String,
        val added: Int,
        val updated: Int,
        val unchanged: Int,
        val errors: Int,
        val errorSamples: List<String> = emptyList(),
        val success: Boolean = (errors == 0 || added + updated > 0)
    )

    // ── API mode ──────────────────────────────────────────────────────────

    /**
     * Fetch the pricebook from the Modisoft API and upsert all items.
     *
     * Reads credentials from [PreferencesHelper].
     * Returns a [SyncResult] describing what changed.
     */
    suspend fun syncViaApi(context: Context): SyncResult = withContext(Dispatchers.IO) {
        val logId = syncLogRepository.startLog(MODE_API)
        Log.d(TAG, "syncViaApi — start (logId=$logId)")

        val baseUrl = PreferencesHelper.getModisoftApiBaseUrl(context)
        val storeId = PreferencesHelper.getModisoftStoreId(context)
        val apiKey  = PreferencesHelper.getModisoftApiKey(context)

        if (baseUrl.isBlank() || storeId.isBlank() || apiKey.isBlank()) {
            val msg = "API credentials not configured. Go to Settings → Modisoft Sync."
            syncLogRepository.failLog(logId, msg)
            return@withContext SyncResult(MODE_API, 0, 0, 0, 1, listOf(msg), success = false)
        }

        try {
            val client = ModisoftApiClient(baseUrl, storeId, apiKey)
            val token = client.authenticate()
            val items = client.fetchPricebook(token)
            val result = upsertItems(items.map { it.toItem() })
            syncLogRepository.finishLog(logId, result.added, result.updated, result.unchanged, result.errors)
            PreferencesHelper.setLastSyncTimestamp(context, System.currentTimeMillis())
            result.copy(mode = MODE_API)
        } catch (e: ModisoftApiClient.ModisoftApiException) {
            val msg = "API error (${e.httpCode}): ${e.message}"
            syncLogRepository.failLog(logId, msg)
            SyncResult(MODE_API, 0, 0, 0, 1, listOf(msg), success = false)
        } catch (e: Exception) {
            val msg = "Network error: ${e.message}"
            syncLogRepository.failLog(logId, msg)
            SyncResult(MODE_API, 0, 0, 0, 1, listOf(msg), success = false)
        }
    }

    // ── CSV mode ──────────────────────────────────────────────────────────

    /**
     * Parse a Modisoft-exported pricebook CSV from [fileUri] and upsert all items.
     *
     * Uses [CsvPricebookImporter] for column auto-detection.
     */
    suspend fun syncViaCsv(context: Context, fileUri: Uri): SyncResult = withContext(Dispatchers.IO) {
        val logId = syncLogRepository.startLog(MODE_CSV)
        Log.d(TAG, "syncViaCsv — uri=$fileUri logId=$logId")

        try {
            val importer = CsvPricebookImporter()
            val parsed = importer.parseUri(context, fileUri)

            if (parsed.rows.isEmpty()) {
                val msg = "No valid rows found in CSV. " +
                        parsed.errorSamples.joinToString("; ")
                syncLogRepository.failLog(logId, msg)
                return@withContext SyncResult(
                    MODE_CSV, 0, 0, 0, parsed.parseErrors + 1, parsed.errorSamples, success = false
                )
            }

            val result = upsertItems(parsed.rows.map { it.toItem() })
            val totalErrors = result.errors + parsed.parseErrors
            syncLogRepository.finishLog(logId, result.added, result.updated, result.unchanged, totalErrors,
                parsed.errorSamples.joinToString("\n").ifBlank { null })
            PreferencesHelper.setLastSyncTimestamp(context, System.currentTimeMillis())
            result.copy(mode = MODE_CSV, errors = totalErrors, errorSamples = parsed.errorSamples)
        } catch (e: Exception) {
            val msg = "CSV import failed: ${e.message}"
            syncLogRepository.failLog(logId, msg)
            SyncResult(MODE_CSV, 0, 0, 0, 1, listOf(msg), success = false)
        }
    }

    // ── Manual placeholder ────────────────────────────────────────────────

    /**
     * Manual / offline mode placeholder — no network call made.
     *
     * Use this when Modisoft does not provide API or CSV export access.
     * Items must be added manually via the Search / Edit screens.
     *
     * TODO: If Modisoft ever provides official web-portal export capability
     * (e.g. a downloadable report URL), implement it here. Do not add any
     * web-scraping or credential-based login without explicit legal approval
     * from the account owner and Modisoft.
     */
    suspend fun syncManual(context: Context): SyncResult {
        val logId = syncLogRepository.startLog(MODE_MANUAL)
        val msg = "Manual mode: no automatic sync available. Use API or CSV import, " +
                "or add items manually from the Search screen."
        syncLogRepository.failLog(logId, msg)
        return SyncResult(MODE_MANUAL, 0, 0, 0, 0, listOf(msg), success = true)
    }

    // ── Shared upsert logic ───────────────────────────────────────────────

    /**
     * Upsert a list of items into Room.
     *
     * For each item:
     *  - If the barcode already exists → update fields if anything changed (count as updated)
     *  - If the barcode is new → insert (count as added)
     *  - If barcode exists and all fields match → count as unchanged
     */
    private suspend fun upsertItems(incoming: List<Item>): SyncResult {
        var added = 0; var updated = 0; var unchanged = 0; var errors = 0

        for (item in incoming) {
            try {
                val existing = itemRepository.findByBarcode(item.barcode)
                if (existing == null) {
                    itemRepository.insert(item)
                    added++
                } else {
                    val changed = existing.description != item.description
                            || existing.price != item.price
                            || existing.cost != item.cost
                            || existing.department != item.department
                            || existing.size != item.size
                    if (changed) {
                        itemRepository.update(
                            existing.copy(
                                description = item.description,
                                price = item.price,
                                cost = item.cost,
                                department = item.department,
                                size = item.size,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                        updated++
                    } else {
                        unchanged++
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error upserting barcode=${item.barcode}: ${e.message}")
                errors++
            }
        }

        Log.d(TAG, "upsert complete: added=$added updated=$updated unchanged=$unchanged errors=$errors")
        return SyncResult("", added, updated, unchanged, errors)
    }

    // ── Extension helpers ─────────────────────────────────────────────────

    private fun ModisoftApiClient.PricebookItem.toItem() = Item(
        barcode = barcode, description = description, price = price,
        cost = cost, department = department, size = size,
        lastUpdated = System.currentTimeMillis()
    )

    private fun CsvPricebookImporter.PricebookRow.toItem() = Item(
        barcode = barcode, description = description, price = price,
        cost = cost, department = department, size = size,
        lastUpdated = System.currentTimeMillis()
    )
}
