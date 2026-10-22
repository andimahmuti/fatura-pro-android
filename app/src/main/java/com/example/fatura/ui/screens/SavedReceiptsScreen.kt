package com.example.fatura.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fatura.data.entity.CartItem
import com.example.fatura.data.entity.Transaction
import com.example.fatura.ui.components.ReceiptDetailDialog
import com.example.fatura.ui.viewmodel.MainViewModel
import com.example.fatura.util.BluetoothPrinterManager
import com.example.fatura.util.ReceiptBuilder
import com.example.fatura.util.saveReceiptAsJpgToDownloads
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedReceiptsScreen(navController: NavHostController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transactions by viewModel.allTransactions.collectAsState()
    val template by viewModel.receiptTemplate.collectAsState()
    val printerManager = remember { BluetoothPrinterManager(context) }
    
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var dayToDelete by remember { mutableStateOf<Long?>(null) }
    var showDailySummary by remember { mutableStateOf<Long?>(null) }

    val filteredTransactions = transactions.sortedByDescending { it.date }

    val groupedTransactions = filteredTransactions.groupBy { 
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale("sq", "AL")).format(Date(it.date))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Faturat e Ruajtura") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nuk ka fatura të regjistruara.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    groupedTransactions.forEach { (dateStr, transactionsInDate) ->
                        item {
                            val firstTransaction = transactionsInDate.firstOrNull()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateStr,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row {
                                    IconButton(
                                        onClick = { showDailySummary = firstTransaction?.date }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Assessment,
                                            contentDescription = "Xhiro Ditore",
                                            tint = Color(0xFFD4AF37)
                                        )
                                    }
                                    
                                    Spacer(Modifier.width(80.dp)) // Distancim i madh i kërkuar
                                    
                                    if (firstTransaction != null) {
                                        IconButton(
                                            onClick = { dayToDelete = firstTransaction.date }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Fshi ditën",
                                                tint = Color.Red.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        items(transactionsInDate, key = { it.id }) { transaction ->
                            TransactionListItem(
                                transaction = transaction,
                                onClick = { selectedTransaction = transaction }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }

    if (selectedTransaction != null) {
        ReceiptDetailDialog(
            transaction = selectedTransaction!!,
            viewModel = viewModel,
            onDismiss = { selectedTransaction = null },
            onPrint = {
                val builder = ReceiptBuilder(template)
                val receiptText = builder.buildReceipt(selectedTransaction!!)
                
                scope.launch {
                    val addr = viewModel.getSetting("printer_address")
                    val template = viewModel.receiptTemplate.value
                    printerManager.printWithLogo(receiptText, template.logoPath, addr)
                }
            },
            onSaveToDevice = {
                saveReceiptAsJpgToDownloads(context, selectedTransaction!!)
            },
            onDelete = {
                transactionToDelete = selectedTransaction
                selectedTransaction = null
            },
            onEdit = {
                viewModel.loadTransactionToCart(selectedTransaction!!)
                selectedTransaction = null
                navController.navigate(com.example.fatura.ui.Screen.Sales.route)
            }
        )
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Fshi Faturën") },
            text = { Text("Dëshironi ta fshini këtë faturë përgjithmonë?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transactionToDelete!!)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Fshi")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Anulo")
                }
            }
        )
    }

    if (dayToDelete != null) {
        AlertDialog(
            onDismissRequest = { dayToDelete = null },
            title = { Text("Konfirmo Fshirjen") },
            text = { Text("A jeni të sigurt që dëshironi të fshini të gjitha faturat e kësaj dite?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.performDailyDelete(dayToDelete!!)
                        dayToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Fshij", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { dayToDelete = null }) {
                    Text("Anulo")
                }
            }
        )
    }

    if (showDailySummary != null) {
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale("sq", "AL")).format(Date(showDailySummary!!))
        val dayTransactions = transactions.filter { 
            SimpleDateFormat("ddMMyyyy").format(Date(it.date)) == SimpleDateFormat("ddMMyyyy").format(Date(showDailySummary!!))
        }
        
        val aggregatedItems = mutableMapOf<String, Pair<Double, Double>>() // Emri -> (Sasia, Total)
        dayTransactions.forEach { trans ->
            try {
                val items = Json.decodeFromString<List<CartItem>>(trans.itemsJson)
                items.forEach { item ->
                    val current = aggregatedItems[item.name] ?: (0.0 to 0.0)
                    aggregatedItems[item.name] = (current.first + item.quantity) to (current.second + item.total)
                }
            } catch (_: Exception) {}
        }

        AlertDialog(
            onDismissRequest = { showDailySummary = null },
            title = { Text("Përmbledhja: $dateStr", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Xhiro Totale: Lek ${dayTransactions.sumOf { it.total }.toInt()}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        aggregatedItems.forEach { (name, data) ->
                            item {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${data.first.toInt()}x $name", modifier = Modifier.weight(1f))
                                    Text("Lek ${data.second.toInt()}", fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDailySummary = null }) { Text("MBYLL") } }
        )
    }
}

@Composable
fun TransactionListItem(transaction: Transaction, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                text = timeFormat.format(Date(transaction.date)),
                color = Color.Gray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (!transaction.customerName.isNullOrBlank()) {
                Spacer(Modifier.width(20.dp))
                Text(
                    text = transaction.customerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFFFFB300)
                )
            }
        }
        Text(
            text = "Lek ${transaction.total.toInt()}",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
