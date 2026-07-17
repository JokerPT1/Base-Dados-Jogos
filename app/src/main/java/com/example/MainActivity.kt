package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.ResellViewModel
import com.example.ui.ResellViewModelFactory
import com.example.ui.screens.AddEditScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: ResellViewModel = viewModel(
                    factory = ResellViewModelFactory(application)
                )

                NavHost(
                    navController = navController,
                    startDestination = "dashboard"
                ) {
                    // 1. Dashboard Screen (Primary Inventory Hub)
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onAddItem = {
                                navController.navigate("add_edit/0")
                            },
                            onEditItem = { itemId ->
                                navController.navigate("add_edit/$itemId")
                            },
                            onNavigateToAnalytics = {
                                navController.navigate("analytics")
                            },
                            onNavigateToCloudSync = {
                                navController.navigate("cloud_sync")
                            },
                            onNavigateToScanner = {
                                navController.navigate("scanner")
                            }
                        )
                    }

                    // 2. Add / Edit Item Form Screen
                    composable(
                        route = "add_edit/{itemId}?barcode={barcode}",
                        arguments = listOf(
                            navArgument("itemId") { type = NavType.LongType },
                            navArgument("barcode") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
                        val barcode = backStackEntry.arguments?.getString("barcode")
                        AddEditScreen(
                            viewModel = viewModel,
                            itemId = itemId,
                            scannedBarcode = barcode,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // 3. Barcode Scanner View Screen
                    composable("scanner") {
                        ScannerScreen(
                            onBarcodeScanned = { scannedValue ->
                                // Pop scanner and navigate to edit screen with the newly scanned barcode pre-loaded
                                navController.popBackStack()
                                navController.navigate("add_edit/0?barcode=$scannedValue")
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // 4. Monthly analytics and visual profit graphs
                    composable("analytics") {
                        AnalyticsScreen(
                            viewModel = viewModel,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // 5. Cloud backup / JSON backup and restore merge screen
                    composable("cloud_sync") {
                        CloudSyncScreen(
                            viewModel = viewModel,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
