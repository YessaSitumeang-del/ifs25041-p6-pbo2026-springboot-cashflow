# Cashflow - Pencatat Transaksi Keuangan Pribadi

Aplikasi web pencatat pemasukan (`INCOME`) dan pengeluaran (`EXPENSE`) pribadi.
Stack: Java 25, Spring Boot 4.1.2, Vaadin 25.3.4, PostgreSQL (JDBC murni), Clean Architecture.

## Menjalankan (Windows PowerShell)

```powershell
# 1. Buat database sekali saja (sebagai superuser PostgreSQL)
psql -U postgres -f database.sql

# 2. Siapkan konfigurasi
Copy-Item .env.example .env      # lalu sesuaikan DB_*, ROOT_*, OPENAI_*, SESSION_SECRET

# 3. Jalankan
.\mvnw.cmd spring-boot:run       # http://localhost:8080 (atau APP_PORT)
```

Tabel `users` dan `transactions` dibuat otomatis saat start. Admin awal di-seed dari
`ROOT_USERNAME` / `ROOT_PASSWORD`.

## Uji & coverage

```powershell
.\mvnw.cmd clean verify          # unit test + Jacoco 100% (view & auth dikecualikan)
```

Unit test AI memakai server HTTP lokal / `InMemoryFinanceAiService`; tidak pernah memanggil server AI sungguhan.

## Konfigurasi `.env`

| Key | Keterangan |
|---|---|
| `APP_PORT` | Port aplikasi |
| `DB_HOST`, `DB_PORT`, `DB_DATABASE`, `DB_USERNAME`, `DB_PASSWORD` | Koneksi PostgreSQL (`pbo2026_cashflow`) |
| `ROOT_USERNAME`, `ROOT_PASSWORD` | Akun ADMIN awal |
| `UPLOAD_DIR` | Folder bukti transaksi & foto profil (disajikan di `/uploads/**`) |
| `SESSION_SECRET` | Kunci penanda tangan cookie sesi. Kosong = acak tiap start (user login ulang setelah restart). Isi >= 32 karakter acak di server agar sesi bertahan |
| `OPENAI_ENDPOINT`, `OPENAI_KEY`, `OPENAI_MODEL` | Layanan AI kompatibel OpenAI Chat Completions |

## Rute & hak akses

| Rute | View | Akses |
|---|---|---|
| `/login`, `/register` | LoginView, RegisterView | Publik (sudah login -> `/`) |
| `/` | FinanceView | USER & ADMIN |
| `/profile` | ProfileView | USER & ADMIN |
| `/users` | UsersView | ADMIN |
| `/user-transactions/{id}` | UserTransactionsView | ADMIN (read-only) |
| `/uploads/**`, `/icons/**`, `styles.css` | Aset statis | Publik |

## Struktur (dependency hanya mengarah ke dalam)

```
domain/      entity, exception, port (repository + AI)   <- tidak mengimpor Vaadin/Spring
usecase/     AuthUseCase, FinanceUseCase, FinanceAiUseCase, UserUseCase
adapter/     repository (JDBC + in-memory), ai, storage, presenter
config/      AppConfig (composition root)
framework/   auth (sesi), view (Vaadin)
```

`Jenkinsfile` adalah acuan pipeline dari dosen dan tidak boleh diubah.
