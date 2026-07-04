package com.example.pricescanner

import android.Manifest
import android.os.Bundle
import android.util.Log
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pricescanner.ui.camera.BarcodeScannerScreen
import com.example.pricescanner.ui.camera.CameraScreen
import com.example.pricescanner.ui.history.ComparisonScreen
import com.example.pricescanner.ui.history.HistoryScreen
import com.example.pricescanner.ui.manual.ManualEntryScreen
import com.example.pricescanner.ui.theme.PriceScannerTheme
import com.example.pricescanner.viewmodel.PriceViewModel

class MainActivity : ComponentActivity() {

    sealed class Screen(val route: String) {
        object Camera : Screen("camera_screen")
        object Barcode : Screen("barcode_screen")
        object History : Screen("history_screen")
        object Manual : Screen("manual_screen?name={name}") {
            fun createRoute(name: String = "") = "manual_screen?name=$name"
        }
        object Comparison : Screen("comparison_screen/{itemName}") {
            fun createRoute(itemName: String) = "comparison_screen/$itemName"
        }
    }
    
    private lateinit var viewModel: PriceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[PriceViewModel::class.java]

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("MainActivity", "Camera permission granted")
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
                onNavigateToManual = { navController.navigate(MainActivity.Screen.Manual.createRoute()) },
                onNavigateToBarcode = { navController.navigate(MainActivity.Screen.Barcode.route) },
                onNavigateToHistory = { navController.navigate(MainActivity.Screen.History.route) },
                onNavigateToComparison = { itemName ->
                    navController.navigate(MainActivity.Screen.Comparison.createRoute(itemName))
                }
            )
        }

        composable(MainActivity.Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onItemClick = { itemName ->
                    navController.navigate(MainActivity.Screen.Comparison.createRoute(itemName))
                }
            )
        }

        composable(
            route = MainActivity.Screen.Manual.route,
            arguments = listOf(navArgument("name") { 
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            })
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            ManualEntryScreen(
                viewModel = viewModel,
                initialName = name,
                onBack = { navController.popBackStack() },
                onSaveSuccess = { itemName ->
                    navController.navigate(MainActivity.Screen.Comparison.createRoute(itemName)) {
                        popUpTo(MainActivity.Screen.Camera.route)
                    }
                }
            )
        }

        composable(MainActivity.Screen.Barcode.route) {
            BarcodeScannerScreen(
                viewModel = viewModel,
                onProductFound = { name ->
                    navController.navigate(MainActivity.Screen.Manual.createRoute(name)) {
                        popUpTo(MainActivity.Screen.Barcode.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = MainActivity.Screen.Comparison.route,
            arguments = listOf(navArgument("itemName") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemName = backStackEntry.arguments?.getString("itemName") ?: ""
            ComparisonScreen(
                viewModel = viewModel,
                itemName = itemName,
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
