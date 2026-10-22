package com.example.fatura.util

import com.example.fatura.data.entity.CartItem
import com.example.fatura.data.entity.ReceiptTemplate
import com.example.fatura.data.entity.Transaction
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class ReceiptBuilder(private val template: ReceiptTemplate) {
    private val maxWidth = if (template.paperSize == "80mm") 42 else 32
    private val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("sq", "AL"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("sq", "AL"))

    // ESC/POS Direct Commands
    private val fontA = "\u001b\u004d\u0000"
    private val boldOn = "\u001b\u0045\u0001"
    private val boldOff = "\u001b\u0045\u0000"
    private val doubleSizeOn = "\u001d\u0021\u0011"
    private val doubleSizeOff = "\u001d\u0021\u0000"
    private val alignCenter = "\u001b\u0061\u0001"
    private val alignLeft = "\u001b\u0061\u0000"
    private val alignRight = "\u001b\u0061\u0002"

    fun buildReceipt(transaction: Transaction): String {
        val sb = StringBuilder()
        sb.append(fontA)
        if (template.fontType == "Bold") sb.append(boldOn)

        // Logo printing would ideally be a separate method sending raw bytes, 
        // as ESC/POS images are not strings. 
        // For now, we add a placeholder marker that the PrinterManager can recognize.
        if (template.showLogo && template.logoPath != null) {
            sb.append("[LOGO]\n") 
        }

        // 1. Emri i Biznesit - Qendërzim Hardware
        if (template.showBusinessName) {
            sb.append(alignCenter)
                .append(boldOn)
                .append(doubleSizeOn)
                .append(template.businessName.uppercase())
                .append(doubleSizeOff)
                .append(boldOff)
                .append("\n")
        }

        // 2. Kreu i përbashkët (Data, Ora, Vizat)
        addCommonHeader(sb, transaction)

        // 3. Artikujt
        sb.append(alignLeft)
        val items = getItems(transaction)
        when (template.style) {
            "Minimal" -> buildMinimalContent(sb, items)
            "Yjet" -> buildYjetContent(sb, items)
            "Kuti" -> buildKutiContent(sb, items)
            "Pikat" -> buildPikatContent(sb, items)
            "Luxury" -> buildLuxuryContent(sb, items)
            "Tech" -> buildTechContent(sb, items)
            "Elegante" -> buildEleganteContent(sb, items)
            "Retro" -> buildRetroContent(sb, items)
            "Kompakt+" -> buildKompaktPlusContent(sb, items)
            else -> buildClassicContent(sb, items)
        }

        // 4. Fundi i Faturës (Vizat, TOTALI, Footer)
        addCommonFooter(sb, transaction)
        
        return sb.toString()
    }

    private fun addCommonHeader(sb: StringBuilder, transaction: Transaction) {
        sb.append(alignCenter)
        if (template.showCustomHeader1 && template.customHeader1.isNotEmpty()) {
            sb.append(template.customHeader1).append("\n")
        }
        if (!transaction.customerName.isNullOrBlank()) {
            sb.append(boldOn).append("Klienti: ").append(transaction.customerName.uppercase()).append(boldOff).append("\n")
        }
        if (template.showDividerTop) {
            sb.append("-".repeat(maxWidth)).append("\n")
        }
        
        val date = Date(transaction.date)
        var dateStr = if (template.showDate) dateFormat.format(date) else ""
        // Capitalize first letter of month (e.g. "23 Korrik 2026")
        if (dateStr.isNotEmpty()) {
            val parts = dateStr.split(" ")
            if (parts.size >= 2) {
                val day = parts[0]
                val month = parts[1].replaceFirstChar { it.uppercase() }
                val year = if (parts.size > 2) parts[2] else ""
                dateStr = "$day $month $year".trim()
            }
        }
        
        val timeStr = if (template.showTime) "Ora ${timeFormat.format(date)}" else ""
        val combined = "$dateStr  $timeStr".trim()
        if (combined.isNotEmpty()) {
            sb.append(combined).append("\n")
        }
        
        if (template.showDividerBeforeTotals) {
            sb.append("-".repeat(maxWidth)).append("\n")
        }
        sb.append(alignLeft)
    }

    private fun addCommonFooter(sb: StringBuilder, transaction: Transaction) {
        if (template.showDividerAfterTotals) {
            sb.append(alignLeft).append("-".repeat(maxWidth)).append("\n")
        }
        
        // TOTAL - I bashkuar dhe i qendërzuar Hardware
        if (template.showTotal) {
            val isClassic = template.style == "Klasik" || template.style == "Classic" || template.style == "Klasike" || template.style.isBlank()
            val totalLabel = if (isClassic) "TOTAL: ${transaction.total.toInt()} Lek" else "TOTAL: ${transaction.total.toInt()}"
            
            sb.append(alignCenter)
                .append(boldOn)
                .append(doubleSizeOn)
                .append(totalLabel)
                .append(doubleSizeOff)
                .append(boldOff)
                .append("\n")
        }
        
        if (template.showDividerBottom) {
            sb.append(alignLeft).append("-".repeat(maxWidth)).append("\n")
        }
        
        if (template.showCustomFooter1 && template.customFooter1.isNotEmpty()) {
            sb.append(alignCenter).append(template.customFooter1).append("\n")
        }
        sb.append("\n\n\n\n")
    }

    private fun buildClassicContent(sb: StringBuilder, items: List<CartItem>) {
        items.forEach { item ->
            // R1: Emri Majtas
            if (template.showItemName) {
                sb.append(alignLeft).append(item.name.uppercase()).append("\n")
            }
            
            // R2: Sasia & çmimi të shtyra me hapësirë nga e majta (e.g. "   10 x 100 Lek"), totali djathtas (e.g. "1000 Lek")
            val qtyInfo = if (template.showQuantity && template.showUnitPrice) "   ${item.quantity} x ${item.unitPrice.toInt()} Lek" else ""
            val totalStr = "${item.total.toInt()} Lek"
            
            val lineBuilder = StringBuilder(" ".repeat(maxWidth))
            // Qty info pushed from left
            if (qtyInfo.isNotEmpty()) {
                for (i in qtyInfo.indices) if (i < maxWidth) lineBuilder.setCharAt(i, qtyInfo[i])
            }
            // Total on right
            val totalStart = (maxWidth - totalStr.length).coerceAtLeast(0)
            for (i in totalStr.indices) if (totalStart + i < maxWidth) lineBuilder.setCharAt(totalStart + i, totalStr[i])
            
            sb.append(alignLeft).append(lineBuilder.toString()).append("\n")
        }
    }

    private fun buildMinimalContent(sb: StringBuilder, items: List<CartItem>) {
        items.forEach { item ->
            val qty = if (template.showQuantity) item.quantity.toString().padEnd(3) else ""
            val calcPart = if (template.showUnitPrice) "x${item.unitPrice.toInt()} -- ${item.total.toInt()}" else "${item.total.toInt()}"
            val rightPadding = if (template.paperSize == "80mm") 6 else 4
            val nameMaxLen = (maxWidth - qty.length - calcPart.length - rightPadding - 1).coerceAtLeast(2)
            val name = if (template.showItemName) (if (item.name.length > nameMaxLen) item.name.substring(0, nameMaxLen - 1).uppercase() + "." else item.name.uppercase().padEnd(nameMaxLen)) else ""
            val midSpaces = " ".repeat((maxWidth - qty.length - name.length - calcPart.length - rightPadding).coerceAtLeast(0))
            sb.append(qty).append(name).append(midSpaces).append(calcPart).append(" ".repeat(rightPadding)).append("\n")
        }
    }

    private fun buildYjetContent(sb: StringBuilder, items: List<CartItem>) {
        sb.append("*".repeat(maxWidth)).append("\n")
        items.forEach { item ->
            if (template.showItemName) sb.append(item.name.uppercase()).append("\n")
            val info = if (template.showQuantity && template.showUnitPrice) "  ${item.quantity} x ${item.unitPrice.toInt()}" else ""
            val tot = "${item.total.toInt()}"
            val spaces = maxWidth - info.length - tot.length
            sb.append(info).append(" ".repeat(if (spaces > 0) spaces else 1)).append(tot).append("\n")
        }
        sb.append("*".repeat(maxWidth)).append("\n")
    }

    private fun buildKutiContent(sb: StringBuilder, items: List<CartItem>) {
        sb.append("| ARTIKULLI").append(" ".repeat((maxWidth - 19).coerceAtLeast(0))).append("| SHUMA |\n")
        sb.append("-".repeat(maxWidth)).append("\n")
        items.forEach { item ->
            val name = if (item.name.length > (maxWidth - 12)) item.name.substring(0, maxWidth - 13) + "." else item.name.uppercase().padEnd(maxWidth - 12)
            val price = item.total.toInt().toString().padStart(6)
            if (template.showItemName) sb.append("| ").append(name).append(" | ").append(price).append(" |\n")
            val info = if (template.showQuantity && template.showUnitPrice) "${item.quantity} x ${item.unitPrice.toInt()}" else ""
            if (info.isNotEmpty()) sb.append("|   ").append(info.padEnd(maxWidth - 6)).append(" |\n")
        }
    }

    private fun buildPikatContent(sb: StringBuilder, items: List<CartItem>) {
        items.forEach { item ->
            val price = "${item.total.toInt()}"
            val nameLen = maxWidth - price.length - 1
            val name = if (item.name.length > nameLen) item.name.substring(0, nameLen - 1) else item.name.uppercase()
            val dots = maxWidth - name.length - price.length
            if (template.showItemName) sb.append(name).append(".".repeat(if (dots > 0) dots else 1)).append(price).append("\n")
            val info = if (template.showQuantity && template.showUnitPrice) "( ${item.quantity} x ${item.unitPrice.toInt()} )" else ""
            if (info.isNotEmpty()) sb.append("  ").append(info).append("\n")
        }
    }

    private fun buildLuxuryContent(sb: StringBuilder, items: List<CartItem>) {
        items.forEach { item ->
            if (template.showItemName) sb.append("  ").append(item.name.uppercase()).append("\n")
            val info = if (template.showQuantity && template.showUnitPrice) "  ${item.quantity} x ${item.unitPrice.toInt()}" else ""
            val tot = "${item.total.toInt()}"
            val spaces = maxWidth - info.length - tot.length - 2
            sb.append(info).append(" ".repeat(if (spaces > 0) spaces else 1)).append(tot).append("  \n\n")
        }
    }

    private fun buildTechContent(sb: StringBuilder, items: List<CartItem>) {
        items.forEach { item ->
            val qty = if (template.showQuantity) "(${item.quantity.toInt().toString().padStart(2, '0')}) " else ""
            val price = "${item.total.toInt()}"
            val unitP = if (template.showUnitPrice) "x${item.unitPrice.toInt()}" else ""
            val nameMaxLen = maxWidth - qty.length - price.length - unitP.length - 5
            val name = if (item.name.length > nameMaxLen) item.name.substring(0, (nameMaxLen - 1).coerceAtLeast(1)).uppercase() + "." else item.name.uppercase().padEnd(nameMaxLen.coerceAtLeast(1))
            if (template.showItemName) sb.append(qty).append(name).append("..").append(unitP).append(" = ").append(price).append("\n")
            else sb.append(qty).append("..").append(unitP).append(" = ").append(price).append("\n")
        }
    }

    private fun buildEleganteContent(sb: StringBuilder, items: List<CartItem>) {
        items.forEach { item ->
            if (template.showItemName) sb.append(item.name.uppercase()).append("\n")
            val detail = if (template.showQuantity && template.showUnitPrice) "  > ${item.quantity} X ${item.unitPrice.toInt()} =" else if (template.showQuantity) "  > ${item.quantity}" else ""
            val sum = "${item.total.toInt()}"
            val spaces = maxWidth - detail.length - sum.length
            sb.append(detail).append(" ".repeat(if (spaces > 0) spaces else 1)).append(sum).append("\n")
        }
    }

    private fun buildRetroContent(sb: StringBuilder, items: List<CartItem>) {
        items.forEach { item ->
            val qtyPart = if (template.showQuantity) "[${item.quantity.toInt()}] " else ""
            val left = if (template.showItemName) "$qtyPart${item.name.uppercase()}" else qtyPart
            val right = "${item.total.toInt()}"
            if (left.length + right.length + 1 < maxWidth) {
                sb.append(left).append(" ".repeat(maxWidth - left.length - right.length)).append(right).append("\n")
            } else {
                sb.append(left.substring(0, (maxWidth - right.length - 2).coerceAtLeast(1))).append("..").append(right).append("\n")
            }
            val unitPart = if (template.showUnitPrice) "     (X ${item.unitPrice.toInt()})" else ""
            if (unitPart.isNotEmpty()) sb.append(unitPart).append("\n")
        }
    }

    private fun buildKompaktPlusContent(sb: StringBuilder, items: List<CartItem>) {
        items.forEach { item ->
            val qty = if (template.showQuantity) "${item.quantity.toInt()}x" else ""
            val name = if (template.showItemName) "${item.name.uppercase()}x" else ""
            val price = if (template.showUnitPrice) "${item.unitPrice.toInt()}=" else ""
            val line = "$qty$name$price${item.total.toInt()}"
            sb.append(if (line.length > maxWidth) line.substring(0, maxWidth) else line).append("\n")
        }
    }

    private fun getItems(transaction: Transaction): List<CartItem> {
        return try {
            Json.decodeFromString<List<CartItem>>(transaction.itemsJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun centerText(text: String, width: Int = maxWidth): String {
        if (text.length >= width) return text
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text + " ".repeat(width - text.length - padding)
    }
}
