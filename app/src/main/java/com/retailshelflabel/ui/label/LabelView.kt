package com.retailshelflabel.ui.label

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.util.BarcodeUtils

/**
 * Custom View that renders a shelf label preview using the Android Canvas API.
 *
 * The same [setItem] parameters are used by [PrinterManager] to build the
 * ESC/POS or TSPL commands for the physical printer.
 *
 * Label layout:
 *  ┌──────────────────────────────┐
 *  │         STORE NAME           │  (centred, small caps)
 *  │  ITEM DESCRIPTION            │  (bold, wrapping)
 *  │           $2.99              │  (large bold price)
 *  │  ||||||||||||||||||||||||     │  (barcode bitmap)
 *  │  012345678905                │  (barcode digits)
 *  │  Beverages        12 oz      │  (dept + size, optional)
 *  └──────────────────────────────┘
 */
class LabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var item: Item? = null
    private var storeName = ""
    private var currency = "$"
    private var barcodeType = "CODE128"

    // Paints
    private val bgPaint = Paint().apply { color = Color.WHITE }
    private val borderPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val storeNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        color = Color.DKGRAY
    }
    private val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        color = Color.BLACK
    }
    private val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
    }
    private val barcodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        color = Color.DKGRAY
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.GRAY
    }

    fun setItem(item: Item, storeName: String, currency: String, barcodeType: String) {
        this.item = item
        this.storeName = storeName
        this.currency = currency
        this.barcodeType = barcodeType
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = w * 0.04f

        // Background + border
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        canvas.drawRect(0f, 0f, w, h, borderPaint)

        val current = item ?: run {
            drawPlaceholder(canvas, w, h)
            return
        }

        var y = pad * 2

        // Store name
        storeNamePaint.textSize = w * 0.045f
        y += storeNamePaint.textSize
        if (storeName.isNotBlank()) {
            canvas.drawText(storeName.uppercase(), w / 2, y, storeNamePaint)
            y += storeNamePaint.textSize * 0.4f
            canvas.drawLine(pad, y, w - pad, y, borderPaint)
            y += pad
        }

        // Description
        descPaint.textSize = w * 0.055f
        y += descPaint.textSize
        canvas.drawText(
            current.description.uppercase().take(36),
            pad, y, descPaint
        )
        y += pad

        // Price
        pricePaint.textSize = w * 0.13f
        y += pricePaint.textSize
        canvas.drawText("$currency${String.format("%.2f", current.price)}", w / 2, y, pricePaint)
        y += pad * 1.5f

        // Barcode bitmap
        val normalisedBarcode = BarcodeUtils.normalise(current.barcode, barcodeType)
        val barcodeW = (w - pad * 2).toInt().coerceAtLeast(1)
        val barcodeH = (h * 0.22f).toInt().coerceAtLeast(1)
        val barcodeBitmap = BarcodeUtils.encode(normalisedBarcode, barcodeType, barcodeW, barcodeH)

        if (barcodeBitmap != null) {
            canvas.drawBitmap(barcodeBitmap, pad, y, null)
            y += barcodeH + pad * 0.5f
        }

        // Barcode number
        barcodePaint.textSize = w * 0.038f
        y += barcodePaint.textSize
        canvas.drawText(normalisedBarcode, w / 2, y, barcodePaint)
        y += pad

        // Department + size
        if (current.department.isNotBlank() || current.size.isNotBlank()) {
            smallPaint.textSize = w * 0.04f
            y += smallPaint.textSize
            val meta = listOf(current.department, current.size)
                .filter { it.isNotBlank() }
                .joinToString("  |  ")
            canvas.drawText(meta, w / 2, y, smallPaint)
        }
    }

    private fun drawPlaceholder(canvas: Canvas, w: Float, h: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = Color.LTGRAY
            textSize = w * 0.05f
        }
        canvas.drawText("Select an item to preview", w / 2, h / 2, p)
    }
}
