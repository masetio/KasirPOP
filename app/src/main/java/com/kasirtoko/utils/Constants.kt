package com.kasirtoko.utils

object Constants {
    
    // User Roles
    const val ROLE_ADMIN = "admin"
    const val ROLE_KASIR = "kasir"
    
    // Payment Methods
    const val PAYMENT_CASH = "CASH"
    const val PAYMENT_QRIS = "QRIS"
    const val PAYMENT_DEBT = "DEBT"
    
    // Payment Status
    const val STATUS_PAID = "PAID"
    const val STATUS_UNPAID = "UNPAID"
    
    // Stock Movement Types
    const val STOCK_IN = "IN"
    const val STOCK_OUT = "OUT"
    
    // Date Formats
    const val DATE_FORMAT_DISPLAY = "dd/MM/yyyy"
    const val DATE_FORMAT_FILE = "yyyyMMdd"
    const val DATETIME_FORMAT_DISPLAY = "dd/MM/yyyy HH:mm:ss"
    const val DATETIME_FORMAT_RECEIPT = "dd/MM/yyyy HH:mm"
    
    // File Types
    const val MIME_TYPE_CSV = "text/csv"
    const val MIME_TYPE_PDF = "application/pdf"
    const val MIME_TYPE_IMAGE = "image/*"
    
    // Preferences Keys
    const val PREF_USER_SESSION = "user_session"
    const val PREF_PRINTER = "printer_prefs"
    const val PREF_SYNC = "sync_prefs"
    
    // Permission Request Codes
    const val PERMISSION_CAMERA = 1001
    const val PERMISSION_BLUETOOTH = 1002
    const val PERMISSION_STORAGE = 1003
    
    // Default Values
    const val DEFAULT_LOW_STOCK_THRESHOLD = 5
    const val DEFAULT_THERMAL_PRINTER_WIDTH = 58f
    const val DEFAULT_THERMAL_PRINTER_DPI = 203
    const val DEFAULT_THERMAL_PRINTER_CHARS = 32
    
    // App Settings Keys
    const val SETTING_SHOP_NAME = "shop_name"
    const val SETTING_FOOTER_TEXT_1 = "footer_text_1"
    const val SETTING_FOOTER_TEXT_2 = "footer_text_2"
    const val SETTING_LOGO_PATH = "logo_path"
    const val SETTING_LOW_STOCK_THRESHOLD = "low_stock_threshold"
    const val SETTING_AUTO_SYNC_ENABLED = "auto_sync_enabled"
    const val SETTING_AUTO_SYNC_INTERVAL = "auto_sync_interval"
    
    // Sync Settings
    const val SYNC_BATCH_SIZE_PRODUCTS = 50
    const val SYNC_BATCH_SIZE_TRANSACTIONS = 20
    const val SYNC_BATCH_SIZE_STOCK_MOVEMENTS = 100
    const val SYNC_TIMEOUT_SECONDS = 30L
    
    // Receipt Settings
    const val RECEIPT_MAX_ITEM_NAME_LENGTH = 20
    const val RECEIPT_SEPARATOR = "--------------------------------"
    const val RECEIPT_DOUBLE_SEPARATOR = "================================"
    
    // Import/Export
    const val CSV_IMPORT_MAX_ROWS = 1000
    const val CSV_EXPORT_CHUNK_SIZE = 500
    const val PDF_MAX_ITEMS_PER_PAGE = 40
    
    // Validation
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_USERNAME_LENGTH = 20
    const val MAX_PRODUCT_NAME_LENGTH = 100
    const val MAX_CATEGORY_NAME_LENGTH = 50
    
    // Network
    const val NETWORK_TIMEOUT_SECONDS = 30L
    const val RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 1000L
}
