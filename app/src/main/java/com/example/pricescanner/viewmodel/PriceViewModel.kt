package com.example.pricescanner.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricescanner.data.local.AppDatabase
import com.example.pricescanner.data.local.PriceEntry
import com.example.pricescanner.data.local.ProductLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PriceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val priceDao = database.priceEntryDao()
    private val productDao = database.productLookupDao()
    private val sharedPrefs = application.getSharedPreferences("price_scanner_prefs", Context.MODE_PRIVATE)

    val allPrices: Flow<List<PriceEntry>> = priceDao.getAllItems()

    var currentStoreName by mutableStateOf(sharedPrefs.getString("last_store", "") ?: "")
        private set

    var showStoreDialog by mutableStateOf(currentStoreName.isBlank())
        private set

    fun setStore(newName: String) {
        currentStoreName = newName
        showStoreDialog = false
        sharedPrefs.edit().putString("last_store", newName).apply()
    }

    fun openStoreDialog() {
        showStoreDialog = true
    }

    fun getComparisonResults(itemName: String): Flow<List<PriceEntry>> {
        return priceDao.getEntriesByItemName(itemName)
    }

    suspend fun getProductName(barcode: String): String? {
        return withContext(Dispatchers.IO) {
            productDao.getProductByBarcode(barcode)?.productName
        }
    }

    fun saveProductMapping(barcode: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            productDao.insertProduct(ProductLookup(barcode, name))
        }
    }

    /**
     * Helper to parse raw text into Item Name and Price.
     */
    fun parseInfoFromText(rawText: String): Pair<String, Double> {
        val potentialPrices = Regex("""\d+\s*[.,\s]\s*\d{1,2}|\d+""").findAll(rawText)
            .map { it.value }
            .toList()

        if (potentialPrices.isEmpty()) return "Unknown Item" to 0.0

        val priceMatch = potentialPrices
            .filter { it.contains(Regex("[.,]")) || (it.contains(" ") && it.trim().split(" ").last().length == 2) }
            .map { it.replace(Regex("""\s+"""), "").replace(",", ".") }
            .maxByOrNull { it.toDoubleOrNull() ?: 0.0 }
            ?: potentialPrices.maxByOrNull { it.toDoubleOrNull() ?: 0.0 }

        val priceValue = priceMatch?.toDoubleOrNull() ?: 0.0

        var itemName = rawText
        if (priceMatch != null) {
            val originalMatch = potentialPrices.find { 
                it.replace(Regex("""\s+"""), "").replace(",", ".") == priceMatch 
            }
            if (originalMatch != null) {
                itemName = rawText.replace(originalMatch, "")
            }
        }

        itemName = itemName
            .replace("$", "")
            .replace(Regex("""\d+\s*(oz|lb|kg|g|ml|L)\b""", RegexOption.IGNORE_CASE), "")
            .replace("/lb", "", true)
            .replace("/kg", "", true)
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (itemName.isBlank()) itemName = "Unknown Item"
        return itemName to priceValue
    }

    /**
     * Checks if an entry already exists for this item in the current store.
     */
    suspend fun checkForExistingEntry(rawText: String): PriceEntry? {
        val parsed = parseInfoFromText(rawText)
        return withContext(Dispatchers.IO) {
            priceDao.getEntryByStoreAndItem(parsed.first, currentStoreName)
        }
    }

    suspend fun savePriceEntry(rawText: String): String {
        val (itemName, priceValue) = parseInfoFromText(rawText)

        withContext(Dispatchers.IO) {
            val existing = priceDao.getEntryByStoreAndItem(itemName, currentStoreName)
            if (existing != null) {
                priceDao.updateItem(existing.copy(
                    price = priceValue, 
                    pricePerUnit = priceValue,
                    timestamp = System.currentTimeMillis()
                ))
            } else {
                val newEntry = PriceEntry(
                    itemName = itemName,
                    storeName = currentStoreName.ifBlank { "Unknown Store" },
                    price = priceValue,
                    pricePerUnit = priceValue,
                    timestamp = System.currentTimeMillis()
                )
                priceDao.insertItem(newEntry)
            }
        }
        return itemName
    }

    suspend fun saveManualEntry(name: String, price: Double, quantity: Double, unit: String): String {
        val calculatedPricePerUnit = if (quantity > 0) price / quantity else price
        
        withContext(Dispatchers.IO) {
            val existing = priceDao.getEntryByStoreAndItem(name, currentStoreName)
            if (existing != null) {
                priceDao.updateItem(existing.copy(
                    price = price,
                    quantity = quantity,
                    unit = unit,
                    pricePerUnit = calculatedPricePerUnit,
                    timestamp = System.currentTimeMillis()
                ))
            } else {
                val newEntry = PriceEntry(
                    itemName = name,
                    storeName = currentStoreName.ifBlank { "Unknown Store" },
                    price = price,
                    quantity = quantity,
                    unit = unit,
                    pricePerUnit = calculatedPricePerUnit,
                    timestamp = System.currentTimeMillis()
                )
                priceDao.insertItem(newEntry)
            }
        }
        return name
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            priceDao.deleteAll()
        }
    }

    fun deleteEntry(entry: PriceEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            priceDao.deleteItem(entry)
        }
    }
}
