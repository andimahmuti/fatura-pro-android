package com.example.fatura.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fatura.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Items
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<Item>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItem(item: Item)
    
    @Update
    fun updateItem(item: Item)
    
    @Delete
    fun deleteItem(item: Item)
    
    @Query("SELECT * FROM items WHERE barcode = :barcode LIMIT 1")
    fun getItemByBarcode(barcode: String): Item?

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Insert
    fun insertTransaction(transaction: Transaction)

    @Delete
    fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE date >= :start AND date <= :end")
    fun deleteTransactionsInRange(start: Long, end: Long): Int
    
    @Query("SELECT * FROM transactions WHERE date >= :startOfDay ORDER BY date DESC")
    fun getTransactionsToday(startOfDay: Long): Flow<List<Transaction>>

    // Receipt Templates
    @Query("SELECT * FROM receipt_templates LIMIT 1")
    fun getReceiptTemplate(): Flow<ReceiptTemplate?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReceiptTemplate(template: ReceiptTemplate)

    // Settings
    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Setting?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSetting(setting: Setting)
}
