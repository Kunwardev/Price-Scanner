package com.example.pricescanner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_lookup")
data class ProductLookup(
    @PrimaryKey
    val barcode: String,
    val productName: String
)