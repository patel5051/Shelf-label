package com.retailshelflabel.ui.sync

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.retailshelflabel.ShelfLabelApplication
import com.retailshelflabel.data.db.SyncLog
import com.retailshelflabel.data.repository.ItemRepository
import com.retailshelflabel.data.repository.SyncLogRepository
import com.retailshelflabel.sync.ModisoftSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ModisoftSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ShelfLabelApplication
    private val syncLogRepository = SyncLogRepository(app.database.syncLogDao())
    private val itemRepository = ItemRepository(app.database.itemDao())
    private val syncManager = ModisoftSyncManager(itemRepository, syncLogRepository)

    val recentLogs: LiveData<List<SyncLog>> = syncLogRepository.recentLogs

    private val _syncState = MutableLiveData<SyncState>(SyncState.Idle)
    val syncState: LiveData<SyncState> = _syncState

    private val viewModelJob = SupervisorJob()
    private val ioScope = CoroutineScope(Dispatchers.IO + viewModelJob)

    sealed class SyncState {
        object Idle : SyncState()
        object Running : SyncState()
        data class Done(
            val added: Int, val updated: Int, val unchanged: Int,
            val errors: Int, val success: Boolean, val message: String
        ) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    fun syncNow(mode: String, csvUri: Uri? = null) {
        if (_syncState.value is SyncState.Running) return
        _syncState.value = SyncState.Running

        ioScope.launch {
            val result = when (mode) {
                ModisoftSyncManager.MODE_API    -> syncManager.syncViaApi(getApplication())
                ModisoftSyncManager.MODE_CSV    -> {
                    if (csvUri == null) {
                        _syncState.postValue(SyncState.Error("No CSV file selected"))
                        return@launch
                    }
                    syncManager.syncViaCsv(getApplication(), csvUri)
                }
                ModisoftSyncManager.MODE_MANUAL -> syncManager.syncManual(getApplication())
                else -> {
                    _syncState.postValue(SyncState.Error("Unknown sync mode: $mode"))
                    return@launch
                }
            }

            val msg = when {
                result.mode == ModisoftSyncManager.MODE_MANUAL ->
                    result.errorSamples.firstOrNull() ?: "Manual mode active"
                result.success && result.errors == 0 ->
                    "Sync complete — ${result.added} added, ${result.updated} updated, ${result.unchanged} unchanged"
                result.success ->
                    "Sync partial — ${result.added} added, ${result.updated} updated, ${result.errors} errors"
                else ->
                    "Sync failed: ${result.errorSamples.firstOrNull() ?: "unknown error"}"
            }

            _syncState.postValue(
                SyncState.Done(
                    result.added, result.updated, result.unchanged,
                    result.errors, result.success, msg
                )
            )
        }
    }

    fun resetState() { _syncState.value = SyncState.Idle }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}
