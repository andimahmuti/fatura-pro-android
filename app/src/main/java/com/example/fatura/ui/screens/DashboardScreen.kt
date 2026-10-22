package com.example.fatura.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fatura.R
import com.example.fatura.ui.Screen
import com.example.fatura.ui.viewmodel.MainViewModel
import com.example.fatura.util.BluetoothPrinterManager
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(navController: NavHostController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val printerManager = remember { BluetoothPrinterManager(context) }
    var isConnected by remember { mutableStateOf(printerManager.isConnected()) }
    
    val lowStockItems by viewModel.lowStockItems.collectAsState(initial = emptyList())
    
    val isCalculatorVisible by viewModel.isFloatingCalculatorVisible.collectAsState()

    // Pulse animation for the status dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        while(true) {
            val savedAddress = viewModel.getSetting("printer_address")
            val connected = printerManager.isConnected()
            isConnected = connected
            if (!connected && !savedAddress.isNullOrBlank()) {
                printerManager.connect(savedAddress) { _ -> }
            }
            kotlinx.coroutines.delay(5.seconds)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.dashboard), fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            // Printer Status Indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) Color.Green.copy(alpha = alpha) else Color.Red)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            
            Text(
                text = "Mirëdita!! Gati për shitjet sot?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Grid of buttons (smaller height)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickButton(
                        text = stringResource(R.string.new_sale),
                        icon = Icons.Default.AddShoppingCart,
                        colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)),
                        onClick = { navController.navigate(Screen.Sales.route) },
                        modifier = Modifier.weight(1f),
                        height = 65.dp
                    )
                    QuickButton(
                        text = stringResource(R.string.items),
                        icon = Icons.Default.Inventory,
                        colors = listOf(Color(0xFF2196F3), Color(0xFF1565C0)),
                        onClick = { navController.navigate(Screen.Items.route) },
                        modifier = Modifier.weight(1f),
                        height = 65.dp
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickButton(
                        text = "Faturat",
                        icon = Icons.Default.Folder,
                        colors = listOf(Color(0xFFFF9800), Color(0xFFEF6C00)),
                        onClick = { navController.navigate(Screen.SavedReceipts.route) },
                        modifier = Modifier.weight(1f),
                        height = 65.dp
                    )
                    QuickButton(
                        text = stringResource(R.string.settings),
                        icon = Icons.Default.Settings,
                        colors = listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2)),
                        onClick = { navController.navigate(Screen.ReceiptSettings.route) },
                        modifier = Modifier.weight(1f),
                        height = 65.dp
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickButton(
                        text = if (isCalculatorVisible) "Mbyll Llog." else "Kalkulatori",
                        icon = Icons.Default.Calculate,
                        colors = if (isCalculatorVisible) listOf(Color.Red, Color(0xFFB71C1C)) else listOf(Color(0xFFD4AF37), Color(0xFF8B0000)),
                        onClick = { viewModel.toggleFloatingCalculator(!isCalculatorVisible) },
                        modifier = Modifier.weight(1f),
                        height = 65.dp
                    )
                    QuickButton(
                        text = "Printim i Avancuar",
                        icon = Icons.Default.Print,
                        colors = listOf(Color(0xFF607D8B), Color(0xFF455A64)),
                        onClick = { navController.navigate(Screen.AdvancedPrint.route) },
                        modifier = Modifier.weight(1f),
                        height = 65.dp
                    )
                }
            }

            // Low Stock Alerts Section (Fixed 3 slots, auto-rotating)
            if (lowStockItems.isNotEmpty()) {
                Text(
                    text = "Njoftime Stoku",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Current page state for "news-like" rotation
                var currentPage by remember { mutableIntStateOf(0) }
                val displayedItems = lowStockItems.take(12) // Limit to top 12 items
                
                // Auto-rotate every 10 seconds if more than 3 items
                if (displayedItems.size > 3) {
                    LaunchedEffect(displayedItems.size) {
                        while (true) {
                            kotlinx.coroutines.delay(10000)
                            currentPage = (currentPage + 3) % displayedItems.size
                        }
                    }
                }

                // Take up to 3 items to show at once
                val currentItemsToShow = if (displayedItems.size <= 3) {
                    displayedItems
                } else {
                    val start = currentPage
                    (0 until 3).map { i -> 
                        displayedItems[(start + i) % displayedItems.size]
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = currentItemsToShow,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(1000)).togetherWith(
                                fadeOut(animationSpec = tween(1000))
                            )
                        },
                        label = "StockAlertsFade"
                    ) { itemsToShow ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            itemsToShow.forEach { item ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it != SwipeToDismissBoxValue.Settled) {
                                            viewModel.dismissAlert(item.id)
                                            true
                                        } else false
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Gray.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                                        )
                                    }
                                ) {
                                    val alertColor = if (item.stock <= 0) Color.Red else Color(0xFF4CAF50) // E kuqe ose Jeshile
                                    val alertText = if (item.stock <= 0) 
                                        "Të ka mbaruar ${item.name}" 
                                    else 
                                        "Po të mbaron ${item.name} ke dhe ${item.stock} stok"

                                    Card(
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        border = BorderStroke(1.dp, alertColor.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (item.stock <= 0) Icons.Default.Warning else Icons.Default.Inventory,
                                                contentDescription = null,
                                                tint = alertColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = alertText,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                color = alertColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            
            // Custom Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Build with love by Andi Mahmuti x CodeBuddy x DeepSeek 💛",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 10.sp,
                        color = Color.Gray
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun QuickButton(
    text: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 80.dp
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(colors))
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = text, 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}
