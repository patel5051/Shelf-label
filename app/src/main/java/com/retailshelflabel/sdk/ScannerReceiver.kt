package com.retailshelflabel.sdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver for SUNMI L3 scanner intents.
 *
 * Registered in AndroidManifest.xml for:
 *   "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"
 *   "android.intent.ACTION_DECODE_DATA"
 *
 * Broadcasts the barcode via [ScanResultBus] — any active Fragment can
 * collect results without tight coupling to this receiver.
 */
class ScannerReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScannerReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val barcode = when (intent.action) {
            ScannerManager.ACTION_SCAN_RESULT ->
                intent.getStringExtra(ScannerManager.EXTRA_BARCODE_DATA)

            ScannerManager.ACTION_SCAN_RESULT_ALT ->
                intent.getStringExtra(ScannerManager.EXTRA_BARCODE_DATA_ALT)
                    ?: intent.getStringExtra(ScannerManager.EXTRA_BARCODE_DATA)

            else -> null
        }

        Log.d(TAG, "onReceive action=${intent.action}, barcode=$barcode")

        if (!barcode.isNullOrBlank()) {
            ScanResultBus.emit(barcode.trim())
        }
    }
}
