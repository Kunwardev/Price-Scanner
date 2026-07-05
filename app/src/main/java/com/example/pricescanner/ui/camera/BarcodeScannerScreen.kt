package com.example.pricescanner.ui.camera

import android.text.format.DateUtils
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pricescanner.viewmodel.PriceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    viewModel: PriceViewModel,
    onProductFound: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    
    var detectedCode by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var isUnknownItem by remember { mutableStateOf(false) }

    // Observe price history for the identified product
    val history by produceState(initialValue = emptyList(), productName) {
        if (productName.isNotEmpty() && !isUnknownItem) {
            viewModel.getComparisonResults(productName).collect { value = it }
        } else {
            value = emptyList()
        }
    }

    val currentStoreEntry = history.find { it.storeName.equals(viewModel.currentStoreName, ignoreCase = true) }
    val otherStoresHistory = history.filter { !it.storeName.equals(viewModel.currentStoreName, ignoreCase = true) }

    LaunchedEffect(detectedCode) {
        if (detectedCode.isNotEmpty()) {
            val dbName = viewModel.getProductName(detectedCode)
            if (dbName != null) {
                productName = dbName
                isUnknownItem = false
            } else {
                productName = "Unknown Item"
                isUnknownItem = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SKU / Barcode Scanner") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            if (detectedCode.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Code: $detectedCode", style = MaterialTheme.typography.labelSmall)
                        
                        if (isUnknownItem) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Product Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(text = productName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            
                            if (currentStoreEntry != null) {
                                val currentPrice = String.format(Locale.US, "%.2f", currentStoreEntry.price)
                                val relativeTime = DateUtils.getRelativeTimeSpanString(
                                    currentStoreEntry.timestamp,
                                    System.currentTimeMillis(),
                                    DateUtils.MINUTE_IN_MILLIS
                                ).toString()
                                
                                Text(
                                    text = "Last seen here ($relativeTime): $$currentPrice",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { 
                                val finalName = if (isUnknownItem) customName else productName
                                if (isUnknownItem && customName.isNotBlank()) {
                                    viewModel.saveProductMapping(detectedCode, customName)
                                }
                                onProductFound(finalName) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUnknownItem || customName.isNotBlank()
                        ) {
                            Text(if (currentStoreEntry != null) "Update Price" else "Set New Price")
                        }

                        // Display comparison BELOW the button
                        if (otherStoresHistory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Prices in other stores:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            otherStoresHistory.take(3).forEach { entry ->
                                val displayPrice = viewModel.getDisplayNormalizedPrice(entry.pricePerUnit, entry.unit)
                                val entryTime = DateUtils.getRelativeTimeSpanString(
                                    entry.timestamp,
                                    System.currentTimeMillis(),
                                    DateUtils.MINUTE_IN_MILLIS
                                ).toString()
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.storeName, style = MaterialTheme.typography.bodySmall)
                                        Text(entryTime, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                    Text(displayPrice, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analyzer = BarcodeAnalyzer { code -> detectedCode = code }
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer) }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
        } catch (e: Exception) {
            Log.e("BarcodeScanner", "Binding failed", e)
        }
    }
}
