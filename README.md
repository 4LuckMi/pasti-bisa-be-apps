# PastiSiap Backend

Backend modular monolith berbasis Spring Boot 4.1.2 dan Java 25, mengikuti PRD PastiSiap v2.0. Kontrak API dan aturan domain yang menjadi acuan ada di [PRD PastiSiap](https://skorin-flow-lab.annualgnome.chatgpt.site/).

Dokumentasi lengkap tersedia di [dokumentasi teknis backend](docs/backend/README.md): arsitektur/flowchart,
ERD dan kamus database, status fitur/roadmap, serta setiap endpoint beserta request/response dan validasinya.
Tersedia juga [versi HTML offline](docs/backend/backend-documentation.html) dengan diagram yang sudah dirender.

## Status implementasi

Backend sudah mencakup login/register, katalog, lifecycle tryout/practice, penyimpanan jawaban, scoring asinkron,
bank soal terversi, serta upload PDF teks/scan dengan review kandidat. Riset, keputusan dan urutan implementasi
tersedia di [rencana arsitektur](docs/learning-architecture-plan.md). Pembayaran dan pengelolaan blueprint lewat UI/API admin belum dikerjakan.

| Area | Status di repo saat ini |
|---|---|
| Runtime Java 25, Spring MVC, validasi, PostgreSQL | Setup tersedia |
| Migrasi Flyway dan schema PRD | V1–V6; V6 menambahkan metadata impor dan guard lifecycle/snapshot |
| Clean architecture | `modules/<domain>/{domain,application,adapter}` dan `shared` |
| Health | `GET /api/v1/health` dan Actuator health |
| CSRF | `GET /api/v1/csrf` mengeluarkan token session untuk mutasi |
| Identity | Register/login password dan Google OAuth2/OIDC, profil dan target siswa, session logout dan CSRF; reset password, rate limit, dan provisioning role internal belum tersedia |
| Katalog, tryout, practice | Detail dan kesiapan pool, start/resume idempoten, autosave revision, deadline, finalize, hasil/review |
| Bank soal dan PDF/OCR | Upload privat, worker PDFBox/Tesseract, review kandidat, draft, publish, versi baru, arsip |
| Scoring | BINARY/OPTION_WEIGHT; indeks latihan terversi, bukan skor resmi/IRT; leaderboard kompetisi tidak diisi dari sesi acak |
| Dummy | Profil `demo`: 4 kategori/katalog, 48 soal matematika contoh dan PDF contoh 2 soal |
| API dan worker terpisah | Compose menjalankan PostgreSQL, API dan worker job persisten dengan lease/retry |
| Pembayaran/admin | Skema dasar dan beberapa read API; belum ada integrasi gateway atau manajemen admin lengkap |

Endpoint bisnis baru sebaiknya mengikuti kontrak prefix `/api/v1` dalam bagian “API & batas transaksi” PRD. Untuk menjaga integritas, attempt, kuota WIB, finalisasi, webhook payment, dan publikasi konten harus dibuat sebagai transaksi idempoten dengan constraint database. Jangan menganggap kolom/tabel yang sudah ada sebagai implementasi aturan bisnis tersebut.

## Tool dan dependency

- JDK 25 dan Maven Wrapper (`mvnw.cmd` disertakan).
- PostgreSQL 17 untuk database lokal; Flyway mengelola skema dari `src/main/resources/db/migration`.
- Flyway memakai `spring-boot-starter-flyway` dan modul PostgreSQL `flyway-database-postgresql`, agar integrasi auto-configuration Spring Boot 4 berjalan saat aplikasi startup.
- Docker Desktop/Compose opsional, memakai `compose.yaml` dan `Dockerfile`.
- Tesseract OCR dengan model bahasa Indonesia dan Inggris dipasang pada image backend. PDFBox membaca teks/render; worker menjalankan OCR pada halaman dengan teks minim, di luar thread request.
- springdoc-openapi 3.2.x dipakai untuk Spring Boot 4.1.x; PDFBox 3.0.8 dipakai untuk ekstraksi/render PDF.
- Spring Security memakai sesi server dan proteksi CSRF. Email/password dan Google OAuth tersedia. Registrasi publik selalu membuat user type `SISWA` dengan role `CUSTOMER`; role `QUESTION_STAFF` dan `PLATFORM_ADMIN` sudah menjadi kode role database, tetapi belum ada provisioning admin/petugas.
- Google OAuth2/OIDC tersedia sebagai pilihan selain email/password. Atur `GOOGLE_CLIENT_ID` dan `GOOGLE_CLIENT_SECRET` dari Google Cloud Console; callback lokal default `http://localhost:8080/login/oauth2/code/google` harus didaftarkan sebagai Authorized redirect URI. `OAUTH_SUCCESS_REDIRECT` mengatur halaman tujuan setelah login berhasil (default `/`).
- Springdoc menyediakan Swagger UI pada `/swagger-ui.html` dan dokumen OpenAPI pada `/api-docs`.

## Menjalankan dengan Docker Compose (cara termudah)

1. Pasang Docker Desktop dan pastikan Docker Engine berjalan.
2. Pada setup pertama, jika `.env` belum ada, salin konfigurasi contoh (pertahankan `.env` lama jika sudah berisi kredensial):

   ```powershell
   Copy-Item .env.example .env
   ```

3. `SPRING_PROFILES_ACTIVE=demo` mengaktifkan katalog/soal dummy dan login email/password. Jika Google SSO sudah dikonfigurasi, gunakan `SPRING_PROFILES_ACTIVE=oauth,demo`. Untuk database tanpa fixture demo, hilangkan `demo` dari profil. Menonaktifkan profil tidak menghapus fixture yang sudah tersimpan. Profil `oauth` memerlukan kedua kredensial Google.

4. Jalankan PostgreSQL, API dan worker:

   ```powershell
   docker compose up --build -d
   docker compose logs -f api worker
   ```

   Compose menunggu database sehat. Flyway menerapkan migration otomatis; profil demo menambah fixture idempoten. Worker memproses scoring, deadline dan impor PDF. Login ulang setelah API dibangun ulang karena session masih disimpan di memori.
5. Buka `http://localhost:8080/api/v1/health`. Swagger tersedia di `http://localhost:8080/swagger-ui.html`.
6. Hentikan layanan dengan:

   ```powershell
   docker compose down
   ```

Data PostgreSQL tersimpan di named volume `postgres_data`; file yang diunggah disimpan di folder `storage`. `docker compose down` mempertahankan data tersebut.

Nilai `.env.example` hanya untuk development lokal. Jangan gunakan password contoh atau `COOKIE_SECURE=false` pada deployment HTTPS.

### Build dan unduhan dependency

Perintah untuk membangun sekaligus menjalankan adalah `docker compose up --build`, bukan `docker compose build --up`.
Build pertama mengunduh image Maven/JDK 25 untuk kompilasi, image JRE 25 untuk runtime, library dan plugin Maven,
serta Tesseract dan model OCR bahasa Indonesia/Inggris. Compose juga mengunduh PostgreSQL jika image belum ada.
Maven mengompilasi kode dan membungkusnya menjadi JAR; sesudah container mulai, Flyway menerapkan migrasi database.

Dockerfile memakai cache Maven `/root/.m2` agar perubahan source tidak memerlukan unduhan ulang semua dependency.
Penggantian builder, penghapusan cache, atau perubahan dependency dapat memicu unduhan lagi. Build pertama setelah
penambahan cache ini mengisi cache baru. Langkah `dependency:go-offline` sudah dilepas sehingga build langsung
menjalankan `package`; `-ntp` mengurangi tampilan progres unduhan, sedangkan fase build tetap dicetak.

Setelah source berubah, jalankan:

```powershell
docker compose up --build -d
docker compose logs -f api worker
```

Untuk menjalankan image yang sudah dibangun tanpa perubahan source, cukup `docker compose up -d`.
Java 25 tersedia di container sehingga Java lokal tidak perlu diganti saat menjalankan lewat Compose.

Jika startup melaporkan bean `com.fasterxml.jackson.databind.ObjectMapper` tidak ditemukan, pastikan image sudah
dibangun ulang dari source terbaru. Spring Boot 4 memakai Jackson 3 (`tools.jackson.databind.json.JsonMapper`);
controller soal menggunakan mapper tersebut dan node JSON dari paket `tools.jackson.databind`.

## Menjalankan tanpa Docker

### Prasyarat

- JDK 25 (JDK, bukan JRE).
- PostgreSQL 17 dan database/user `pastisiap`.
- Tesseract dengan paket `ind` dan `eng` diperlukan pada mesin yang menjalankan worker OCR.

Atur koneksi database pada PowerShell untuk terminal saat ini:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/pastisiap"
$env:DB_USERNAME = "pastisiap"
$env:DB_PASSWORD = "password-lokal-anda"
$env:COOKIE_SECURE = "false"
```

Isi juga `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, dan `OAUTH_REDIRECT_URI` seperti konfigurasi OAuth lokal di atas. Jalankan aplikasi dari root repository:

```powershell
java -version
.\mvnw.cmd spring-boot:run
```

Untuk memproses job tanpa Compose, jalankan proses aplikasi kedua dengan profil `worker` dan koneksi database/storage yang sama:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=worker
```

`java -version` harus menunjukkan 25. Jika `JAVA_HOME` atau `Path` masih mengarah ke Java 18, atur keduanya ke instalasi JDK 25 lalu buka terminal baru. Migrasi Flyway berjalan otomatis saat startup.

### Masa berlaku login

Session memakai idle timeout `SESSION_TIMEOUT=30m`: 30 menit tanpa request yang memakai session tersebut.
Request aktif memperbarui waktu aktivitas session. Ini berbeda dengan masa berlaku access token Google; login aplikasi
menggunakan cookie session. Nilai `SESSION_TIMEOUT` bisa diatur di `.env` (misalnya `30m` atau `1h`), lalu
terapkan dengan `docker compose up -d`. Mengubah timeout tidak membuat session lama bertahan melewati restart.

Saat ini session ada di memori API, sehingga restart/recreate container menghapus session dan pengguna harus login lagi.
Cookie browser dan Postman juga terpisah kecuali disinkronkan. `401` berarti belum terautentikasi, sedangkan
`403 CSRF_INVALID` berarti token CSRF tidak sesuai dan belum tentu berarti logout. Request `POST /auth/logout`
menghapus session segera; jalankan folder logout paling akhir saat mencoba collection. Persistence session lintas
restart belum dikonfigurasi; dapat memakai Spring Session JDBC/Redis bila menjadi kebutuhan produk.

## Endpoint yang tersedia sekarang

| Method | Path | Kegunaan |
|---|---|---|
| `GET` | `/api/v1/health` | Status aplikasi dan waktu UTC |
| `GET` | `/api/v1/csrf` | Token CSRF untuk client sebelum mengirim mutasi berbasis session |
| `POST` | `/api/v1/auth/register` | Membuat user SISWA, login otomatis, mengembalikan user tanpa data sensitif |
| `POST` | `/api/v1/auth/login` | Login email/password, session cookie, dan ringkasan user |
| `POST` | `/api/v1/auth/logout` | Logout session (wajib CSRF token) |
| `GET` | `/api/v1/users/current` | Profil akun yang sedang login |
| `GET` | `/api/v1/users/current/profile` | Profil siswa, sekolah/kelas, target, dan skor sasaran |
| `PATCH` | `/api/v1/users/current/profile` | Memperbarui profil dan target belajar siswa |
| `GET` | `/api/v1/tryouts?category=UTBK` | Katalog tryout, jumlah soal, dan ringkasan riwayat per paket |
| `GET` | `/api/v1/practice/topics?category=UTBK` | Daftar topik latihan dan jumlah soal terbit |
| `GET` | `/api/v1/users/current/tryout-quota` | Kuota tryout harian berdasarkan tanggal WIB |
| `GET` | `/api/v1/users/current/attempts` | Riwayat sesi tryout milik siswa |
| `GET` | `/api/v1/users/current/subscription` | Status dan periode langganan siswa |
| `GET` | `/api/v1/leaderboard?category=UTBK` | Peringkat cohort aktif untuk siswa yang ikut leaderboard |
| `GET` | `/api/v1/users/current/attempts/{attemptId}/questions/{position}` | Isi soal dalam sesi milik siswa; kunci dan pembahasan hanya setelah sesi dinilai |
| `GET` | `/api/v1/users/current/attempts/{attemptId}/questions/{position}/assets/{assetId}` | Membaca asset privat soal setelah kepemilikan sesi diverifikasi |
| `GET` | `/oauth2/authorization/google` | Memulai login/registrasi Google OAuth2/OIDC |
| `GET` | `/login/oauth2/code/google` | Callback OAuth Google (ditangani Spring Security) |
| `GET` | `/actuator/health` | Health check deployment, detail komponen disembunyikan |
| `GET` | `/swagger-ui.html` | Swagger UI |
| `GET` | `/api-docs` | Spesifikasi OpenAPI |

Endpoint akun yang sedang login memakai prefix `/api/v1/users/current`. Identitas pengguna diambil dari
session login, sehingga frontend tidak perlu mengirim user ID. Gunakan prefix ini juga untuk profil,
kuota, subscription dan attempt; import ulang collection Postman setelah pembaruan endpoint.

Contoh:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
```

Register/login biasa menerima JSON; contoh register: `{"email":"siswa@example.com","displayName":"Nama Siswa","password":"PasswordAman123!"}`. Login menerima `email` dan `password`. Keduanya mengembalikan `{ "user": { "id", "email", "displayName", "userType", "roles" } }` dan membuat session cookie. Password minimal 12 karakter. Untuk Google SSO arahkan browser ke `http://localhost:8080/oauth2/authorization/google`; akun baru mendapat type `SISWA` dan role `CUSTOMER`. Endpoint mutasi tetap mengirim CSRF header dari `GET /api/v1/csrf`; endpoint logout memerlukan token dan cookie session.

### Postman

Import [`postman/PastiSiap-Backend.postman_collection.json`](postman/PastiSiap-Backend.postman_collection.json) ke Postman. Collection memuat auth, profil, katalog, lifecycle tryout/practice dan bank soal/impor. Jalankan **Get CSRF Token** sebelum register/login, lalu katalog/topik untuk mengisi ID otomatis. Google SSO tetap dimulai melalui browser; cookie session harus tersedia di cookie jar Postman. Panduan urutan uji ada di [panduan learning API](docs/learning-api.md).

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

### DTO dan akses database

Alur kode siswa sekarang: **Controller → Service → Repository interface → JDBC repository**.
Controller memvalidasi request DTO dan mengembalikan response DTO. Service menjalankan aturan aplikasi dan transaksi;
repository di `adapter/out/persistence` menyimpan SQL serta mapping baris database. SQL memakai parameter bernama
seperti `:email`, `:targetId`, dan `:category`, dengan UUID dikirim sebagai UUID. Tidak ada SQL di controller.

- `identity/application/dto`: `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserProfileResponse`, serta DTO profil/target.
- `exam/application/dto/StudentLearningDtos`: query kategori, response tryout, topik, kuota, riwayat, subscription, dan leaderboard.
- `exam/application/dto/AttemptQuestionDtos`: query milik sesi, response soal/opsi, dan projection database internal.
- Response JSON mempertahankan nama field sebelumnya. DTO Java memakai camelCase; `@JsonProperty` menjaga field lama
  seperti `question_count` dan `latest_score`. Record kredensial tidak mencetak password melalui `toString()`.
- `stem` dan `content` tetap `JsonNode` untuk konten soal yang terstruktur; metadata dan envelope response bertipe eksplisit.
- Penyimpanan file memakai port `QuestionAssetStorage`; pemeriksaan path filesystem berada di adapter storage lokal.

Refactor DTO tidak mengubah skema database dan tidak memerlukan migration baru.

## Skema database dan Flyway

Flyway menerapkan migration secara berurutan saat aplikasi mulai. `V1` membuat tabel inti, `V2` melengkapi domain PRD, `V3` menambahkan OAuth, `V4` menambahkan user type, `V5` melengkapi profil/target, dan `V6` menambahkan metadata impor, mode katalog/practice, request hash dan guard snapshot/jawaban. Migration lama dipertahankan; perubahan selanjutnya memakai V7 atau berikutnya.

Dengan Docker Compose, jalankan atau bangun ulang API agar Flyway memproses migration yang ada di image:

```powershell
docker compose up --build -d
docker compose logs -f api
```

Cari log Flyway seperti `Successfully applied ... migration` atau `Successfully validated ...`. Jika lokasi folder migration tidak ditemukan, API sekarang berhenti saat startup agar masalah konfigurasi terlihat jelas. Memulai ulang API akan memeriksa migration lagi; versi yang sudah tercatat tidak dijalankan ulang.

Untuk memeriksa status migration di PostgreSQL, jalankan:

```powershell
docker compose exec postgres psql -U pastisiap -d pastisiap -c "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Jika `flyway_schema_history` tidak ditemukan, Flyway belum berjalan pada database tersebut. Jika riwayat menyatakan V1 sukses tetapi tabel `app_user` tidak ada, pastikan API dan perintah `psql` mengarah ke database/schema yang sama; jangan hapus volume sebelum memeriksa datanya.

## Catatan konfigurasi

### Melacak request dan error API

Setiap request mendapat `X-Request-ID` dari server. Untuk error JSON, `requestId` dalam body sama dengan header
dan log. Log mencatat method, path tanpa query string, HTTP status, durasi, serta kode error. Response 4xx dicatat
sebagai WARN dan 5xx sebagai ERROR dengan stack trace penyebabnya. Error validasi, login, akses, dan CSRF juga
memakai format error API yang sama. Log tidak mencatat body, Authorization, cookie, token CSRF, atau query OAuth.

```powershell
docker compose logs -f api
docker compose logs --since 10m api | Select-String -SimpleMatch 'REQUEST_ID_DARI_RESPONSE' -Context 0,20
```

Contoh log: `requestId=... method=PATCH path=/api/v1/users/current/profile status=500 durationMs=46 code=INTERNAL_ERROR`.
Detail internal hanya ada di log server; response tetap berisi pesan umum. Pengecekan log dan CSRF dapat dijalankan
dengan `docker compose build --build-arg MAVEN_TEST_ARGS=-Dtest=ApiRequestLoggingTests api`.

- Timestamp disimpan dalam UTC. Kuota tryout harian harus dihitung eksplisit memakai zona `Asia/Jakarta`.
- Jangan mengaktifkan `ddl-auto=update`; perubahan database dibuat lewat migrasi Flyway baru.
- File upload harus disimpan di storage privat; jangan menyimpan PDF biner dalam PostgreSQL atau memasukkan rahasia ke payload job/log.
- Gunakan `COOKIE_SECURE=true` di balik HTTPS. Reverse proxy dan frontend same-origin belum termasuk dalam Compose development ini.
- Gateway pembayaran, SMTP, backup off-host, Caddy, frontend React, serta alert produksi belum dikonfigurasi karena vendor/credential/deployment belum disediakan.

## Verifikasi learning API

Tes memakai PostgreSQL terpisah di memori sementara, Java 25 dan OCR sungguhan; tidak memakai database development:

```powershell
docker compose -p pastisiap-verify -f compose.verify.yaml up --build --abort-on-container-exit --exit-code-from verify
docker compose -p pastisiap-verify -f compose.verify.yaml down
```

Tes mencakup Flyway, fixture, start bersamaan/idempoten, kuota, revision, deadline, kepemilikan, versi immutable,
scoring, lease worker, upload/publish, PDF teks/scan dan pembatasan storage. Lihat [alur dan endpoint](docs/learning-api.md).

Berikutnya: manajemen paket/blueprint dan akun internal, gateway pembayaran, recovery session, deployment/backup,
serta uji kualitas parser dengan PDF nyata. Rumus, tabel, gambar dan layout kompleks tetap memerlukan review petugas.
