@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username AND password = :password AND isActive = 1")
    suspend fun login(username: String, password: String): User?
    
    @Query("SELECT * FROM users WHERE isActive = 1")
    suspend fun getAllActiveUsers(): List<User>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Query("UPDATE users SET isActive = 0 WHERE id = :userId")
    suspend fun deactivateUser(userId: String)
    
    @Query("SELECT * FROM users WHERE lastSyncAt = 0 OR updatedAt > lastSyncAt")
suspend fun getUsersForSync(): List<User>

@Query("SELECT * FROM users WHERE id = :id")
suspend fun getUserById(id: String): User?

@Query("UPDATE users SET lastSyncAt = :syncTime WHERE id = :id")
suspend fun updateSyncTime(id: String, syncTime: Long)
}
