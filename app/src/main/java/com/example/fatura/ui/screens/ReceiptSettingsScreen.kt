package com.example.fatura.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.fatura.R
import com.example.fatura.data.entity.ReceiptTemplate
import com.example.fatura.ui.viewmodel.MainViewModel
import com.example.fatura.util.saveImageLocally
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReceiptSettingsScreen(navController: NavHostController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentTemplate by viewModel.receiptTemplate.collectAsState()
    var template by remember(currentTemplate) { mutableStateOf(currentTemplate) }
    
    val isDarkModeSetting by viewModel.isDarkMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val currentThemeIsDark = isDarkModeSetting ?: isSystemDark

    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val localPath = saveImageLocally(context, it)
            if (localPath != null) {
                template = template.copy(logoPath = localPath)
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            uri?.let { saveItemsToFile(context, it, viewModel.getItemsJson()) }
        }
    )

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { readItemsFromFile(context, it) { json -> viewModel.importItemsFromJson(json) } }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.receipt_settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { 
                            viewModel.saveReceiptTemplate(template)
                            Toast.makeText(context, "Konfigurimet u ruajtën!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack() // Kthehu te Paneli
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("RUAJ", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionHeader("Tema e Aplikacionit") }
            item {
                SettingsSwitch(label = "Dark Mode", checked = currentThemeIsDark) { isDark ->
                    viewModel.saveSetting("dark_mode", isDark.toString())
                }
            }
            
            item {
                Text("Stili i Artikujve te Shitjet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = template.salesItemStyle == 0, onClick = { template = template.copy(salesItemStyle = 0) })
                    Text("Kuti (Grid)")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = template.salesItemStyle == 1, onClick = { template = template.copy(salesItemStyle = 1) })
                    Text("Listë (1.1)")
                }
            }

            item { SectionHeader("Logo e Biznesit") }
            item {
                SettingsSwitch("Shfaq Logon", template.showLogo) { template = template.copy(showLogo = it) }
            }
            if (template.showLogo) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .clickable { logoLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (template.logoPath != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = template.logoPath,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                    )
                                    IconButton(
                                        onClick = { template = template.copy(logoPath = null) },
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
                                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            }
                        }
                        Text("Prek për të ngarkuar logon", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            item { SectionHeader("Rezervimi i Artikujve") }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { 
                        val fileName = "Artikujt_Backup_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.txt"
                        createDocumentLauncher.launch(fileName) 
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Shkarko")
                    }
                    Button(onClick = { openDocumentLauncher.launch(arrayOf("text/plain")) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ngarko")
                    }
                }
            }

            item { SectionHeader("Printeri") }
            item {
                OutlinedButton(onClick = { navController.navigate("printer_settings") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.printer_settings))
                }
            }

            item {
                Text("Madhësia e Letrës", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = template.paperSize == "58mm", onClick = { template = template.copy(paperSize = "58mm") })
                    Text("58mm")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = template.paperSize == "80mm", onClick = { template = template.copy(paperSize = "80mm") })
                    Text("80mm")
                }
            }

            item { SectionHeader("Stili i Faturës") }
            item {
                FlowRow(modifier = Modifier.fillMaxWidth(), maxItemsInEachRow = 3, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val styles = listOf("Classic", "Minimal", "Yjet", "Kuti", "Pikat", "Luxury", "Tech", "Elegante", "Retro", "Kompakt+")
                    styles.forEach { style ->
                        FilterChip(selected = template.style == style, onClick = { template = template.copy(style = style) }, label = { Text(style) })
                    }
                }
            }

            item { SectionHeader("Trashësia e Shkrimit (Në Letër)") }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val fonts = listOf("Normal", "Bold")
                    fonts.forEach { font ->
                        FilterChip(selected = template.fontType == font, onClick = { template = template.copy(fontType = font) }, label = { Text(if(font == "Bold") "I Trashë" else "Normal") })
                    }
                }
            }

            item { SectionHeader("Kreu i Faturës") }
            item { SettingsSwitch("Emri i biznesit", template.showBusinessName) { template = template.copy(showBusinessName = it) } }
            if (template.showBusinessName) {
                item { OutlinedTextField(value = template.businessName, onValueChange = { template = template.copy(businessName = it) }, label = { Text("Emri i Biznesit") }, modifier = Modifier.fillMaxWidth()) }
            }
            item { SettingsSwitch("Tekst personalizuar (Kreu)", template.showCustomHeader1) { template = template.copy(showCustomHeader1 = it) } }
            if (template.showCustomHeader1) {
                item { OutlinedTextField(value = template.customHeader1, onValueChange = { template = template.copy(customHeader1 = it) }, label = { Text("Teksti i Kreut") }, modifier = Modifier.fillMaxWidth()) }
            }
            item { SettingsSwitch("Data", template.showDate) { template = template.copy(showDate = it) } }
            item { SettingsSwitch("Ora", template.showTime) { template = template.copy(showTime = it) } }
            item { SettingsSwitch("Vija sipër", template.showDividerTop) { template = template.copy(showDividerTop = it) } }

            item { SectionHeader("Seksioni i Artikujve") }
            item { SettingsSwitch("Emri i artikullit", template.showItemName) { template = template.copy(showItemName = it) } }
            item { SettingsSwitch("Sasia", template.showQuantity) { template = template.copy(showQuantity = it) } }
            item { SettingsSwitch("Çmimi", template.showUnitPrice) { template = template.copy(showUnitPrice = it) } }
            item { SettingsSwitch("Vija para totaleve", template.showDividerBeforeTotals) { template = template.copy(showDividerBeforeTotals = it) } }

            item { SectionHeader("Përmbledhja") }
            item { SettingsSwitch("Totali", template.showTotal) { template = template.copy(showTotal = it) } }
            item { SettingsSwitch("Vija pas totaleve", template.showDividerAfterTotals) { template = template.copy(showDividerAfterTotals = it) } }

            item { SectionHeader("Fundi i Faturës") }
            item { SettingsSwitch("Tekst personalizuar", template.showCustomFooter1) { template = template.copy(showCustomFooter1 = it) } }
            if (template.showCustomFooter1) {
                item { OutlinedTextField(value = template.customFooter1, onValueChange = { template = template.copy(customFooter1 = it) }, label = { Text("Teksti i Fundit") }, modifier = Modifier.fillMaxWidth()) }
            }
            item { SettingsSwitch("Vija poshtë", template.showDividerBottom) { template = template.copy(showDividerBottom = it) } }
            
            item {
                Button(onClick = { template = ReceiptTemplate() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Rivendos në Parazgjedhje")
                }
            }

            item { SectionHeader("Parashikimi i Stilit") }
            item { ReceiptPreview(template) }
        }
    }
}

