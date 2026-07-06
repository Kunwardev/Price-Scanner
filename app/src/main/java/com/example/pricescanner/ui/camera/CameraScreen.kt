package com.example.pricescanner.ui.camera

import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pricescanner.data.local.PriceEntry
import com.example.pricescanner.viewmodel.PriceViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CameraScreen(
    viewModel: PriceViewModel,
    onNavigateToManual: () -> Unit,
    onNavigateToBarcode: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToShoppingList: () -> Unit,
    onNavigateToComparison: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var detectedText by remember { mutableStateOf("") }
    val previewView = remember { PreviewView(context) }
    
    var existingInStore by remember { mutableStateOf<PriceEntry?>(null) }
    var otherStoresPrice by remember { mutableStateOf<List<PriceEntry>>(emptyList()) }

    LaunchedEffect(detectedText) {
        if (detectedText.isNotBlank()) {
            val (itemName, _) = viewModel.parseInfoFromText(detectedText)
            existingInStore = viewModel.checkForExistingEntry(detectedText)
            
            viewModel.getComparisonResults(itemName).collect { history ->
                otherStoresPrice = history.filter { !it.storeName.equals(viewModel.currentStoreName, ignoreCase = true) }
            }
        } else {
            existingInStore = null
            otherStoresPrice = emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Store and Scanning Info (Top Center)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.small)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Store: ${viewModel.currentStoreName}", color = Color.White)
            Text(text = "Seeing: $detectedText", color = Color.Yellow, style = MaterialTheme.typography.labelSmall)
            TextButton(onClick = { viewModel.openStoreDialog() }) {
                Text("Change Store", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }

        // Action and Comparison Card (Bottom Center)
        if (detectedText.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .fillMaxWidth(0.85f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (existingInStore != null) {
                        val priceString = String.format(Locale.US, "%.2f", existingInStore!!.price)
                        Text(
                            text = "Last seen here: $$priceString",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val itemName = viewModel.savePriceEntry(detectedText)
                                Toast.makeText(context, "Price Saved/Updated", Toast.LENGTH_SHORT).show()
                                onNavigateToComparison(itemName)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (existingInStore != null) "Update Price" else "Capture Price")
                    }

                    if (otherStoresPrice.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text("Prices in other stores:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        otherStoresPrice.take(3).forEach { entry ->
                            val otherPrice = String.format(Locale.US, "%.2f", entry.price)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(entry.storeName, style = MaterialTheme.typography.bodySmall)
                                Text("$$otherPrice", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Side Navigation (Bottom Right)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Shopping List FAB
            FloatingActionButton(
                onClick = onNavigateToShoppingList,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Shopping List")
            }

            FloatingActionButton(onClick = onNavigateToHistory, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "History")
            }
            
            FloatingActionButton(onClick = onNavigateToBarcode, containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                Icon(Icons.Default.Search, contentDescription = "Scan SKU")
            }
            
            FloatingActionButton(onClick = onNavigateToManual) {
                Icon(Icons.Default.Edit, contentDescription = "Manual Entry")
            }
        }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analyzer = PriceTagAnalyzer { text -> detectedText = text }
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer) }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
        } catch (e: Exception) {
            Log.e("CameraScreen", "Binding failed", e)
        }
    }
}
