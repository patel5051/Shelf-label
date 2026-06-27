package com.retailshelflabel.util

import android.content.Context
import android.net.Uri
import com.retailshelflabel.data.db.Item
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Parses a CSV file for bulk item import.
 *
 * Expected columns (header row required, order does not matter):
 *   barcode, description, price, department, size
 *
 * Rules:
 *  - Blank lines are skipped.
 *  - Rows missing [barcode] or [description] are counted as errors.
 *  - [price] is parsed as a Double; non-numeric values default to 0.00.
 *  - [department] and [size] are optional.
 */
object CsvParser {

    data class ImportResult(
        val items: List<Item>,
        val errorRows: Int,
        val errorMessages: List<String>
    )

    /**
     * Parse a CSV [Uri] opened via the Storage Access Framework.
     *
     * @param context Application context for [ContentResolver]
     * @param uri     URI returned by [ActivityResultContracts.GetContent]
     * @return        [ImportResult] with parsed items and error count
     */
    fun parse(context: Context, uri: Uri): ImportResult {
        val items = mutableListOf<Item>()
        val errors = mutableListOf<String>()
        var lineNumber = 0
        var headerMap: Map<String, Int> = emptyMap()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.lineSequence().forEach { rawLine ->
                        lineNumber++
                        val line = rawLine.trim()
                        if (line.isBlank()) return@forEach

                        val cols = splitCsvLine(line)

                        // First non-blank line is always the header
                        if (lineNumber == 1 || headerMap.isEmpty()) {
                            headerMap = cols.mapIndexed { i, col ->
                                col.trim().lowercase() to i
                            }.toMap()
                            return@forEach
                        }

                        // Parse data row
                        val barcode = cols.getOrNull(headerMap["barcode"] ?: -1)?.trim()
                        val description = cols.getOrNull(headerMap["description"] ?: -1)?.trim()
                        val priceStr = cols.getOrNull(headerMap["price"] ?: -1)?.trim()
                        val department = cols.getOrNull(headerMap["department"] ?: -1)?.trim() ?: ""
                        val size = cols.getOrNull(headerMap["size"] ?: -1)?.trim() ?: ""

                        if (barcode.isNullOrBlank()) {
                            errors += "Line $lineNumber: missing barcode"
                            return@forEach
                        }
                        if (description.isNullOrBlank()) {
                            errors += "Line $lineNumber: missing description (barcode=$barcode)"
                            return@forEach
                        }

                        val price = priceStr
                            ?.replace(Regex("[^0-9.]"), "")
                            ?.toDoubleOrNull() ?: 0.00

                        items += Item(
                            barcode = barcode,
                            description = description,
                            price = price,
                            department = department,
                            size = size,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                }
            } ?: errors.add("Could not open file")
        } catch (e: Exception) {
            errors += "File read error: ${e.message}"
        }

        return ImportResult(items, errors.size, errors)
    }

    /**
     * Splits a single CSV line into tokens, respecting double-quoted fields
     * that may contain commas.
     */
    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result += current.toString()
        return result
    }
}
