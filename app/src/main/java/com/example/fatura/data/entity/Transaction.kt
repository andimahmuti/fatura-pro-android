package com.example.fatura.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val receiptNumber: String,
    val date: Long = System.currentTimeMillis(),
    val itemsJson: String, // Serialized List<CartItem>
    val subtotal: Double,
    val tax: Double,
    val discount: Double,
    val total: Double,
    val paymentMethod: String,
    val amountPaid: Double,
    val change: Double,
    val customerName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class CartItem(
    val itemId: Int,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val taxRate: Double,
    val total: Double
)
