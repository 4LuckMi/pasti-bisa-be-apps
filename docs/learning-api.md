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
5. **Submit attempt** menghasilkan status `FINALIZING`. Poll hasil hanya ketika `processing.poll=true`, dengan jeda `retryAfterSeconds`; worker harus hidup.
   Hasil yang belum siap mempunyai `totalScore=null`, bukan nilai nol. Lalu buka review/pembahasan.
   Jika `processing.state=FAILED`, hentikan polling dan tampilkan pesan dukungan; admin dapat retry job gagal.
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

## Kontrak untuk FE terbaru — 21 November 2026

Backend menyediakan endpoint baru tanpa mengganti bentuk array pada endpoint lama. Semua endpoint pada
tabel berikut membutuhkan session dengan role CUSTOMER. Response baru memakai camelCase.

| Kebutuhan layar | Endpoint | Cara menghubungkan FE |
|---|---|---|
| Dashboard | `GET /api/v1/users/current/dashboard?category=UTBK&mode=TRYOUT` | Gunakan total, elapsedSeconds, currentStreakDays, recentActivity, quota dan membership dari server. Pilih scoreGroup untuk examType/policy yang sama. |
| Riwayat | `GET /api/v1/users/current/learning-history?category=UTBK&period=MONTH&limit=6&offset=0` | Render items dan total; filter/pagination dikirim ke server. |
| Katalog tryout | `GET /api/v1/catalog/tryouts?category=UTBK&ready=true&sort=NEWEST&limit=12&offset=0` | Render items; rekomendasi dashboard dapat memakai ready=true dan limit=3. |
| Katalog latihan | `GET /api/v1/catalog/practice-topics?category=UTBK&limit=12&offset=0` | Search/category/subtest/examType/readiness dari server. |
| Harga paket | `GET /api/v1/plans` dan `GET /api/v1/plans/{id}` | Gunakan amountMinor/currency/months dan ID versi; hindari harga tetap di FE. |
| Hak premium | `GET /api/v1/users/current/membership` | Tampilkan tier, interval dan snapshot paket; fitur leaderboard mengikuti benefits.fullLeaderboard. |
| Periode ranking | `GET /api/v1/leaderboards/cohorts?category=UTBK` | Simpan ID cohort yang dipilih sebelum mengambil entry. includeEnded=true untuk periode lampau yang masih ACTIVE. |
| Ranking | `GET /api/v1/leaderboards/cohorts/{id}/entries?offset=0&limit=20` | Render items dan ownRank terpisah; PREVIEW dibatasi 3 server-side, FULL mendukung pagination/filter target. |
| Persetujuan ranking | `GET/PATCH /api/v1/users/current/privacy` | PATCH body `{"leaderboardOptIn":true}` hanya setelah pengguna memilih setuju. Mutasi memakai CSRF. |

Collection terbaru memiliki folder **Student screens - current contracts**. Jalankan daftar cohort/paket
untuk mengisi cohortId/planId. Cohort kosong valid karena enrollment dan job pembentuk ranking belum dibuat;
collection tidak menghasilkan ranking atau pembayaran palsu.

Dashboard menghitung seluruh data yang cocok dengan kategori/mode, termasuk ketika riwayat melebihi 100 sesi.
Rerata/trend dipisahkan menurut examType dan policyVersion untuk menghindari mencampur skala ujian. Trend
berisi maksimal 12 titik per kelompok, urut lama ke baru. elapsedSeconds adalah waktu berlalu sejak mulai,
dibatasi finalize/deadline/waktu server; angka ini bukan rekaman waktu interaksi aktif siswa.

Riwayat mendukung category, mode, status, search, tryoutId atau topicId, from/to, period, offset, limit.
from/to adalah tanggal WIB inklusif berdasarkan startedAt; WEEK adalah tujuh hari termasuk hari ini, MONTH
dari awal bulan sampai hari ini. Jangan mengirim period bersama rentang tanggal. Default limit 6, maksimal 100.
summary.completed menghitung status teknis COMPLETED; periksa terminalReason untuk membedakan abandon.

