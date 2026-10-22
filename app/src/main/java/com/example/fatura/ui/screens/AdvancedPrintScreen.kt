package com.example.fatura.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fatura.ui.viewmodel.MainViewModel
import com.example.fatura.util.BluetoothPrinterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedPrintScreen(navHostController: NavHostController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val printerManager = remember { BluetoothPrinterManager(context) }
    
    var textToPrint by remember { mutableStateOf("") }
    var isBold by remember { mutableStateOf(false) }
    var textSize by remember { mutableStateOf(1) }
    var isCentered by remember { mutableStateOf(true) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPrinting by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                selectedFileUri = it
                textToPrint = "" 
                scope.launch {
                    previewBitmap = generatePreview(context, it)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Printim i Avancuar") },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Teksti për Printim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (textToPrint.isNotEmpty() || selectedFileUri != null) {
                        Button(
                            onClick = { 
                                textToPrint = ""
                                selectedFileUri = null
                                previewBitmap = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CLEAR", fontSize = 12.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = textToPrint,
                    onValueChange = { 
                        textToPrint = it
                        if (it.isNotEmpty()) {
                            selectedFileUri = null
                            previewBitmap = null
                        }
                    },
                    label = { Text("Shkruani tekstin këtu") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    trailingIcon = {
                        if (textToPrint.isNotEmpty()) {
                            IconButton(onClick = { textToPrint = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Text("KONFIGURIMI I TEKSTIT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = isBold,
                            onClick = { isBold = !isBold },
                            label = { Text("BOLD") },
                            leadingIcon = if (isBold) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = isCentered,
                            onClick = { isCentered = !isCentered },
                            label = { Text("NË MES") },
                            leadingIcon = if (isCentered) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("MADHËSIA (MAX x10):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val sizes = listOf(1, 2, 3, 4, 6, 8, 10)
                        sizes.forEach { size ->
                            FilterChip(
                                selected = textSize == size,
                                onClick = { textSize = size },
                                label = { Text("x$size") }
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Printo Skedar (PDF, Foto)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (selectedFileUri != null) {
                        IconButton(onClick = { selectedFileUri = null; previewBitmap = null }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("image/*", "application/pdf", "text/plain")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedFileUri != null) "NDRYSHO SKEDARIN" else "ZGJIDH SKEDARIN")
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("PARASHIKIMI I FLETËS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (selectedFileUri != null && previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (textToPrint.isNotEmpty()) {
                        val visualSize = when {
                            textSize >= 10 -> 40.sp
                            textSize >= 8 -> 32.sp
                            textSize >= 4 -> 24.sp
                            textSize >= 3 -> 20.sp
                            textSize >= 2 -> 16.sp
                            else -> 13.sp
                        }
                        Text(
                            text = textToPrint,
                            color = Color.Black,
                            fontSize = visualSize, 
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
                            lineHeight = visualSize,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                            Text("Asgjë për të parashikuar", color = Color.LightGray, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            isPrinting = true
                            try {
                                val address = viewModel.getSetting("printer_address")
                                
                                if (address == null) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Lidhni printerin te Cilësimet!", Toast.LENGTH_LONG).show()
                                        isPrinting = false
                                    }
                                    return@launch
                                }

                                val bytes = if (selectedFileUri != null) {
                                    prepareFileBytes(context, selectedFileUri!!)
                                } else {
                                    prepareTextBytes(textToPrint, isBold, textSize, isCentered)
                                }
                                
                                if (bytes.isNotEmpty()) {
                                    printerManager.print(bytes, address) { success ->
                                        scope.launch(Dispatchers.Main) {
                                            isPrinting = false
                                            if (success) {
                                                Toast.makeText(context, "Printimi u dërgua!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Gabim gjatë printimit!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        isPrinting = false
                                        Toast.makeText(context, "Gabim në përgatitjen e të dhënave", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isPrinting = false
                                    Toast.makeText(context, "Gabim i sistemit!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isPrinting && (textToPrint.isNotEmpty() || selectedFileUri != null)
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("PRINTO TANI", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(50.dp))
            }
        }
    }
}

private suspend fun generatePreview(context: android.content.Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType?.contains("pdf") == true) {
            val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            val renderer = PdfRenderer(fd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                return@withContext bitmap
            }
            renderer.close()
        } else {
            val input = context.contentResolver.openInputStream(uri)
            return@withContext BitmapFactory.decodeStream(input)
        }
    } catch (_: Exception) {}
    null
}

private fun prepareTextBytes(text: String, bold: Boolean, size: Int, centered: Boolean): ByteArray {
    val bytes = mutableListOf<Byte>()
    bytes.addAll(byteArrayOf(0x1B, 0x40).toList()) 
    
    if (centered) {
        bytes.addAll(byteArrayOf(0x1B, 0x61, 0x01).toList()) 
    } else {
        bytes.addAll(byteArrayOf(0x1B, 0x61, 0x00).toList()) 
    }

    if (bold) bytes.addAll(byteArrayOf(0x1B, 0x45, 0x01).toList())
    else bytes.addAll(byteArrayOf(0x1B, 0x45, 0x00).toList())
    
    val magnitude = when {
        size >= 10 -> 0x44 
        size >= 8 -> 0x33  
        size >= 6 -> 0x22  
        size >= 4 -> 0x21  
        size >= 3 -> 0x11  
        size >= 2 -> 0x01  
        else -> 0x00
    }
    bytes.addAll(byteArrayOf(0x1D, 0x21, magnitude.toByte()).toList())
    
    bytes.addAll(text.toByteArray(java.nio.charset.Charset.forName("ISO-8859-1")).toList())
    bytes.addAll(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A).toList())
    
    return bytes.toByteArray()
}

private suspend fun prepareFileBytes(context: android.content.Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
    try {
        val bitmap = generatePreview(context, uri) ?: return@withContext byteArrayOf()
        val scaled = scaleBitmap(bitmap, 384)
        return@withContext bitmapToEscPos(scaled)
    } catch (_: Exception) {
        byteArrayOf()
    }
}

private fun scaleBitmap(bitmap: Bitmap, targetWidth: Int): Bitmap {
    val factor = targetWidth.toFloat() / bitmap.width
    val targetHeight = (bitmap.height * factor).toInt()
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

private fun bitmapToEscPos(bitmap: Bitmap): ByteArray {
    val width = bitmap.width
    val height = bitmap.height
    val bytes = mutableListOf<Byte>()
    
    val xL = (width / 8) % 256
    val xH = (width / 8) / 256
    val yL = height % 256
    val yH = height / 256
    
    bytes.addAll(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL.toByte(), xH.toByte(), yL.toByte(), yH.toByte()).toList())
    
    val threshold = 128
    for (y in 0 until height) {
        for (x in 0 until width / 8) {
            var b = 0
            for (bit in 0 until 8) {
                if (x * 8 + bit < width) {
                    val px = bitmap.getPixel(x * 8 + bit, y)
                    val r = (px shr 16) and 0xFF
                    val g = (px shr 8) and 0xFF
                    val bl = px and 0xFF
                    val gray = (r + g + bl) / 3
                    if (gray < threshold) b = b or (1 shl (7 - bit))
                }
            }
            bytes.add(b.toByte())
        }
    }
    bytes.addAll(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A).toList())
    return bytes.toByteArray()
}
