package com.retailshelflabel.sdk

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.data.db.PrintJob
import com.retailshelflabel.data.repository.PrintJobRepository
import com.retailshelflabel.util.BarcodeUtils
import com.retailshelflabel.util.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SUNMI Printer abstraction layer.
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  HOW TO INTEGRATE THE REAL SUNMI SDK                                     │
 * │                                                                          │
 * │  1. Download the SUNMI Printer SDK from:                                 │
 * │     https://developer.sunmi.com/en-US/                                   │
 * │  2. Place the following files in  app/libs/ :                            │
 * │       SunmiPrinterService.aar  (or .jar)                                 │
 * │       woyou.aidlservice.aar    (inner printer AIDL, if separate)         │
 * │  3. Add to app/build.gradle.kts:                                         │
 * │       implementation(fileTree("libs") { include("*.aar", "*.jar") })     │
 * │  4. Replace every TODO block below with the real SDK calls.              │
 * │     Refer to SUNMI Printer SDK v2 documentation for API details.         │
 * └──────────────────────────────────────────────────────────────────────────┘
 *
 * The stub implementation logs every call so you can verify the flow in
 * Logcat without physical hardware. All status states are modelled so the
 * UI can react correctly once the real SDK is wired in.
 *
 * @param printJobRepository  Optional repository for persisting print history.
 *   When provided, every successful [printShelfLabel] call inserts a [PrintJob]
 *   record so staff can reprint labels from the History screen.
 */
