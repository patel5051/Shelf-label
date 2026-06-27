package com.retailshelflabel.sdk

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Simple in-process event bus for scanner results.
 *
 * [ScannerReceiver] emits barcodes here; Fragments collect from [events]
 * in their view lifecycle so they automatically unsubscribe when the view
 * is destroyed.
 *
 * Usage (Fragment):
 *   viewLifecycleOwner.lifecycleScope.launch {
 *       ScanResultBus.events.collect { barcode -> handleBarcode(barcode) }
 *   }
 */
object ScanResultBus {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Emit a scanned barcode. Called from [ScannerReceiver] on the main thread. */
    fun emit(barcode: String) {
        _events.tryEmit(barcode)
    }
}