Profil tetap menggunakan `PATCH /api/v1/users/current/profile`. Field yang tidak dikirim dipertahankan;
field opsional dapat dihapus dengan null. Contoh perubahan nama saja: `{"displayName":"Nama Baru"}`.
CPNS/KEDINASAN tidak memerlukan sekolah/kelas; UTBK/SEKOLAH memerlukannya ketika target dipilih. Mengganti
kategori membutuhkan institusi baru. `{"targetCategory":null}` menghapus target. Tampilkan fieldErrors dari
400 VALIDATION_FAILED agar siswa tahu field yang perlu diperbaiki.

Result sekarang menambahkan `processing:{state,poll,retryAfterSeconds,errorCode}`. Poll hanya jika poll=true;
FAILED berarti tampilkan kegagalan penilaian dan hentikan polling, lalu gunakan dukungan/admin retry. Attempt
tetap FINALIZING selama recovery, sehingga satu sesi aktif dan snapshot lama terlindungi. Tidak ada retry
atau pembatalan scoring oleh siswa. Key lama pada result tetap tersedia.

API paket belum melakukan checkout, webhook atau provisioning premium. Membership hanya mengakui grant
aktif dari order PAID, serta benefits snapshot saat pembelian. Tidak ada perubahan premium dari flag FE.

Implementasi repo FE saat pemeriksaan masih menggunakan endpoint lama dan sebagian perhitungan lokal.
Perubahan backend ini menyiapkan kontrak migrasinya; hook/container FE perlu disambungkan ke endpoint di atas.
Contoh request/response lengkap tersedia di [referensi API](backend/03-api-reference.md) dan
[OpenAPI](backend/openapi.json).

## Sumber soal dan impor Excel per sheet — V9

Satu `question_set` mendefinisikan jenis ujian dan tahun sumber. Satu upload Excel mengarah ke tepat satu
set; tiap sheet dipetakan ke subtes yang terdaftar dalam jenis ujian tersebut. Upload tidak menciptakan
subtes baru. Satu set bisa menerima beberapa unggahan/koreksi; file bukan identitas master soal.

### Urutan penggunaan

1. Login sebagai QUESTION_STAFF dan ambil CSRF. Buat sumber melalui `POST /api/v1/staff/question-sets`:

```json
{
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "code": "UTBK_2025_A",
  "sourceYear": 2025,
  "title": "Sumber UTBK 2025 paket A",
  "reason": "Menyiapkan sumber soal tahun 2025"
}
```

2. Baca `GET /staff/question-sets/{id}/template-mapping` dan unduh `/template`, keduanya di bawah `/api/v1`.
   Template memiliki satu sheet per subtes aktif, header, dropdown kunci/difficulty, dan baris judul beku.
   Isi file; jika hanya memuat sebagian subtes, hapus sheet lain dan hapus pemetaannya dari metadata.
3. Kirim `POST /api/v1/staff/workbook-imports` sebagai multipart: `file` adalah XLSX dan `metadata` part JSON
   dengan Content-Type `application/json`:

```json
{
  "questionSetId": "00000000-0000-4000-8000-000000000100",
  "sheets": [
    {"sheetName": "NUMERIK", "subtestId": "00000000-0000-4000-8000-000000000004"}
  ]
}
```

4. Respons 202 memberikan ID batch. Worker memproses IMPORT_XLSX. Baca `GET /staff/workbook-imports/{id}`
   sampai READY, NEEDS_INPUT atau FAILED. READY berarti selesai ekstraksi dan siap review, bukan published.
5. Baca `/staff/workbook-imports/{id}/candidates?offset=0&limit=20`, opsional `sheetId`. Error dikaitkan dengan
   `sheetName`, `sourceRow` dan `sourceItemCode`. Koreksi melalui PATCH pada `/candidates/{candidateId}`:

