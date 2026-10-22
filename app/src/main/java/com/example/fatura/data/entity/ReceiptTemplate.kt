package com.example.fatura.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipt_templates")
data class ReceiptTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "Default",
    val style: String = "Classic", // "Classic", "Minimal", "Modern"
    val paperSize: String = "58mm", // "58mm" or "80mm"
    val fontType: String = "Normal", // "Normal" (Font A), "Vogël" (Font B)
    
    // Header
    val showLogo: Boolean = true,
    val showBusinessName: Boolean = true,
    val customHeader1: String = "",
    val showCustomHeader1: Boolean = false,
    val showAddress: Boolean = false,
    val showPhone: Boolean = false,
    val showNipt: Boolean = false,
    val showReceiptNumber: Boolean = true,
    val showDate: Boolean = true,
    val showTime: Boolean = true,
    val showCashierName: Boolean = false,
    val showDividerTop: Boolean = true,
    
    // Items
    val showColumnHeaders: Boolean = true,
    val showItemName: Boolean = true,
    val showQuantity: Boolean = true,
    val showUnitPrice: Boolean = true,
    val showItemTotal: Boolean = true,
    val showDividerBeforeTotals: Boolean = true,
    
    // Summary
    val showSubtotal: Boolean = false,
    val showTax: Boolean = false,
    val showDiscount: Boolean = false,
    val showTotal: Boolean = true,
    val showDividerAfterTotals: Boolean = true,
    
    // Payment
    val showPaymentMethod: Boolean = false,
    val showAmountPaid: Boolean = false,
    val showChange: Boolean = false,
    
    // Footer
    val showThankYou: Boolean = true,
    val showDividerBottom: Boolean = true,
    val customFooter1: String = "",
    val showCustomFooter1: Boolean = false,
    val customFooter2: String = "",
    val showCustomFooter2: Boolean = false,
    val customFooter3: String = "",
    val showCustomFooter3: Boolean = false,
    
    // Business Info
    val businessName: String = "FATURE SHITJE",
    val address: String = "",
    val phone: String = "",
    val nipt: String = "",
    val logoPath: String? = null,
    
    // UI Settings
    val salesItemStyle: Int = 0 // 0: Grid, 1: List-like Grid
)
