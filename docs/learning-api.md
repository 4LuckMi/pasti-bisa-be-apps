# Panduan katalog, tryout, practice dan bank soal

## Menjalankan dummy

Set `.env` ke `SPRING_PROFILES_ACTIVE=demo`, atau `oauth,demo` jika Google SSO sudah dikonfigurasi.
Jalankan `docker compose up --build -d`, kemudian `docker compose logs -f api worker`.
Java 25 sudah tersedia di image Docker; Java 18 pada laptop tidak dipakai container.

Profil demo menambahkan 4 katalog (UTBK, CPNS, Kedinasan, Sekolah), masing-masing 12 soal aritmetika
dan tryout 3 soal. Seluruhnya bertanda contoh, bukan soal/skor resmi masing-masing kategori.
Fixture idempoten dan tidak menghapus data yang sudah ada. PDF 2 soal dibuat di
`storage/demo/sample-questions.pdf`; template teksnya ada di [sample-questions.txt](sample-questions.txt).
Tidak ada akun petugas/admin atau password bawaan.

## Struktur penyimpanan

| Rangkaian tabel | Tanggung jawab |
|---|---|
| `exam_category → exam_type → subtest → topic` | Taksonomi dengan relasi lingkup yang divalidasi |
| `question → question_version → question_option` | Identitas soal, isi/kunci/pembahasan dan versi immutable setelah terbit |
| `blueprint → blueprint_version → blueprint_section` | Katalog, durasi, jumlah/filter per subtes dan policy penilaian |
| `attempt → attempt_item → answer` | Sesi pengguna, snapshot ID versi + urutan opsi, jawaban/revision |
| `quota_day → quota_usage` | Maksimal 2 start tryout per hari WIB; practice tidak memakai kuota |
| `scoring_policy → score_run → score_section` | Policy terversi dan hasil yang dapat direproduksi |
| `stored_file → import_batch → import_candidate` | Metadata PDF, provenance halaman, hasil ekstraksi dan koreksi |
| `job` | Antrean persisten untuk scoring dan impor, lease 15 menit, maksimal 5 percobaan |

File PDF berada di disk privat dengan nama UUID, bukan biner di database. Database menyimpan path relatif,
ukuran, checksum SHA-256 dan pengunggah. Upload identik dalam lingkup pengunggah/subtes/topik yang sama
mengembalikan batch lama. Isi soal yang dibaca siswa berasal dari JSON versi yang dibekukan pada sesi.
File sumber dapat berisi kunci, sehingga hanya dapat diunduh petugas. Endpoint asset siswa mengecualikan
file `IMPORT_SOURCE`. Dukungan menempelkan gambar/asset baru lewat upload terpisah belum dibuat.

Controller memakai request/response DTO. Service mengatur transaksi dan use case; port repository/storage/PDF
memisahkan adapter JDBC, filesystem dan PDFBox. Parser, validasi konten dan kalkulator penilaian bekerja
dengan model domain, tanpa ketergantungan ke controller atau repository.

## Urutan mencoba sebagai siswa di Postman

1. Import collection terbaru; `baseUrl=http://localhost:8080`. Jalankan **Get CSRF Token** lalu register/login.
   Cookie jar harus aktif; semua mutasi mengirim token CSRF dari session yang sama.
2. Jalankan **Tryout catalog** dan **Practice topics**. Script mengisi `tryoutId` dan `topicId` dari kategori terpilih.
3. Jalankan detail, lalu **Start tryout**. Header `Idempotency-Key={{startKey}}` dibuat otomatis jika kosong.
   Key yang sama + body sama mengembalikan sesi yang sama; ganti/kosongkan key untuk sesi baru.
   Bila masih ada sesi aktif, server mengembalikannya agar tidak ada dua sesi paralel.