```json
{
  "sourceItemCode": "Q001",
  "topicId": null,
  "difficulty": "MEDIUM",
  "content": {
    "stemText": "Berapa hasil 2 + 3?",
    "options": [
      {"code": "A", "text": "4", "correct": false, "weight": 0},
      {"code": "B", "text": "5", "correct": true, "weight": 1}
    ],
    "explanation": "Dua ditambah tiga sama dengan lima.",
    "shuffleAllowed": true
  }
}
```

6. Simpan kandidat terpilih melalui `POST /staff/workbook-imports/{id}/save` dengan body
   `{"candidateIds":["00000000-0000-4000-8000-000000000103"],"reason":"Review kandidat selesai"}`.
   Maksimal 100 ID unik per request. Transaksi seluruh pilihan berhasil atau rollback. Hasil berupa array
   StaffQuestionResponse. Lanjutkan review dan publish melalui endpoint question yang sudah ada.
7. Admin membaca `/api/v1/admin/question-sets`, lalu menambahkan referensi sumber pada **versi blueprint baru**:

```json
{
  "durationSeconds": 5400,
  "scoringPolicyId": "00000000-0000-4000-8000-000000000070",
  "sections": [
    {
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "questionCount": 3,
      "questionSetIds": ["00000000-0000-4000-8000-000000000100"],
      "difficultyQuotas": [
        {"difficulty": "EASY", "count": 1},
        {"difficulty": "MEDIUM", "count": 2}
      ],
      "repeatPolicy": "UNSEEN_TODAY"
    }
  ],
  "reason": "Komposisi berdasarkan sumber soal terverifikasi"
}
```

Semua UUID contoh sintetis; ambil ID nyata dari taxonomy/source/policy API. Di Postman, folder
**Question sources and Excel import** mengisi questionSetId, pemetaan sheet, batch, dan kandidat. Setelah
save, gunakan folder petugas untuk publish master yang sudah direview; tidak ada auto-publish.

### Isi dan batas workbook

| Kolom | Aturan |
|---|---|
| question_code | Wajib, stabil per sumber/subtes, 1–80 karakter huruf/angka/titik/underscore/hyphen; case-sensitive |
| topic_id | Opsional UUID topik aktif dalam subtes sheet; bukan nama/kode topik |
| question_text | Wajib, maksimal 20.000 karakter |
| option_a sampai option_e | 2–5 opsi terisi, maksimal 10.000 karakter per opsi |
| correct_answer | Tepat satu kode A–E yang opsinya terisi |
| explanation | Wajib, maksimal 20.000 karakter |
| difficulty | EASY/MEDIUM/HARD; kosong menjadi MEDIUM |
| shuffle_allowed | true/false; kosong menjadi true |
| weight_a sampai weight_e | Opsional 0–1000, maksimal 4 desimal; default 1 untuk kunci dan 0 untuk lainnya |

Header wajib: question_code, question_text, option_a, option_b, correct_answer, explanation. Header lain
opsional tetapi namanya harus dikenali; header duplikat/tidak dikenal ditolak. Nomor baris bukan identitas
soal: memindah baris tidak boleh membuat master baru. Simpan question_code sebagai teks jika memakai nol awal.

Saat ini didukung **XLSX berisi soal pilihan ganda teks**. XLS/XLSM, formula, sheet tersembunyi, merge cell,
macro, link eksternal, gambar/drawing/embedded object ditolak; tidak diabaikan diam-diam. Batas upload default
25 MiB, maksimum 30 sheet dan 2.000 baris data total. Pemeriksaan sebelum POI memuat workbook membatasi
expanded ZIP 16 MiB, 512 bagian dan 40.000 cell. File dengan format tidak valid berhenti FAILED tanpa
retry otomatis berulang; gangguan sementara worker tetap mengikuti retry antrean. Retry manual memakai
endpoint staff/imports/{id}/retry atau admin/jobs/{id}/retry yang sudah ada. File salah harus diperbaiki
dan diunggah sebagai file baru, bukan terus di-retry.

