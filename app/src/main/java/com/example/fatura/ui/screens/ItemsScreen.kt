package com.example.fatura.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.fatura.R
import com.example.fatura.data.entity.Item
import com.example.fatura.ui.viewmodel.MainViewModel
import com.example.fatura.util.BarcodeScannerDialog
import com.example.fatura.util.FuzzySearchUtil
import com.example.fatura.util.saveImageLocally

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(navController: NavHostController, viewModel: MainViewModel) {
    val items by viewModel.allItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Item?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = items.filter { FuzzySearchUtil.matches(searchQuery, it.name) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.items)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Kërko artikuj...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems) { item ->
                    ItemRow(
                        item = item,
                        onEdit = { editingItem = item },
                        onDelete = { viewModel.deleteItem(item) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { viewModel.addItem(it) }
        )
    }

    if (editingItem != null) {
        ItemDialog(
            item = editingItem,
            onDismiss = { editingItem = null },
            onSave = { viewModel.updateItem(it) }
        )
    }
}

@Composable
fun ItemRow(item: Item, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.padding(16.dp).height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item Image 1x1 with Overlay for Low Stock
            Box {
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
                
                if (item.stock > 0 && item.lowStockThreshold > 0 && item.stock <= item.lowStockThreshold) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFA500))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "!", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.price.toInt()} L",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.quantity > 0) {
                    Text(text = "Sasi: ${item.quantity}", style = MaterialTheme.typography.bodySmall)
                }
                if (item.stock > 0) {
                    Text(text = "Stok: ${item.stock}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ItemDialog(
    item: Item? = null,
    onDismiss: () -> Unit,
    onSave: (Item) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf(item?.name ?: "") }
    var price by remember { mutableStateOf(item?.price?.toInt()?.toString() ?: "") }
    var stock by remember { mutableStateOf(item?.stock?.let { if (it > 0) it.toString() else "" } ?: "") }
    var saleQty by remember { mutableStateOf(item?.quantity?.let { if (it > 0) it.toString() else "" } ?: "") }
    var lowStockThreshold by remember { mutableStateOf(item?.lowStockThreshold?.let { if (it > 0) it.toString() else "" } ?: "") }
    var barcode by remember { mutableStateOf(item?.barcode ?: "") }
    var imageUri by remember { mutableStateOf(item?.imageUri) }
    var showScanner by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val localPath = saveImageLocally(context, it)
            if (localPath != null) {
                imageUri = localPath
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) stringResource(R.string.add_item) else stringResource(R.string.edit_item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Image Picker
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { imageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fshij", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Text("Prek për të ngarkuar foto", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.item_name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text(stringResource(R.string.price)) }, modifier = Modifier.fillMaxWidth())
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stok") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = saleQty, onValueChange = { saleQty = it }, label = { Text("Sasi") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lowStockThreshold, onValueChange = { lowStockThreshold = it }, label = { Text("Pika Kuqe") }, modifier = Modifier.weight(1f))
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barkodi") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Skano")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newItem = Item(
                    id = item?.id ?: 0,
                    name = name,
                    price = price.toDoubleOrNull() ?: 0.0,
                    category = "",
                    taxRate = 0.0,
                    stock = stock.toIntOrNull() ?: 0,
                    quantity = saleQty.toIntOrNull() ?: 1,
                    lowStockThreshold = if (lowStockThreshold == "-") 0 else (lowStockThreshold.toIntOrNull() ?: 0),
                    barcode = if (barcode.isEmpty()) null else barcode,
                    imageUri = imageUri
                )
                onSave(newItem)
                onDismiss()
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showScanner) {
        BarcodeScannerDialog(
            onResult = { barcode = it },
            onDismiss = { showScanner = false }
        )
    }
}
