package com.example.pricescanner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceEntryDao {
    // Save a new price we found with the camera
    @Insert
    suspend fun insertItem(item: PriceEntry)

    // Get everything we've ever scanned
    @Query("SELECT * FROM price_history ORDER BY id DESC")
    fun getAllItems() : Flow<List<PriceEntry>>

    // Clear all entries
    @Query("DELETE FROM price_history")
    suspend fun deleteAll()

    // Delete a specific entry
    @Delete
    suspend fun deleteItem(item: PriceEntry)
}