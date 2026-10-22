package com.example.fatura

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.example.fatura.ui.NavGraph
import com.example.fatura.ui.Screen
import com.example.fatura.ui.components.FloatingCalculator
import com.example.fatura.ui.theme.FaturaTheme
import com.example.fatura.ui.viewmodel.MainViewModel
import com.example.fatura.util.BluetoothPrinterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        permissions.add(Manifest.permission.CAMERA)
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isTablet = resources.getBoolean(R.bool.is_tablet)
        requestedOrientation = if (isTablet) {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        requestPermissions()

        // Modo Full Screen i Plote ne te gjitha ekranet
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())

        // Lidhje dhe ri-lidhje automatike e vazhdueshme ne prapaskene me Printerin Favorit
        lifecycleScope.launch(Dispatchers.IO) {
            val printerManager = BluetoothPrinterManager(applicationContext)
            while (true) {
                val savedAddress = viewModel.getSetting("printer_address")
                if (!savedAddress.isNullOrBlank() && !printerManager.isConnected()) {
                    printerManager.ensureConnection(savedAddress) { _ -> }
                }
                kotlinx.coroutines.delay(3000)
            }
        }

        setContent {
            val isDarkModeSetting by viewModel.isDarkMode.collectAsState()
            val darkTheme = isDarkModeSetting ?: isSystemInDarkTheme()
            FaturaTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavGraph(navController = navController, viewModel = viewModel)
                        if (currentRoute == Screen.Sales.route || currentRoute == Screen.SavedReceipts.route || currentRoute == Screen.Dashboard.route) {
                            FloatingCalculator(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