class PrinterManager(
    private val context: Context,
    private val printJobRepository: PrintJobRepository? = null
) {

    companion object {
        private const val TAG = "PrinterManager"

        // ── Printer status codes (mirrors SUNMI SDK WoyouConsts) ─────────────
        /** Printer is functioning normally */
        const val STATUS_NORMAL = 0
        /** Preparing / initialising */
        const val STATUS_PREPARING = 1
        /** Abnormal state (cover open, mechanical jam, etc.) */
        const val STATUS_ABNORMAL = 2
        /** Out of paper */
        const val STATUS_OUT_OF_PAPER = 3
        /** Firmware upgrade in progress */
        const val STATUS_UPGRADING = 4
        /** Unknown or unrecoverable error */
        const val STATUS_ERROR = 5

        /** Human-readable label for each status code — shown in UI Snackbars */
        fun statusLabel(code: Int): String = when (code) {
            STATUS_NORMAL -> "Printer ready"
            STATUS_PREPARING -> "Printer initialising — please wait"
            STATUS_ABNORMAL -> "Printer error: check cover and paper path"
            STATUS_OUT_OF_PAPER -> "Out of paper — please reload"
            STATUS_UPGRADING -> "Printer firmware upgrading — please wait"
            STATUS_ERROR -> "Printer error — restart the device"
            else -> "Unknown printer status ($code)"
        }
    }

    private var connected = false

    /**
     * Initialise the SUNMI printer service.
     * Calls [onReady] when the printer is available, [onError] with a status
     * label if connection fails or the printer is not in a printable state.
     *
     * TODO: replace stub with:
     *   InnerPrinterManager.getInstance().bindService(context, object : InnerPrinterCallback() {
     *       override fun onCalling(innerPrinterException: InnerPrinterException?) { ... }
     *   })
     */
    fun initPrinter(onReady: () -> Unit = {}, onError: (String) -> Unit = {}) {
        Log.d(TAG, "initPrinter() — stub mode")
        // TODO: bind real SUNMI InnerPrinterManager AIDL service

        // Simulate a ready printer in stub mode
        connected = true
        onReady()
    }

    /**
     * Release the printer service binding on Activity/Fragment destroy.
     *
     * TODO: InnerPrinterManager.getInstance().unBindService(context, callback)
     */
    fun releasePrinter() {
        Log.d(TAG, "releasePrinter()")
        connected = false
    }

    /**
     * Query the current printer hardware status.
     *
     * @return One of the STATUS_* constants above.
     *
     * TODO: Replace with synchronous InnerPrinterManager query:
     *   val status = InnerPrinterManager.getInstance().queryPrinterStatus()
     *   return status  // maps to STATUS_NORMAL / STATUS_OUT_OF_PAPER / STATUS_ABNORMAL etc.
     */
    fun getPrinterStatus(): Int {
        Log.d(TAG, "getPrinterStatus() — stub returning ${if (connected) "NORMAL" else "PREPARING"}")
        return if (connected) STATUS_NORMAL else STATUS_PREPARING
    }

    /**
     * Print a shelf label for [item].
     *
     * Checks printer status before printing and surfaces a human-readable
     * error if the printer is not ready (out of paper, abnormal, etc.).
     *
     * On success, inserts a [PrintJob] record into the database when a
     * [printJobRepository] was provided at construction time, so the History
     * screen can offer one-tap reprints.
     *
     * Label layout:
     *   [STORE NAME]
     *   ITEM DESCRIPTION
     *   $PRICE
     *   |||||||||||||||| (barcode bitmap)
     *   012345678905     (barcode digits)
     *   [Department]  [Size]
     *
     * @param item    The item to print
     * @param copies  Number of copies (default = 1)
     * @param onDone  (success: Boolean, userMessage: String?) — called when done
     *
     * TODO: Replace stub block with real SUNMI ESC/POS or TSPL commands.
     */
    fun printShelfLabel(
        item: Item,
        copies: Int = 1,
        onDone: (success: Boolean, message: String?) -> Unit = { _, _ -> }
    ) {
        Log.d(TAG, "printShelfLabel(barcode=${item.barcode}, copies=$copies)")

        // ── Pre-flight status check ──────────────────────────────────────────
        if (!connected) {
            onDone(false, statusLabel(STATUS_PREPARING))
            return
        }
        val status = getPrinterStatus()
        if (status != STATUS_NORMAL) {
            onDone(false, statusLabel(status))
            return
        }

        // ── Build label content ──────────────────────────────────────────────
        val storeName  = PreferencesHelper.getStoreName(context)
        val currency   = PreferencesHelper.getCurrencySymbol(context)
        val barcodeType = PreferencesHelper.getBarcodeType(context)
        val normBarcode = BarcodeUtils.normalise(item.barcode, barcodeType)
        val barcodeBitmap: Bitmap? = BarcodeUtils.encode(normBarcode, barcodeType)

        /*
         * ── TODO: Real SUNMI receipt printer (ESC/POS) ──────────────────────
         *
         * val svc = InnerPrinterManager.getInstance().getService()
         * repeat(copies) {
         *     svc.printerInit(null)
         *     svc.setAlignment(1, null)                            // centre
         *     if (storeName.isNotBlank()) {
         *         svc.printText("$storeName\n", null)
         *         svc.printLine(null)
         *     }
         *     svc.setAlignment(0, null)                            // left
         *     svc.printTextWithFont(item.description, null, 24f, null)
         *     svc.setAlignment(1, null)                            // centre
         *     svc.setFontSize(52f, null)
         *     svc.printText("$currency${String.format("%.2f", item.price)}\n", null)
         *     svc.setFontSize(24f, null)
         *     if (barcodeBitmap != null) svc.printBitmap(barcodeBitmap, null)
         *     svc.printText("$normBarcode\n", null)
         *     if (item.department.isNotBlank()) svc.printText("[${item.department}]\n", null)
         *     svc.lineWrap(3, null)
         * }
         *
         * ── TODO: SUNMI label printer (TSPL / ZPL mode) ─────────────────────
         *
         * Use SunmiLabelPrinter SDK commands to define label size,
         * set text fields, draw the barcode, and commit the print job.
         * Refer to SUNMI Label Printer SDK docs for TSPL command wrappers.
         *
         * ── Printer error callback ───────────────────────────────────────────
         *
         * Both SDKs report async errors via callbacks/listeners. Map each
         * error code to a STATUS_* constant and call:
         *   onDone(false, statusLabel(mappedStatus))
         *
         * For out-of-paper: STATUS_OUT_OF_PAPER  → "Out of paper — please reload"
         * For cover open:   STATUS_ABNORMAL       → "Printer error: check cover and paper path"
         */

        Log.d(TAG, "Stub print — store=$storeName item=${item.description} " +
                "price=$currency${item.price} barcode=$normBarcode copies=$copies")

        // ── Record print history ─────────────────────────────────────────────
        printJobRepository?.let { repo ->
            CoroutineScope(Dispatchers.IO).launch {
                repo.insert(
                    PrintJob(
                        barcode = item.barcode,
                        description = item.description,
                        price = item.price,
                        copies = copies
                    )
                )
            }
        }

        onDone(true, null)
    }

    /**
     * Print a test label to verify connectivity and label alignment.
     *
     * TODO: replace with real printer test-page command / status check.
     */
    fun printTestLabel(onDone: (success: Boolean, message: String?) -> Unit = { _, _ -> }) {
        Log.d(TAG, "printTestLabel()")
        if (!connected) {
            onDone(false, statusLabel(STATUS_PREPARING))
            return
        }
        val status = getPrinterStatus()
        if (status == STATUS_OUT_OF_PAPER) {
            onDone(false, statusLabel(STATUS_OUT_OF_PAPER))
            return
        }
        if (status != STATUS_NORMAL) {
            onDone(false, statusLabel(status))
            return
        }
        // TODO: send actual test-print ESC/POS or TSPL command
        onDone(true, "Test label printed (stub)")
    }
}
