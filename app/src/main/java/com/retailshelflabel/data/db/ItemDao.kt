package com.retailshelflabel.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Data Access Object for [Item].
 *
 * All queries that power the UI return [LiveData] so the UI automatically
 * updates whenever the database changes.
 */
@Dao
interface ItemDao {

    // ── Insert / update ──────────────────────────────────────────────────────

    /**
     * Insert a new item. Returns the new row ID.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: Item): Long

    /**
     * Upsert: replace on barcode conflict — used by CSV import.
     * Returns the row ID (negative if no change on Android Room's impl).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Item): Long

    /**
     * Update an existing item by primary key.
     */
    @Update
    suspend fun update(item: Item)

    // ── Delete ───────────────────────────────────────────────────────────────

    @Delete
    suspend fun delete(item: Item)

    @Query("DELETE FROM items WHERE item_id = :id")
    suspend fun deleteById(id: Long)

    // ── Queries ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM items ORDER BY description ASC")
    fun getAllItems(): LiveData<List<Item>>

    @Query("SELECT * FROM items WHERE item_id = :id")
    fun getItemById(id: Long): LiveData<Item?>

    @Query("SELECT * FROM items WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): Item?

    /**
     * Search by description OR barcode, case-insensitive substring match.
     * Used by the search screen.
     */
    @Query(
        """
        SELECT * FROM items
        WHERE description LIKE '%' || :query || '%'
           OR barcode LIKE '%' || :query || '%'
        ORDER BY description ASC
        """
    )
    fun searchItems(query: String): LiveData<List<Item>>

    /**
     * Fetch a set of items by their primary-key IDs.
     * Used by the bulk-print screen to load the user's selection.
     */
    @Query("SELECT * FROM items WHERE item_id IN (:ids) ORDER BY description ASC")
    suspend fun getItemsByIds(ids: List<Long>): List<Item>

    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int
}