### Versi, sumber dan pemilihan sesi

Kode sama dalam sumber/subtes yang sama mengarah ke master sama. Konten berubah membuat DRAFT baru jika
versi sebelumnya sudah PUBLISHED. Baseline revisi hanya memakai versi PUBLISHED yang immutable: jika master
masih DRAFT saat ekstraksi, selesaikan draft dahulu lalu unggah ulang. Ini mencegah edit draft lalu publish
dengan ID versi yang sama melewati pemeriksaan stale. Replay kandidat yang sudah tersimpan tetap memakai
resolvedVersionId. Konten yang sama memakai versi sama; perbedaan skala angka seperti 1 dan
1.0000 tidak membuat revisi. Draft yang belum selesai harus direview dahulu. expectedVersionId
dibekukan saat ekstraksi; perubahan master setelah itu menghasilkan 409 CANDIDATE_STALE. Upload ulang dari
versi terkini untuk melakukan review baru. Replay kandidat tersimpan selalu mengembalikan resolvedVersionId.

Tabel question_set_item memungkinkan master dipakai beberapa sumber. Hubungkan master yang sudah ada
secara eksplisit lewat `POST /staff/question-sets/{id}/items`; sistem tidak menggabungkan soal hanya karena
teksnya mirip. Daftar anggota tersedia melalui GET pada path yang sama. Hasil PDF/manual baru juga dapat
dihubungkan melalui endpoint ini. Data sebelum V9 yang memiliki source_year diisi dibackfill per examType/tahun
dari versi master terbaru; tahun kosong tidak ditebak. Kolom source_year pada versi tetap menjadi provenance
legacy. Jika questionSetIds dipilih, tahun sumber berasal dari set; jangan menggabungkannya dengan sourceYear.

Kebijakan pengulangan pada section:

| repeatPolicy | Perilaku |
|---|---|
| UNSEEN_TODAY | Default tryout; tidak memakai master yang sudah masuk paket siswa pada hari WIB yang sama |
| NEVER_SEEN | Tidak memakai master yang pernah masuk paket siswa, termasuk practice dan sesi abandon |
| RECYCLE_OLDEST | Prioritaskan yang belum pernah dipakai lalu yang paling lama dipakai; pengulangan diperbolehkan |

Difficulty quotas wajib unik dan totalnya sama dengan questionCount. Soal lama tanpa difficulty tidak
memenuhi alokasi difficulty tertentu. Kesiapan global memakai view published_question_pool dan function
question_section_available; kandidat harus berasal dari versi terbit terbaru, scope aktif, opsi/kunci lengkap,
serta bobot positif jika policy OPTION_WEIGHT. Start juga memeriksa riwayat personal. Stok kurang menghasilkan
409 POOL_NOT_READY dan rollback, tanpa mengurangi jumlah soal atau memakai kuota.

Pemilihan dilakukan sekali saat start dan dibekukan pada attempt_item. Refresh/replay start membaca sesi
yang sama. question_id digunakan untuk exposure agar revisi versi tidak mengelabui NEVER_SEEN. Arsip sumber
tidak mengarsipkan master dan tidak mengubah snapshot sesi lama; katalog legacy tanpa pembatas sumber tetap
mempertahankan scope lamanya. Jumlah stok terbatas tidak menjamin kombinasi selalu berbeda pada kebijakan
RECYCLE_OLDEST. Leaderboard kompetitif tetap memerlukan form tetap/kesetaraan yang diuji.

Parser menggunakan [Apache POI](https://poi.apache.org/components/spreadsheet/); parsing, storage, repository,
dan worker dipisahkan melalui port. Ukuran workbook dibatasi sebelum model workbook dimuat ke memori.
