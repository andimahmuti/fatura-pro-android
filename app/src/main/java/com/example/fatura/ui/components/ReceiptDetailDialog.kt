package com.example.fatura.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fatura.data.entity.CartItem
import com.example.fatura.data.entity.Transaction
import com.example.fatura.ui.viewmodel.MainViewModel
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReceiptDetailDialog(
    transaction: Transaction,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onPrint: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onEdit: () -> Unit
) {
    val items = try {
        Json.decodeFromString<List<CartItem>>(transaction.itemsJson)
    } catch (_: Exception) {
        emptyList()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Edit (Kthe në Shportë)") },
                                    onClick = { onEdit(); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ruaj në Downloads (JPG)") },
                                    onClick = { 
                                        onSaveToDevice()
                                        showMenu = false 
                                    },
                                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Printo") },
                                    onClick = { onPrint(); showMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) }
                                )
                                if (onDelete != null) {
                                    DropdownMenuItem(
                                        text = { Text("Fshi Faturën") },
                                        onClick = { onDelete(); showMenu = false },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Spacer(Modifier.height(24.dp))
                            if (!transaction.customerName.isNullOrBlank()) {
                                Text(
                                    text = transaction.customerName,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB300),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            Text(
                                text = "Lek ${transaction.total.toInt()}",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(text = "Totali", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(16.dp))
                        }
                        items(items) { item ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    Text("${item.quantity} x Lek ${item.unitPrice.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                }
                                Text("Lek ${item.total.toInt()}", fontWeight = FontWeight.Bold)
                            }
                        }
                        item {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(32.dp))
                            Text(
                                text = SimpleDateFormat("d/M/yy HH:mm", Locale.getDefault()).format(Date(transaction.date)),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
            
            // Show floating calculator on top of the dialog UI
            FloatingCalculator(viewModel = viewModel)
        }
    }
}
