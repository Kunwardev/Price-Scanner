package com.example.pricescanner.ui.camera

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pricescanner.viewmodel.PriceViewModel

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

    // Check database when a code is detected
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

            // Scanning area guide
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
                        .padding(bottom = 48.dp)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Code: $detectedCode", style = MaterialTheme.typography.labelSmall)
                        
                        if (isUnknownItem) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("What is this product?") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            Text(text = productName, style = MaterialTheme.typography.headlineSmall)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { 
                                val finalName = if (isUnknownItem) customName else productName
                                if (isUnknownItem && customName.isNotBlank()) {
                                    // Remember this product for next time!
                                    viewModel.saveProductMapping(detectedCode, customName)
                                }
                                onProductFound(finalName) 
                            },
                            enabled = !isUnknownItem || customName.isNotBlank()
                        ) {
                            Text("Set Price for this Item")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analyzer = BarcodeAnalyzer { code ->
            detectedCode = code
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
        } catch (e: Exception) {
            Log.e("BarcodeScanner", "Binding failed", e)
        }
    }
}
