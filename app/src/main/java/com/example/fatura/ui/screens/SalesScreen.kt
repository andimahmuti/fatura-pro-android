package com.example.fatura.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.fatura.R
import com.example.fatura.data.entity.CartItem
import com.example.fatura.data.entity.Item
import com.example.fatura.data.entity.Transaction
import com.example.fatura.ui.viewmodel.MainViewModel
import com.example.fatura.util.BarcodeScannerDialog
import com.example.fatura.util.BluetoothPrinterManager
import com.example.fatura.util.FuzzySearchUtil
import com.example.fatura.util.ReceiptBuilder
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.os.Build
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(navController: NavHostController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val items by viewModel.allItems.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val total by viewModel.cartTotal.collectAsState(0.0)
    val receiptTemplate by viewModel.receiptTemplate.collectAsState()
    
    val printerManager = remember { BluetoothPrinterManager(context) }
    
    var showCheckout by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var lastTransaction by remember { mutableStateOf<Transaction?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var customerName by remember { mutableStateOf("") }

    val isX86 = remember { Build.SUPPORTED_ABIS.any { it.contains("x86") } }

    var cartButtonPosition by remember { mutableStateOf(Offset.Zero) }
    val flyingItems = remember { mutableStateListOf<Pair<Offset, Color>>() }

    val itemColors = remember(items) {
        val colors = listOf(
            Color(0xFFD4AF37), 
            Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
            Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF00BCD4),
            Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A),
            Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFF795548),
        )
        items.associateBy({ it.id }, { colors[it.id % colors.size] })
    }

    val filteredItems = items.filter { FuzzySearchUtil.matches(searchQuery, it.name) }

    LaunchedEffect(Unit) {
        val address = viewModel.getSetting("printer_address")
        address?.let { 
            if (!printerManager.isConnected()) {
                printerManager.connect(it) {} 
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sales)) },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.navigate(com.example.fatura.ui.Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!isX86) {
                        BadgedBox(
                            badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } },
                            modifier = Modifier.padding(end = 4.dp).onGloballyPositioned { cartButtonPosition = it.positionInRoot() }
                        ) {
                            IconButton(onClick = { showCheckout = true }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(if (isX86) 0.6f else 1f).fillMaxHeight()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        placeholder = { Text("Kërko artikuj...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Fshi", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                IconButton(onClick = { showScanner = true }) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Skano")
                                }
                            }
                        }
                    )

                    if (receiptTemplate.salesItemStyle == 0) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(120.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredItems) { item ->
                                var itemPosition by remember { mutableStateOf(Offset.Zero) }
                                ItemButton(
                                    item = item,
                                    color = itemColors[item.id] ?: MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.onGloballyPositioned {
                                        itemPosition = it.positionInRoot()
                                    }
                                ) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    if (!isX86) {
                                        val color = itemColors[item.id] ?: Color.Gray
                                        flyingItems.add(itemPosition to color)
                                    }
                                    viewModel.addToCart(item)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredItems) { item ->
                                var itemPosition by remember { mutableStateOf(Offset.Zero) }
                                ItemRowSimple(
                                    item = item,
                                    color = itemColors[item.id] ?: MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.onGloballyPositioned {
                                        itemPosition = it.positionInRoot()
                                    },
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        if (!isX86) {
                                            val color = itemColors[item.id] ?: Color.Gray
                                            flyingItems.add(itemPosition to color)
                                        }
                                        viewModel.addToCart(item)
                                    }
                                )
                            }
                        }
                    }
                }

                if (isX86) {
                    Surface(
                        modifier = Modifier.weight(0.4f).fillMaxHeight(),
                        tonalElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        CheckoutContent(
                            cartItems = cartItems,
                            total = total,
                            onUpdateQuantity = { item, qty -> viewModel.updateQuantity(item, qty) },
                            onUpdatePrice = { item, price -> viewModel.updateUnitPrice(item, price) },
                            onRemove = { cartItem -> 
                                viewModel.removeFromCart(cartItem) 
                            },
                            onClear = { viewModel.clearCart() },
                            onAddNewProduct = { showAddItemDialog = true },
                            onComplete = { method, paid ->
                                val transaction = Transaction(
                                    receiptNumber = (System.currentTimeMillis() % 1000000).toString(),
                                    itemsJson = Json.encodeToString(cartItems),
                                    subtotal = total,
                                    tax = 0.0,
                                    discount = 0.0,
                                    total = total,
                                    paymentMethod = method,
                                    amountPaid = paid,
                                    change = if (paid > total) paid - total else 0.0,
                                    customerName = null
                                )
                                viewModel.saveTransaction(transaction)
                                viewModel.clearCart()
                                lastTransaction = transaction
                                scope.launch {
                                    showSuccessAnimation = true
                                    delay(1500.milliseconds)
                                    showSuccessAnimation = false
                                }
                            }
                        )
                    }
                }
            }

            flyingItems.forEach { (startPos, color) ->
                var visible by remember { mutableStateOf(true) }
                val animProgress = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    animProgress.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
                    visible = false
                    flyingItems.removeIf { it.first == startPos && it.second == color }
                }
                if (visible) {
                    val x = startPos.x + (cartButtonPosition.x - startPos.x) * animProgress.value
                    val y = startPos.y + (cartButtonPosition.y - startPos.y) * animProgress.value
                    val s = 1f - (0.5f * animProgress.value)
                    Box(modifier = Modifier.offset { IntOffset(x.toInt(), y.toInt()) }.size(30.dp).scale(s).clip(CircleShape).background(color))
                }
            }

            AnimatedVisibility(visible = showSuccessAnimation, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Card(shape = CircleShape, colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.size(120.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(80.dp))
                        }
                    }
                }
            }
        }
    }

    if (showCheckout && !isX86) {
        Dialog(
            onDismissRequest = { showCheckout = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Customer Name input centered in the TopAppBar title area
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(0.9f).height(38.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (customerName.isEmpty()) {
                                                    Text(
                                                        text = "Emri Klientit",
                                                        color = Color.Gray,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.align(Alignment.CenterStart)
                                                    )
                                                }
                                                BasicTextField(
                                                    value = customerName,
                                                    onValueChange = { 
                                                        if (it.length <= 25) {
                                                            customerName = it.uppercase()
                                                        }
                                                    },
                                                    singleLine = true,
                                                    textStyle = TextStyle(
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        lineHeight = 14.sp
                                                    ),
                                                    cursorBrush = SolidColor(Color(0xFFFFB300)),
                                                    modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart)
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { 
                                    showCheckout = false
                                    navController.navigate(com.example.fatura.ui.Screen.Dashboard.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kthehu te paneli")
                                }
                            },
                            actions = {
                                // Shopping Cart Icon at the far right with badge count
                                BadgedBox(
                                    badge = { if (cartItems.isNotEmpty()) Badge { Text(cartItems.size.toString()) } },
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    IconButton(onClick = { showCheckout = false }) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = "Kthehu te shitjet",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { cartPadding ->
                    Box(modifier = Modifier.padding(cartPadding).fillMaxSize()) {
                        CheckoutContent(
                            cartItems = cartItems,
                            total = total,
                            onUpdateQuantity = { item, qty -> viewModel.updateQuantity(item, qty) },
                            onUpdatePrice = { item, price -> viewModel.updateUnitPrice(item, price) },
                            onRemove = { viewModel.removeFromCart(it) },
                            onClear = { viewModel.clearCart() },
                            onAddNewProduct = { showAddItemDialog = true },
                            onComplete = { method, paid ->
                                val transaction = Transaction(
                                    receiptNumber = (System.currentTimeMillis() % 1000000).toString(),
                                    itemsJson = Json.encodeToString(cartItems),
                                    subtotal = total,
                                    tax = 0.0,
                                    discount = 0.0,
                                    total = total,
                                    paymentMethod = method,
                                    amountPaid = paid,
                                    change = if (paid > total) paid - total else 0.0,
                                    customerName = customerName.ifBlank { null }
                                )
                                viewModel.saveTransaction(transaction)
                                viewModel.clearCart()
                                customerName = "" // Clear customer name only after successful payment ("Para në dorë")
                                showCheckout = false
                                lastTransaction = transaction
                                scope.launch {
                                    showSuccessAnimation = true
                                    delay(1500.milliseconds)
                                    showSuccessAnimation = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (lastTransaction != null) {
        val navigateToDashboard = {
            scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                lastTransaction = null
                navController.navigate(com.example.fatura.ui.Screen.Dashboard.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        SuccessDialog(
            transaction = lastTransaction!!,
            template = receiptTemplate,
            onDismiss = { 
                navigateToDashboard()
            },
            onPrint = {
                val builder = ReceiptBuilder(receiptTemplate)
                val receiptText = builder.buildReceipt(lastTransaction!!)
                try {
                    val txtFolder = File(context.filesDir, "receipts")
                    if (!txtFolder.exists()) txtFolder.mkdirs()
                    val txtFileName = "Fatura_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(lastTransaction!!.date))}.txt"
                    FileOutputStream(File(txtFolder, txtFileName)).use { it.write(receiptText.toByteArray()) }
                } catch (_: Exception) {}

                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val addr = viewModel.getSetting("printer_address")
                    printerManager.printWithLogo(receiptText, receiptTemplate.logoPath, addr) { _ ->
                        navigateToDashboard()
                    }
                }
            }
        )
    }

    if (showAddItemDialog) {
        ItemDialog(onDismiss = { showAddItemDialog = false }, onSave = { viewModel.addItem(it) })
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onResult = { scannedCode ->
                val item = items.find { it.barcode == scannedCode }
                if (item != null) {
                    viewModel.addToCart(item)
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } else {
                    Toast.makeText(context, "Produkti nuk u gjet!", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showScanner = false }
        )
    }
}

@Composable
fun SuccessDialog(transaction: Transaction, template: com.example.fatura.data.entity.ReceiptTemplate, onDismiss: () -> Unit, onPrint: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(100.dp))
                Spacer(Modifier.height(16.dp))
                Text("Shitja u krye me sukses!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Totali: Lek ${transaction.total.toInt()}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(32.dp))
                Button(onClick = onPrint, modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Printo Faturën", fontSize = 18.sp)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.medium) {
                    Text("KTHEHU TE PANELI", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun ItemButton(item: Item, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "tilt")
    val tiltAngle by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "tilt"
    )

    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .graphicsLayer {
                rotationX = tiltAngle
                rotationY = tiltAngle
            },
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imageUri ?: R.drawable.ic_launcher_foreground,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = item.name, 
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(3f, 3f),
                            blurRadius = 10f
                        )
                    ), 
                    fontWeight = FontWeight.ExtraBold, 
                    maxLines = 2, 
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                
                Spacer(Modifier.height(8.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant, 
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "${item.price.toInt()} L", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Black, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemRowSimple(item: Item, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().height(80.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUri ?: R.drawable.ic_launcher_foreground,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name, 
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(text = "Sasia: ${item.quantity} | Stoku: ${item.stock}", style = MaterialTheme.typography.bodySmall)
            }
            
            Text(
                text = "${item.price.toInt()} L",
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun CheckoutContent(cartItems: List<CartItem>, total: Double, onUpdateQuantity: (CartItem, Int) -> Unit, onUpdatePrice: (CartItem, Double) -> Unit, onRemove: (CartItem) -> Unit, onClear: () -> Unit, onAddNewProduct: () -> Unit, onComplete: (String, Double) -> Unit) {
    val isCartEmpty = cartItems.isEmpty()
    val hasInvalidQtyOrPrice = cartItems.any { it.quantity <= 0 || it.unitPrice <= 0 }
    val isTotalInvalid = total <= 0
    val isCartValid = !isCartEmpty && !hasInvalidQtyOrPrice && !isTotalInvalid

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (isCartEmpty) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.RemoveShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Shporta është bosh!",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cartItems) { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = item.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onRemove(item) }) { 
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) 
                        }
                        var qtyText by remember(item.quantity) { 
                            mutableStateOf(if (item.quantity > 0) item.quantity.toString() else "") 
                        }
                        var priceText by remember(item.unitPrice) { 
                            mutableStateOf(if (item.unitPrice > 0) item.unitPrice.toInt().toString() else "") 
                        }
                        
                        OutlinedTextField(
                            value = qtyText, 
                            onValueChange = { newValue -> 
                                qtyText = newValue
                                val newQty = newValue.toIntOrNull() ?: 0
                                onUpdateQuantity(item, newQty)
                            }, 
                            modifier = Modifier.width(85.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                            singleLine = true, 
                            isError = (item.quantity <= 0),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp)
                        )
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(
                            value = priceText, 
                            onValueChange = { newValue -> 
                                priceText = newValue
                                val newPrice = newValue.toDoubleOrNull() ?: 0.0
                                onUpdatePrice(item, newPrice)
                            }, 
                            modifier = Modifier.width(85.dp), 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                            singleLine = true, 
                            isError = (item.unitPrice <= 0),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp)
                        )
                    }
                }
            }
        }

        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(text = stringResource(R.string.total), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = "${total.toInt()} L", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
        }

        if (!isCartValid && !isCartEmpty) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Vendosni sasinë për të kryer shitjen",
                color = Color(0xFFFF6B6B),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { 
                    if (isCartValid) {
                        onComplete("Para në dorë", total) 
                    }
                }, 
                enabled = isCartValid,
                modifier = Modifier.weight(1f).height(56.dp), 
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE5C158),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF2C2C2E),
                    disabledContentColor = Color(0xFF7F7F7F)
                )
            ) {
                Text("Para në dorë", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
