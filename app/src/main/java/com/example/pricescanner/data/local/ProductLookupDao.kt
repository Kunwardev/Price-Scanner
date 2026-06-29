package com.example.pricescanner.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductLookupDao {
    @Query("SELECT * FROM product_lookup WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductLookup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductLookup)

    @Query("SELECT * FROM product_lookup")
    suspend fun getAllProducts(): List<ProductLookup>
}