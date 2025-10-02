package com.kasirtoko.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kasirtoko.data.remote.SupabaseSyncHelper
import com.kasirtoko.data.remote.SyncResult
import kotlinx.coroutines.launch

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    
    private val syncHelper = SupabaseSyncHelper(application)
    
    private val _syncResult = MutableLiveData<SyncResult>()
    val syncResult: LiveData<SyncResult> get() = _syncResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    
    private val _lastSyncTime = MutableLiveData<String>()
    val lastSyncTime: LiveData<String> get() = _lastSyncTime
    
    init {
        updateLastSyncTime()
    }
    
    fun syncAll() {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val result = syncHelper.syncAll()
                _syncResult.value = result
                updateLastSyncTime()
            } catch (e: Exception) {
                _syncResult.value = SyncResult().apply {
                    success = false
                    message = "Sync error: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun updateLastSyncTime() {
        _lastSyncTime.value = syncHelper.getLastSyncTimeFormatted()
    }
    
    fun getLastSyncTimestamp(): Long {
        return syncHelper.getLastSyncTime()
    }
}