@Composable
fun ReceiptPreview(template: ReceiptTemplate) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = Color.White, shape = RoundedCornerShape(4.dp)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val monoStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = Color.Black, fontSize = 11.sp,
                fontWeight = if (template.fontType == "Bold") FontWeight.Bold else FontWeight.Normal
            )
            val bigMono = monoStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val maxWidth = 32

            if (template.showBusinessName) {
                Text(template.businessName.uppercase(), style = bigMono, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            if (template.showCustomHeader1 && template.customHeader1.isNotEmpty()) {
                Text(template.customHeader1, style = monoStyle, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            if (template.showDividerTop) { Text("-".repeat(maxWidth), style = monoStyle) }
            
            val dateStr = if (template.showDate) "20 Korrik 2026" else ""
            val timeStr = if (template.showTime) "Ora 16:45" else ""
            val combined = "$dateStr  $timeStr".trim()
            if (combined.isNotEmpty()) {
                Text(combined, style = monoStyle, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            if (template.showDividerBeforeTotals) { Text("-".repeat(maxWidth), style = monoStyle) }

            when (template.style) {
                "Minimal" -> PreviewMinimal(template, monoStyle, maxWidth)
                "Yjet" -> PreviewYjet(template, monoStyle, maxWidth)
                "Kuti" -> PreviewKuti(template, monoStyle, maxWidth)
                "Pikat" -> PreviewPikat(template, monoStyle, maxWidth)
                "Luxury" -> PreviewLuxury(template, monoStyle, maxWidth)
                "Tech" -> PreviewTech(template, monoStyle, maxWidth)
                "Elegante" -> PreviewElegante(template, monoStyle, maxWidth)
                "Retro" -> PreviewRetro(template, monoStyle, maxWidth)
                "Kompakt+" -> PreviewKompaktPlus(template, monoStyle, maxWidth)
                else -> PreviewClassic(template, monoStyle, maxWidth)
            }
            
            if (template.showDividerAfterTotals) { Text("-".repeat(maxWidth), style = monoStyle) }
            if (template.showTotal) {
                val isClassic = template.style == "Klasik" || template.style == "Classic" || template.style == "Klasike" || template.style.isBlank()
                val totalStr = if (isClassic) "TOTAL: 1300 Lek" else "TOTAL: 1300"
                Text(totalStr, style = bigMono, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            if (template.showDividerBottom) { Text("-".repeat(maxWidth), style = monoStyle) }
            if (template.showCustomFooter1 && template.customFooter1.isNotEmpty()) {
                Text(template.customFooter1, style = monoStyle, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun PreviewClassic(t: ReceiptTemplate, s: androidx.compose.ui.text.TextStyle, w: Int) {
    listOf("ARTIKULLI 1", "ARTIKULLI 2", "ARTIKULLI 3").forEachIndexed { i, name ->
        if (t.showItemName) Text(name, style = s, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        val qty = if (i == 0) 10 else if (i == 1) 5 else 2
        val up = if (i == 0) 100 else if (i == 1) 50 else 25
        val qtyInfo = if (t.showQuantity && t.showUnitPrice) "   $qty x $up Lek" else if (t.showQuantity) "   $qty x" else ""
        val price = "${qty * up} Lek"
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
            if (qtyInfo.isNotEmpty()) Text(qtyInfo, style = s, modifier = Modifier.align(Alignment.CenterStart))
            Text(price, style = s, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
fun PreviewMinimal(t: ReceiptTemplate, s: androidx.compose.ui.text.TextStyle, w: Int) {
    listOf("ARTIKULLI", "ARTIKULLI", "ARTIKULLI").forEachIndexed { i, name ->
        val q = if(i==0)10 else if(i==1)5 else 2
        val up = if(i==0)100 else if(i==1)50 else 25
        val qty = if (t.showQuantity) "$q ".padEnd(3) else ""
        val calc = if (t.showUnitPrice) "x$up -- ${q*up}" else "${q*up}"
        val itemName = if (t.showItemName) name.padEnd(10) else ""
        Text("$qty$itemName $calc", style = s, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun PreviewYjet(t: ReceiptTemplate, s: androidx.compose.ui.text.TextStyle, w: Int) {
    Text("*".repeat(w), style = s)
    listOf("ARTIKULLI", "ARTIKULLI", "ARTIKULLI").forEachIndexed { i, name ->
        val q = if(i==0)10 else if(i==1)5 else 2
        val up = if(i==0)100 else if(i==1)50 else 25
        if (t.showItemName) Text(name, style = s, modifier = Modifier.fillMaxWidth())
        val info = if (t.showQuantity && t.showUnitPrice) "  $q x $up" else ""
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(info, style = s, modifier = Modifier.align(Alignment.CenterStart))
            Text("${q*up}", style = s, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
    Text("*".repeat(w), style = s)
}

@Composable
fun PreviewKuti(t: ReceiptTemplate, s: androidx.compose.ui.text.TextStyle, w: Int) {
    Text("| ARTIKULLI       | SHUMA |", style = s)
    Text("-".repeat(w), style = s)
    listOf("ARTIKULLI", "ARTIKULLI", "ARTIKULLI").forEachIndexed { i, name ->
        val q = if(i==0)10 else if(i==1)5 else 2
        val up = if(i==0)100 else if(i==1)50 else 25
        if (t.showItemName) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text("| $name", style = s, modifier = Modifier.align(Alignment.CenterStart))
                Text("| ${q*up} |", style = s, modifier = Modifier.align(Alignment.CenterEnd))
            }
        }
        val info = if (t.showQuantity && t.showUnitPrice) "  $q x $up" else ""
        if (info.isNotEmpty()) Text("| $info".padEnd(w-1) + "|", style = s)
    }
}

@Composable
fun PreviewPikat(t: ReceiptTemplate, s: androidx.compose.ui.text.TextStyle, w: Int) {
    listOf("ARTIKULLI", "ARTIKULLI", "ARTIKULLI").forEachIndexed { i, name ->
        val q = if(i==0)10 else if(i==1)5 else 2
        val up = if(i==0)100 else if(i==1)50 else 25
        if (t.showItemName) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(name, style = s, modifier = Modifier.align(Alignment.CenterStart))
                Text(".......${q*up}", style = s, modifier = Modifier.align(Alignment.CenterEnd))
            }
        }
        val info = if (t.showQuantity && t.showUnitPrice) "( $q x $up )" else ""
        if (info.isNotEmpty()) Text("  $info", style = s)
    }
}

@Composable
fun PreviewLuxury(t: ReceiptTemplate, s: androidx.compose.ui.text.TextStyle, w: Int) {
    listOf("ARTIKULLI", "ARTIKULLI", "ARTIKULLI").forEachIndexed { i, name ->
        val q = if(i==0)10 else if(i==1)5 else 2
        val up = if(i==0)100 else if(i==1)50 else 25
        if (t.showItemName) Text("  $name", style = s)
        val info = if (t.showQuantity && t.showUnitPrice) "  $q x $up" else ""
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(info, style = s, modifier = Modifier.align(Alignment.CenterStart))
            Text("${q*up}  ", style = s, modifier = Modifier.align(Alignment.CenterEnd))
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun PreviewTech(t: ReceiptTemplate, s: androidx.compose.ui.text.TextStyle, w: Int) {
    listOf("ARTIKULLI", "ARTIKULLI", "ARTIKULLI").forEachIndexed { i, name ->
        val q = if(i==0)10 else if(i==1)5 else 2
        val up = if(i==0)100 else if(i==1)50 else 25
        val qty = if (t.showQuantity) "($q) " else ""
        val unit = if (t.showUnitPrice) "x$up" else ""
        val itemName = if (t.showItemName) name else ""
        Text("$qty$itemName...$unit = ${q*up}", style = s)
    }
}

@Composable
fun PreviewElegante(t: ReceiptTemplate, s: androidx.compose.ui.text.TextStyle, w: Int) {
    Text("~".repeat(w), style = s)
    listOf("ARTIKULLI", "ARTIKULLI", "ARTIKULLI").forEachIndexed { i, name ->
        val q = if(i==0)10 else if(i==1)5 else 2
        val up = if(i==0)100 else if(i==1)50 else 25
        if (t.showItemName) Text(name, style = s)
        val detail = if (t.showQuantity && t.showUnitPrice) "  > $q X $up =" else ""
        Box(modifier = Modifier.fillMaxWidth()) {
            if (detail.isNotEmpty()) Text(detail, style = s, modifier = Modifier.align(Alignment.CenterStart))
            Text("${q*up}", style = s, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
    Text("~".repeat(w), style = s)
}

@Composable
fun PreviewRetro(t: ReceiptTemplate, s: androidx.compose.ui.text.TextStyle, w: Int) {
    Text("-".repeat(w), style = s)
    listOf("ARTIKULLI", "ARTIKULLI", "ARTIKULLI").forEachIndexed { i, name ->
        val q = if(i==0)10 else if(i==1)5 else 2
        val up = if(i==0)100 else if(i==1)50 else 25
        val qty = if (t.showQuantity) "[$q] " else ""
        val unit = if (t.showUnitPrice) "     (X $up)" else ""
        val itemName = if (t.showItemName) name else ""
        Box(modifier = Modifier.fillMaxWidth()) {
            Text("$qty$itemName", style = s, modifier = Modifier.align(Alignment.CenterStart))
            Text("${q*up}", style = s, modifier = Modifier.align(Alignment.CenterEnd))
        }
        if (unit.isNotEmpty()) Text(unit, style = s)
    }
    Text("-".repeat(w), style = s)
}

@Composable
fun PreviewKompaktPlus(t: ReceiptTemplate, s: androidx.compose.ui.text.TextStyle, w: Int) {
    listOf("ARTIKULLI", "ARTIKULLI", "ARTIKULLI").forEachIndexed { i, name ->
        val q = if(i==0)10 else if(i==1)5 else 2
        val up = if(i==0)100 else if(i==1)50 else 25
        val qty = if (t.showQuantity) "${q}x" else ""
        val unit = if (t.showUnitPrice) "x${up}=" else "="
        val itemName = if (t.showItemName) name else ""
        Text("$qty$itemName$unit${q*up}", style = s)
    }
}

fun saveItemsToFile(context: Context, uri: Uri, content: String) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
        Toast.makeText(context, "Artikujt u shkarkuan me sukses", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Gabim gjatë shkarkimit", Toast.LENGTH_SHORT).show()
    }
}

fun readItemsFromFile(context: Context, uri: Uri, onContentRead: (String) -> Unit) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader -> onContentRead(reader.readText()) }
        }
        Toast.makeText(context, "Artikujt u ngarkuan me sukses", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Gabim gjatë ngarkimit", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
    HorizontalDivider()
}

@Composable
fun SettingsSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
