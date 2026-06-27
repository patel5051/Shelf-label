package com.retailshelflabel.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single retail item in the local database.
 *
 * The [barcode] field has a UNIQUE index — it is used as the natural key for
 * CSV upsert logic (update if exists, insert if not).
 */
@Entity(
    tableName = "items",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class Item(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "item_id")
    val itemId: Long = 0,

    /** UPC / EAN / CODE128 barcode string */
    @ColumnInfo(name = "barcode")
    val barcode: String,

    /** Human-readable product description */
    @ColumnInfo(name = "description")
    val description: String,

    /** Retail price, e.g. 2.99 */
    @ColumnInfo(name = "price")
    val price: Double,

    /** Department or category (e.g. "Beverages", "Snacks") */
    @ColumnInfo(name = "department")
    val department: String = "",

    /** Size or unit (e.g. "12 oz", "1 lb") */
    @ColumnInfo(name = "size")
    val size: String = "",

    /** Wholesale / cost price (populated from Modisoft sync; 0 if unknown) */
    @ColumnInfo(name = "cost")
    val cost: Double = 0.0,

    /** Unix epoch milliseconds of last modification */
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
