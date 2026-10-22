package com.example.fatura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fatura.R
import com.example.fatura.data.entity.CartItem
import com.example.fatura.data.entity.Transaction
import com.example.fatura.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavHostController, viewModel: MainViewModel) {
    val transactions by viewModel.allTransactions.collectAsState()
    var showAggregatedDialog by remember { mutableStateOf<Long?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    
    // Group transactions by date
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("sq", "AL"))
    val groupedTransactions = transactions.groupBy { 
        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }.toList().sortedByDescending { it.first }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            groupedTransactions.forEach { (dateMillis, dayTransactions) ->
                val dayTotal = dayTransactions.sumOf { it.total }
                
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dateFormat.format(Date(dateMillis)).replaceFirstChar { it.uppercase() },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Xhiro: ${dayTotal.toInt()} Lekë",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            }
                            
                            IconButton(onClick = { showAggregatedDialog = dateMillis }) {
                                Icon(Icons.Default.Summarize, contentDescription = "Përmbledhje")
                            }
                        }
                    }
                }

                items(dayTransactions, key = { it.id }) { transaction ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.StartToEnd) {
                                transactionToDelete = transaction
                                false // Do not dismiss automatically, wait for dialog
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromEndToStart = false,
                        backgroundContent = {
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Color.Red
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Fshij", tint = Color.White)
                            }
                        }
                    ) {
                        TransactionItemView(transaction)
                    }
                }
            }
        }
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Konfirmo Fshirjen") },
            text = { Text("A jeni të sigurt që dëshironi të fshini këtë faturë?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transactionToDelete!!)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Po", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Jo")
                }
            }
        )
    }

    if (showAggregatedDialog != null) {
        val aggregatedItems by viewModel.getAggregatedItemsForDate(showAggregatedDialog!!).collectAsState(initial = emptyList())
        
        AlertDialog(
            onDismissRequest = { showAggregatedDialog = null },
            title = { Text("Përmbledhje Artikujsh - ${dateFormat.format(Date(showAggregatedDialog!!))}") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (aggregatedItems.isEmpty()) {
                        Text("Nuk ka artikuj për këtë ditë.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Text("Artikulli", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text("Sasia", modifier = Modifier.width(60.dp), fontWeight = FontWeight.Bold)
                                    Text("Totali", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider()
                            }
                            items(aggregatedItems) { item ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(item.name, modifier = Modifier.weight(1f))
                                    Text(item.quantity.toString(), modifier = Modifier.width(60.dp))
                                    Text("${item.total.toInt()} L", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAggregatedDialog = null }) {
                    Text("Mbyll")
                }
            }
        )
    }
}

@Composable
fun TransactionItemView(transaction: com.example.fatura.data.entity.Transaction) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Fatura #${transaction.receiptNumber}", fontWeight = FontWeight.Bold)
                Text(text = timeFormat.format(Date(transaction.date)), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "${transaction.total.toInt()} L",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
