package com.example.pricescanner

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pricescanner.ui.camera.CameraScreen
import com.example.pricescanner.ui.history.HistoryScreen
import com.example.pricescanner.ui.manual.ManualEntryScreen
import com.example.pricescanner.ui.theme.PriceScannerTheme
import com.example.pricescanner.viewmodel.PriceViewModel

class MainActivity : ComponentActivity() {

    sealed class Screen(val route: String) {
        object Camera : Screen("camera_screen")
        object History : Screen("history_screen")
        object Manual : Screen("manual_screen")
    }
    
    private lateinit var viewModel: PriceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Since PriceViewModel is an AndroidViewModel, we use the default provider.
        // It automatically handles passing the Application context.
        viewModel = ViewModelProvider(this)[PriceViewModel::class.java]

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission allowed!
            }
        }

        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        enableEdgeToEdge()

        setContent {
            PriceScannerTheme {
                StoreInputDialog(viewModel)
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: PriceViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MainActivity.Screen.Camera.route
    ) {
        composable(MainActivity.Screen.Camera.route) {
            CameraScreen(
                viewModel = viewModel,
                onNavigateToHistory = { navController.navigate(MainActivity.Screen.History.route) },
                onNavigateToManual = { navController.navigate(MainActivity.Screen.Manual.route) }
            )
        }

        composable(MainActivity.Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(MainActivity.Screen.Manual.route) {
            ManualEntryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun StoreInputDialog(viewModel: PriceViewModel) {
    if (viewModel.showStoreDialog) {
        var tempInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { },
            title = { Text("Welcome! Which store are you in?") },
            text = {
                TextField(
                    value = tempInput,
                    onValueChange = { tempInput = it },
                    placeholder = { Text("e.g. Walmart, Costco") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    enabled = tempInput.isNotBlank(),
                    onClick = { viewModel.setStore(tempInput) }
                ) {
                    Text("Start Scanning")
                }
            }
        )
    }
}
