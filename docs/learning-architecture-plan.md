# Katalog, bank soal, tryout, practice, dan impor PDF

## Acuan dan temuan riset

PRD PastiSiap v2.0 (16 November 2026), dibaca dari `PastiSiap-PRD.md` milik chat
"Desain Dashboard Siswa SKORIN". Figma: `CG4pwdhDxG6PdCup6leOEm`, flow siswa Tryout/Latihan.
Implementasi sebelumnya mempunyai query katalog, tetapi belum memiliki lifecycle attempt, scoring, upload, dan worker.

- [PDFBox 3](https://pdfbox.apache.org/3.0/migration.html): `Loader.loadPDF`, ekstraksi teks,
  render halaman untuk OCR. PDF bukan format database soal; layout bebas memerlukan koreksi Petugas.
- [PostgreSQL row locking](https://www.postgresql.org/docs/17/sql-select.html): lock akun untuk
  start session, lock attempt untuk answer/finalize, `SKIP LOCKED` hanya untuk antrean worker.
- [OWASP file upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html):
  UUID filename, limit ukuran, signature/parser validation, storage di luar webroot, akses lewat handler berizin.

## Keputusan arsitektur

1. Tetap modular monolith: Controller → Service → port Repository/Storage/Extractor → adapter JDBC/disk/PDFBox.
   DTO eksplisit; query named parameter. Flyway V1–V5 tidak diubah.
2. `exam_category → exam_type → subtest → topic → question → question_version → question_option`
   menyimpan konten terstruktur. Versi terbit immutable; edit menghasilkan versi baru. Jangan menyimpan PDF berulang per user.
3. `stored_file → import_batch → import_candidate → question` menyimpan provenance.
   File fisik privat `imports/<uuid>.pdf`, checksum SHA-256 dan metadata di PostgreSQL.
   Siswa hanya mendapat JSON soal snapshot; sumber PDF yang dapat berisi kunci tidak menjadi asset siswa.
4. Upload PDF mengembalikan 202 dan job persisten; worker terpisah memproses halaman secara terbatas.
   Parser template eksplisit (nomor, A–E, Kunci, Pembahasan). Teks/OCR yang ambigu tetap kandidat belum lengkap.
   OCR tidak menebak jawaban; Petugas wajib memeriksa dan menerbitkan konten valid.
5. Tryout memakai blueprint yang diterbitkan dan section dengan stok cukup. Practice membuat blueprint internal
   per permintaan topik dengan batas 1–50 soal. Keduanya memakai attempt/item/answer yang sama dan satu sesi aktif.
   Tryout memakai 1 kuota dari 2/hari WIB; practice tidak. Pool kurang harus rollback seluruh transaksi.
6. Pemilihan soal mengutamakan belum dilihat lalu paparan tertua, kategori/subtes/topik/tahun tetap dibatasi.
   Filter opsi yang bergantung urutan menghormati `shuffle_allowed`. Snapshot item/opsi tidak berubah saat resume.
7. Setiap start memakai `Idempotency-Key`; request dengan key sama dan isi berbeda ditolak.
   Autosave memakai revision; update terlambat/stale ditolak. Jam server menentukan deadline.
8. Submit/abandon/deadline memfinalisasi sekali dan membuat job scoring. Worker menyimpan score_run/section
   dari policy/version beku. Hasil gagal proses berstatus PROCESSING, bukan nilai nol.
   Baseline mendukung BINARY (akurasi 0–100) dan OPTION_WEIGHT; policy lain ditolak sebelum start.
   Soal acak pribadi tidak otomatis menjadi entri leaderboard kompetisi.
9. Job memakai lease, retry terbatas, identitas lease, dan transaksi idempoten. Restart tidak menggandakan kandidat/hasil.
10. Fixture demo terpisah dari migrasi produksi. Empat kategori dengan katalog/soal contoh, bukan format/skor resmi.
    Profil `demo` memuat fixture idempoten; tidak menciptakan akun internal dengan password bawaan.

## Urutan pelaksanaan

1. Tambah V6 untuk metadata impor, idempotency request hash, terminal reason, dan guard snapshot/answer.
2. Lifecycle attempt dan scoring worker; detail katalog, hasil dan review; gunakan endpoint baca yang sudah ada.
3. Storage upload privat, ekstraktor PDF/OCR, parser kandidat dan endpoint konten dengan role QUESTION_STAFF.
4. Fixture demo dan contoh PDF/JSON yang bisa diimpor; update Compose, README, Postman.
5. Build Java 25; tes rules/scoring/parser; integration PostgreSQL untuk migrasi, kuota, revision, ownership,
   frozen version, retry/idempotency, upload dan read source. Jalankan smoke di Docker dengan fixture.

## Batas parser dan operasional

Layout PDF multikolom, rumus, tabel dan gambar tidak selalu dapat dipisahkan akurat. Kandidat selalu dapat dikoreksi
sebelum terbit. Batas 25 MB, 100 halaman, timeout OCR dan batas piksel digunakan sebagai batas awal konfigurasi.
File belum dipindai antivirus otomatis; penyedia scanner/CDR dan benchmarking PDF nyata masih diperlukan sebelum
upload dibuka untuk sumber tidak tepercaya. Jangan mengaktifkan eksekusi tautan, JavaScript PDF atau instruksi dalam PDF.

## Hasil pelaksanaan

Langkah 1–4 selesai: V6, lifecycle/snapshot/scoring, storage/worker/import/review/publish,
fixture 4 katalog + 48 soal dan koleksi Postman. Model domain parser/validasi/scoring terpisah dari DTO HTTP.
Langkah 5 diverifikasi dengan PostgreSQL terisolasi dan OCR Tesseract sungguhan; panduan endpoint dan batas
fitur tersedia di [learning-api.md](learning-api.md). Konfigurasi blueprint/paket via API admin, image asset
authoring dan gateway pembayaran tetap menjadi tahap berikutnya. Empat kategori demo menggunakan soal
aritmetika generik yang dilabeli contoh, bukan cakupan materi produksi per kategori.

Verifikasi 19 November 2026: 19 tes lulus tanpa failure/error/skip. Flyway V1–V6 tervalidasi;
database development berisi 4 katalog demo dan 48 soal. Smoke lewat HTTP Docker berhasil
start practice 2 soal → autosave revision 1 → finalize → worker `SUCCEEDED` → `COMPLETED`/review;
kuota tryout tetap 0. API dan worker tetap berjalan melalui Compose setelah pemeriksaan selesai.
