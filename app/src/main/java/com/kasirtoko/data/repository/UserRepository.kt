package com.kasirtoko.data.repository

import com.kasirtoko.data.database.AppDatabase
import com.kasirtoko.data.entities.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val database: AppDatabase
) {
    
    suspend fun login(username: String, password: String) = database.userDao().login(username, password)
    
    suspend fun getAllActiveUsers() = database.userDao().getAllActiveUsers()
    
    suspend fun getUserById(id: String) = database.userDao().getUserById(id)
    
    suspend fun insertUser(user: User) = database.userDao().insertUser(user)
    
    suspend fun updateUser(user: User) = database.userDao().updateUser(user)
    
    suspend fun deactivateUser(userId: String) = database.userDao().deactivateUser(userId)
    
    suspend fun getUsersForSync() = database.userDao().getUsersForSync()
    
    suspend fun updateSyncTime(userId: String, syncTime: Long) = 
        database.userDao().updateSyncTime(userId, syncTime)
}
