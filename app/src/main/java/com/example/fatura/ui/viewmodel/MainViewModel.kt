package com.example.fatura.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.fatura.data.AppDatabase
import com.example.fatura.data.AppRepository
import com.example.fatura.data.entity.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository
    
    val allItems: StateFlow<List<Item>>
    val allTransactions: StateFlow<List<Transaction>>
    val receiptTemplate: StateFlow<ReceiptTemplate>
    val transactionsToday: StateFlow<List<Transaction>>
    val totalTurnoverToday: StateFlow<Double>

    private val _dismissedAlerts = MutableStateFlow<Map<Int, Long>>(emptyMap()) // itemId -> dismissalTime
    private val _currentTimeTicker = MutableStateFlow(System.currentTimeMillis())

    private val _printerBatteryLevel = MutableStateFlow<Int?>(null)
    val printerBatteryLevel: StateFlow<Int?> = _printerBatteryLevel

    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode

    init {
        val appDao = AppDatabase.getDatabase(application).appDao()
        repository = AppRepository(appDao)
        
        allItems = repository.allItems.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        allTransactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        receiptTemplate = repository.receiptTemplate
            .map { it ?: ReceiptTemplate() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ReceiptTemplate())
        transactionsToday = repository.getTransactionsToday()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
            
        totalTurnoverToday = transactionsToday.map { list ->
            list.sumOf { it.total }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

        viewModelScope.launch {
            val darkSetting = repository.getSetting("dark_mode")
            _isDarkMode.value = darkSetting?.toBoolean()
            
            // Load dismissed alerts from database to persist across restarts
            val alertsJson = repository.getSetting("dismissed_alerts")
            if (alertsJson != null) {
                try {
                    _dismissedAlerts.value = Json.decodeFromString(alertsJson)
                } catch (_: Exception) {}
            }
        }

        // Refresh low stock logic every minute
        viewModelScope.launch {
            while(true) {
                kotlinx.coroutines.delay(60000)
                _currentTimeTicker.value = System.currentTimeMillis()
            }
        }
    }

    val lowStockItems: Flow<List<Item>> by lazy {
        combine(allItems, _dismissedAlerts, _currentTimeTicker) { items, dismissed, currentTime ->
            items.filter { item ->
                // Njoftimi del nëse pika e kuqe është e përcaktuar (> 0) dhe stoku është <= pikës së kuqë (përfshirë kur stoku shkon 0)
                val isLow = item.lowStockThreshold > 0 && item.stock <= item.lowStockThreshold
                // Show if it was never dismissed OR if 30 mins have passed
                val lastDismissal = dismissed[item.id] ?: 0L
                val hasExpired = currentTime - lastDismissal > 30 * 60 * 1000
                isLow && hasExpired
            }
        }
    }

    fun dismissAlert(itemId: Int) {
        val current = _dismissedAlerts.value.toMutableMap()
        current[itemId] = System.currentTimeMillis()
        _dismissedAlerts.value = current
        
        // Save to database
        viewModelScope.launch {
            repository.saveSetting("dismissed_alerts", Json.encodeToString(current))
        }
    }

    fun updatePrinterBattery(level: Int?) {
        _printerBatteryLevel.value = level
    }

    fun getTurnoverForDate(date: Long): Flow<Double> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis
        
        return allTransactions.map { list ->
            list.filter { it.date in start..end }.sumOf { it.total }
        }
    }

    /**
     * Aggregates items sold across all transactions for a specific date range.
     */
    fun getAggregatedItemsForDate(date: Long): Flow<List<CartItem>> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return allTransactions.map { list ->
            val dailyTransactions = list.filter { it.date in start..end }
            val allCartItems = dailyTransactions.flatMap { tx ->
                try {
                    Json.decodeFromString<List<CartItem>>(tx.itemsJson)
                } catch (_: Exception) {
                    emptyList()
                }
            }
            
            allCartItems.groupBy { it.itemId }
                .map { (id, items) ->
                    val first = items.first()
                    CartItem(
                        itemId = id,
                        name = first.name,
                        quantity = items.sumOf { it.quantity },
                        unitPrice = first.unitPrice,
                        taxRate = first.taxRate,
                        total = items.sumOf { it.total }
                    )
                }
        }
    }

    fun performDailyDelete(dateMillis: Long) = viewModelScope.launch {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        val end = start + 86400000L - 1
        repository.deleteTransactionsInRange(start, end)
    }

    // Item Operations
    fun addItem(item: Item) = viewModelScope.launch { repository.insertItem(item) }
    fun updateItem(item: Item) = viewModelScope.launch { repository.updateItem(item) }
    fun deleteItem(item: Item) = viewModelScope.launch { repository.deleteItem(item) }

    // Transaction Operations
    fun saveTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.insertTransaction(transaction)
        
        // Decrement Stock
        try {
            val itemsInCart = Json.decodeFromString<List<CartItem>>(transaction.itemsJson)
            itemsInCart.forEach { cartItem ->
                val existingItem = allItems.value.find { it.id == cartItem.itemId }
                if (existingItem != null) {
                    val newStock = (existingItem.stock - cartItem.quantity).coerceAtLeast(0)
                    updateItem(existingItem.copy(stock = newStock))
                }
            }
        } catch (_: Exception) {}
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repository.deleteTransaction(transaction)
    }

    private val jsonConfig = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    fun getItemsJson(): String {
        return jsonConfig.encodeToString<List<Item>>(allItems.value)
    }

    fun importItemsFromJson(json: String) = viewModelScope.launch {
        try {
            val items = jsonConfig.decodeFromString<List<Item>>(json)
            items.forEach { oldItem ->
                val migratedItem = if (oldItem.quantity == 1 && oldItem.stock > 1) {
                    oldItem.copy(id = 0, quantity = oldItem.stock, stock = 0)
                } else {
                    oldItem.copy(id = 0)
                }
                repository.insertItem(migratedItem)
            }
        } catch (_: Exception) {}
    }

    // Settings
    fun saveReceiptTemplate(template: ReceiptTemplate) = viewModelScope.launch {
        repository.saveReceiptTemplate(template)
    }
    
    suspend fun getSetting(key: String) = repository.getSetting(key)
    fun saveSetting(key: String, value: String) = viewModelScope.launch {
        repository.saveSetting(key, value)
        if (key == "dark_mode") {
            _isDarkMode.value = value.toBoolean()
        }
    }

    // Sales/Cart Logic
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    fun addToCart(item: Item) {
        val currentList = _cartItems.value.toMutableList()
        val existingItem = currentList.find { it.itemId == item.id }
        if (existingItem != null) {
            val index = currentList.indexOf(existingItem)
            val newQty = existingItem.quantity + item.quantity 
            currentList[index] = existingItem.copy(
                quantity = newQty,
                total = newQty * existingItem.unitPrice,
            )
        } else {
            currentList.add(
                CartItem(
                    itemId = item.id,
                    name = item.name,
                    quantity = item.quantity, 
                    unitPrice = item.price,
                    taxRate = item.taxRate,
                    total = item.quantity * item.price
                )
            )
        }
        _cartItems.value = currentList
    }

    fun removeFromCart(cartItem: CartItem) {
        val currentList = _cartItems.value.toMutableList()
        val existingItem = currentList.find { it.itemId == cartItem.itemId }
        if (existingItem != null) {
            currentList.remove(existingItem)
        }
        _cartItems.value = currentList
    }

    fun updateQuantity(cartItem: CartItem, newQuantity: Int) {
        val currentList = _cartItems.value.toMutableList()
        val existingItem = currentList.find { it.itemId == cartItem.itemId } ?: return
        val index = currentList.indexOf(existingItem)
        val qty = if (newQuantity < 0) 0 else newQuantity
        currentList[index] = existingItem.copy(
            quantity = qty,
            total = qty * existingItem.unitPrice
        )
        _cartItems.value = currentList
    }

    fun updateUnitPrice(cartItem: CartItem, newPrice: Double) {
        val currentList = _cartItems.value.toMutableList()
        val existingItem = currentList.find { it.itemId == cartItem.itemId } ?: return
        val index = currentList.indexOf(existingItem)
        val price = if (newPrice < 0.0) 0.0 else newPrice
        currentList[index] = existingItem.copy(
            unitPrice = price,
            total = existingItem.quantity * price
        )
        _cartItems.value = currentList
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun loadTransactionToCart(transaction: Transaction) {
        val items = try {
            Json.decodeFromString<List<CartItem>>(transaction.itemsJson)
        } catch (_: Exception) {
            emptyList()
        }
        _cartItems.value = items
    }

    val cartSubtotal = cartItems.map { items -> items.sumOf { it.total } }
    val cartTotal = cartSubtotal

    // Floating Calculator Logic
    private val _isFloatingCalculatorVisible = MutableStateFlow(false)
    val isFloatingCalculatorVisible: StateFlow<Boolean> = _isFloatingCalculatorVisible

    private val _isCalculatorMinimized = MutableStateFlow(true)
    val isCalculatorMinimized: StateFlow<Boolean> = _isCalculatorMinimized

    fun toggleFloatingCalculator(visible: Boolean) {
        _isFloatingCalculatorVisible.value = visible
    }

    fun setCalculatorMinimized(minimized: Boolean) {
        _isCalculatorMinimized.value = minimized
    }
}
