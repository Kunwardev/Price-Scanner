package com.example.pricescanner.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pricescanner.data.local.AppDatabase
import com.example.pricescanner.data.local.PriceEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PriceViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).priceEntryDao()

    val allPrices: Flow<List<PriceEntry>> = dao.getAllItems()

    var currentStoreName by mutableStateOf("")
        private set

    var showStoreDialog by mutableStateOf(true)
        private set

    fun setStore(newName: String) {
        currentStoreName = newName
        showStoreDialog = false
    }

    fun openStoreDialog() {
        showStoreDialog = true
    }

    /**
     * Intelligently parses raw text from the scanner.
     */
    fun savePriceEntry(rawText: String) {
        val priceRegex = Regex("""\d+[.,]\d{1,2}|\d+""")
        val priceMatch = priceRegex.find(rawText)
        val priceValue = priceMatch?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

        var itemName = rawText
            .replace(priceRegex, "")
            .replace("$", "")
            .replace("/lb", "", true)
            .replace("/kg", "", true)
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (itemName.isBlank()) itemName = "Unknown Item"

        val newEntry = PriceEntry(
            itemName = itemName,
            storeName = currentStoreName.ifBlank { "Unknown Store" },
            price = priceValue,
            pricePerUnit = priceValue // Default for raw scans
        )

        viewModelScope.launch(Dispatchers.IO) {
            dao.insertItem(newEntry)
        }
    }

    /**
     * Saves a manual entry with specific quantity and calculates price per unit.
     */
    fun saveManualEntry(name: String, price: Double, quantity: Double, unit: String) {
        val calculatedPricePerUnit = if (quantity > 0) price / quantity else price
        
        val newEntry = PriceEntry(
            itemName = name,
            storeName = currentStoreName.ifBlank { "Unknown Store" },
            price = price,
            quantity = quantity,
            unit = unit,
            pricePerUnit = calculatedPricePerUnit
        )

        viewModelScope.launch(Dispatchers.IO) {
            dao.insertItem(newEntry)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAll()
        }
    }

    fun deleteEntry(entry: PriceEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteItem(entry)
        }
    }
}