4. Baca nomor dengan **Current question and answer revision**. Script mengisi contoh opsi dan revision.
   **Save answer** mengirim `selectedOptionId`, `flagged`, `expectedRevision`. Revision awal 0; sukses menjadi 1.
   Revision stale mendapat `409 REVISION_CONFLICT`; baca ulang soal sebelum menyimpan lagi.
5. **Submit attempt** menghasilkan status `FINALIZING`. Poll hasil sampai `COMPLETED`; worker harus hidup.
   Hasil yang belum siap mempunyai `totalScore=null`, bukan nilai nol. Lalu buka review/pembahasan.
6. Untuk latihan, ganti `startKey` dan kirim topik/jumlah 1–50 lewat **Start practice**.
   Practice tidak menggunakan kuota tryout, tetapi tetap berbagi batas satu sesi aktif.

Jam server menentukan deadline. Worker memfinalisasi sesi kedaluwarsa. Abandon tetap memakai kuota;
hasil pribadi tidak masuk leaderboard kompetisi secara otomatis. Hasil/riwayat sendiri tetap bisa dibaca
sesuai PRD v2, termasuk saat langganan berakhir. Kunci/pembahasan tidak dikirim sebelum scoring selesai.
Mengubah/pengarsipan master tidak mengganti soal pada sesi lama.

## Endpoint lifecycle

Semua path di bawah berawalan `/api/v1` dan membutuhkan role `CUSTOMER`.

| Method | Path | Isi utama |
|---|---|---|
| GET | `/tryouts`, `/tryouts/{id}` | Katalog dan kesiapan stok per section |
| GET | `/practice/topics` | Pilihan topik terbit |
| GET | `/practice/topics/{id}` | Detail topik, `ready`, `defaultQuestionCount`, `maxQuestionCount`, `secondsPerQuestion` |
| POST | `/attempts` | `{ "tryoutId": "uuid" }`, wajib `Idempotency-Key` |
| POST | `/practice/attempts` | `{ "topicId": "uuid", "questionCount": 3 }`, wajib key |
| GET | `/users/current/attempts/active` | Resume sesi atau body kosong jika tidak ada |
| GET | `/users/current/attempts`, `/users/current/attempts/{id}` | Riwayat dan navigator/revision |
| GET | `/users/current/attempts/{id}/questions/{position}` | Isi nomor dan jawaban sendiri |
| PUT | `/users/current/attempts/{id}/answers/{position}` | `{ "selectedOptionId": "uuid atau null", "flagged": false, "expectedRevision": 0 }` |
| POST | `/users/current/attempts/{id}/finalize` | `{ "abandon": false }`; `{}` berarti submit biasa |
| GET | `/users/current/attempts/{id}/result` | Status scoring, metode, policy version dan hasil |
| GET | `/users/current/attempts/{id}/review` | Soal/kunci/pembahasan setelah `COMPLETED` |

`POOL_NOT_READY` tidak membuat attempt atau memotong kuota. Pemilihan hanya memakai versi terbit
dalam subtes/topik/tahun yang cocok; mengutamakan belum dilihat lalu paparan tertua. Soal yang sudah
dilihat pada hari WIB yang sama dikeluarkan dari pool tryout baru. Urutan opsi hanya diacak bila diizinkan.

## Kontrak untuk frontend tryout dan latihan

Riwayat menerima `mode=TRYOUT&tryoutId={id}` atau `mode=PRACTICE&topicId={id}`, dengan
`offset=0&limit=20` (default limit 100, maksimal 100). Filter yang bertentangan mendapat
`400 INVALID_HISTORY_FILTER`. Response tetap array dan selalu dibatasi ke akun yang login.
`tryout_id` pada latihan merupakan ID blueprint internal; gunakan `topic_id` untuk membuka detail topik.
Ringkasan katalog tryout mencakup seluruh versi blueprint, termasuk versi yang sudah tidak aktif.

