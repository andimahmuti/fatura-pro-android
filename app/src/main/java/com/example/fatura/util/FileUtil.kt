package com.example.fatura.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.fatura.data.entity.CartItem
import com.example.fatura.data.entity.Transaction
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun saveImageLocally(context: Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val folder = File(context.filesDir, "item_images")
        if (!folder.exists()) folder.mkdirs()
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val outFile = File(folder, fileName)
        inputStream.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        outFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

fun saveReceiptAsJpgToDownloads(context: Context, transaction: Transaction) {
    try {
        val width = 600
        val items = Json.decodeFromString<List<CartItem>>(transaction.itemsJson)
        val height = 400 + (items.size * 100)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 30f
            isAntiAlias = true
        }
        
        val boldPaint = Paint(paint).apply {
            isFakeBoldText = true
            textSize = 40f
        }
        
        var y = 80f
        canvas.drawText("FATURE SHITJE", width / 2f - 120f, y, boldPaint)
        
        y += 80f
        canvas.drawText("Data: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(transaction.date))}", 50f, y, paint)
        
        y += 60f
        canvas.drawLine(40f, y, width - 40f, y, paint)
        
        y += 60f
        items.forEach { item ->
            canvas.drawText(item.name, 50f, y, boldPaint)
            y += 40f
            canvas.drawText("${item.quantity} x ${item.unitPrice.toInt()} Lek", 50f, y, paint)
            canvas.drawText("${item.total.toInt()} Lek", width - 200f, y, boldPaint)
            y += 60f
        }
        
        canvas.drawLine(40f, y, width - 40f, y, paint)
        y += 80f
        canvas.drawText("TOTALI: Lek ${transaction.total.toInt()}", 50f, y, boldPaint)
        
        val fileName = "Fatura_${transaction.receiptNumber}.jpg"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            file.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
        }
        
        Toast.makeText(context, "Fatura u ruajt në Downloads (JPG)", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Gabim: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
