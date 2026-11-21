# Status fitur dan pekerjaan berikutnya

[Indeks dokumentasi](README.md) · Snapshot 21 November 2026.

Status **tersedia** berarti ada implementasi kode/use case. Status ini tidak sama dengan telah diuji
pada seluruh skenario produksi, seluruh format PDF atau seluruh provider eksternal.

## Sudah dikerjakan

| Area | Implementasi | Bukti/acuan |
|---|---|---|
| Runtime | Java 25, Spring Boot, PostgreSQL, Docker API/worker | Dockerfile, compose.yaml |
| Migrasi | Flyway V1–V9, 45 tabel dan view pool; FK scope sumber/sheet/master serta snapshot immutable | Migration dan metadata dictionary |
| Identity password | Register/login, BCrypt, session, logout, profil | IdentityController dan service/repository |
| Google SSO | OIDC provider, email verified, provisioning/linking | PastiSiapOidcUserService, OAuthAccountProvisioningService |
| Tipe dan role | SISWA default, CUSTOMER/QUESTION_STAFF/PLATFORM_ADMIN | V4, user_role dan method security |
| CSRF/error/log | Token session, error envelope, request ID dan stack trace server | shared/security dan shared/web |
| Profil siswa | Sekolah/kelas, target institusi/program, skor target | StudentProfileService |
| Katalog | List/detail, stok section, riwayat ringkas per paket | StudentLearningService, ExamService |
| Tryout | Idempotent start, satu sesi aktif, kuota WIB 2/hari | Transaksi start dan DB constraint |
| Practice | Topik, 1–50 soal, blueprint internal, tanpa kuota tryout | practiceBlueprint dan startPractice |
| Snapshot | Versi soal dan urutan opsi dibekukan per attempt | attempt_item, immutable published versions |
| Jawaban | Ownership, validasi opsi, optimistic revision | saveAnswer dan trigger |
| Finalisasi | Submit, abandon, timeout, antrean scoring | ExamService dan worker |
| Scoring | BINARY/OPTION_WEIGHT, policy/version, rincian subtes | ScoreCalculator dan score_run/section |
| Hasil/review | Poll hasil, kunci/pembahasan setelah scoring | result/review dan AttemptQuestionService |
| Riwayat pribadi | Filter mode/paket/topik, pagination offset/limit hingga 100 per request | findHistory |
| Upload PDF | Disk privat, checksum, provenance, dedup per uploader/scope | LocalImportFileStorage dan ContentImportService |
| Upload Excel | XLSX per sheet/subtes, set jenis/tahun, template/mapping, staging, koreksi, simpan DRAFT dan revisi stale guard | WorkbookImportService, WorkbookRepository dan PoiWorkbookParser |
| Sumber dan variasi | Membership N:M, sumber per section, kuota difficulty dan tiga repeatPolicy, stok bersama untuk katalog/admin/start | QuestionPoolRepository, view dan function V9 |
| Ekstraksi | PDF teks/scan OCR, kandidat dengan flag, template konservatif | PdfBoxExtractor dan TemplateQuestionParser |
| Bank soal | Manual draft, kandidat→draft, publish, versi baru, archive | ContentController dan repository |
| Petugas lanjutan | Filter kategori/jenis/subtes/topik/tahun/status/batch, total pagination, versi historis, edit draft/revision, bulk save dan retry | QuestionBankController/Service |
| Admin akun | Daftar/detail dukungan, akun internal SSO, status/role dan admin terakhir | AdministrationController/Service |
| Pencabutan sesi | Revision tersimpan di DB; role/status/password/email berubah mencabut sesi password/Google lama | AccountSessionGuard dan trigger V7 |
| Admin produk | Taxonomy dengan parent tetap, harga/policy/blueprint terversi, stok/publish dan arsip | CatalogManagementController/Service |
| Admin operasional | Dashboard, order/event tersensor, subscription/revoke, audit, daftar/retry job | AdministrationRepository |
| Worker | Queue PostgreSQL, lease/token, retry, idempotensi | JdbcJobQueue dan LearningWorker |
| Demo | Empat katalog, 48 soal generik, PDF contoh | Profil demo, DemoDataSeeder |
| Integrasi developer | Postman, Swagger, dokumentasi dan tes terisolasi | postman/, compose.verify.yaml |

## Sudah ada sebagian

