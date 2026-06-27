package com.retailshelflabel.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.retailshelflabel.ShelfLabelApplication
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.data.repository.ItemRepository
import kotlinx.coroutines.launch

class ItemDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ItemRepository(
        (application as ShelfLabelApplication).database.itemDao()
    )

    /** The item currently being viewed. Populated by [loadItem] or [lookupBarcode]. */
    private val _item = MutableLiveData<Item?>()
    val item: LiveData<Item?> = _item

    /** True when a barcode scan found no match in the database. */
    private val _notFound = MutableLiveData(false)
    val notFound: LiveData<Boolean> = _notFound

    fun loadItem(itemId: Long) {
        repository.getItemById(itemId).observeForever { _item.value = it }
    }

    fun lookupBarcode(barcode: String) {
        viewModelScope.launch {
            val found = repository.findByBarcode(barcode)
            if (found != null) {
                _item.value = found
                _notFound.value = false
            } else {
                _item.value = null
                _notFound.value = true
            }
        }
    }

    fun deleteItem(onDone: () -> Unit) {
        val current = _item.value ?: return
        viewModelScope.launch {
            repository.deleteById(current.itemId)
            onDone()
        }
    }
}
