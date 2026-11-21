# Menjalankan, menguji dan mengoperasikan backend

[Indeks dokumentasi](README.md) · [README utama](../../README.md) · [Postman](../../postman/PastiSiap-Backend.postman_collection.json)

## Docker Compose development

Java 25 ada pada container build/runtime. Laptop dengan Java 18 tetap dapat menjalankan stack melalui Docker.
Jika menjalankan Maven langsung di laptop, gunakan JDK 25 dan PostgreSQL yang dapat diakses.

Pada setup pertama, salin `.env.example` hanya bila `.env` belum ada. Jangan menimpa file yang sudah berisi
kredensial Google. Contoh konfigurasi nonrahasia:

```dotenv
SERVER_PORT=8080
SESSION_TIMEOUT=30m
SPRING_PROFILES_ACTIVE=demo
COOKIE_SECURE=false
```

Google SSO memerlukan GOOGLE_CLIENT_ID/GOOGLE_CLIENT_SECRET pada server dan profil `oauth` atau `oauth,demo`.
Callback lokal yang dipakai kode adalah `http://localhost:8080/login/oauth2/code/google`; samakan dengan
redirect URI pada client Google. `OAUTH_SUCCESS_REDIRECT` adalah tujuan sesudah login, bukan callback Google.

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f api worker
```

API: `http://localhost:8080/api/v1/health`, Swagger: `http://localhost:8080/swagger-ui.html`,
OpenAPI: `http://localhost:8080/api-docs`. Worker diperlukan untuk scoring, expiry dan impor, bukan hanya API.

Untuk image yang sama tanpa source berubah: `docker compose up -d`.
Hentikan stack dengan `docker compose down`; named volume PostgreSQL dan bind mount storage tetap ada.
Jangan menghapus volume sebagai langkah umum memperbaiki migrasi.

Build pertama mengunduh image Java/Maven/PostgreSQL, library/plugin Maven, PDFBox serta Tesseract/model bahasa.
Perubahan source berikutnya memanfaatkan cache Maven dan layer image. Rebuild API juga membuat ulang proses
dan menghilangkan session yang masih disimpan di memori; frontend/Postman harus login ulang.

## Konfigurasi penting

| Variabel/properti | Default/use saat ini |
|---|---|
| DB_URL | URL JDBC; Compose memakai host service `postgres` |
| DB_NAME / DB_USERNAME / DB_PASSWORD | Konfigurasi database lokal; jangan ekspor nilai rahasia ke dokumentasi |
| SERVER_PORT | Host port default 8080 |
| SESSION_TIMEOUT | Idle timeout 30m |
| COOKIE_SECURE | false untuk HTTP lokal; true untuk deployment HTTPS |
| SPRING_PROFILES_ACTIVE | `demo`, `oauth`, kombinasi `oauth,demo`; worker diatur Compose tersendiri |
| GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET | Hanya dibaca server pada profil oauth |
| OAUTH_REDIRECT_URI | Callback Google ke backend |
| OAUTH_SUCCESS_REDIRECT | Redirect setelah session terbentuk, default `/` |
| STORAGE_ROOT | `./storage` langsung di host, `/app/storage` pada Compose |
| MAX_UPLOAD_BYTES | Batas adapter file default 26214400 byte; Compose belum meneruskan override ini secara eksplisit |
| IMPORT_MAX_PAGES | Default 100; Compose belum meneruskan override ini secara eksplisit |
| WORKER_POLL_INTERVAL | Default PT2S; Compose belum meneruskan override ini secara eksplisit |

Multipart HTTP juga mempunyai batas file 25MB dan request 26MB pada application.properties; mengubah batas
adapter saja tidak mengubah semua batas HTTP. OCR per halaman 20 detik, loop ekstraksi tiga menit dan render
12 juta piksel diimplementasikan dalam extractor. Lease 15 menit/retry delay 10 detik ditulis pada adapter queue.
`max_attempts` default 5 berasal dari kolom job; properti worker max-attempts yang ada belum dipakai saat insert/claim.

Worker diberi heap maksimum 512MB dan limit container 768MB. Performa serta kebutuhan memory PDF nyata
perlu diukur sebelum menaikkan throughput. Satu worker memproses satu job per tick; OCR panjang dapat menunda
scoring dan pemeriksaan expiry. Pemisahan antrean/kapasitas worker termasuk roadmap.

## Flyway

