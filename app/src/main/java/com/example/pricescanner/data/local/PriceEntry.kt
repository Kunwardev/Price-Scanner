package com.example.pricescanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_history")
data class PriceEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val storeName: String, // Where you saw it
    val itemName: String,  // What it is (Milk, Headphones, Drill, etc.)
    val price: Double,      // How much it costs
    val category: String = "General" // Optional: helps you filter later!
)