| Area | Yang sudah ada | Yang belum tersedia |
|---|---|---|
| Subscription | Paket aktif dan ringkasan membership dari snapshot order/grant; read API legacy | Checkout, webhook payment, eksekusi refund dan entitlement provisioning |
| Leaderboard | Cohort read, preview server 3 + ownRank, full pagination/filter premium dan consent | Pembentukan cohort/form, enrollment, ranking job dan policy tie-break produk |
| Admin | 42 endpoint akun/produk/operasi termasuk metadata sumber soal | Integrasi FE, MFA/invitation email, provider rekonsiliasi/refund dan regrade/cohort |
| Petugas | Upload, kandidat, master, pencarian, versi dan retry | Integrasi FE endpoint baru, retry per halaman dan lampiran kunci terpisah |
| Refund | Kasus idempoten PENDING_PROVIDER dan guard nominal/order | Eksekusi provider, signature/callback dan transisi sukses terverifikasi |
| Asset soal | Relasi question_asset dan baca file privat per attempt | Upload/attach asset gambar baru serta authoring rich content |
| Reset/verifikasi email | Tabel token | Endpoint, pengiriman email, konsumsi token dan expiry flow |
| Audit | Sebagian mutasi konten menulis audit_event | Audit seluruh auth/koreksi/perubahan sensitif dan UI audit |
| Google login | Logic dan konfigurasi provider, redirect tersedia | Uji callback/consent end-to-end dengan akun Google dan domain deployment yang dipilih |
| Hak langganan | Query active paid grant, snapshot benefits untuk akses leaderboard, kolom entitlement_snapshot | Snapshot hak pada attempt yang benar-benar diisi start use case |

`entitlement_snapshot` saat start sekarang memakai default database; jangan menganggap hak premium sudah
dibekukan/ditegakkan oleh start service. Pembahasan dan riwayat sendiri tetap tersedia sesuai policy awal PRD.
Tidak ada API yang membuat hak premium hanya dari flag frontend.

## Urutan pekerjaan yang disarankan

Ini rencana teknis untuk direview, bukan fitur yang sudah aktif atau janji jadwal rilis.

| Urutan | Pekerjaan | Hasil/acceptance yang perlu dibuktikan |
|---|---|---|
| 1 | Integrasi frontend siswa dan QA kontrak | Login→profil→katalog→latihan/tryout→hasil/review; handle 401/403/409, processing dan empty state |
| 2 | Penguatan auth/session | MFA internal, rate limit login/register, email verification/reset, invitation email, persistensi session JDBC/Redis, uji OAuth callback dan linking |
| 3 | Penguatan lifecycle | Race deadline vs DB timestamp, recovery scoring FAILED, expiry saat worker sibuk OCR, uji restart/concurrency lebih luas |
| 4 | Integrasi layar admin/petugas | Hubungkan endpoint baru ke FE, QA loading/error/pagination dan alur dukungan beralasan |
| 5 | Kualitas bank soal dan rich assets | PDF nyata multikolom/rumus/gambar, upload asset, sumber pada review siswa, update draft yang jelas dan preview petugas |
| 6 | Commerce/entitlement | Vendor gateway dipilih, signature webhook, event idempoten, snapshot order, grant/refund, akses server-side |
| 7 | Leaderboard kompetisi | Pembentukan form/cohort sebanding, policy attempt/tie-break dan job ranking; consent/read API sudah ada |
| 8 | Operasional produksi | HTTPS/proxy, CORS/origin, backup+restore DB/file, metrics/alerts, scan/CDR upload dan retention/orphan cleanup |

## Catatan kontrak yang perlu dipertahankan atau diputuskan

- CamelCase dan beberapa snake_case masih bercampur demi kompatibilitas. Perubahan berikutnya perlu keputusan
  versi API/deprecation, bukan mengganti key frontend diam-diam.
- `PATCH profile` sudah partial; validasi sekolah/kelas mengikuti kategori target. FE tetap perlu menampilkan fieldErrors.
- `details` di history adalah string JSON, sedangkan `details` di result adalah object JSON.
- Beberapa endpoint void mengembalikan 200 body kosong; logout 204. Jangan memaksakan envelope baru tanpa review.
- `GET .../attempts/active` tanpa sesi memberi 200 body kosong. UI harus menangani respons kosong.
- Stok `ready` katalog masih global; endpoint start tetap sumber keputusan final bagi pengguna.
- Bank soal saat ini mewajibkan tepat satu opsi correct; variasi multi-answer/essay belum didukung.
- `shuffleAllowed` dan `OptionDraft.correct` sebaiknya dikirim eksplisit karena merupakan primitive boolean request.
- Read API lama belum seluruhnya memakai guard role CUSTOMER. Pencabutan sesi karena status/role sudah
  diterapkan; penyamaan izin per operasi dan kebijakan penanganan ujian akun suspended tetap perlu review produk.
