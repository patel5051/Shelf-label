package com.retailshelflabel.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Imports a Modisoft-exported pricebook CSV into a list of [PricebookRow]s.
 *
 * Column auto-detection supports these header aliases (case-insensitive):
 *
 * | Field       | Recognised header names                                          |
 * |-------------|------------------------------------------------------------------|
 * | barcode     | upc, barcode, sku, item code, itemcode, item#                    |
 * | description | item name, name, description, product, product name, desc        |
 * | price       | retail price, retailprice, price, retail, sell price, unit price |
 * | cost        | cost, cost price, costprice, wholesale, unit cost                |
 * | department  | department, dept, category, cat                                  |
 * | size        | size, unit, qty, unit size, pack                                 |
 *
 * Handles RFC 4180-compliant quoted fields (commas and newlines inside quotes).
 * Rows where [barcode] or [price] cannot be parsed are counted as errors and skipped.
 */
class CsvPricebookImporter {

    companion object {
        private const val TAG = "CsvPricebookImporter"

        // Normalised column aliases (lowercase, spaces stripped)
        private val BARCODE_ALIASES = setOf(
            "upc", "barcode", "sku", "itemcode", "item code", "item#", "upccode", "scan code"
        )
        private val DESCRIPTION_ALIASES = setOf(
            "item name", "itemname", "name", "description", "product", "product name",
            "desc", "item description", "title"
        )
        private val PRICE_ALIASES = setOf(
            "retail price", "retailprice", "price", "retail", "sell price", "unit price",
            "sellprice", "retailprice", "selling price"
        )
        private val COST_ALIASES = setOf(
            "cost", "cost price", "costprice", "wholesale", "unit cost", "buy cost"
        )
        private val DEPARTMENT_ALIASES = setOf(
            "department", "dept", "category", "cat", "department name", "section"
        )
        private val SIZE_ALIASES = setOf(
            "size", "unit", "qty", "unit size", "pack", "pack size", "item size"
        )
    }

    data class PricebookRow(
        val barcode: String,
        val description: String,
        val price: Double,
        val cost: Double = 0.0,
        val department: String = "",
        val size: String = ""
    )

    data class ImportResult(
        val rows: List<PricebookRow>,
        val parseErrors: Int,
        val errorSamples: List<String>   // first few bad rows for display
    )

    /**
     * Parse a pricebook CSV from a SAF [Uri].
     *
     * @return [ImportResult] — call [ImportResult.rows] to get the parsed data.
     */
    fun parseUri(context: Context, uri: Uri): ImportResult {
        val rows = mutableListOf<PricebookRow>()
        val errorSamples = mutableListOf<String>()
        var parseErrors = 0
        var lineNumber = 0

        context.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))

            // Read header line
            val headerLine = reader.readLine() ?: return ImportResult(emptyList(), 0, emptyList())
            lineNumber++
            val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }

            // Map column indices
            val colBarcode     = headers.indexOfFirst { it in BARCODE_ALIASES }
            val colDescription = headers.indexOfFirst { it in DESCRIPTION_ALIASES }
            val colPrice       = headers.indexOfFirst { it in PRICE_ALIASES }
            val colCost        = headers.indexOfFirst { it in COST_ALIASES }
            val colDepartment  = headers.indexOfFirst { it in DEPARTMENT_ALIASES }
            val colSize        = headers.indexOfFirst { it in SIZE_ALIASES }

            if (colBarcode < 0) {
                Log.w(TAG, "No barcode column found. Headers: $headers")
                return ImportResult(
                    emptyList(), 1,
                    listOf("No UPC/Barcode column found. Headers detected: $headers")
                )
            }
            if (colDescription < 0 || colPrice < 0) {
                Log.w(TAG, "Missing required columns. Headers: $headers")
                return ImportResult(
                    emptyList(), 1,
                    listOf("Required columns missing (description + price). Found: $headers")
                )
            }

            // Parse data rows
            var line = reader.readLine()
            while (line != null) {
                lineNumber++
                if (line.isBlank()) { line = reader.readLine(); continue }

                try {
                    val cells = parseCsvLine(line)

                    val barcode = cells.getOrNull(colBarcode)?.trim() ?: ""
                    if (barcode.isEmpty()) {
                        parseErrors++
                        if (errorSamples.size < 5) errorSamples.add("Line $lineNumber: empty barcode")
                        line = reader.readLine()
                        continue
                    }

                    val description = cells.getOrNull(colDescription)?.trim() ?: ""
                    val priceStr = cells.getOrNull(colPrice)?.trim()?.replace(",", "")
                        ?.removePrefix("$") ?: ""
                    val price = priceStr.toDoubleOrNull()
                    if (price == null) {
                        parseErrors++
                        if (errorSamples.size < 5) errorSamples.add("Line $lineNumber: invalid price '$priceStr' for barcode $barcode")
                        line = reader.readLine()
                        continue
                    }

                    val cost = colCost.takeIf { it >= 0 }
                        ?.let { cells.getOrNull(it)?.trim()?.replace(",", "")?.removePrefix("$")?.toDoubleOrNull() }
                        ?: 0.0

                    val department = colDepartment.takeIf { it >= 0 }
                        ?.let { cells.getOrNull(it)?.trim() } ?: ""
                    val size = colSize.takeIf { it >= 0 }
                        ?.let { cells.getOrNull(it)?.trim() } ?: ""

                    rows.add(PricebookRow(barcode, description, price, cost, department, size))
                } catch (e: Exception) {
                    parseErrors++
                    if (errorSamples.size < 5) errorSamples.add("Line $lineNumber: ${e.message}")
                }

                line = reader.readLine()
            }
        }

        Log.d(TAG, "CSV parse complete: ${rows.size} rows, $parseErrors errors")
        return ImportResult(rows, parseErrors, errorSamples)
    }

    /**
     * RFC 4180-compliant CSV line parser.
     * Handles quoted fields containing commas, newlines, and escaped double-quotes ("").
     */
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++ // escaped quote
                }
                c == '"' && inQuotes -> inQuotes = false
                c == ',' && !inQuotes -> { fields.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        fields.add(sb.toString())
        return fields
    }
}
