package com.example.fatura.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fatura.R
import com.example.fatura.ui.viewmodel.MainViewModel
import com.example.fatura.util.BluetoothPrinterManager

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(navController: NavHostController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val printerManager = remember { BluetoothPrinterManager(context) }
    var devices by remember { mutableStateOf(emptyList<BluetoothDevice>()) }
    var connectedDeviceAddress by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    var isCurrentlyConnected by remember { mutableStateOf(printerManager.isConnected()) }

    val refreshStatus = {
        isCurrentlyConnected = printerManager.isConnected()
    }

    LaunchedEffect(Unit) {
        devices = printerManager.getPairedDevices()
        connectedDeviceAddress = viewModel.getSetting("printer_address")
        
        // Auto-connect if address is saved
        connectedDeviceAddress?.let { address ->
            if (!printerManager.isConnected()) {
                isConnecting = true
                printerManager.connect(address) { success ->
                    isConnecting = false
                    refreshStatus()
                }
            }
        }
        refreshStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.printer_settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.paired_devices), 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
                
                if (isCurrentlyConnected) {
                    Button(
                        onClick = {
                            printerManager.disconnect()
                            viewModel.saveSetting("printer_address", "")
                            connectedDeviceAddress = null
                            refreshStatus()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Shkëput")
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(devices) { device ->
                    val isFavorite = connectedDeviceAddress == device.address
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = if (isFavorite) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        } else {
                            CardDefaults.cardColors()
                        },
                        border = if (isFavorite) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        onClick = {
                            val previousAddress = connectedDeviceAddress
                            connectedDeviceAddress = device.address
                            viewModel.saveSetting("printer_address", device.address)
                            if (!printerManager.isConnected() || previousAddress != device.address) {
                                isConnecting = true
                                printerManager.connect(device.address) { success ->
                                    isConnecting = false
                                    refreshStatus()
                                }
                            }
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.Bluetooth, 
                                contentDescription = null,
                                tint = if (isFavorite) androidx.compose.ui.graphics.Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name ?: stringResource(R.string.unknown_device), 
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = device.address, style = MaterialTheme.typography.bodySmall)
                            }
                            if (isFavorite) {
                                Icon(
                                    imageVector = if (isCurrentlyConnected) Icons.Default.CheckCircle else Icons.Default.BluetoothConnected, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isCurrentlyConnected) stringResource(R.string.connected) else "Lidhje...", 
                                    color = MaterialTheme.colorScheme.primary, 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
            }

            Button(
                onClick = {
                    val testData = "PROVE PRINTIMI\n--------------\nFatura App\nBluetooth OK\n\n\n".toByteArray()
                    printerManager.print(testData)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                enabled = isCurrentlyConnected
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.test_print))
            }
        }
    }
}
