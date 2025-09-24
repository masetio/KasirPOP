@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettings(): List<AppSetting>
    
    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettingsForSync(): List<AppSetting>
    
    @Query("SELECT * FROM app_settings WHERE key = :key")
    suspend fun getSetting(key: String): AppSetting?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)
    
    @Query("UPDATE app_settings SET value = :value, updatedAt = :updatedAt WHERE key = :key")
    suspend fun updateSetting(key: String, value: String, updatedAt: Long = System.currentTimeMillis())
}
