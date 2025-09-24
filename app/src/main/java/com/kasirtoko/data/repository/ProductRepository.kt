package com.kasirtoko.data.repository

import com.kasirtoko.data.database.AppDatabase
import com.kasirtoko.data.entities.Product
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val database: AppDatabase
) {
    
    fun getAllProducts() = database.productDao().getAllProducts()
    
    fun searchProducts(query: String) = database.productDao().searchProducts(query)
    
    fun getProductsByCategory(category: String) = database.productDao().getProductsByCategory(category)
    
    suspend fun getProductByKode(kodeBarang: String) = database.productDao().getProductByKode(kodeBarang)
    
    suspend fun getProductByBarcode(barcode: String) = database.productDao().getProductByBarcode(barcode)
    
    suspend fun getAllCategories() = database.productDao().getAllCategories()
    
    suspend fun insertProduct(product: Product) = database.productDao().insertProduct(product)
    
    suspend fun insertProducts(products: List<Product>) = database.productDao().insertProducts(products)
    
    suspend fun updateProduct(product: Product) = database.productDao().updateProduct(product)
    
    suspend fun deleteProduct(product: Product) = database.productDao().deleteProduct(product)
    
    suspend fun getLowStockProducts(threshold: Int = 5) = database.productDao().getLowStockProducts(threshold)
    
    suspend fun getAllProductsSync() = database.productDao().getAllProductsSync()
    
    suspend fun deleteProductsWithZeroStock() = database.productDao().deleteProductsWithZeroStock()
}
