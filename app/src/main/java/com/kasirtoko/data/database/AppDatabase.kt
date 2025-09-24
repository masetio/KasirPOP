// database/AppDatabase.kt
@Database(
    entities = [
        User::class,
        Product::class,
        StockMovement::class,
        Transaction::class,
        TransactionItem::class,
        AppSetting::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun appSettingDao(): AppSettingDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasir_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
    
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Insert default admin user
            db.execSQL("""
                INSERT INTO users (id, username, password, role, fullName, isActive) 
                VALUES ('admin-001', 'admin', 'admin123', 'admin', 'Administrator', 1)
            """)
            
            // Insert default settings
            db.execSQL("""
                INSERT INTO app_settings (key, value) VALUES 
                ('shop_name', 'Toko Saya'),
                ('footer_text_1', 'Terima kasih telah berbelanja'),
                ('footer_text_2', 'Follow Instagram: @toko_saya'),
                ('logo_path', '')
            """)
        }
    }
}
