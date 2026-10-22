package com.example.fatura.data

import com.example.fatura.data.dao.AppDao
import com.example.fatura.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class AppRepository(private val appDao: AppDao) {
    val allItems: Flow<List<Item>> = appDao.getAllItems()
    val allTransactions: Flow<List<Transaction>> = appDao.getAllTransactions()
    val receiptTemplate: Flow<ReceiptTemplate?> = appDao.getReceiptTemplate()

    suspend fun insertItem(item: Item) = withContext(Dispatchers.IO) { appDao.insertItem(item) }
    suspend fun updateItem(item: Item) = withContext(Dispatchers.IO) { appDao.updateItem(item) }
    suspend fun deleteItem(item: Item) = withContext(Dispatchers.IO) { appDao.deleteItem(item) }
    
    suspend fun insertTransaction(transaction: Transaction) = withContext(Dispatchers.IO) { appDao.insertTransaction(transaction) }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) { appDao.deleteTransaction(transaction) }

    suspend fun deleteTransactionsInRange(start: Long, end: Long): Int = withContext(Dispatchers.IO) {
        appDao.deleteTransactionsInRange(start, end)
    }
    
    fun getTransactionsToday(): Flow<List<Transaction>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return appDao.getTransactionsToday(calendar.timeInMillis)
    }

    suspend fun saveReceiptTemplate(template: ReceiptTemplate) = withContext(Dispatchers.IO) { appDao.insertReceiptTemplate(template) }
    
    suspend fun getSetting(key: String): String? = withContext(Dispatchers.IO) { appDao.getSetting(key)?.value }
    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) { appDao.insertSetting(Setting(key, value)) }
}
