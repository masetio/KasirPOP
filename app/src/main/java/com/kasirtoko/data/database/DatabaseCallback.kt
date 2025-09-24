package com.kasirtoko.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class DatabaseCallback : RoomDatabase.Callback() {
    
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        // Insert default admin user
        db.execSQL("""
            INSERT INTO users (id, username, password, role, fullName, isActive, createdAt) 
            VALUES ('admin-001', 'admin', 'admin123', 'admin', 'Administrator', 1, ${System.currentTimeMillis()})
        """)
        
        // Insert default settings
        val currentTime = System.currentTimeMillis()
        db.execSQL("""
            INSERT INTO app_settings (key, value, updatedAt) VALUES 
            ('shop_name', 'Toko Saya', $currentTime),
            ('footer_text_1', 'Terima kasih telah berbelanja', $currentTime),
            ('footer_text_2', 'Follow Instagram: @toko_saya', $currentTime),
            ('logo_path', '', $currentTime)
        """)
        
        // Insert sample products (optional)
        db.execSQL("""
            INSERT INTO products (kodeBarang, namaBarang, unit, hargaJual, hargaBeli, kategori, stok, createdAt, updatedAt) VALUES 
            ('BRG001', 'Indomie Goreng', 'pcs', 3000, 2500, 'Makanan', 100, $currentTime, $currentTime),
            ('BRG002', 'Aqua 600ml', 'pcs', 3500, 3000, 'Minuman', 50, $currentTime, $currentTime),
            ('BRG003', 'Buku Tulis', 'pcs', 2000, 1500, 'ATK', 25, $currentTime, $currentTime)
        """)
    }
}
