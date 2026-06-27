package com.retailshelflabel.data.repository

import androidx.lifecycle.LiveData
import com.retailshelflabel.data.db.Item
import com.retailshelflabel.data.db.ItemDao

/**
 * Repository that abstracts access to [ItemDao].
 *
 * ViewModels interact only with this class — they never touch the DAO directly.
 * All database operations are suspend functions or return LiveData and must be
 * called from a coroutine / observed on the main thread respectively.
 */
class ItemRepository(private val dao: ItemDao) {

    val allItems: LiveData<List<Item>> = dao.getAllItems()

    fun searchItems(query: String): LiveData<List<Item>> = dao.searchItems(query)

    suspend fun insert(item: Item): Long = dao.insert(item)

    suspend fun upsert(item: Item): Long = dao.upsert(item)

    suspend fun update(item: Item) = dao.update(item)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun findByBarcode(barcode: String): Item? = dao.findByBarcode(barcode)

    fun getItemById(id: Long): LiveData<Item?> = dao.getItemById(id)

    suspend fun count(): Int = dao.count()

    suspend fun getItemsByIds(ids: List<Long>): List<Item> = dao.getItemsByIds(ids)
}
