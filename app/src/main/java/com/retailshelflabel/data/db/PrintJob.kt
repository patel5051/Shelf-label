package com.retailshelflabel.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single completed print job.
 *
 * A record is inserted every time [com.retailshelflabel.sdk.PrinterManager.printShelfLabel]
 * succeeds, so staff can revisit recent prints and reprint any label in one tap.
 */
@Entity(tableName = "print_jobs")
data class PrintJob(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "job_id")
    val jobId: Long = 0,

    /** Barcode of the item that was printed */
    @ColumnInfo(name = "barcode")
    val barcode: String,

    /** Description of the item at the time of printing */
    @ColumnInfo(name = "description")
    val description: String,

    /** Price of the item at the time of printing */
    @ColumnInfo(name = "price")
    val price: Double,

    /** Number of copies printed */
    @ColumnInfo(name = "copies")
    val copies: Int,

    /** Unix epoch milliseconds when the print job completed */
    @ColumnInfo(name = "printed_at")
    val printedAt: Long = System.currentTimeMillis()
)
