package com.retailshelflabel.ui.csv

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.retailshelflabel.ShelfLabelApplication
import com.retailshelflabel.data.repository.ItemRepository
import com.retailshelflabel.util.CsvParser
import kotlinx.coroutines.launch

class CsvImportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ItemRepository(
        (application as ShelfLabelApplication).database.itemDao()
    )

    private val _state = MutableLiveData<ImportState>(ImportState.Idle)
    val state: LiveData<ImportState> = _state

    sealed class ImportState {
        object Idle : ImportState()
        object Loading : ImportState()
        data class Complete(
            val newItems: Int,
            val updatedItems: Int,
            val errors: Int,
            val errorMessages: List<String>
        ) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    fun importCsv(uri: Uri) {
        if (_state.value is ImportState.Loading) return
        _state.value = ImportState.Loading

        viewModelScope.launch {
            try {
                val ctx = getApplication<ShelfLabelApplication>()
                val parsed = CsvParser.parse(ctx, uri)
                var newCount = 0
                var updatedCount = 0

                for (item in parsed.items) {
                    val existing = repository.findByBarcode(item.barcode)
                    if (existing != null) {
                        repository.upsert(item.copy(itemId = existing.itemId))
                        updatedCount++
                    } else {
                        repository.upsert(item)
                        newCount++
                    }
                }

                _state.value = ImportState.Complete(
                    newItems = newCount,
                    updatedItems = updatedCount,
                    errors = parsed.errorRows,
                    errorMessages = parsed.errorMessages
                )
            } catch (e: Exception) {
                _state.value = ImportState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun reset() { _state.value = ImportState.Idle }
}
