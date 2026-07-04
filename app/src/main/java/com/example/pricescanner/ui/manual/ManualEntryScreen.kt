package com.example.pricescanner.ui.manual

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pricescanner.data.local.PriceEntry
import com.example.pricescanner.viewmodel.PriceViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    viewModel: PriceViewModel,
    initialName: String = "",
    onBack: () -> Unit,
    onSaveSuccess: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var priceInput by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("oz") }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val units = listOf("oz", "lb", "kg", "g", "count", "ml", "L")

    // State for existing data
    var existingEntry by remember { mutableStateOf<PriceEntry?>(null) }
    var otherStorePrices by remember { mutableStateOf<List<PriceEntry>>(emptyList()) }

    // Check for existing data as name changes
    LaunchedEffect(name) {
        if (name.isNotBlank()) {
            viewModel.getComparisonResults(name).collect { history ->
                existingEntry = history.find { it.storeName.equals(viewModel.currentStoreName, ignoreCase = true) }
                otherStorePrices = history.filter { !it.storeName.equals(viewModel.currentStoreName, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Price") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                modifier = Modifier.fillMaxWidth()
            )

            existingEntry?.let { entry ->
                val formattedPrice = String.format(Locale.US, "%.2f", entry.price)
                val relativeTime = DateUtils.getRelativeTimeSpanString(
                    entry.timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
                
                Text(
                    text = "Previously saved here at $$formattedPrice ($relativeTime)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Price ($)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = { Text("Qty") },
                    modifier = Modifier.weight(0.6f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    units.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = { unit = selectionOption; expanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        val p = priceInput.toDoubleOrNull() ?: 0.0
                        val q = quantityInput.toDoubleOrNull() ?: 1.0
                        viewModel.saveManualEntry(name, p, q, unit)
                        onSaveSuccess(name)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && priceInput.isNotBlank()
            ) {
                Text(if (existingEntry != null) "Update Price" else "Save Price")
            }

            // Comparison below the button
            if (otherStorePrices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Prices in other stores:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    otherStorePrices.take(3).forEach { entry ->
                        val formattedUnitPrice = String.format(Locale.US, "%.2f", entry.pricePerUnit)
                        val entryTime = DateUtils.getRelativeTimeSpanString(
                            entry.timestamp,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString()
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.storeName, style = MaterialTheme.typography.bodyMedium)
                                Text(entryTime, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Text(
                                text = "$$formattedUnitPrice/${entry.unit}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
