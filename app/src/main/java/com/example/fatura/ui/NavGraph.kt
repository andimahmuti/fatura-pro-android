package com.example.fatura.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fatura.ui.screens.*
import com.example.fatura.ui.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Sales : Screen("sales")
    object Items : Screen("items")
    object History : Screen("history")
    object Reports : Screen("reports")
    object ReceiptSettings : Screen("receipt_settings")
    object PrinterSettings : Screen("printer_settings")
    object SavedReceipts : Screen("saved_receipts")
    object AdvancedPrint : Screen("advanced_print")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) { DashboardScreen(navController, viewModel) }
        composable(Screen.Sales.route) { SalesScreen(navController, viewModel) }
        composable(Screen.Items.route) { ItemsScreen(navController, viewModel) }
        composable(Screen.History.route) { HistoryScreen(navController, viewModel) }
        composable(Screen.Reports.route) { ReportsScreen(navController, viewModel) }
        composable(Screen.ReceiptSettings.route) { ReceiptSettingsScreen(navController, viewModel) }
        composable(Screen.PrinterSettings.route) { PrinterSettingsScreen(navController, viewModel) }
        composable(Screen.SavedReceipts.route) { SavedReceiptsScreen(navController, viewModel) }
        composable(Screen.AdvancedPrint.route) { AdvancedPrintScreen(navController, viewModel) }
    }
}
