# Dokumentasi teknis backend PastiSiap

Snapshot implementasi: **19 November 2026**. Dokumentasi ini menjelaskan perilaku kode yang ada di repo,
bukan menganggap seluruh fitur PRD sudah selesai. Prefix akun aktif adalah `/api/v1/users/current`.

**[Buka dokumentasi HTML lengkap](backend-documentation.html)** untuk membaca seluruh bab dengan 13 diagram
yang sudah dirender, navigasi bab, dan tombol Cetak/Simpan PDF. HTML dapat dibaca offline tanpa plugin.

## Peta dokumen

| Dokumen | Isi | Pembaca utama |
|---|---|---|
| [Arsitektur dan flowchart](01-architecture.md) | Runtime, layer kode, auth, sesi belajar, impor, worker, aturan transaksi | Backend, frontend, reviewer |
| [Model data dan ERD](02-database.md) | Relasi, snapshot, versi, JSONB, status dan aturan integritas | Backend, DBA |
| [Referensi setiap endpoint](03-api-reference.md) | Fungsi, akses, header, parameter, contoh request/response dan error | Frontend, QA |
| [Kamus DTO dan kontrak JSON](data-contracts.md) | Field, tipe, validasi dan nullability penting | Frontend, backend |
| [Kamus seluruh tabel](database-dictionary.md) | Kolom, tipe, nullable, default, constraint, index dan trigger | Backend, DBA |
| [Status fitur dan roadmap](04-roadmap.md) | Sudah berjalan, implementasi sebagian, pekerjaan berikutnya dan acceptance | Product, engineer |
| [Operasional dan pengujian](05-operations.md) | Docker, konfigurasi, Flyway, Postman, troubleshooting, deployment | Engineer, QA |
| [OpenAPI lengkap](openapi.json) | Kontrak API yang diperkaya dengan auth/error, termasuk endpoint security/infrastruktur | Integrasi, generator client |
| [Snapshot OpenAPI runtime](openapi-runtime.json) | Hasil langsung `/api-docs`; sebagian endpoint filter Security tidak muncul di sini | Audit kontrak |
| [Metadata skema](database-schema.json) | Metadata tabel/constraint dari PostgreSQL V6; tidak memuat baris pengguna | Audit skema |

Mulai dari arsitektur untuk gambaran besar; frontend dapat langsung membaca referensi endpoint dan kamus DTO.
Diagram Mermaid disertakan di Markdown agar dapat dipelihara bersama kode; versi SVG tersedia pada
[folder diagram](diagrams/manifest.json) dan sudah tertanam di HTML.

## Ringkasan sistem

Backend berbentuk modular monolith Spring Boot 4.1.2 / Java 25. Satu image menjalankan proses API dan proses worker
terpisah. PostgreSQL 17 menyimpan akun, konten terversi, sesi, jawaban, hasil, metadata file dan job persisten.
PDF berada di storage disk privat yang dibagi API/worker. Login menggunakan session cookie, baik melalui password
maupun Google OAuth2/OIDC; mutasi memakai CSRF.

Flow yang tersedia: register/login → pilih katalog/topik → start/resume → simpan jawaban → finalize → worker scoring
→ hasil/review. Petugas mempunyai flow PDF → kandidat → koreksi → draft → publish → versi baru/arsip.

## Acuan dan batas dokumentasi

- Acuan utama: controller, DTO, service, adapter persistence, konfigurasi dan migrasi V1–V6 pada repo ini.
- Contoh JSON memakai data sintetis. UUID, cookie, token dan nilai contoh bukan kredensial atau ID dari akun nyata.
- Tidak ada envelope sukses tunggal. Beberapa endpoint mengembalikan objek, array, body kosong atau binary.
- Field legacy seperti `question_count` tetap snake_case; DTO baru kebanyakan camelCase. Dokumentasi mempertahankan kontrak aktual.
- Tabel pembayaran/token reset/kompetisi sudah ada, tetapi keberadaan tabel tidak berarti flow produknya telah berjalan.
- Diagram proses menggambarkan jalur yang tersedia; pekerjaan rencana diberi status tersendiri pada roadmap.
- Artefak generated berasal dari snapshot runtime dan catatan yang direview terhadap source. Setelah API berubah,
  perbarui snapshot lalu jalankan `node docs/backend/generate-reference.mjs`.

## Tautan terkait

- [README cara menjalankan](../../README.md)
- [Postman collection](../../postman/PastiSiap-Backend.postman_collection.json)
- [Riset/keputusan implementasi learning](../learning-architecture-plan.md)
- [Tutorial mencoba learning API](../learning-api.md)
- [Migrasi Flyway](../../src/main/resources/db/migration)

Untuk mencoba cepat: pastikan `.env` memakai profil `demo` atau `oauth,demo`, lalu jalankan
`docker compose up --build -d`. API lokal berada di `http://localhost:8080`.
