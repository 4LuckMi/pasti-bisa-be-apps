# Status fitur dan pekerjaan berikutnya

[Indeks dokumentasi](README.md) · Snapshot 19 November 2026.

Status **tersedia** berarti ada implementasi kode/use case. Status ini tidak sama dengan telah diuji
pada seluruh skenario produksi, seluruh format PDF atau seluruh provider eksternal.

## Sudah dikerjakan

| Area | Implementasi | Bukti/acuan |
|---|---|---|
| Runtime | Java 25, Spring Boot, PostgreSQL, Docker API/worker | Dockerfile, compose.yaml |
| Migrasi | Flyway V1–V6, relasi dan guard cross-row | Migration dan metadata dictionary |
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
| Riwayat pribadi | Maksimal 100 sesi terbaru, termasuk practice | findHistory |
| Upload PDF | Disk privat, checksum, provenance, dedup per uploader/scope | LocalImportFileStorage dan ContentImportService |
| Ekstraksi | PDF teks/scan OCR, kandidat dengan flag, template konservatif | PdfBoxExtractor dan TemplateQuestionParser |
| Bank soal | Manual draft, kandidat→draft, publish, versi baru, archive | ContentController dan repository |
| Worker | Queue PostgreSQL, lease/token, retry, idempotensi | JdbcJobQueue dan LearningWorker |
| Demo | Empat katalog, 48 soal generik, PDF contoh | Profil demo, DemoDataSeeder |
| Integrasi developer | Postman, Swagger, dokumentasi dan tes terisolasi | postman/, compose.verify.yaml |

## Sudah ada sebagian

| Area | Yang sudah ada | Yang belum tersedia |
|---|---|---|
| Subscription | Tabel dan read API status/grant | Order, checkout, webhook payment, refund dan entitlement provisioning |
| Leaderboard | Tabel/cohort integrity dan read API | Pembentukan cohort/form, enrollment, ranking job dan policy tie-break produk |
| Admin | Kode PLATFORM_ADMIN dan beberapa tabel audit/config | CRUD blueprint/paket, manajemen role/user dan dashboard admin |
| Petugas | Upload, kandidat dan master | UI petugas, taxonomy CRUD, navigation bank soal yang lebih lengkap, workflow edit draft di tempat |
| Asset soal | Relasi question_asset dan baca file privat per attempt | Upload/attach asset gambar baru serta authoring rich content |
| Reset/verifikasi email | Tabel token | Endpoint, pengiriman email, konsumsi token dan expiry flow |
| Audit | Sebagian mutasi konten menulis audit_event | Audit seluruh auth/koreksi/perubahan sensitif dan UI audit |
| Google login | Logic dan konfigurasi provider, redirect tersedia | Uji callback/consent end-to-end dengan akun Google dan domain deployment yang dipilih |
| Hak langganan | Query active grant dan kolom entitlement_snapshot | Policy akses premium pada permintaan baru dan snapshot hak yang benar-benar diisi use case |

`entitlement_snapshot` saat start sekarang memakai default database; jangan menganggap hak premium sudah
dibekukan/ditegakkan oleh start service. Pembahasan dan riwayat sendiri tetap tersedia sesuai policy awal PRD.
Tidak ada API yang membuat hak premium hanya dari flag frontend.

## Urutan pekerjaan yang disarankan

Ini rencana teknis untuk direview, bukan fitur yang sudah aktif atau janji jadwal rilis.

| Urutan | Pekerjaan | Hasil/acceptance yang perlu dibuktikan |
|---|---|---|
| 1 | Integrasi frontend siswa dan QA kontrak | Login→profil→katalog→latihan/tryout→hasil/review; handle 401/403/409, processing dan empty state |
| 2 | Penguatan auth/session | Rate limit login/register, email verification/reset, persistensi session JDBC/Redis, uji OAuth callback dan linking |
| 3 | Penguatan lifecycle | Race deadline vs DB timestamp, recovery scoring FAILED, expiry saat worker sibuk OCR, uji restart/concurrency lebih luas |
| 4 | Admin taxonomy/blueprint/policy terversi | Publish ditolak bila scope/pool/policy tidak valid; sesi lama tetap konsisten; provisioning role diaudit |
| 5 | Kualitas bank soal dan rich assets | PDF nyata multikolom/rumus/gambar, upload asset, sumber pada review siswa, update draft yang jelas dan preview petugas |
| 6 | Commerce/entitlement | Vendor gateway dipilih, signature webhook, event idempoten, snapshot order, grant/refund, akses server-side |
| 7 | Leaderboard kompetisi | Form/cohort sebanding, consent opt-in, policy attempt/tie-break dan job ranking |
| 8 | Operasional produksi | HTTPS/proxy, CORS/origin, backup+restore DB/file, metrics/alerts, scan/CDR upload dan retention/orphan cleanup |

## Catatan kontrak yang perlu dipertahankan atau diputuskan

- CamelCase dan beberapa snake_case masih bercampur demi kompatibilitas. Perubahan berikutnya perlu keputusan
  versi API/deprecation, bukan mengganti key frontend diam-diam.
- `PATCH profile` sekarang mewajibkan beberapa field seperti form lengkap; belum partial update per field.
- `details` di history adalah string JSON, sedangkan `details` di result adalah object JSON.
- Beberapa endpoint void mengembalikan 200 body kosong; logout 204. Jangan memaksakan envelope baru tanpa review.
- `GET .../attempts/active` tanpa sesi memberi 200 body kosong. UI harus menangani respons kosong.
- Stok `ready` katalog masih global; endpoint start tetap sumber keputusan final bagi pengguna.
- Bank soal saat ini mewajibkan tepat satu opsi correct; variasi multi-answer/essay belum didukung.
- `shuffleAllowed` dan `OptionDraft.correct` sebaiknya dikirim eksplisit karena merupakan primitive boolean request.
- Read API lama belum seluruhnya memakai guard role CUSTOMER. Penonaktifan akun terhadap session lama,
  revokasi role dan izin per operasi perlu pengujian/policy yang konsisten.
- Field error pada resource binary atau framework tidak selalu sama bentuknya dengan JSON bisnis; contoh ditulis pada API reference.
- Error konversi tipe parameter path/query belum mempunyai handler eksplisit untuk semua kasus; perlu dipetakan
  konsisten menjadi 400 dan diuji agar input UUID yang keliru tidak jatuh ke INTERNAL_ERROR generik.

## Bukti pengujian dan batasnya

Suite terisolasi terakhir sebelum dokumentasi: **19 tests passed, 0 failure/error/skip** pada PostgreSQL
terpisah, termasuk OCR Tesseract nyata. Mencakup parser/scoring, DTO, logging/CSRF, file limits/path, migrasi,
concurrent idempotent start, kuota, revision, ownership, timeout, import/publish, immutable version dan lease lama.
Smoke Docker API+worker juga menyelesaikan practice sampai COMPLETED dengan quota tryout tetap nol.

Tes tersebut belum mencakup seluruh auth attack/rate limit, semua PDF nyata, gateway payment, produksi
multi-instance, pemulihan dari semua titik crash, atau Google consent/callback interaktif end-to-end.
Perbaikan anotasi OpenAPI pada pekerjaan dokumentasi hanya mengubah deskripsi schema, bukan payload bisnis.
