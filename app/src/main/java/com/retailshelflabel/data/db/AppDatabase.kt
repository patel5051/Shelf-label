package com.retailshelflabel.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database singleton.
 *
 * Version history:
 *  1 — initial schema (items table)
 *  2 — added print_jobs table for print history
 *  3 — added bulk_jobs and bulk_job_items tables for bulk print history
 *  4 — added cost column to items; added sync_logs table for Modisoft sync history
 */
@Database(
    entities = [Item::class, PrintJob::class, BulkJob::class, BulkJobItem::class, SyncLog::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun printJobDao(): PrintJobDao
    abstract fun bulkJobDao(): BulkJobDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        private const val DATABASE_NAME = "shelf_label_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
