package com.retailshelflabel.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException

/**
 * Utility for generating barcode Bitmaps using ZXing Core.
 *
 * Supports CODE128, EAN-13, and UPC-A — selectable in Settings.
 * No camera or scanning dependency required.
 */
object BarcodeUtils {

    /**
     * Renders a barcode as a [Bitmap].
     *
     * @param content     The barcode value (digits or alphanumeric depending on format)
     * @param formatName  One of "CODE128", "EAN13", "UPC_A" (matches [PreferencesHelper] values)
     * @param widthPx     Desired bitmap width in pixels
     * @param heightPx    Desired bitmap height in pixels
     * @return            Rendered [Bitmap], or null on encoding failure
     */
    fun encode(
        content: String,
        formatName: String,
        widthPx: Int = 600,
        heightPx: Int = 150
    ): Bitmap? {
        val format = when (formatName.uppercase()) {
            "EAN13" -> BarcodeFormat.EAN_13
            "UPC_A", "UPCA" -> BarcodeFormat.UPC_A
            else -> BarcodeFormat.CODE_128
        }

        val hints = mapOf(EncodeHintType.MARGIN to 0)

        return try {
            val bitMatrix = MultiFormatWriter().encode(content, format, widthPx, heightPx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height) { i ->
                if (bitMatrix[i % width, i / width]) Color.BLACK else Color.WHITE
            }
            Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565)
        } catch (e: WriterException) {
            null
        } catch (e: IllegalArgumentException) {
            // Content incompatible with chosen format (e.g. non-numeric for EAN-13)
            // Fall back to CODE128
            if (format != BarcodeFormat.CODE_128) {
                encode(content, "CODE128", widthPx, heightPx)
            } else {
                null
            }
        }
    }

    /**
     * Pad or trim a barcode string to the required digit count for EAN-13 / UPC-A.
     * CODE128 accepts arbitrary content so no padding is needed.
     */
    fun normalise(barcode: String, formatName: String): String {
        val digits = barcode.filter { it.isDigit() }
        return when (formatName.uppercase()) {
            "EAN13" -> digits.padStart(13, '0').take(13)
            "UPC_A", "UPCA" -> digits.padStart(12, '0').take(12)
            else -> barcode
        }
    }
}
