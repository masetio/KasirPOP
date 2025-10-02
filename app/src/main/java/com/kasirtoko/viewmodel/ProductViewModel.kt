class ProductViewModel(application: Application) : AndroidViewModel(application) {
    
    private val productDao = AppDatabase.getDatabase(application).productDao()
    
    val allProducts: LiveData<List<Product>> = productDao.getAllProducts()
    
    fun getProductsByCategory(category: String): LiveData<List<Product>> {
        return productDao.getProductsByCategory(category)
    }
    
    fun searchProducts(query: String): LiveData<List<Product>> {
        return productDao.searchProducts(query)
    }
    
    fun insertProduct(product: Product) = viewModelScope.launch {
        productDao.insertProduct(product)
    }
    
    fun updateProduct(product: Product) = viewModelScope.launch {
        productDao.updateProduct(product)
    }
    
    fun deleteProduct(product: Product) = viewModelScope.launch {
        productDao.deleteProduct(product)
    }
}

// viewmodel/TransactionViewModel.kt
class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()
    
    val allTransactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions()
    val unpaidTransactions: LiveData<List<Transaction>> = transactionDao.getUnpaidTransactions()
    
    fun getTransactionsByKasir(kasirId: String): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByKasir(kasirId)
    }
    
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }
    
    fun insertTransaction(transaction: Transaction, items: List<TransactionItem>) = viewModelScope.launch {
        val transactionId = transactionDao.insertTransaction(transaction)
        transactionDao.insertTransactionItems(items)
    }
    
    fun markAsPaid(transactionId: String) = viewModelScope.launch {
        transactionDao.markAsPaid(transactionId, System.currentTimeMillis())
    }
}
