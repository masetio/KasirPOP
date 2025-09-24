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
}
