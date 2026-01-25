package com.example.pricescanner.data.local

import com.example.pricescanner.data.local.PriceEntry
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PriceEntryDao {
    // Save a new price we found with the camera
    @Insert
    suspend fun insertItem(item: PriceEntry)

    // Get everything we've ever scanned
    @Query("SELECT * FROM price_history ORDER BY id DESC")
    fun getAllItems(): List<PriceEntry>
}