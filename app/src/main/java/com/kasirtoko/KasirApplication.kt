class KasirApplication : Application() {
    
    val database by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any required services
        setupCrashReporting()
        setupBackgroundSync()
    }
    
    private fun setupCrashReporting() {
        // Setup crash reporting if needed
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("KasirApp", "Uncaught exception", exception)
            
            // You can implement crash reporting here
            // For example, send crash logs to your server
        }
    }
    
    private fun setupBackgroundSync() {
        // Setup periodic sync with cloud database
        // This can be implemented using WorkManager
    }
}
