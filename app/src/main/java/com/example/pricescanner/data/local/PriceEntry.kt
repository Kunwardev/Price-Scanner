package com.example.pricescanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_history")
data class PriceEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val storeName: String,
    val itemName: String,
    val price: Double,
    val quantity: Double = 1.0,
    val unit: String = "unit",
    val pricePerUnit: Double = price,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)
