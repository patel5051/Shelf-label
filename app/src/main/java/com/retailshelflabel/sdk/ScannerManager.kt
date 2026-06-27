package com.retailshelflabel.sdk

import android.content.Context
import android.content.IntentFilter
import android.util.Log

/**
 * SUNMI Scanner abstraction layer.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  HOW TO INTEGRATE THE REAL SUNMI L3 SCANNER SDK                         │
 * │                                                                          │
 * │  The SUNMI L3 sends scan results via Android broadcasts. No extra        │
 * │  SDK file is required — just register [ScannerReceiver] (already done   │
 * │  in AndroidManifest.xml) and listen for the broadcast intents below.    │
 * │                                                                          │
 * │  Broadcast actions (both are used by different SUNMI firmware versions): │
 * │    "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"  extra key: "data"      │
 * │    "android.intent.ACTION_DECODE_DATA"            extra key: "barcode_string" │
 * │                                                                          │
 * │  If you have the SUNMI Scanner SDK .aar:                                 │
 * │  Place it in  app/libs/  and call ScanManager.getInstance().startDecode()│
 * │  Replace the broadcast approach with the SDK callback below.             │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
class ScannerManager(private val context: Context) {

    companion object {
        private const val TAG = "ScannerManager"

        /** Broadcast action sent by SUNMI L3 firmware */
        const val ACTION_SCAN_RESULT = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"
        /** Alternative action on older SUNMI firmware */
        const val ACTION_SCAN_RESULT_ALT = "android.intent.ACTION_DECODE_DATA"

        /** Extra key carrying the barcode string in SUNMI broadcasts */
        const val EXTRA_BARCODE_DATA = "data"
        const val EXTRA_BARCODE_DATA_ALT = "barcode_string"

        fun buildIntentFilter(): IntentFilter {
            val filter = IntentFilter()
            filter.addAction(ACTION_SCAN_RESULT)
            filter.addAction(ACTION_SCAN_RESULT_ALT)
            return filter
        }
    }

    /**
     * Trigger a single scan cycle.
     *
     * On physical SUNMI hardware the trigger button starts the scanner
     * automatically. This method can be called to trigger a software-initiated
     * scan when a "Scan" button is tapped in the UI.
     *
     * TODO: Replace stub with ScanManager.getInstance().startDecode() if you
     *       have the SUNMI Scanner SDK .aar.
     */
    fun startScan() {
        Log.d(TAG, "startScan() — press the hardware trigger button on the SUNMI L3, " +
                "or integrate ScanManager SDK for software-triggered scan")
        // TODO: ScanManager.getInstance().startDecode()
    }

    /**
     * Stop an in-progress scan cycle.
     *
     * TODO: ScanManager.getInstance().stopDecode()
     */
    fun stopScan() {
        Log.d(TAG, "stopScan() — stub")
        // TODO: ScanManager.getInstance().stopDecode()
    }

    /**
     * Process a barcode string received from the scanner broadcast.
     *
     * The [ScannerReceiver] calls this after extracting the barcode from the intent.
     * The result is dispatched via [ScanResultBus].
     *
     * @param barcode Raw barcode string from the scanner
     */
    fun handleScanResult(barcode: String) {
        val trimmed = barcode.trim()
        Log.d(TAG, "handleScanResult: '$trimmed'")
        if (trimmed.isNotEmpty()) {
            ScanResultBus.emit(trimmed)
        }
    }
}
