package com.example.fatura.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "items")
@Serializable
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val category: String,
    val taxRate: Double,
    val stock: Int,
    val quantity: Int = 1, // Sasia e shitjes (sa shtohet ne shporte)
    val lowStockThreshold: Int = 2, // Sasia kur ndizet pika e kuqe
    val barcode: String? = null,
    val imageUri: String? = null, // Path-i i fotos nga galeria
    val createdAt: Long = System.currentTimeMillis()
)