Session membawa `context` (judul, kategori, jenis ujian, topik dan section dari versi sesi yang dibekukan),
serta `finalizedAt`. Soal membawa `scope` untuk nama subtes/topik. Result juga membawa `finalizedAt`,
sehingga frontend tidak perlu mencari waktu selesai di halaman riwayat. Bedakan `terminalReason`:
`SUBMITTED`, `TIMED_OUT`, dan `ABANDONED`. Pada riwayat, nama field tetap snake_case (`terminal_reason`).

Detail topik menentukan jumlah soal awal/batas stok: default maksimal 10, batas maksimal 50 atau stok
terbit yang tersedia, dan 120 detik per soal. Stok tetap divalidasi kembali secara transaksional saat start.
Penyesuaian kontrak ini tidak menambah tabel atau migrasi database.

## Upload dan publikasi petugas

Akun publik selalu `CUSTOMER`. Untuk uji lokal oleh operator database, daftarkan akun internal terpisah,
kemudian berikan role pada email yang dipilih secara eksplisit:

```powershell
Get-Content docs/provision-question-staff.sql | docker compose exec -T postgres psql -U pastisiap -d pastisiap -v staff_email='petugas@example.com' -f -
```

Periksa ID hasil perintah. Jika tidak ada baris, email tidak cocok/akun belum aktif. Sesuaikan database/user
bila konfigurasi berbeda. Login ulang untuk memperbarui role session. Ini tidak memberikan role admin.

1. **Get active content taxonomy** mengisi `subtestId` dan `topicId` yang valid.
2. **Upload PDF**: multipart `file` = PDF, `metadata` bertipe `application/json` berisi
   `subtestId`, `topicId` opsional, `sourceTitle`, `sourceYear` opsional. Jangan menetapkan header multipart
   secara manual karena Postman harus membuat boundary. Response `202` berisi batch ID.
3. Poll status lalu daftar kandidat. Worker membaca teks atau OCR `ind+eng` jika teks minim.
4. Periksa stem, opsi, kunci, pembahasan dan sumber halaman. Koreksi kandidat yang perlu input.
   Parser tidak menebak jawaban; beberapa kunci berbeda pun ditandai ambigu.
5. **Save candidate as draft**, periksa detail, lalu **Publish validated draft**. Ekstraksi tidak otomatis
   menerbitkan soal. Soal belum lengkap ditolak saat publish. Alternatif: **Create manual question draft**.
6. Perubahan konten setelah publish memakai **New question version** lalu publish versi baru.
   Versi lama dan snapshot sesi tetap utuh. Arsip mengeluarkan soal dari pool sesi baru.

Semua path berikut membutuhkan `QUESTION_STAFF` (role admin saja tidak otomatis membuka bank soal):

| Method | Path (prefix `/api/v1/staff`) |
|---|---|
| GET | `/taxonomy` |
| POST / GET | `/imports` |
| GET | `/imports/{id}`, `/imports/{id}/candidates`, `/imports/{id}/source` |
| PATCH | `/imports/{id}/candidates/{candidateId}` |
| POST | `/imports/{id}/candidates/{candidateId}/save` |
| POST / GET | `/questions` |
| GET | `/questions/{id}` |
| POST | `/questions/{id}/versions`, `/questions/{id}/publish`, `/questions/{id}/archive` |

List impor/kandidat/master mendukung `offset` dan `limit` 1–100. Master juga menerima `subtestId` dan `search`.
API admin kini dapat membuat katalog/blueprint terversi; fixture tetap tersedia untuk contoh awal.

## Endpoint internal tambahan

Semua endpoint admin membutuhkan `PLATFORM_ADMIN`, petugas membutuhkan `QUESTION_STAFF`. Daftar lengkap
beserta contoh tiap key ada pada [referensi API](backend/03-api-reference.md) dan collection Postman terbaru.

