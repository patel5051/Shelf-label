package com.retailshelflabel.ui.label

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.retailshelflabel.ShelfLabelApplication
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.data.repository.ItemRepository
import com.retailshelflabel.data.repository.PrintJobRepository
import com.retailshelflabel.sdk.PrinterManager
import kotlinx.coroutines.launch

class LabelPreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ShelfLabelApplication
    private val repository = ItemRepository(app.database.itemDao())
    private val printJobRepository = PrintJobRepository(app.database.printJobDao())
    private val printerManager = PrinterManager(application, printJobRepository)

    private val _item = MutableLiveData<Item?>()
    val item: LiveData<Item?> = _item

    private val _copies = MutableLiveData(1)
    val copies: LiveData<Int> = _copies

    private val _printResult = MutableLiveData<PrintResult?>()
    val printResult: LiveData<PrintResult?> = _printResult

    sealed class PrintResult {
        data class Success(val copies: Int) : PrintResult()
        data class Error(val message: String) : PrintResult()
    }

    fun loadItem(itemId: Long) {
        repository.getItemById(itemId).observeForever { _item.value = it }
    }

    fun setCopies(n: Int) {
        _copies.value = n.coerceAtLeast(1)
    }

    fun print() {
        val item = _item.value ?: return
        val copies = _copies.value ?: 1
        printerManager.initPrinter(
            onReady = {
                printerManager.printShelfLabel(item, copies) { success, message ->
                    if (success) {
                        _printResult.value = PrintResult.Success(copies)
                    } else {
                        _printResult.value = PrintResult.Error(message ?: "Unknown error")
                    }
                }
            },
            onError = { err -> _printResult.value = PrintResult.Error(err) }
        )
    }

    fun printTest() {
        printerManager.initPrinter(
            onReady = {
                printerManager.printTestLabel { success, message ->
                    if (success) {
                        _printResult.value = PrintResult.Success(0)
                    } else {
                        _printResult.value = PrintResult.Error(message ?: "Test print failed")
                    }
                }
            },
            onError = { err -> _printResult.value = PrintResult.Error(err) }
        )
    }

    fun clearResult() { _printResult.value = null }
}
