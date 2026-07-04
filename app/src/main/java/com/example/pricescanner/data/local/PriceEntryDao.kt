package com.example.pricescanner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceEntryDao {
    // Save a new price we found with the camera
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(item: PriceEntry)

    // Update an existing item
    @Update
    suspend fun updateItem(item: PriceEntry)

    // Get everything we've ever scanned
    @Query("SELECT * FROM price_history ORDER BY id DESC")
    fun getAllItems() : Flow<List<PriceEntry>>

    // Get entries for a specific item, ordered by price per unit descending
    @Query("SELECT * FROM price_history WHERE itemName = :name ORDER BY pricePerUnit DESC")
    fun getEntriesByItemName(name: String): Flow<List<PriceEntry>>

    // Find an existing entry for a specific item in a specific store
    @Query("SELECT * FROM price_history WHERE itemName = :itemName AND storeName = :storeName LIMIT 1")
    suspend fun getEntryByStoreAndItem(itemName: String, storeName: String): PriceEntry?

    // Clear all entries
    @Query("DELETE FROM price_history")
    suspend fun deleteAll()

    // Delete a specific entry
    @Delete
    suspend fun deleteItem(item: PriceEntry)
}