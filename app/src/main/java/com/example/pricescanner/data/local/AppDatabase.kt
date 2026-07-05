package com.example.pricescanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PriceEntry::class, ProductLookup::class, ShoppingListItem::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun priceEntryDao(): PriceEntryDao
    abstract fun productLookupDao(): ProductLookupDao
    abstract fun shoppingListDao(): ShoppingListDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "price_history_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}