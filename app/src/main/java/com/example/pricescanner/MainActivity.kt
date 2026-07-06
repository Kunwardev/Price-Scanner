package com.example.pricescanner

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import com.example.pricescanner.ui.shopping.ShoppingListScreen
import com.example.pricescanner.ui.theme.PriceScannerTheme
import com.example.pricescanner.viewmodel.PriceViewModel

class MainActivity : ComponentActivity() {

    sealed class Screen(val route: String) {
        object Camera : Screen("camera_screen")
        object Barcode : Screen("barcode_screen")
        object History : Screen("history_screen")
        object ShoppingList : Screen("shopping_list_screen")
        object Manual : Screen("manual_screen?name={name}") {
            fun createRoute(name: String = "") = "manual_screen?name=$name"
        }
        object Comparison : Screen("comparison_screen/{itemName}") {
            fun createRoute(itemName: String) = "comparison_screen/$itemName"
        }
    }
    
    private lateinit var viewModel: PriceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen before calling super.onCreate()
        installSplashScreen()
        
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
                onNavigateToShoppingList = { navController.navigate(MainActivity.Screen.ShoppingList.route) },
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

        composable(MainActivity.Screen.ShoppingList.route) {
            ShoppingListScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
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
                    // Jump to Manual entry with the name pre-filled
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
            confirmButton = {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = tempInput.isNotBlank(),
                    onClick = { viewModel.setStore(tempInput) }
                ) {
                    Text("Start Scanning")
                }
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Price Scanner",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Track and compare prices across stores effortlessly.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = tempInput,
                        onValueChange = { tempInput = it },
                        label = { Text("Enter Store Name") },
                        placeholder = { Text("e.g. Walmart, Costco") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}
