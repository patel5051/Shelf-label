package com.retailshelflabel

import android.app.Application
import android.util.Log
import com.retailshelflabel.data.db.AppDatabase
import com.retailshelflabel.data.repository.ItemRepository
import com.retailshelflabel.data.repository.SyncLogRepository
import com.retailshelflabel.sync.ModisoftSyncManager
import com.retailshelflabel.util.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class — initialises the Room database singleton and optionally
 * triggers an automatic Modisoft pricebook sync on launch.
 */
class ShelfLabelApplication : Application() {

    /** Lazily-initialised Room database instance shared across the app. */
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        maybeAutoSync()
    }

    /**
     * If "Auto-sync on launch" is enabled in Settings, trigger a background
     * Modisoft sync respecting the configured interval.
     *
     * This never blocks the main thread — the sync runs on [applicationScope].
     * The local database remains fully usable while the sync is in progress.
     */
    private fun maybeAutoSync() {
        if (!PreferencesHelper.isModisoftAutoSyncEnabled(this)) return

        val intervalMs = PreferencesHelper.getModisoftSyncIntervalHours(this) * 3_600_000L
        val lastSync   = PreferencesHelper.getLastSyncTimestamp(this)
        val due        = (System.currentTimeMillis() - lastSync) >= intervalMs

        if (!due) {
            Log.d("ShelfLabelApp", "Auto-sync skipped — next sync not due yet")
            return
        }

        Log.d("ShelfLabelApp", "Auto-sync starting on launch")
        applicationScope.launch {
            val syncManager = ModisoftSyncManager(
                ItemRepository(database.itemDao()),
                SyncLogRepository(database.syncLogDao())
            )
            val mode = PreferencesHelper.getModisoftSyncMode(this@ShelfLabelApplication)
            try {
                when (mode) {
                    ModisoftSyncManager.MODE_API -> syncManager.syncViaApi(this@ShelfLabelApplication)
                    // CSV and Manual cannot auto-sync (require user file selection / no-op)
                    else -> Log.d("ShelfLabelApp", "Auto-sync skipped — mode=$mode requires user action")
                }
            } catch (e: Exception) {
                Log.w("ShelfLabelApp", "Auto-sync failed: ${e.message}")
            }
        }
    }
}
