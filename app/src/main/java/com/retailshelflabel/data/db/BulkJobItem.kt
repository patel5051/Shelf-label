package com.retailshelflabel.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing one item line within a [BulkJob].
 *
 * Stores a snapshot of the item at print time (description, price, barcode)
 * so the job can be replayed even if the catalogue entry is later edited or
 * deleted.
 */
@Entity(
    tableName = "bulk_job_items",
    foreignKeys = [
        ForeignKey(
            entity = BulkJob::class,
            parentColumns = ["bulk_job_id"],
            childColumns = ["bulk_job_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bulk_job_id")]
)
data class BulkJobItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** FK to the parent [BulkJob] */
    @ColumnInfo(name = "bulk_job_id")
    val bulkJobId: Long,

    /** Snapshot of item barcode at print time */
    @ColumnInfo(name = "barcode")
    val barcode: String,

    /** Snapshot of item description at print time */
    @ColumnInfo(name = "description")
    val description: String,

    /** Snapshot of item price at print time */
    @ColumnInfo(name = "price")
    val price: Double,

    /** Number of copies printed for this item */
    @ColumnInfo(name = "copies")
    val copies: Int
)
