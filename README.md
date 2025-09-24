# Aplikasi Kasir dan Logistik Toko

Aplikasi Android untuk manajemen kasir dan logistik toko dengan sistem multi-user dan sinkronisasi cloud.

## Fitur Utama

### 🔐 Sistem Multi-User
- **Admin**: Akses penuh untuk manajemen produk, stok, user, dan laporan
- **Kasir**: Akses terbatas hanya untuk transaksi penjualan

### 📦 Manajemen Produk & Stok
- CRUD produk dengan kode barang unik
- Tracking stok masuk dan keluar otomatis
- Import data produk dari file CSV
- Kategori dan barcode support

### 💰 Sistem Transaksi
- Multiple payment methods (Cash, QRIS, Utang)
- Scanner barcode/QR code
- Pencarian produk berdasarkan nama atau kategori
- Shopping cart dengan quantity control

### 🖨️ Pencetakan & Laporan
- Cetak nota thermal printer 58mm via Bluetooth
- Export laporan ke CSV dan PDF
- Laporan penjualan, keuangan, dan stok
- Filter laporan berdasarkan tanggal, kasir, dan kategori

### ☁️ Sinkronisasi Cloud
- Backup data ke Supabase
- Multi-device synchronization
- Offline-first dengan auto-sync

## Teknologi

- **Platform**: Android (API 28+)
- **Language**: Kotlin
- **Database**: SQLite (Room)
- **Cloud**: Supabase
- **UI**: Material Design 3
- **Architecture**: MVVM + Repository Pattern

## Setup Proyek

### 1. Clone Repository
```bash
git clone <repository-url>
cd kasir-toko
```

### 2. Setup Supabase
1. Buat project baru di [Supabase](https://supabase.com)
2. Buat tabel-tabel berikut di SQL Editor:

```sql
-- Users table
CREATE TABLE users (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL,
    full_name TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT true,
    updated_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);

-- Products table
CREATE TABLE products (
    kode_barang TEXT PRIMARY KEY,
    nama_barang TEXT NOT NULL,
    unit TEXT NOT NULL,
    harga_jual DECIMAL(10,2) NOT NULL,
    harga_beli DECIMAL(10,2) DEFAULT 0,
    kode_barcode TEXT,
    kategori TEXT NOT NULL,
    stok INTEGER DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);

-- Transactions table
CREATE TABLE transactions (
    id TEXT PRIMARY KEY,
    kasir_id TEXT NOT NULL,
    kasir_name TEXT NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    payment_method TEXT NOT NULL,
    payment_status TEXT NOT NULL,
    cash_received DECIMAL(12,2),
    cash_change DECIMAL(12,2),
    customer_name TEXT,
    notes TEXT,
    created_at BIGINT NOT NULL,
    paid_at BIGINT
);

-- Transaction Items table
CREATE TABLE transaction_items (
    id TEXT PRIMARY KEY,
    transaction_id TEXT NOT NULL,
    kode_barang TEXT NOT NULL,
    nama_barang TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    harga_satuan DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

-- Stock Movements table
CREATE TABLE stock_movements (
    id TEXT PRIMARY KEY,
    kode_barang TEXT NOT NULL,
    movement_type TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    harga_beli DECIMAL(10,2),
    reference_id TEXT,
    notes TEXT,
    created_by TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

-- App Settings table
CREATE TABLE app_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);
```

3. Update `build.gradle` dengan Supabase credentials:
```kotlin
buildConfigField "String", "SUPABASE_URL", "\"https://your-project.supabase.co\""
buildConfigField "String", "SUPABASE_ANON_KEY", "\"your-anon-key\""
```

### 3. Build & Run
```bash
./gradlew assembleDebug
```

## Default Login

- **Username**: `admin`
- **Password**: `admin123`
- **Role**: Admin

## Struktur Database

### Local (SQLite)
- Semua data disimpan local-first
- Auto-sync dengan cloud database
- Offline functionality penuh

### Cloud (Supabase)
- Backup semua data transaksi
- Multi-device synchronization
- Real-time updates

## Panduan Penggunaan

### Admin
1. **Login** dengan akun admin
2. **Kelola Produk**: Tambah, edit, hapus produk
3. **Import Data**: Upload CSV untuk bulk product insert
4. **Kelola User**: Tambah kasir baru
5. **Laporan**: Generate dan export laporan
6. **Sinkronisasi**: Sync data dengan cloud

### Kasir
1. **Login** dengan akun kasir
2. **Pilih Produk**: Search atau scan barcode
3. **Tambah ke Keranjang**: Set quantity
4. **Checkout**: Pilih metode pembayaran
5. **Print Nota**: Otomatis print ke thermal printer

## Printer Setup

### Supported Printers
- Thermal printer 58mm
- Bluetooth connection
- ESC/POS compatible

### Setup Steps
1. Pair printer dengan Android device
2. Printer akan otomatis terdeteksi
3. Test print dari menu Settings

## Troubleshooting

### Database Issues
```bash
# Clear app data
adb shell pm clear com.kasirtoko
```

### Sync Issues
- Check internet connection
- Verify Supabase credentials
- Check device date/time

### Printer Issues
- Ensure Bluetooth is enabled
- Re-pair printer if needed
- Check printer paper and battery

## Contributing

1. Fork repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## License

This project is licensed under the MIT License - see LICENSE file for details.

## Support

For support, email: support@kasirtoko.com
```
