package com.example.pricescanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.pricescanner.ui.camera.CameraScreen
import androidx.compose.material3.Surface
import android.Manifest
import com.example.pricescanner.ui.theme.PriceScannerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Request the camera permission
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission allowed!
            } else {
                // Handle permission denied (e.g., show a message)
            }
        }

        // Trigger the popup
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        enableEdgeToEdge()
        setContent {
            PriceScannerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 2. Call the CameraScreen we created earlier
                    CameraScreen()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PriceScannerTheme {
        Greeting("Android")
    }
}