| Versi | Isi |
|---|---|
| V1 | Tabel inti akun, katalog, question/version, attempt, score, commerce, file, job/audit |
| V2 | Profil/target/topik/kandidat/asset/policy/form/subscription/leaderboard/token dan guard relasi |
| V3 | OAuth account, password boleh NULL untuk SSO |
| V4 | user_type default SISWA |
| V5 | Sekolah, kelas dan skor target |
| V6 | Mode/demo blueprint, idempotency hash/reason, metadata impor, lease token dan guard snapshot/answer/source |
| V7 | Revision sesi/draft, keluarga/revisi harga immutable, ledger permintaan refund dan index audit |

Migrasi berjalan otomatis saat startup aplikasi. File migration harus berada dalam JAR image terbaru.

```powershell
docker compose exec postgres psql -U pastisiap -d pastisiap -c "SELECT version,description,success FROM flyway_schema_history ORDER BY installed_rank;"
```

Sesuaikan database/user sesuai konfigurasi. Error relation tidak ada memerlukan pengecekan database/schema,
startup Flyway dan versi image; jangan menjalankan repair/baseline/reset secara membabi buta.

## Data demo dan role petugas

Profil demo memuat 4 kategori/katalog, 48 soal aritmetika generik dan PDF contoh di
`storage/demo/sample-questions.pdf`. Fixture idempoten; menonaktifkan profil tidak menghapus data yang sudah masuk.
Tidak ada akun internal/password default. Register publik tetap SISWA+CUSTOMER.

Untuk uji petugas lokal, operator memilih akun existing dan menjalankan provisioning yang eksplisit:

```powershell
Get-Content docs/provision-question-staff.sql | docker compose exec -T postgres psql -U pastisiap -d pastisiap -v staff_email='petugas@example.com' -f -
```

Script tidak membuat akun atau memberi PLATFORM_ADMIN. Unique role mencegah duplikasi. Jika role sudah ada,
`ON CONFLICT DO NOTHING` dapat menghasilkan nol baris juga; verifikasi user_role sebelum menyimpulkan email salah.
Login ulang setelah perubahan role; session lama dicabut oleh revision DB. Setelah admin pertama tersedia,
gunakan API admin untuk role/status/pembuatan akun internal agar alasan dan pelakunya tercatat.

### Bootstrap admin pertama

Tidak ada akun/password admin otomatis. Operator memilih akun existing ACTIVE, lalu menjalankan:

```powershell
Get-Content docs/provision-platform-admin.sql | docker compose exec -T postgres psql -U pastisiap -d pastisiap -v admin_email='admin@example.com' -f -
```

Ganti email contoh dengan akun yang memang ditunjuk. Pastikan hasil INSERT mengembalikan ID yang dimaksud;
tanpa baris berarti akun tidak ditemukan/nonaktif atau role sudah ada. SQL bootstrap ini tidak memberikan
QUESTION_STAFF. Login ulang, lalu gunakan folder **Platform admin - complete APIs** pada Postman.
`managedUserId` harus dipilih eksplisit; collection tidak mengisinya dengan admin yang sedang login.

`POST /api/v1/admin/users` membuat INTERNAL dengan password NULL dan role internal yang dipilih. Akun tersebut
masuk melalui Google verified email yang sama; belum ada pengiriman undangan atau pembuatan password oleh admin.
Untuk akun password existing, gunakan `PUT /admin/users/{id}/roles`. User type dan role adalah konsep terpisah.

### Menjalankan endpoint internal

1. Dari repo backend jalankan `docker compose up --build -d`; V7 berjalan otomatis saat API mulai.
2. Import ulang collection Postman, login akun berizin lalu ambil CSRF dari session/cookie yang sama.
3. Petugas: taxonomy → upload/status/candidates → koreksi → save draft → GET draft/revision → edit/publish.
4. Admin: taxonomy → scoring policy → blueprint → versi → readiness → publish. Kirim active version terakhir
   sebagai expectedActiveVersionId; publikasi pertama mengirim null. Revisi harga memakai ID plan baru.
5. Reconcile menghasilkan 503 PAYMENT_PROVIDER_NOT_CONFIGURED. Refund request 202/PENDING_PROVIDER merupakan
   kasus operasional saja; tidak memindahkan uang. Jangan menganggap response tersebut sebagai refund sukses.

Tidak perlu dependency eksternal baru untuk endpoint ini. Session tetap memori API; restart meminta login ulang.

## Pengujian

```powershell
docker compose -p pastisiap-verify -f compose.verify.yaml up --build --abort-on-container-exit --exit-code-from verify
docker compose -p pastisiap-verify -f compose.verify.yaml down
```

