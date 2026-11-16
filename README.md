# PastiSiap Backend

Backend modular monolith berbasis Spring Boot 4.1.2 dan Java 25, mengikuti PRD PastiSiap v2.0. Kontrak API dan aturan domain yang menjadi acuan ada di [PRD PastiSiap](https://skorin-flow-lab.annualgnome.chatgpt.site/).

## Status implementasi

Repo awal hanya berisi skeleton Spring Boot. Perubahan setup ini menambahkan fondasi runtime, konfigurasi, migrasi skema awal, security dasar, penanganan error, dokumentasi API, serta endpoint health. Ini belum berarti seluruh flow PRD sudah menjadi fitur backend.

| Area | Status di repo saat ini |
|---|---|
| Runtime Java 25, Spring MVC, validasi, PostgreSQL | Setup tersedia |
| Migrasi Flyway dan schema PRD | V1 membuat schema inti; V2 melengkapi entitas ERD dan constraint relasi |
| Clean architecture | `modules/<domain>/{domain,application,adapter}` dan `shared` |
| Health | `GET /api/v1/health` dan Actuator health |
| CSRF | `GET /api/v1/csrf` mengeluarkan token session untuk mutasi |
| Identity | Registrasi Customer, login/logout session, CSRF, dan `GET /me` tersedia; reset password, verifikasi email, rate limit, dan pemberian role internal belum tersedia |
| Katalog soal, sesi ujian, scoring, pembayaran, impor PDF/OCR, leaderboard, admin | Dependency/skema sebagian disiapkan; logic dan endpoint belum selesai |
| API dan worker terpisah | Compose saat ini menjalankan API saja; worker/job processor perlu diimplementasikan sebelum diaktifkan |

Endpoint bisnis baru sebaiknya mengikuti kontrak prefix `/api/v1` dalam bagian “API & batas transaksi” PRD. Untuk menjaga integritas, attempt, kuota WIB, finalisasi, webhook payment, dan publikasi konten harus dibuat sebagai transaksi idempoten dengan constraint database. Jangan menganggap kolom/tabel yang sudah ada sebagai implementasi aturan bisnis tersebut.

## Tool dan dependency

- JDK 25 dan Maven Wrapper (`mvnw.cmd` disertakan).
- PostgreSQL 17 untuk database lokal; Flyway mengelola skema dari `src/main/resources/db/migration`.
- Docker Desktop/Compose opsional, memakai `compose.yaml` dan `Dockerfile`.
- Tesseract OCR dengan model bahasa Indonesia dan Inggris dipasang pada image backend. Saat pemrosesan PDF dibangun, PDFBox menangani teks/render dan OCR harus dijalankan oleh worker, bukan thread request.
- springdoc-openapi 3.2.x dipakai untuk Spring Boot 4.1.x; PDFBox 3.0.8 dipakai untuk ekstraksi/render PDF.
- Spring Security memakai sesi server dan proteksi CSRF. Registrasi/login Customer sudah tersedia; role-based authorization lengkap dan role internal belum selesai.
- Springdoc menyediakan Swagger UI pada `/swagger-ui.html` dan dokumen OpenAPI pada `/api-docs`.

## Menjalankan dengan Docker Compose (cara termudah)

1. Pasang Docker Desktop dan pastikan Docker Engine berjalan.
2. Dari root repo, salin konfigurasi contoh:

   ```powershell
   Copy-Item .env.example .env
   ```

3. Jalankan PostgreSQL dan API:

   ```powershell
   docker compose up --build
   ```

   Compose menunggu database sehat sebelum memulai API. Flyway membuat skema saat aplikasi startup.
4. Buka `http://localhost:8080/api/v1/health`. Swagger tersedia di `http://localhost:8080/swagger-ui.html`.
5. Hentikan dengan `Ctrl+C`, lalu:

   ```powershell
   docker compose down
   ```

Data PostgreSQL tersimpan di named volume `postgres_data`; file yang diunggah disimpan di folder `storage`. Untuk menghapus volume database lokal beserta datanya, jalankan `docker compose down -v`.

Nilai `.env.example` hanya untuk development lokal. Jangan gunakan password contoh atau `COOKIE_SECURE=false` pada deployment HTTPS.

## Menjalankan tanpa Docker

### Prasyarat

- JDK 25 (JDK, bukan JRE).
- PostgreSQL 17 dan database/user `pastisiap`.
- Tesseract dengan paket `ind` dan `eng` hanya diperlukan ketika flow OCR diimplementasikan.

Atur koneksi database pada PowerShell untuk terminal saat ini:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/pastisiap"
$env:DB_USERNAME = "pastisiap"
$env:DB_PASSWORD = "password-lokal-anda"
$env:COOKIE_SECURE = "false"
```

Jalankan aplikasi dari root repository:

```powershell
java -version
.\mvnw.cmd spring-boot:run
```

`java -version` harus menunjukkan 25. Jika `JAVA_HOME` atau `Path` masih mengarah ke Java 18, atur keduanya ke instalasi JDK 25 lalu buka terminal baru. Migrasi Flyway berjalan otomatis saat startup.

## Endpoint yang tersedia sekarang

| Method | Path | Kegunaan |
|---|---|---|
| `GET` | `/api/v1/health` | Status aplikasi dan waktu UTC |
| `GET` | `/api/v1/csrf` | Token CSRF untuk client sebelum mengirim mutasi berbasis session |
| `POST` | `/api/v1/auth/register` | Registrasi akun Customer dan hash password BCrypt |
| `POST` | `/api/v1/auth/login` | Login dan pembentukan session server |
| `POST` | `/api/v1/auth/logout` | Logout session (wajib CSRF token) |
| `GET` | `/api/v1/me` | Profil akun yang sedang login |
| `GET` | `/actuator/health` | Health check deployment, detail komponen disembunyikan |
| `GET` | `/swagger-ui.html` | Swagger UI |
| `GET` | `/api-docs` | Spesifikasi OpenAPI |

Contoh:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
```

Endpoint mutasi yang memakai session harus mengirim CSRF header yang dikembalikan oleh `GET /api/v1/csrf` dan mempertahankan cookie session. Registrasi saat ini hanya dapat membuat role `CUSTOMER`; role internal belum memiliki jalur provisioning.

### Postman

Import [`postman/PastiSiap-Backend.postman_collection.json`](postman/PastiSiap-Backend.postman_collection.json) ke Postman. URL default collection adalah `http://localhost:8080`. Untuk alur session, jalankan request secara berurutan: **Get CSRF Token → Register Customer → Login → Get current profile → Logout**. Postman menyimpan cookie session otomatis. Collection hanya mencakup endpoint yang sudah tersedia pada backend saat ini.

## Struktur kode

```text
src/main/java/com/pasti_bisa/backend/
  modules/<domain>/
    domain/       # aturan dan model domain, tanpa ketergantungan framework
    application/  # use case dan port
    adapter/in/   # controller/consumer masuk
    adapter/out/  # database, filesystem, provider eksternal
  shared/         # konfigurasi lintas modul, security, error API
src/main/resources/
  db/migration/   # migrasi skema Flyway yang immutable
```

Modul yang dituju dari PRD: `identity`, `catalog-content`, `exam`, `assessment`, `commerce`, dan `operations`. Modul saling berkomunikasi lewat use case/port, bukan mengakses repository modul lain secara langsung.

## Skema database dan Flyway

Flyway menerapkan migration secara berurutan saat aplikasi mulai. `V1__initial_schema.sql` membuat tabel identitas, katalog, attempt, commerce, file/job, dan audit yang menjadi fondasi awal. `V2__complete_prd_schema.sql` menambahkan profil/target, topik dan kandidat impor, aset soal, scoring policy, form ujian, subscription/leaderboard, token identity, snapshot transaksi, indeks, foreign key, dan trigger integritas. Migration lama dipertahankan; perubahan skema berikutnya harus ditambahkan sebagai `V3__...sql` atau nomor berikutnya, bukan mengedit migration yang sudah pernah dijalankan.

Untuk memeriksa status migration di PostgreSQL, jalankan:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Catatan konfigurasi

- Timestamp disimpan dalam UTC. Kuota tryout harian harus dihitung eksplisit memakai zona `Asia/Jakarta`.
- Jangan mengaktifkan `ddl-auto=update`; perubahan database dibuat lewat migrasi Flyway baru.
- File upload harus disimpan di storage privat; jangan menyimpan PDF biner dalam PostgreSQL atau memasukkan rahasia ke payload job/log.
- Gunakan `COOKIE_SECURE=true` di balik HTTPS. Reverse proxy dan frontend same-origin belum termasuk dalam Compose development ini.
- Gateway pembayaran, SMTP, backup off-host, Caddy, frontend React, serta alert produksi belum dikonfigurasi karena vendor/credential/deployment belum disediakan.

## Berikutnya untuk mencapai flow PRD

Urutan implementasi yang disarankan: identity + session/CSRF/RBAC; katalog dan versi konten; transaksi start-attempt/quota/autosave/finalisasi; worker PostgreSQL dengan lease/SKIP LOCKED dan scoring idempoten; order/webhook/entitlement melalui adapter gateway; impor PDF/OCR dan review petugas; leaderboard; lalu admin/audit, backup dan deployment VPS. Setiap flow perlu integration test PostgreSQL dan uji concurrency sebelum dinyatakan siap produksi.
