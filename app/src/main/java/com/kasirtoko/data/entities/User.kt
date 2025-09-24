@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val password: String,
    val role: String, // "admin" or "kasir"
    val fullName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val lastSyncAt: Long = 0
)
