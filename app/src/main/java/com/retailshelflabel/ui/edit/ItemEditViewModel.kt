package com.retailshelflabel.ui.edit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.retailshelflabel.ShelfLabelApplication
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.data.repository.ItemRepository
import kotlinx.coroutines.launch

class ItemEditViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ItemRepository(
        (application as ShelfLabelApplication).database.itemDao()
    )

    private val _item = MutableLiveData<Item?>()
    val item: LiveData<Item?> = _item

    private val _saveResult = MutableLiveData<SaveResult?>()
    val saveResult: LiveData<SaveResult?> = _saveResult

    sealed class SaveResult {
        data class Success(val itemId: Long) : SaveResult()
        data class Error(val message: String) : SaveResult()
    }

    fun loadItem(itemId: Long) {
        repository.getItemById(itemId).observeForever { _item.value = it }
    }

    fun save(
        barcode: String,
        description: String,
        priceStr: String,
        department: String,
        size: String
    ) {
        if (barcode.isBlank()) {
            _saveResult.value = SaveResult.Error("Barcode is required")
            return
        }
        if (description.isBlank()) {
            _saveResult.value = SaveResult.Error("Description is required")
            return
        }
        val price = priceStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.00
        val existing = _item.value

        viewModelScope.launch {
            try {
                if (existing != null) {
                    repository.update(
                        existing.copy(
                            barcode = barcode.trim(),
                            description = description.trim(),
                            price = price,
                            department = department.trim(),
                            size = size.trim(),
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                    _saveResult.value = SaveResult.Success(existing.itemId)
                } else {
                    val id = repository.insert(
                        Item(
                            barcode = barcode.trim(),
                            description = description.trim(),
                            price = price,
                            department = department.trim(),
                            size = size.trim()
                        )
                    )
                    _saveResult.value = SaveResult.Success(id)
                }
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error(e.message ?: "Save failed")
            }
        }
    }

    fun clearResult() { _saveResult.value = null }
}