Suite memakai database tmpfs terpisah dan Maven/JDK 25 + OCR asli. Tidak menyentuh akun/database development.
28 tes yang ada dijelaskan di roadmap; hasil pengujian sebelumnya tidak berarti semua skenario produksi lulus.
Untuk perubahan dokumentasi/annotation, verifikasi tambahan memeriksa semua operasi runtime terdokumentasi,
semua contoh terhadap key DTO, referensi schema/link dan tidak adanya endpoint lama pada kontrak baru.

Urutan Postman: CSRF → register/login → profile/katalog/topik → start → soal → answer → finalize → poll result → review.
Jalankan logout terakhir. Set key start baru untuk sesi baru; gunakan key sama untuk retry request yang sama.
SSO Google harus diselesaikan lewat browser; Postman tidak menyelesaikan consent interaktif hanya dengan Send.

## Troubleshooting

| Gejala | Pemeriksaan |
|---|---|
| 401 setelah rebuild | Session di memori hilang; login ulang dan pastikan cookie jar benar |
| CSRF_INVALID | Ambil CSRF dengan cookie session yang sama; POST login/register juga dilindungi |
| 403 staff | Pastikan role QUESTION_STAFF, bukan hanya PLATFORM_ADMIN; login ulang |
| 409 pool/quota | Periksa kode error, tanggal WIB, stok terbit dan exposure pengguna |
| 409 revision | Fetch soal/navigator lagi dan gunakan revision terakhir |
| FINALIZING berkepanjangan | Periksa worker dan job; admin dapat retry job FAILED, bukan PROCESSING atau hasil yang sudah selesai |
| Batch QUEUED | Pastikan worker hidup dan storage mount sama |
| Batch FAILED | Periksa jobId/log extractor, PDF encrypted/limit/OCR dan file tersedia |
| 500 | Cocokkan requestId response dengan log; jangan hanya membaca ringkasan startup |
| Image registry/DNS error | Periksa koneksi/DNS/proxy Docker; ini berbeda dari compile error kode |

```powershell
docker compose logs --since 10m api | Select-String -SimpleMatch 'REQUEST_ID' -Context 0,20
docker compose logs --since 10m worker | Select-String -SimpleMatch 'JOB_ID' -Context 0,20
```

Tidak semua WARNING startup berarti aplikasi berhenti. Acuan proses: status container, health dan exception
yang mengikuti request/job terkait. Log normal tidak mencetak payload password, cookie atau secret OAuth.

## Sebelum deployment produksi

Pekerjaan yang masih perlu dibuat/diverifikasi: HTTPS dan reverse proxy, origin/CORS yang sesuai frontend,
session persistence dan revokasi, secret management, rate limits, monitoring/recovery job, backup/restore
PostgreSQL **dan** storage, retensi/orphan cleanup, kualitas parser PDF nyata dan scanner/CDR.
Jangan menganggap bind mount storage lokal otomatis merupakan backup off-host.

## Memperbarui dokumentasi

1. Review controller, DTO, service, migrasi dan konfigurasi yang berubah.
2. Perbarui snapshot runtime `/api-docs` setelah rebuild, tanpa menyalin secret/session atau baris pengguna.
3. Perbarui catatan/example pada `generate-reference.mjs`. Export ulang metadata skema jika ada migrasi baru.
4. Jalankan `node docs/backend/generate-reference.mjs`, lalu `node docs/backend/validate-docs.mjs`.
5. Review hasil Markdown, diagram dan schema; update Postman bila kontrak berubah.

Generator tidak melakukan deployment, migrasi atau mutasi data pengguna; hanya membaca snapshot JSON lokal
dan menulis dokumen turunan pada folder ini.

### Membangun ulang HTML dan SVG

Ini tooling dokumentasi opsional, bukan dependency backend. Memerlukan Node.js 22.12+ dan Chrome/Edge.
Library dipasang pada folder sementara agar tidak menambah node_modules ke repo:

```powershell
$renderRoot = Join-Path $env:TEMP 'pastisiap-doc-renderer'
npm install --prefix $renderRoot --no-save --package-lock=false --ignore-scripts --no-audit --no-fund mermaid@12.3.0 marked@18.2.1 puppeteer-core@25.18.1
node docs/backend/build-html.mjs
```

Renderer memakai profil browser headless terpisah, memvalidasi 14 diagram dengan engine Mermaid,
menyimpan SVG dan menggabungkan seluruh bab menjadi HTML offline. Library renderer tidak diperlukan
untuk membaca hasil HTML. Lokasi package/browser alternatif dapat diberikan sebagai dua argumen script.