- Field error pada resource binary atau framework tidak selalu sama bentuknya dengan JSON bisnis; contoh ditulis pada API reference.
- Konversi UUID/path/query yang keliru dipetakan 400 INVALID_REQUEST; konflik FK/immutable/unique
  dipetakan 409 DATA_CONFLICT tanpa membocorkan SQL ke client.

## Bukti pengujian dan batasnya

Suite terisolasi terbaru: **46 tests passed, 0 failure/error/skip** pada PostgreSQL
terpisah, termasuk OCR Tesseract nyata. Mencakup parser/scoring, DTO, logging/CSRF, file limits/path, migrasi,
concurrent idempotent start, kuota, revision, ownership, timeout, import/publish, immutable version dan lease lama.
Smoke Docker API+worker juga menyelesaikan practice sampai COMPLETED dengan quota tryout tetap nol.

Tes tersebut belum mencakup seluruh auth attack/rate limit, semua PDF nyata, gateway payment, produksi
multi-instance, pemulihan dari semua titik crash, atau Google consent/callback interaktif end-to-end.
Penyesuaian FE 20 November menambah detail latihan, konteks sesi/scope soal, finalizedAt dan filter riwayat
tanpa migrasi database. Field/endpoint lama tetap tersedia. Tes baru memverifikasi riwayat latihan berdasarkan
topic ID, isolasi antar akun, pagination/filter invalid, serta statistik katalog setelah pergantian versi blueprint.

Tes internal membuktikan pemisahan tiga role, sesi dicabut saat role/status berubah, restore tidak menghidupkan
sesi lama, dua suspend admin bersamaan menyisakan satu admin aktif, revisi draft stale ditolak, versi publish
lama tetap utuh, blueprint kurang stok ditahan, attempt lama tidak ikut berganti komposisi, harga lama tetap,
retry impor tidak membuat job kedua, dan refund pending tidak dianggap sukses/pendapatan dikurangi.

Tes read siswa mencakup 103 attempt tanpa terpotong batas legacy, isolasi kategori/akun, rentang WIB,
partial PATCH/validasi CPNS vs UTBK, audit consent, katalog berhalaman, snapshot hak/harga, revoke grant,
preview yang tidak dapat dilewati offset, ownRank, opt-out, cohort masa depan, processing gagal yang
berhenti polling, serta schema OpenAPI dashboard siswa/admin yang terpisah.

Delapan tes Excel membuktikan parsing XLSX nyata, file tidak berubah setelah dibaca, formula/bobot invalid
menjadi row error, header/sheet tersembunyi/expanded ZIP/macro ditolak, mapping lintas jenis ditolak,
upload identik idempoten, save/revisi mempertahankan jumlah master, stale bulk save rollback seluruhnya dan edit draft lalu publish tidak melewati baseline,
master dapat dipakai dua sumber, sumber blueprint terbit immutable, alokasi EASY/MEDIUM terpenuhi,
NEVER_SEEN menghasilkan dua sesi tanpa master berulang, snapshot sesi lama tetap sama, dan workbook rusak
berhenti FAILED tanpa lima percobaan otomatis. Setelah perubahan terakhir delapan tes ini dijalankan ulang
dan lulus. Excel kaya gambar/objek serta format essay/multi-answer belum didukung.

Empat tes akun dummy memverifikasi login password admin/petugas dengan userType INTERNAL dan role tepat,
akses silang ditolak 403, seed ulang tidak mengganti hash/auth_revision, password salah tidak mereset akun,
status/role yang diubah tidak dipulihkan diam-diam, serta bentrok email CUSTOMER tidak diberi role internal
dan membatalkan seluruh transaksi seed. Akun hanya dibuat oleh tool development eksplisit; normal startup tidak memuatnya.

Cakupan petugas saat ini **39 operasi**. Alur PDF/bank soal sudah dipakai FE; UI Excel masih perlu dihubungkan
ke API workbook baru. Upload asset/gambar terstruktur, retry per halaman, dan variasi essay/multi-answer
tetap pekerjaan lanjutan, sehingga cakupan ini tidak berarti seluruh fitur bank soal masa depan sudah selesai.