| Ruang | Endpoint utama | Perilaku |
|---|---|---|
| Petugas | `GET /staff/question-bank`, `/dashboard`, `/pool-requirements` | Filter lengkap, total pagination dan kebutuhan pasokan |
| Petugas | `GET /staff/questions/{id}/versions`, `/versions/{versionId}`, `/draft`; `PUT /draft` | Versi historis; edit DRAFT dengan expectedVersionId/expectedRevision |
| Petugas | `GET /staff/import-history`, `/audit`; `POST /staff/imports/{id}/retry`, `/save` | History tersaring, audit konten, retry gagal dan kandidat terpilih menjadi draft |
| Admin | `/admin/users`, `/users/{id}`, `/users/{id}/roles`, `/users/{id}/status`, `/roles` | Akun internal/dukungan/peran/status; alasan dan audit; admin terakhir dijaga |
| Admin | `/admin/taxonomy`, `/categories`, `/exam-types`, `/subtests`, `/topics` | Hierarki terkontrol; parent dan kode stabil |
| Admin | `/admin/plans`, `/plans/{id}/versions`, `/plans/{id}/availability`, `/scoring-policies` | Harga/manfaat/policy terversi |
| Admin | `/admin/blueprints`, `/blueprints/{id}/versions`, `/versions/{versionId}/readiness`, `/publish` | Komposisi versi baru, validasi pool, publish tanpa menimpa attempt lama |
| Admin | `/admin/dashboard`, `/orders`, `/orders/{id}`, `/subscriptions`, `/jobs`, `/audit` | Ringkasan dan metadata operasional tersensor |
| Admin | `POST /admin/jobs/{id}/retry`, `/subscriptions/{id}/revoke` | Retry FAILED saja; pencabutan grant beralasan |
| Admin | `POST /admin/orders/{id}/reconcile`, `/refunds` | Reconcile 503 sampai gateway tersedia; refund 202 hanya PENDING_PROVIDER |

List internal baru memakai `{items,total,offset,limit}` (maksimal 100); list lama tetap array.
Semua mutasi memerlukan CSRF. Perubahan role/status mencabut sesi password maupun Google lama;
pemulihan status tetap memerlukan login baru. Bootstrap admin pertama dijelaskan pada
[panduan operasional](backend/05-operations.md#bootstrap-admin-pertama).

## Batas pemrosesan dan troubleshooting

Upload maksimal 25 MB; PDF maksimal 100 halaman, tidak terenkripsi, signature PDF diperiksa. Render dibatasi
12 juta piksel/halaman; OCR maksimal 20 detik/halaman dan loop ekstraksi dibatasi 3 menit. Worker dibatasi
memori 768 MB. Job memakai lease/retry dan operasi idempoten; hasil worker dengan lease lama tidak menimpa
hasil pemilik lease baru. Crash setelah commit aman diproses ulang.

Parser awal ditujukan untuk template bernomor dengan opsi A–E, `Kunci: B`, `Pembahasan: ...`.
Layout multikolom, gambar, rumus dan tabel perlu koreksi manual; OCR tidak menjamin akurasi matematika.
Sumber scan nyata perlu diuji sebelum dipakai massal. Penyedia antivirus/CDR belum diintegrasikan.

- `401`: login ulang; restart API menghapus session dalam memori.
- `403 CSRF_INVALID`: ambil token CSRF dari session cookie yang sama.
- `403` pada `/staff`: periksa role lalu login ulang.
- Hasil `FINALIZING` atau batch `QUEUED`: periksa `docker compose logs -f worker`.
- Batch/job gagal: lihat `errorCode`, `jobStatus`, `jobErrorCode` dan log `jobId`.
- Error API: cocokkan `X-Request-ID` / `requestId` dengan log API.

Penilaian baseline mendukung BINARY atau OPTION_WEIGHT dengan agregasi SUM, skala terversi dan pembulatan
HALF_UP. Policy/rule lain ditolak sebelum start. Nilai adalah indeks latihan, bukan skor resmi UTBK/IRT.
