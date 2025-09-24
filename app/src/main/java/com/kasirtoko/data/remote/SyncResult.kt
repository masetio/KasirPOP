package com.kasirtoko.data.remote

data class SyncResult(
    var success: Boolean = false,
    var message: String = "",
    var userSync: EntitySyncResult = EntitySyncResult(),
    var productSync: EntitySyncResult = EntitySyncResult(),
    var transactionSync: EntitySyncResult = EntitySyncResult(),
    var stockMovementSync: EntitySyncResult = EntitySyncResult(),
    var settingsSync: EntitySyncResult = EntitySyncResult()
) {
    val totalUploaded: Int
        get() = userSync.uploaded + productSync.uploaded + transactionSync.uploaded + 
                stockMovementSync.uploaded + settingsSync.uploaded
    
    val totalDownloaded: Int
        get() = userSync.downloaded + productSync.downloaded + transactionSync.downloaded + 
                stockMovementSync.downloaded + settingsSync.downloaded
    
    val totalErrors: Int
        get() = userSync.errors.size + productSync.errors.size + transactionSync.errors.size + 
                stockMovementSync.errors.size + settingsSync.errors.size
    
    val allErrors: List<String>
        get() = userSync.errors + productSync.errors + transactionSync.errors + 
                stockMovementSync.errors + settingsSync.errors
}

data class EntitySyncResult(
    var uploaded: Int = 0,
    var downloaded: Int = 0,
    val errors: MutableList<String> = mutableListOf()
)
