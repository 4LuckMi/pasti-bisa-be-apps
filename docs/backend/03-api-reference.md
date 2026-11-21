# Referensi endpoint backend

[Indeks](README.md) · [Kamus semua DTO](data-contracts.md) · [OpenAPI lengkap](openapi.json)

Snapshot 20 November 2026. **90 operasi controller** dicocokkan dengan runtime; **98 operasi total** mencakup logout/OAuth/Actuator/Swagger. Resource statis internal library tidak didaftar satu per satu.

## Konvensi request dan response

- Base URL lokal: `http://localhost:8080`. Semua contoh memakai ID/data sintetis; ganti dengan ID response nyata.
- Cookie `JSESSIONID` adalah session aplikasi. Tidak ada bearer access/refresh JWT untuk API ini.
- POST/PUT/PATCH/DELETE mengirim CSRF dari `GET /api/v1/csrf` dan cookie yang sama, termasuk login/register.
- Header Content-Type JSON untuk body JSON; upload memakai multipart dengan part metadata application/json.
- GET tidak mempunyai body kecuali dinyatakan lain. Path ID memakai UUID; position nomor mulai 1.
- Sukses dapat berupa object, array, 200 kosong, 204 kosong, redirect atau binary. Jangan selalu memanggil response.json().
- Respons error API biasanya mempunyai X-Request-ID dan envelope di bawah; exception binary/non-API dijelaskan per endpoint.
- Field camelCase/snake_case mengikuti DTO aktual. Semua key dijelaskan pada kamus DTO; tidak ada key password/token pada response user.

### Bentuk error umum

```json
{
  "code": "REVISION_CONFLICT",
  "message": "Fetch the latest question before saving again",
  "fieldErrors": {},
  "requestId": "00000000-0000-4000-8000-000000000099"
}
```

| Status | Kode umum | Makna |
|---|---|---|
| 400 | VALIDATION_FAILED / MALFORMED_JSON / INVALID_REQUEST / HTTP_400 | Body, header atau parameter tidak valid |
| 401 | AUTHENTICATION_REQUIRED / INVALID_CREDENTIALS | Session/login tidak valid |
| 403 | CSRF_INVALID / ACCESS_DENIED | Token session tidak cocok atau role tidak cukup |
| 404 | NOT_FOUND / HTTP_404 | Resource tidak ada/tidak dimiliki; asset bisa body kosong |
| 409 | Kode konflik spesifik | Quota, replay, pool, revision, status atau publish |
| 413 | FILE_TOO_LARGE / HTTP_413 | Limit file/request |
| 500 | INTERNAL_ERROR | Telusuri requestId pada log server |

Contoh fieldErrors untuk validasi: `{"email":"must be a well-formed email address"}`. Pesan validator bisa berbeda; frontend sebaiknya menggunakan status/code dan map fieldErrors.

## Indeks endpoint

| No | Method | Path | Fungsi |
|---|---|---|---|
| 1 | GET | [`/api/v1/health`](#e1) | Health aplikasi |
| 2 | GET | [`/api/v1/csrf`](#e2) | Ambil token CSRF |
| 3 | POST | [`/api/v1/auth/register`](#e3) | Registrasi password |
| 4 | POST | [`/api/v1/auth/login`](#e4) | Login password |
| 5 | POST | [`/api/v1/auth/logout`](#e5) | Logout |
| 6 | GET | [`/api/v1/users/current`](#e6) | Ringkasan akun aktif |
| 7 | GET | [`/api/v1/users/current/profile`](#e7) | Profil dan target siswa |
| 8 | PATCH | [`/api/v1/users/current/profile`](#e8) | Update profil dan target |
| 9 | GET | [`/api/v1/tryouts`](#e9) | Katalog tryout |
| 10 | GET | [`/api/v1/tryouts/{id}`](#e10) | Detail dan kesiapan katalog |
| 11 | GET | [`/api/v1/practice/topics`](#e11) | Topik practice |
| 12 | GET | [`/api/v1/practice/topics/{id}`](#e12) | Detail topik dan batas latihan |
| 13 | GET | [`/api/v1/users/current/tryout-quota`](#e13) | Kuota harian WIB |
| 14 | GET | [`/api/v1/users/current/attempts`](#e14) | Riwayat sesi sendiri |
| 15 | GET | [`/api/v1/users/current/subscription`](#e15) | Daftar subscription sendiri |
| 16 | GET | [`/api/v1/leaderboard`](#e16) | Baca leaderboard |
| 17 | POST | [`/api/v1/attempts`](#e17) | Mulai tryout |
| 18 | POST | [`/api/v1/practice/attempts`](#e18) | Mulai practice |
| 19 | GET | [`/api/v1/users/current/attempts/active`](#e19) | Resume sesi aktif |
| 20 | GET | [`/api/v1/users/current/attempts/{id}`](#e20) | Navigator dan status sesi |
| 21 | GET | [`/api/v1/users/current/attempts/{attemptId}/questions/{position}`](#e21) | Baca satu soal dalam sesi |
| 22 | GET | [`/api/v1/users/current/attempts/{attemptId}/questions/{position}/assets/{assetId}`](#e22) | Baca asset privat soal |
| 23 | PUT | [`/api/v1/users/current/attempts/{id}/answers/{position}`](#e23) | Simpan jawaban/revision |
| 24 | POST | [`/api/v1/users/current/attempts/{id}/finalize`](#e24) | Submit atau abandon |
| 25 | GET | [`/api/v1/users/current/attempts/{id}/result`](#e25) | Status dan hasil scoring |
| 26 | GET | [`/api/v1/users/current/attempts/{id}/review`](#e26) | Review seluruh soal |
| 27 | GET | [`/api/v1/staff/taxonomy`](#e27) | Taksonomi aktif untuk petugas |
| 28 | POST | [`/api/v1/staff/imports`](#e28) | Upload sumber PDF |
| 29 | GET | [`/api/v1/staff/imports`](#e29) | Daftar riwayat impor |
| 30 | GET | [`/api/v1/staff/imports/{id}`](#e30) | Status batch dan job |
| 31 | GET | [`/api/v1/staff/imports/{id}/candidates`](#e31) | Daftar kandidat ekstraksi |
| 32 | PATCH | [`/api/v1/staff/imports/{id}/candidates/{candidateId}`](#e32) | Koreksi satu kandidat |
| 33 | POST | [`/api/v1/staff/imports/{id}/candidates/{candidateId}/save`](#e33) | Kandidat menjadi draft master |
| 34 | GET | [`/api/v1/staff/imports/{id}/source`](#e34) | Download PDF sumber |
| 35 | POST | [`/api/v1/staff/questions`](#e35) | Buat soal manual |
| 36 | GET | [`/api/v1/staff/questions`](#e36) | Cari master soal |
| 37 | GET | [`/api/v1/staff/questions/{id}`](#e37) | Detail master terbaru |
| 38 | POST | [`/api/v1/staff/questions/{id}/versions`](#e38) | Buat versi draft baru |
| 39 | POST | [`/api/v1/staff/questions/{id}/publish`](#e39) | Publish versi terbaru |
| 40 | POST | [`/api/v1/staff/questions/{id}/archive`](#e40) | Arsip keluarga soal |
| 41 | GET | [`/oauth2/authorization/google`](#e41) | Mulai login Google |
| 42 | GET | [`/login/oauth2/code/google`](#e42) | Callback Google |
| 43 | GET | [`/actuator/health`](#e43) | Health komponen |
| 44 | GET | [`/actuator/info`](#e44) | Info Actuator |
| 45 | GET | [`/actuator`](#e45) | Discovery Actuator |
| 46 | GET | [`/swagger-ui.html`](#e46) | Buka Swagger UI |
| 47 | GET | [`/api-docs`](#e47) | OpenAPI runtime |
| 48 | GET | [`/api/v1/staff/questions/{id}/draft`](#e48) | Baca atau simpan draft |
| 49 | PUT | [`/api/v1/staff/questions/{id}/draft`](#e49) | Baca atau simpan draft |
| 50 | PUT | [`/api/v1/admin/users/{id}/roles`](#e50) | Ganti role akun |
| 51 | GET | [`/api/v1/staff/questions/{id}/versions`](#e51) | Riwayat versi master |
| 52 | POST | [`/api/v1/staff/imports/{id}/save`](#e52) | Simpan kandidat terpilih sebagai draft |
| 53 | POST | [`/api/v1/staff/imports/{id}/retry`](#e53) | Retry impor gagal |
| 54 | GET | [`/api/v1/admin/users`](#e54) | Daftar atau buat akun internal |
| 55 | POST | [`/api/v1/admin/users`](#e55) | Daftar atau buat akun internal |
| 56 | POST | [`/api/v1/admin/topics`](#e56) | Buat topik |
| 57 | POST | [`/api/v1/admin/subtests`](#e57) | Buat subtes |
| 58 | POST | [`/api/v1/admin/subscriptions/{id}/revoke`](#e58) | Cabut hak langganan |
| 59 | GET | [`/api/v1/admin/scoring-policies`](#e59) | Daftar atau buat policy skor |
| 60 | POST | [`/api/v1/admin/scoring-policies`](#e60) | Daftar atau buat policy skor |
| 61 | GET | [`/api/v1/admin/plans`](#e61) | Daftar atau buat keluarga paket |
| 62 | POST | [`/api/v1/admin/plans`](#e62) | Daftar atau buat keluarga paket |
| 63 | POST | [`/api/v1/admin/plans/{id}/versions`](#e63) | Buat revisi harga baru |
| 64 | POST | [`/api/v1/admin/orders/{id}/refunds`](#e64) | Catat permintaan refund |
| 65 | POST | [`/api/v1/admin/orders/{id}/reconcile`](#e65) | Rekonsiliasi belum dikonfigurasi |
| 66 | POST | [`/api/v1/admin/jobs/{id}/retry`](#e66) | Retry job gagal |
| 67 | POST | [`/api/v1/admin/exam-types`](#e67) | Buat jenis ujian |
| 68 | POST | [`/api/v1/admin/categories`](#e68) | Buat kategori |
| 69 | GET | [`/api/v1/admin/blueprints`](#e69) | Daftar atau buat paket tryout |
| 70 | POST | [`/api/v1/admin/blueprints`](#e70) | Daftar atau buat paket tryout |
| 71 | POST | [`/api/v1/admin/blueprints/{id}/versions`](#e71) | Buat komposisi draft baru |
| 72 | POST | [`/api/v1/admin/blueprints/{id}/versions/{versionId}/publish`](#e72) | Terbitkan versi blueprint |
| 73 | PATCH | [`/api/v1/admin/users/{id}/status`](#e73) | Suspend atau pulihkan akun |
| 74 | PATCH | [`/api/v1/admin/topics/{id}`](#e74) | Perbarui topik |
| 75 | PATCH | [`/api/v1/admin/subtests/{id}`](#e75) | Perbarui subtes |
| 76 | PATCH | [`/api/v1/admin/plans/{id}/availability`](#e76) | Buka atau hentikan penjualan |
| 77 | PATCH | [`/api/v1/admin/exam-types/{id}`](#e77) | Perbarui jenis ujian |
| 78 | PATCH | [`/api/v1/admin/categories/{id}`](#e78) | Perbarui kategori |
| 79 | PATCH | [`/api/v1/admin/blueprints/{id}/status`](#e79) | Aktifkan atau arsip katalog |
| 80 | GET | [`/api/v1/staff/questions/{id}/versions/{versionId}`](#e80) | Baca versi soal tertentu |
| 81 | GET | [`/api/v1/staff/question-bank`](#e81) | Cari master dengan pagination |
| 82 | GET | [`/api/v1/staff/pool-requirements`](#e82) | Kebutuhan stok blueprint |
| 83 | GET | [`/api/v1/staff/import-history`](#e83) | Cari riwayat unggahan |
| 84 | GET | [`/api/v1/staff/dashboard`](#e84) | Ringkasan bank soal |
| 85 | GET | [`/api/v1/staff/audit`](#e85) | Audit ruang konten |
| 86 | GET | [`/api/v1/admin/users/{id}`](#e86) | Detail akun untuk dukungan |
| 87 | GET | [`/api/v1/admin/users/{id}/attempts`](#e87) | Riwayat sesi untuk dukungan |
| 88 | GET | [`/api/v1/admin/taxonomy`](#e88) | Hierarki kategori lengkap |
| 89 | GET | [`/api/v1/admin/subscriptions`](#e89) | Daftar langganan dan grant |
| 90 | GET | [`/api/v1/admin/roles`](#e90) | Matriks peran |
| 91 | GET | [`/api/v1/admin/plans/{id}`](#e91) | Detail revisi harga |
| 92 | GET | [`/api/v1/admin/orders`](#e92) | Daftar transaksi |
| 93 | GET | [`/api/v1/admin/orders/{id}`](#e93) | Detail transaksi |
| 94 | GET | [`/api/v1/admin/jobs`](#e94) | Daftar job tersensor |
| 95 | GET | [`/api/v1/admin/dashboard`](#e95) | Ringkasan operasional |
| 96 | GET | [`/api/v1/admin/blueprints/{id}`](#e96) | Detail blueprint dan versinya |
| 97 | GET | [`/api/v1/admin/blueprints/{id}/versions/{versionId}/readiness`](#e97) | Cek kebutuhan pool |
| 98 | GET | [`/api/v1/admin/audit`](#e98) | Audit operasional |

## Publik dan identity

<a id="e1"></a>
### 1. Health aplikasi

**GET `/api/v1/health`**

Memeriksa proses API dan waktu UTC. Ini bukan pemeriksaan lengkap semua dependency.

Akses: **Publik**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/health HTTP/1.1
Host: localhost:8080
```

Body request: **tidak ada**.

#### Response

Schema: [ServiceStatus](data-contracts.md#servicestatus).

```json
{
  "status": "UP",
  "service": "pastisiap-backend",
  "timestamp": "2026-11-19T07:00:00Z"
}
```

#### Error/kondisi yang perlu ditangani

- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e2"></a>
### 2. Ambil token CSRF

**GET `/api/v1/csrf`**

Membuat/menggunakan session dan memberikan headerName serta token untuk mutasi.

Akses: **Publik**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/csrf HTTP/1.1
Host: localhost:8080
```

Body request: **tidak ada**.

#### Response

Schema: [CsrfResponse](data-contracts.md#csrfresponse).

```json
{
  "headerName": "X-CSRF-TOKEN",
  "token": "example-csrf-token"
}
```

- Simpan cookie dari respons; kirim token dengan cookie session yang sama. Token contoh dalam dokumen tidak dapat dipakai.

#### Error/kondisi yang perlu ditangani

- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e3"></a>
### 3. Registrasi password

**POST `/api/v1/auth/register`**

Membuat SISWA dengan role CUSTOMER dan login otomatis.

Akses: **Publik**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/auth/register HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [RegisterRequest](data-contracts.md#registerrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "email": "siswa@example.com",
  "displayName": "Nama Siswa",
  "password": "PasswordAman123!"
}
```

#### Response

Schema: [AuthResponse](data-contracts.md#authresponse).

```json
{
  "user": {
    "id": "00000000-0000-4000-8000-000000000001",
    "email": "siswa@example.com",
    "displayName": "Nama Siswa",
    "userType": "SISWA",
    "roles": [
      "CUSTOMER"
    ]
  }
}
```

- Password 12–72 karakter, displayName 2–120, email valid maksimal 320. Tidak menerima pilihan role/userType/premium.
- Respons membuat/merotasi cookie session. Tidak ada accessToken atau refreshToken.

#### Error/kondisi yang perlu ditangani

- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 409 EMAIL_ALREADY_REGISTERED.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e4"></a>
### 4. Login password

**POST `/api/v1/auth/login`**

Memverifikasi email/password lalu membentuk session.

Akses: **Publik**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/auth/login HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [LoginRequest](data-contracts.md#loginrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "email": "siswa@example.com",
  "password": "PasswordAman123!"
}
```

#### Response

Schema: [AuthResponse](data-contracts.md#authresponse).

```json
{
  "user": {
    "id": "00000000-0000-4000-8000-000000000001",
    "email": "siswa@example.com",
    "displayName": "Nama Siswa",
    "userType": "SISWA",
    "roles": [
      "CUSTOMER"
    ]
  }
}
```

- Login password untuk akun SSO tanpa password tidak tersedia. Email dinormalisasi oleh service.

#### Error/kondisi yang perlu ditangani

- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 401 INVALID_CREDENTIALS.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e5"></a>
### 5. Logout

**POST `/api/v1/auth/logout`**

Ditangani filter Spring Security; menghapus session saat ini.

Akses: **Session CSRF; tidak memerlukan autentikasi aktif untuk menghapus session**. Sukses utama: **HTTP 204**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/auth/logout HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
```

Body request: **tidak ada**.

#### Response

HTTP 204, **body kosong**.

- Body respons kosong; jangan memanggil response.json(). Logout juga bisa mengembalikan 204 jika tidak ada autentikasi aktif, selama CSRF valid.

#### Error/kondisi yang perlu ditangani

- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## Profil dan read API siswa

<a id="e6"></a>
### 6. Ringkasan akun aktif

**GET `/api/v1/users/current`**

Membaca akun dari principal session, tanpa user ID dari client.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/users/current HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [UserProfileResponse](data-contracts.md#userprofileresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000001",
  "email": "siswa@example.com",
  "displayName": "Nama Siswa",
  "userType": "SISWA",
  "roles": [
    "CUSTOMER"
  ]
}
```

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e7"></a>
### 7. Profil dan target siswa

**GET `/api/v1/users/current/profile`**

Membaca sekolah, kelas, target dan skor sasaran.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/users/current/profile HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [StudentProfileResponse](data-contracts.md#studentprofileresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000001",
  "email": "siswa@example.com",
  "displayName": "Nama Siswa",
  "userType": "SISWA",
  "schoolName": "SMA Negeri 1 Bandung",
  "schoolGrade": "XII",
  "leaderboardOptIn": false,
  "targetScore": 800,
  "targetKind": "CAMPUS",
  "targetInstitution": "Universitas Contoh",
  "targetProgram": "Informatika"
}
```

- Field sekolah/target boleh null sebelum pengguna melengkapinya. Role tersedia pada ringkasan akun, bukan response profil ini.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e8"></a>
### 8. Update profil dan target

**PATCH `/api/v1/users/current/profile`**

Memperbarui data profil sesuai form lengkap.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PATCH /api/v1/users/current/profile HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [UpdateStudentProfileRequest](data-contracts.md#updatestudentprofilerequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "displayName": "Nama Siswa",
  "schoolName": "SMA Negeri 1 Bandung",
  "schoolGrade": "XII",
  "targetCategory": "UTBK",
  "targetInstitution": "Universitas Contoh",
  "targetProgram": "Informatika",
  "targetScore": 800
}
```

#### Response

Schema: [StudentProfileResponse](data-contracts.md#studentprofileresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000001",
  "email": "siswa@example.com",
  "displayName": "Nama Siswa",
  "userType": "SISWA",
  "schoolName": "SMA Negeri 1 Bandung",
  "schoolGrade": "XII",
  "leaderboardOptIn": false,
  "targetScore": 800,
  "targetKind": "CAMPUS",
  "targetInstitution": "Universitas Contoh",
  "targetProgram": "Informatika"
}
```

- Walaupun method PATCH, displayName, schoolName, schoolGrade, targetCategory dan targetInstitution tetap wajib.
- targetCategory dipetakan ke targetKind: UTBK→CAMPUS, CPNS→CIVIL_SERVICE, KEDINASAN→ACADEMY, SEKOLAH→SCHOOL.
- targetProgram dan targetScore boleh null; targetScore 0–1000. leaderboardOptIn, email, roles dan userType tidak dapat diubah lewat body ini.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 INVALID_REQUEST untuk kategori target tidak didukung.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e9"></a>
### 9. Katalog tryout

**GET `/api/v1/tryouts`**

Membaca paket TRYOUT aktif, jumlah soal dan riwayat ringkas lintas versi dalam keluarga blueprint yang sama.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| category | query | Tidak | string; — |
| category | query | Tidak | string; — |

#### Contoh request

```http
GET /api/v1/tryouts?category=UTBK HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [TryoutResponse](data-contracts.md#tryoutresponse).

```json
[
  {
    "id": "00000000-0000-4000-8000-000000000002",
    "code": "DEMO_TRYOUT",
    "title": "Tryout UTBK - Contoh",
    "category": "UTBK",
    "exam_type": "Tes Numerik Contoh",
    "blueprint_version_id": "00000000-0000-4000-8000-000000000003",
    "duration_seconds": 5400,
    "question_count": 3,
    "attempt_count": 0,
    "latest_score": null,
    "latest_completed_at": null
  }
]
```

- Array kosong valid. attempt_count dan latest_score tetap mencakup sesi versi blueprint sebelumnya; blueprint_version_id menunjuk versi aktif.
- Beberapa key sengaja snake_case, sesuai DTO legacy.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e10"></a>
### 10. Detail dan kesiapan katalog

**GET `/api/v1/tryouts/{id}`**

Menampilkan durasi, metode/skala dan kapasitas pool section.

Akses: **Session + role CUSTOMER**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/tryouts/00000000-0000-4000-8000-000000000002 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [TryoutDetail](data-contracts.md#tryoutdetail).

```json
{
  "id": "00000000-0000-4000-8000-000000000002",
  "title": "Tryout UTBK - Contoh",
  "category": "UTBK",
  "durationSeconds": 5400,
  "scoringMethod": "BINARY",
  "scale": 100,
  "demo": true,
  "ready": true,
  "sections": [
    {
      "id": "00000000-0000-4000-8000-000000000023",
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "name": "Penalaran Numerik Contoh",
      "position": 1,
      "questionCount": 3,
      "available": 12
    }
  ]
}
```

- ready mengukur stok global, bukan ketersediaan personal setelah exposure harian. Start dapat tetap menolak POOL_NOT_READY.
- id adalah blueprint/package ID, bukan blueprint_version_id.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e11"></a>
### 11. Topik practice

**GET `/api/v1/practice/topics`**

Daftar topik aktif dan hitungan soal terbit yang tidak diarsipkan.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| category | query | Tidak | string; — |
| category | query | Tidak | string; — |

#### Contoh request

```http
GET /api/v1/practice/topics?category=UTBK HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [PracticeTopicResponse](data-contracts.md#practicetopicresponse).

```json
[
  {
    "id": "00000000-0000-4000-8000-000000000005",
    "topic": "Aritmetika Dasar Contoh",
    "subtest_code": "NUMERIK",
    "subtest": "Penalaran Numerik Contoh",
    "category": "UTBK",
    "question_count": 12
  }
]
```

- Topik dengan question_count=0 dapat muncul. Ketersediaan final diverifikasi pada start.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e12"></a>
### 12. Detail topik dan batas latihan

**GET `/api/v1/practice/topics/{id}`**

Membaca satu topik dengan scope, stok terbit dan pilihan jumlah/durasi latihan.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/practice/topics/00000000-0000-4000-8000-000000000006 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PracticeTopicDetail](data-contracts.md#practicetopicdetail).

```json
{
  "id": "00000000-0000-4000-8000-000000000005",
  "topic": "Aritmetika Dasar Contoh",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "subtestCode": "NUMERIK",
  "subtest": "Penalaran Numerik Contoh",
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "examType": "Tes Numerik Contoh",
  "category": "UTBK",
  "questionCount": 12,
  "defaultQuestionCount": 10,
  "maxQuestionCount": 12,
  "secondsPerQuestion": 120,
  "ready": true
}
```

- questionCount adalah stok global. maxQuestionCount=min(stok,50); defaultQuestionCount=min(stok,10). Jika stok nol keduanya 0 dan ready=false.
- secondsPerQuestion saat ini 120. UI memilih bilangan bulat 1..maxQuestionCount lalu mengirimnya ke StartPracticeRequest.
- id adalah topic ID, bukan ID blueprint practice internal.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e13"></a>
### 13. Kuota harian WIB

**GET `/api/v1/users/current/tryout-quota`**

Menghitung pemakaian dan sisa dua start tryout per tanggal Asia/Jakarta.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/users/current/tryout-quota HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [QuotaResponse](data-contracts.md#quotaresponse).

```json
{
  "dateWib": "2026-11-19",
  "used": 1,
  "limit": 2,
  "remaining": 1
}
```

- Practice tidak memakai kuota ini. Gratis dan Premium memakai batas teknis yang sama pada implementasi saat ini.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e14"></a>
### 14. Riwayat sesi sendiri

**GET `/api/v1/users/current/attempts`**

Riwayat milik akun, dengan filter mode/paket/topik dan pagination offset/limit.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| mode | query | Tidak | string; pattern=TRYOUT\|PRACTICE |
| tryoutId | query | Tidak | string (uuid); — |
| topicId | query | Tidak | string (uuid); — |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |

#### Contoh request

```http
GET /api/v1/users/current/attempts?offset=0&limit=20 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [AttemptHistoryResponse](data-contracts.md#attempthistoryresponse).

```json
[
  {
    "id": "00000000-0000-4000-8000-000000000006",
    "tryout_id": "00000000-0000-4000-8000-000000000002",
    "tryout_title": "Tryout UTBK - Contoh",
    "mode": "TRYOUT",
    "status": "COMPLETED",
    "started_at": "2026-11-19T07:00:00Z",
    "finalized_at": "2026-11-19T07:05:00Z",
    "total_score": 33.3333,
    "details": "{\"label\":\"Indeks latihan PastiSiap; bukan skor resmi\",\"correct\":1,\"wrong\":0,\"empty\":2}",
    "topic_id": null,
    "topic_name": null,
    "category": "UTBK",
    "terminal_reason": "SUBMITTED",
    "question_count": 3,
    "duration_seconds": 5400
  }
]
```

- Query opsional: mode=TRYOUT/PRACTICE, tryoutId atau topicId, offset>=0, limit=1..100 (default 100). Array response lama tetap dipertahankan.
- Untuk riwayat latihan kirim mode=PRACTICE&topicId=<uuid>; jangan membandingkan tryout_id internal dengan ID topik. Paket tryout difilter memakai tryoutId dan mencakup seluruh versi.
- tryoutId dan topicId tidak boleh dikirim bersamaan; mode harus cocok dengan filter. Filter selalu diterapkan sesudah pembatasan akun sehingga data pengguna lain tidak terbuka.
- terminal_reason membedakan SUBMITTED, ABANDONED dan TIMED_OUT walaupun status telah COMPLETED. details tetap string JSON/null.
- total_score dan finalized_at dapat null untuk sesi yang belum selesai.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 INVALID_HISTORY_FILTER.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e15"></a>
### 15. Daftar subscription sendiri

**GET `/api/v1/users/current/subscription`**

Mengembalikan periode langganan dan active berdasarkan entitlement grant yang masih berlaku.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/users/current/subscription HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [SubscriptionResponse](data-contracts.md#subscriptionresponse).

```json
[
  {
    "id": "00000000-0000-4000-8000-000000000040",
    "starts_at": "2026-11-01T00:00:00Z",
    "ends_at": "2026-12-01T00:00:00Z",
    "status": "ACTIVE",
    "active": true
  }
]
```

- Tanpa langganan mengembalikan []. Contoh item aktif bersifat ilustrasi; belum ada API pembelian/grant.
- Bukan satu objek boolean premium; active dibaca server dari interval grant yang tidak dicabut.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e16"></a>
### 16. Baca leaderboard

**GET `/api/v1/leaderboard`**

Maksimal 100 entry dari cohort aktif yang belum berakhir dan user masih opt-in.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| category | query | Tidak | string; — |
| category | query | Tidak | string; — |

#### Contoh request

```http
GET /api/v1/leaderboard?category=UTBK HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [LeaderboardResponse](data-contracts.md#leaderboardresponse).

```json
[
  {
    "rank": 1,
    "display_name": "Siswa Contoh",
    "total_score": 80,
    "category": "UTBK",
    "period_start": "2026-11-01T00:00:00Z",
    "period_end": "2026-12-01T00:00:00Z"
  }
]
```

- Array kosong adalah respons normal tanpa cohort/entry yang diprovisikan. Contoh item hanya ilustrasi kontrak.
- Sesi acak tidak otomatis ditambahkan ke leaderboard.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## Lifecycle tryout dan practice

<a id="e17"></a>
### 17. Mulai tryout

**POST `/api/v1/attempts`**

Membuat attempt TRYOUT dari blueprint aktif, atau mengembalikan replay/sesi aktif.

Akses: **Session + role CUSTOMER**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| Idempotency-Key | header | Ya | string; minLength=8; maxLength=128; pattern=^[A-Za-z0-9._:-]{8,128}$ |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/attempts HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Idempotency-Key: example-start-001
Content-Type: application/json
```

Body: [StartTryoutRequest](data-contracts.md#starttryoutrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "tryoutId": "00000000-0000-4000-8000-000000000002"
}
```

#### Response

Schema: [SessionResponse](data-contracts.md#sessionresponse).

```json
{
  "attemptId": "00000000-0000-4000-8000-000000000006",
  "tryoutId": "00000000-0000-4000-8000-000000000002",
  "mode": "TRYOUT",
  "status": "IN_PROGRESS",
  "terminalReason": null,
  "serverNow": "2026-11-19T07:00:00Z",
  "startedAt": "2026-11-19T07:00:00Z",
  "deadlineAt": "2026-11-19T08:30:00Z",
  "questions": [
    {
      "position": 1,
      "answered": false,
      "flagged": false,
      "revision": 0
    },
    {
      "position": 2,
      "answered": false,
      "flagged": false,
      "revision": 0
    },
    {
      "position": 3,
      "answered": false,
      "flagged": false,
      "revision": 0
    }
  ],
  "finalizedAt": null,
  "context": {
    "title": "Tryout UTBK - Contoh",
    "category": "UTBK",
    "examType": "Tes Numerik Contoh",
    "topicId": null,
    "topicName": null,
    "sections": [
      {
        "position": 1,
        "subtestId": "00000000-0000-4000-8000-000000000004",
        "name": "Penalaran Numerik Contoh",
        "questionCount": 3
      }
    ]
  }
}
```

- Key 8–128 karakter dari huruf, angka, titik, underscore, colon atau hyphen. Key sama + body berbeda ditolak.
- Jika sesi aktif ada, response bisa berupa sesi berbeda dari paket yang diminta. Selalu baca attemptId/mode/status response.
- Pool kurang rollback seluruh start dan tidak memakai kuota; start berhasil memakai satu kuota.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 IDEMPOTENCY_KEY_REQUIRED.
- 409 IDEMPOTENCY_CONFLICT.
- 409 DAILY_QUOTA_EXHAUSTED.
- 409 POOL_NOT_READY.
- 409 OVERLAPPING_SECTIONS.
- 409 UNSUPPORTED_SCORING_POLICY.
- 409 UNSUPPORTED_BLUEPRINT_FILTER.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e18"></a>
### 18. Mulai practice

**POST `/api/v1/practice/attempts`**

Memulai latihan dalam satu topik, memakai blueprint internal dan pool yang sama.

Akses: **Session + role CUSTOMER**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| Idempotency-Key | header | Ya | string; minLength=8; maxLength=128; pattern=^[A-Za-z0-9._:-]{8,128}$ |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/practice/attempts HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Idempotency-Key: example-start-001
Content-Type: application/json
```

Body: [StartPracticeRequest](data-contracts.md#startpracticerequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "topicId": "00000000-0000-4000-8000-000000000005",
  "questionCount": 3
}
```

#### Response

Schema: [SessionResponse](data-contracts.md#sessionresponse).

```json
{
  "attemptId": "00000000-0000-4000-8000-000000000006",
  "tryoutId": "00000000-0000-4000-8000-000000000070",
  "mode": "PRACTICE",
  "status": "IN_PROGRESS",
  "terminalReason": null,
  "serverNow": "2026-11-19T07:00:00Z",
  "startedAt": "2026-11-19T07:00:00Z",
  "deadlineAt": "2026-11-19T07:06:00Z",
  "questions": [
    {
      "position": 1,
      "answered": false,
      "flagged": false,
      "revision": 0
    },
    {
      "position": 2,
      "answered": false,
      "flagged": false,
      "revision": 0
    },
    {
      "position": 3,
      "answered": false,
      "flagged": false,
      "revision": 0
    }
  ],
  "finalizedAt": null,
  "context": {
    "title": "Latihan Aritmetika Dasar Contoh",
    "category": "UTBK",
    "examType": "Tes Numerik Contoh",
    "topicId": "00000000-0000-4000-8000-000000000005",
    "topicName": "Aritmetika Dasar Contoh",
    "sections": [
      {
        "position": 1,
        "subtestId": "00000000-0000-4000-8000-000000000004",
        "name": "Penalaran Numerik Contoh",
        "questionCount": 3
      }
    ]
  }
}
```

- questionCount wajib dikirim 1–50. Durasi saat ini questionCount × 120 detik; tidak mengambil quota tryout.
- Satu sesi aktif dibagi dengan tryout. tryoutId response tetap ID blueprint internal demi kompatibilitas; link kembali ke detail latihan menggunakan context.topicId.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 IDEMPOTENCY_KEY_REQUIRED.
- 409 IDEMPOTENCY_CONFLICT.
- 409 POOL_NOT_READY.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e19"></a>
### 19. Resume sesi aktif

**GET `/api/v1/users/current/attempts/active`**

Mengembalikan sesi IN_PROGRESS/FINALIZING, atau memfinalisasi sesi yang sudah kedaluwarsa.

Akses: **Session + role CUSTOMER**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/users/current/attempts/active HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [SessionResponse](data-contracts.md#sessionresponse).

```json
{
  "attemptId": "00000000-0000-4000-8000-000000000006",
  "tryoutId": "00000000-0000-4000-8000-000000000002",
  "mode": "TRYOUT",
  "status": "IN_PROGRESS",
  "terminalReason": null,
  "serverNow": "2026-11-19T07:00:00Z",
  "startedAt": "2026-11-19T07:00:00Z",
  "deadlineAt": "2026-11-19T08:30:00Z",
  "questions": [
    {
      "position": 1,
      "answered": false,
      "flagged": false,
      "revision": 0
    },
    {
      "position": 2,
      "answered": false,
      "flagged": false,
      "revision": 0
    },
    {
      "position": 3,
      "answered": false,
      "flagged": false,
      "revision": 0
    }
  ],
  "finalizedAt": null,
  "context": {
    "title": "Tryout UTBK - Contoh",
    "category": "UTBK",
    "examType": "Tes Numerik Contoh",
    "topicId": null,
    "topicName": null,
    "sections": [
      {
        "position": 1,
        "subtestId": "00000000-0000-4000-8000-000000000004",
        "name": "Penalaran Numerik Contoh",
        "questionCount": 3
      }
    ]
  }
}
```

- Jika tidak ada sesi, HTTP 200 dengan body kosong. Client perlu memeriksa body sebelum parsing JSON.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Tidak ada sesi aktif adalah sukses 200 kosong, bukan error.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e20"></a>
### 20. Navigator dan status sesi

**GET `/api/v1/users/current/attempts/{id}`**

Membaca deadline server, status, navigator dan konteks sesi milik pengguna.

Akses: **Session + role CUSTOMER**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/users/current/attempts/00000000-0000-4000-8000-000000000006 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [SessionResponse](data-contracts.md#sessionresponse).

```json
{
  "attemptId": "00000000-0000-4000-8000-000000000006",
  "tryoutId": "00000000-0000-4000-8000-000000000002",
  "mode": "TRYOUT",
  "status": "IN_PROGRESS",
  "terminalReason": null,
  "serverNow": "2026-11-19T07:00:00Z",
  "startedAt": "2026-11-19T07:00:00Z",
  "deadlineAt": "2026-11-19T08:30:00Z",
  "questions": [
    {
      "position": 1,
      "answered": false,
      "flagged": false,
      "revision": 0
    },
    {
      "position": 2,
      "answered": false,
      "flagged": false,
      "revision": 0
    },
    {
      "position": 3,
      "answered": false,
      "flagged": false,
      "revision": 0
    }
  ],
  "finalizedAt": null,
  "context": {
    "title": "Tryout UTBK - Contoh",
    "category": "UTBK",
    "examType": "Tes Numerik Contoh",
    "topicId": null,
    "topicName": null,
    "sections": [
      {
        "position": 1,
        "subtestId": "00000000-0000-4000-8000-000000000004",
        "name": "Penalaran Numerik Contoh",
        "questionCount": 3
      }
    ]
  }
}
```

- context menyediakan title/category/examType, topicId/topicName untuk practice, serta nama/jumlah soal subtes versi blueprint sesi. Frontend tidak perlu menebak kategori atau memanggil detail katalog terbaru.
- finalizedAt berasal dari attempt dan null sebelum finalize; hasil tidak perlu mencari timestamp di 100 history terbaru.
- GET ini dapat mengubah sesi kedaluwarsa menjadi FINALIZING dan membuat job scoring.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e21"></a>
### 21. Baca satu soal dalam sesi

**GET `/api/v1/users/current/attempts/{attemptId}/questions/{position}`**

Membaca versi soal dan urutan opsi snapshot milik pengguna.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| attemptId | path | Ya | string (uuid); — |
| position | path | Ya | integer (int32); — |

#### Contoh request

```http
GET /api/v1/users/current/attempts/00000000-0000-4000-8000-000000000006/questions/1 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [AttemptQuestionResponse](data-contracts.md#attemptquestionresponse).

```json
{
  "position": 1,
  "stem": {
    "text": "Berapa hasil 2 + 3?"
  },
  "options": [
    {
      "id": "00000000-0000-4000-8000-000000000011",
      "code": "A",
      "content": {
        "text": "4"
      },
      "correct": null
    },
    {
      "id": "00000000-0000-4000-8000-000000000012",
      "code": "B",
      "content": {
        "text": "5"
      },
      "correct": null
    }
  ],
  "selectedOptionId": null,
  "flagged": false,
  "revision": 0,
  "correct": null,
  "explanation": null,
  "scope": {
    "subtestId": "00000000-0000-4000-8000-000000000004",
    "subtestName": "Penalaran Numerik Contoh",
    "topicId": "00000000-0000-4000-8000-000000000005",
    "topicName": "Aritmetika Dasar Contoh"
  }
}
```

- position dimulai dari 1. selectedOptionId adalah UUID opsi, bukan huruf display.
- Sebelum score tersedia, options[].correct, correct dan explanation bernilai null. Tidak mengirim score_weight.
- Setelah scoring, correct null juga dapat berarti soal belum dijawab. Response siswa tidak berisi sumber PDF/kunci file.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e22"></a>
### 22. Baca asset privat soal

**GET `/api/v1/users/current/attempts/{attemptId}/questions/{position}/assets/{assetId}`**

Memeriksa kepemilikan attempt dan relasi asset dengan versi soal sebelum mengirim binary.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| attemptId | path | Ya | string (uuid); — |
| position | path | Ya | integer (int32); — |
| assetId | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/users/current/attempts/00000000-0000-4000-8000-000000000006/questions/1/assets/00000000-0000-4000-8000-000000000030 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

```http
HTTP/1.1 200 OK
Content-Type: application/pdf atau MIME file
Cache-Control: no-store

[binary file]
```

- 200 binary dengan Content-Disposition inline dan Cache-Control no-store.
- 404 asset tidak ditemukan dapat memiliki body kosong, bukan ApiError. File IMPORT_SOURCE selalu dikecualikan.
- Tidak ada endpoint upload/attach asset baru pada flow siswa.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e23"></a>
### 23. Simpan jawaban/revision

**PUT `/api/v1/users/current/attempts/{id}/answers/{position}`**

Autosave jawaban terakhir dengan optimistic revision.

Akses: **Session + role CUSTOMER**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| position | path | Ya | integer (int32); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PUT /api/v1/users/current/attempts/00000000-0000-4000-8000-000000000006/answers/1 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [AnswerRequest](data-contracts.md#answerrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "selectedOptionId": "00000000-0000-4000-8000-000000000012",
  "flagged": false,
  "expectedRevision": 0
}
```

#### Response

Schema: [AnswerResponse](data-contracts.md#answerresponse).

```json
{
  "position": 1,
  "selectedOptionId": "00000000-0000-4000-8000-000000000012",
  "flagged": false,
  "revision": 1,
  "receivedAt": "2026-11-19T07:01:00Z"
}
```

- expectedRevision wajib >=0. Awal 0, sukses menjadi 1; stale revision ditolak.
- selectedOptionId null berarti mengosongkan pilihan. flagged tidak dikirim/null dianggap false.
- Update parsial flagged saja tetap dapat mengosongkan pilihan; kirim selectedOptionId yang ingin dipertahankan.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 INVALID_OPTION.
- 409 REVISION_CONFLICT.
- 409 ATTEMPT_CLOSED.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e24"></a>
### 24. Submit atau abandon

**POST `/api/v1/users/current/attempts/{id}/finalize`**

Menutup attempt dan membuat job scoring idempoten.

Akses: **Session + role CUSTOMER**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/users/current/attempts/00000000-0000-4000-8000-000000000006/finalize HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [FinalizeRequest](data-contracts.md#finalizerequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "abandon": false
}
```

#### Response

Schema: [SessionResponse](data-contracts.md#sessionresponse).

```json
{
  "attemptId": "00000000-0000-4000-8000-000000000006",
  "tryoutId": "00000000-0000-4000-8000-000000000002",
  "mode": "TRYOUT",
  "status": "FINALIZING",
  "terminalReason": "SUBMITTED",
  "serverNow": "2026-11-19T07:05:00Z",
  "startedAt": "2026-11-19T07:00:00Z",
  "deadlineAt": "2026-11-19T08:30:00Z",
  "questions": [
    {
      "position": 1,
      "answered": false,
      "flagged": false,
      "revision": 0
    },
    {
      "position": 2,
      "answered": false,
      "flagged": false,
      "revision": 0
    },
    {
      "position": 3,
      "answered": false,
      "flagged": false,
      "revision": 0
    }
  ],
  "finalizedAt": null,
  "context": {
    "title": "Tryout UTBK - Contoh",
    "category": "UTBK",
    "examType": "Tes Numerik Contoh",
    "topicId": null,
    "topicName": null,
    "sections": [
      {
        "position": 1,
        "subtestId": "00000000-0000-4000-8000-000000000004",
        "name": "Penalaran Numerik Contoh",
        "questionCount": 3
      }
    ]
  }
}
```

- Body boleh kosong atau {}; abandon false/null berarti submit. abandon true berarti keluar/abandon.
- Jika deadline lewat, terminalReason TIMED_OUT mengalahkan parameter abandon.
- Status normal berikutnya FINALIZING, bukan nilai langsung. Request berulang tidak membuat hasil/job ganda.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e25"></a>
### 25. Status dan hasil scoring

**GET `/api/v1/users/current/attempts/{id}/result`**

Membaca status attempt dan score terbaru.

Akses: **Session + role CUSTOMER**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/users/current/attempts/00000000-0000-4000-8000-000000000006/result HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [ResultResponse](data-contracts.md#resultresponse).

```json
{
  "attemptId": "00000000-0000-4000-8000-000000000006",
  "status": "COMPLETED",
  "terminalReason": "SUBMITTED",
  "method": "BINARY",
  "policyVersion": "DEMO_BINARY:1",
  "totalScore": 33.3333,
  "details": {
    "label": "Indeks latihan PastiSiap; bukan skor resmi",
    "correct": 1,
    "wrong": 0,
    "empty": 2
  },
  "sections": [
    {
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "correct": 1,
      "wrong": 0,
      "empty": 2,
      "points": 1,
      "maxPoints": 3,
      "accuracy": 33.3333
    }
  ],
  "finalizedAt": "2026-11-19T07:05:00Z"
}
```

- Sebelum hasil: totalScore/method/policyVersion/details null dan sections kosong. Jangan menganggap null sebagai nilai nol.
- status bisa IN_PROGRESS/FINALIZING/COMPLETED. Belum ada status API FAILED khusus scoring; telusuri job/log jika lama.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e26"></a>
### 26. Review seluruh soal

**GET `/api/v1/users/current/attempts/{id}/review`**

Mengembalikan semua nomor snapshot beserta jawaban/kunci/pembahasan setelah COMPLETED.

Akses: **Session + role CUSTOMER**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/users/current/attempts/00000000-0000-4000-8000-000000000006/review HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [AttemptQuestionResponse](data-contracts.md#attemptquestionresponse).

```json
[
  {
    "position": 1,
    "stem": {
      "text": "Berapa hasil 2 + 3?"
    },
    "options": [
      {
        "id": "00000000-0000-4000-8000-000000000011",
        "code": "A",
        "content": {
          "text": "4"
        },
        "correct": false
      },
      {
        "id": "00000000-0000-4000-8000-000000000012",
        "code": "B",
        "content": {
          "text": "5"
        },
        "correct": true
      }
    ],
    "selectedOptionId": "00000000-0000-4000-8000-000000000012",
    "flagged": false,
    "revision": 1,
    "correct": true,
    "explanation": "Dua ditambah tiga sama dengan lima.",
    "scope": {
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "subtestName": "Penalaran Numerik Contoh",
      "topicId": "00000000-0000-4000-8000-000000000005",
      "topicName": "Aritmetika Dasar Contoh"
    }
  }
]
```

- Array ini bukan master soal terbaru. Jumlah contoh dipersingkat menjadi satu item untuk keterbacaan.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 409 RESULT_PROCESSING.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## Bank soal dan impor

<a id="e27"></a>
### 27. Taksonomi aktif untuk petugas

**GET `/api/v1/staff/taxonomy`**

Mengambil kategori, exam type, subtes dan topik dalam bentuk baris datar.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/staff/taxonomy HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [TaxonomyResponse](data-contracts.md#taxonomyresponse).

```json
[
  {
    "categoryId": "00000000-0000-4000-8000-000000000024",
    "categoryCode": "UTBK",
    "categoryName": "UTBK",
    "examTypeId": "00000000-0000-4000-8000-000000000025",
    "examTypeCode": "DEMO",
    "examTypeName": "Tes Numerik Contoh",
    "subtestId": "00000000-0000-4000-8000-000000000004",
    "subtestName": "Penalaran Numerik Contoh",
    "topicId": "00000000-0000-4000-8000-000000000005",
    "topicName": "Aritmetika Dasar Contoh"
  }
]
```

- Satu subtes dengan beberapa topik muncul beberapa baris. topicId/topicName boleh null jika subtes belum mempunyai topik.
- Pengelolaan taxonomy tersedia melalui endpoint admin, terpisah dari izin petugas.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e28"></a>
### 28. Upload sumber PDF

**POST `/api/v1/staff/imports`**

Menyimpan sumber privat dan enqueue IMPORT_PDF di luar thread ekstraksi HTTP.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 202**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/staff/imports HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: multipart/form-data; boundary=example-boundary

--example-boundary
Content-Disposition: form-data; name="metadata"
Content-Type: application/json

{
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceTitle": "Contoh PDF PastiSiap",
  "sourceYear": 2026
}
--example-boundary
Content-Disposition: form-data; name="file"; filename="sample-questions.pdf"
Content-Type: application/pdf

[binary PDF]
--example-boundary--
```

Key `file` berisi binary PDF dan `metadata` mengikuti [ImportMetadata](data-contracts.md#importmetadata). Gunakan file picker Postman agar boundary dibentuk otomatis.

#### Response

Schema: [ImportResponse](data-contracts.md#importresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000020",
  "status": "QUEUED",
  "fileId": "00000000-0000-4000-8000-000000000021",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceTitle": "Contoh PDF PastiSiap",
  "sourceYear": 2026,
  "pageCount": null,
  "candidateCount": 0,
  "validCount": 0,
  "invalidCount": 0,
  "errorCode": null,
  "jobStatus": "QUEUED",
  "jobErrorCode": null,
  "createdAt": "2026-11-19T07:00:00Z"
}
```

- Multipart file + metadata; part metadata wajib application/json. Body metadata mengikuti ImportMetadata.
- Maksimal 25MB, request 26MB. PDF invalid/encrypted/page limit dapat baru gagal pada worker; 202 bukan jaminan konten valid.
- Checksum sama dalam uploader/subtest/topic yang sama mengembalikan batch lama, termasuk bila sourceTitle/sourceYear input berbeda. Tidak otomatis membuat ulang batch yang FAILED.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 PDF_REQUIRED.
- 400 INVALID_PDF_SIGNATURE.
- 400 INVALID_CONTENT_SCOPE.
- 413 FILE_TOO_LARGE / HTTP_413.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e29"></a>
### 29. Daftar riwayat impor

**GET `/api/v1/staff/imports`**

Daftar batch seluruh petugas, terbaru dahulu.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| offset | query | Tidak | integer (int32); default=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100; default=20 |

#### Contoh request

```http
GET /api/v1/staff/imports?offset=0&limit=20 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [ImportResponse](data-contracts.md#importresponse).

```json
[
  {
    "id": "00000000-0000-4000-8000-000000000020",
    "status": "READY",
    "fileId": "00000000-0000-4000-8000-000000000021",
    "subtestId": "00000000-0000-4000-8000-000000000004",
    "topicId": "00000000-0000-4000-8000-000000000005",
    "sourceTitle": "Contoh PDF PastiSiap",
    "sourceYear": 2026,
    "pageCount": 1,
    "candidateCount": 2,
    "validCount": 2,
    "invalidCount": 0,
    "errorCode": null,
    "jobStatus": "SUCCEEDED",
    "jobErrorCode": null,
    "createdAt": "2026-11-19T07:00:00Z"
  }
]
```

- Bukan hanya unggahan akun sendiri. Daftar berupa array tanpa total count/pagination envelope.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e30"></a>
### 30. Status batch dan job

**GET `/api/v1/staff/imports/{id}`**

Memantau status impor, jumlah kandidat dan error/status job.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/staff/imports/00000000-0000-4000-8000-000000000020 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [ImportResponse](data-contracts.md#importresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000020",
  "status": "READY",
  "fileId": "00000000-0000-4000-8000-000000000021",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceTitle": "Contoh PDF PastiSiap",
  "sourceYear": 2026,
  "pageCount": 1,
  "candidateCount": 2,
  "validCount": 2,
  "invalidCount": 0,
  "errorCode": null,
  "jobStatus": "SUCCEEDED",
  "jobErrorCode": null,
  "createdAt": "2026-11-19T07:00:00Z"
}
```

- READY berarti semua kandidat dinilai lengkap oleh validator; belum otomatis terbit.
- Batch status dan jobStatus berbeda. Batch FAILED dapat bersamaan dengan job RETRY_WAIT.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e31"></a>
### 31. Daftar kandidat ekstraksi

**GET `/api/v1/staff/imports/{id}/candidates`**

Membaca kandidat, koreksi, validationErrors dan questionId jika sudah disimpan.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| offset | query | Tidak | integer (int32); default=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100; default=20 |

#### Contoh request

```http
GET /api/v1/staff/imports/00000000-0000-4000-8000-000000000020/candidates?offset=0&limit=20 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [CandidateResponse](data-contracts.md#candidateresponse).

```json
[
  {
    "id": "00000000-0000-4000-8000-000000000022",
    "sourcePage": 1,
    "sourceItemNo": 1,
    "extractedContent": {
      "stemText": "Berapa hasil 2 + 3?",
      "options": [
        {
          "code": "A",
          "text": "4",
          "correct": false,
          "weight": 0
        },
        {
          "code": "B",
          "text": "5",
          "correct": true,
          "weight": 1
        }
      ],
      "explanation": "Dua ditambah tiga sama dengan lima.",
      "shuffleAllowed": false
    },
    "draftContent": null,
    "validationErrors": [],
    "questionId": null
  }
]
```

- extractedContent bisa belum lengkap dan tidak memenuhi batas request QuestionContent; tampilkan error untuk perbaikan.
- sourceItemNo adalah nomor asal yang dinormalisasi per halaman untuk dedup, tidak selalu nomor cetak asli PDF.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e32"></a>
### 32. Koreksi satu kandidat

**PATCH `/api/v1/staff/imports/{id}/candidates/{candidateId}`**

Menulis draft_content dan menghitung ulang validationErrors/batch counts.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| candidateId | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PATCH /api/v1/staff/imports/00000000-0000-4000-8000-000000000020/candidates/00000000-0000-4000-8000-000000000022 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [QuestionContent](data-contracts.md#questioncontent). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "stemText": "Berapa hasil 2 + 3?",
  "options": [
    {
      "code": "A",
      "text": "4",
      "correct": false,
      "weight": 0
    },
    {
      "code": "B",
      "text": "5",
      "correct": true,
      "weight": 1
    }
  ],
  "explanation": "Dua ditambah tiga sama dengan lima.",
  "shuffleAllowed": false
}
```

#### Response

HTTP 200, **body kosong**.

- 200 body kosong. Kirim seluruh konten, bukan JSON Patch.
- Hasil ekstraksi asli tetap tersimpan. Kandidat yang sudah menjadi master tidak dapat dikoreksi lewat endpoint ini.
- Bean validation body berlaku; kunci/pembahasan yang belum valid tetap ditandai oleh validator publikasi.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 409 CANDIDATE_ALREADY_SAVED.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e33"></a>
### 33. Kandidat menjadi draft master

**POST `/api/v1/staff/imports/{id}/candidates/{candidateId}/save`**

Membuat keluarga question dan versi DRAFT dari koreksi terakhir atau ekstraksi asli.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| candidateId | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/staff/imports/00000000-0000-4000-8000-000000000020/candidates/00000000-0000-4000-8000-000000000022/save HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
```

Body request: **tidak ada**.

#### Response

Schema: [StaffQuestionResponse](data-contracts.md#staffquestionresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000007",
  "versionId": "00000000-0000-4000-8000-000000000008",
  "code": "Q-00000000-0000-4000-8000-000000000007",
  "version": 1,
  "status": "DRAFT",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceYear": 2026,
  "sourceTitle": "Contoh PDF PastiSiap",
  "sourcePage": 1,
  "content": {
    "stemText": "Berapa hasil 2 + 3?",
    "options": [
      {
        "code": "A",
        "text": "4",
        "correct": false,
        "weight": 0
      },
      {
        "code": "B",
        "text": "5",
        "correct": true,
        "weight": 1
      }
    ],
    "explanation": "Dua ditambah tiga sama dengan lima.",
    "shuffleAllowed": false
  }
}
```

- Request berulang pada kandidat yang sudah disimpan mengembalikan master yang sama.
- Save bukan publish; draft belum lengkap masih dapat dibuat, kemudian publish menolak konten tidak lengkap.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e34"></a>
### 34. Download PDF sumber

**GET `/api/v1/staff/imports/{id}/source`**

Membaca file asli yang dapat berisi kunci untuk review petugas.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/staff/imports/00000000-0000-4000-8000-000000000020/source HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

```http
HTTP/1.1 200 OK
Content-Type: application/pdf
Cache-Control: no-store

[binary file]
```

- 200 binary; Content-Disposition attachment; filename=source.pdf; Cache-Control no-store.
- Semua QUESTION_STAFF dapat membaca batch yang valid, bukan hanya uploader. File tidak tersedia dapat menghasilkan error internal yang harus dilacak log.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e35"></a>
### 35. Buat soal manual

**POST `/api/v1/staff/questions`**

Membuat master DRAFT tanpa perlu upload PDF.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/staff/questions HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [CreateQuestionRequest](data-contracts.md#createquestionrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceTitle": "Soal manual contoh",
  "sourceYear": 2026,
  "content": {
    "stemText": "Berapa hasil 2 + 3?",
    "options": [
      {
        "code": "A",
        "text": "4",
        "correct": false,
        "weight": 0
      },
      {
        "code": "B",
        "text": "5",
        "correct": true,
        "weight": 1
      }
    ],
    "explanation": "Dua ditambah tiga sama dengan lima.",
    "shuffleAllowed": false
  }
}
```

#### Response

Schema: [StaffQuestionResponse](data-contracts.md#staffquestionresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000007",
  "versionId": "00000000-0000-4000-8000-000000000008",
  "code": "Q-00000000-0000-4000-8000-000000000007",
  "version": 1,
  "status": "DRAFT",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceYear": 2026,
  "sourceTitle": "Soal manual contoh",
  "sourcePage": null,
  "content": {
    "stemText": "Berapa hasil 2 + 3?",
    "options": [
      {
        "code": "A",
        "text": "4",
        "correct": false,
        "weight": 0
      },
      {
        "code": "B",
        "text": "5",
        "correct": true,
        "weight": 1
      }
    ],
    "explanation": "Dua ditambah tiga sama dengan lima.",
    "shuffleAllowed": false
  }
}
```

- subtestId/topicId harus satu lingkup aktif. Konten body harus memenuhi validasi request; aturan lengkap publikasi tetap diperiksa saat publish.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 INVALID_CONTENT_SCOPE.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e36"></a>
### 36. Cari master soal

**GET `/api/v1/staff/questions`**

Daftar question yang belum diarsipkan beserta versi paling baru.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| subtestId | query | Tidak | string (uuid); — |
| search | query | Tidak | string; — |
| offset | query | Tidak | integer (int32); default=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100; default=20 |

#### Contoh request

```http
GET /api/v1/staff/questions?offset=0&limit=20 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [StaffQuestionResponse](data-contracts.md#staffquestionresponse).

```json
[
  {
    "id": "00000000-0000-4000-8000-000000000007",
    "versionId": "00000000-0000-4000-8000-000000000008",
    "code": "Q-00000000-0000-4000-8000-000000000007",
    "version": 1,
    "status": "DRAFT",
    "subtestId": "00000000-0000-4000-8000-000000000004",
    "topicId": "00000000-0000-4000-8000-000000000005",
    "sourceYear": 2026,
    "sourceTitle": "Contoh PDF PastiSiap",
    "sourcePage": 1,
    "content": {
      "stemText": "Berapa hasil 2 + 3?",
      "options": [
        {
          "code": "A",
          "text": "4",
          "correct": false,
          "weight": 0
        },
        {
          "code": "B",
          "text": "5",
          "correct": true,
          "weight": 1
        }
      ],
      "explanation": "Dua ditambah tiga sama dengan lima.",
      "shuffleAllowed": false
    }
  }
]
```

- Filter opsional subtestId dan search. Search mencocokkan kode atau stem pada versi yang tersimpan.
- Versi terbaru yang dikembalikan bisa DRAFT walaupun versi PUBLISHED lama masih dipakai sesi baru.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e37"></a>
### 37. Detail master terbaru

**GET `/api/v1/staff/questions/{id}`**

Detail versi paling baru dengan konten/kunci/bobot dan provenance sumber.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/staff/questions/00000000-0000-4000-8000-000000000007 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [StaffQuestionResponse](data-contracts.md#staffquestionresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000007",
  "versionId": "00000000-0000-4000-8000-000000000008",
  "code": "Q-00000000-0000-4000-8000-000000000007",
  "version": 1,
  "status": "DRAFT",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceYear": 2026,
  "sourceTitle": "Contoh PDF PastiSiap",
  "sourcePage": 1,
  "content": {
    "stemText": "Berapa hasil 2 + 3?",
    "options": [
      {
        "code": "A",
        "text": "4",
        "correct": false,
        "weight": 0
      },
      {
        "code": "B",
        "text": "5",
        "correct": true,
        "weight": 1
      }
    ],
    "explanation": "Dua ditambah tiga sama dengan lima.",
    "shuffleAllowed": false
  }
}
```

- Bukan endpoint untuk memilih nomor versi historis. Detail by ID masih dapat dibaca setelah archive.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e38"></a>
### 38. Buat versi draft baru

**POST `/api/v1/staff/questions/{id}/versions`**

Menambahkan versi berikutnya tanpa menimpa versi PUBLISHED sebelumnya.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/staff/questions/00000000-0000-4000-8000-000000000007/versions HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [QuestionContent](data-contracts.md#questioncontent). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "stemText": "Berapa hasil 2 + 3?",
  "options": [
    {
      "code": "A",
      "text": "4",
      "correct": false,
      "weight": 0
    },
    {
      "code": "B",
      "text": "5",
      "correct": true,
      "weight": 1
    }
  ],
  "explanation": "Dua ditambah tiga sama dengan lima.",
  "shuffleAllowed": false
}
```

#### Response

Schema: [StaffQuestionResponse](data-contracts.md#staffquestionresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000007",
  "versionId": "00000000-0000-4000-8000-000000000080",
  "code": "Q-00000000-0000-4000-8000-000000000007",
  "version": 2,
  "status": "DRAFT",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceYear": 2026,
  "sourceTitle": "Contoh PDF PastiSiap",
  "sourcePage": 1,
  "content": {
    "stemText": "Berapa hasil 2 + 3?",
    "options": [
      {
        "code": "A",
        "text": "4",
        "correct": false,
        "weight": 0
      },
      {
        "code": "B",
        "text": "5",
        "correct": true,
        "weight": 1
      }
    ],
    "explanation": "Dua ditambah tiga sama dengan lima.",
    "shuffleAllowed": false
  }
}
```

- Endpoint ini tidak idempoten: pengiriman ulang membuat versi berikutnya lagi.
- Edit draft memakai PUT /staff/questions/{id}/draft dengan version ID dan revision; publish menargetkan versi terbaru.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e39"></a>
### 39. Publish versi terbaru

**POST `/api/v1/staff/questions/{id}/publish`**

Memvalidasi sumber/stem/opsi/kunci/bobot/pembahasan lalu menerbitkan DRAFT terbaru.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/staff/questions/00000000-0000-4000-8000-000000000007/publish HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
```

Body request: **tidak ada**.

#### Response

Schema: [StaffQuestionResponse](data-contracts.md#staffquestionresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000007",
  "versionId": "00000000-0000-4000-8000-000000000008",
  "code": "Q-00000000-0000-4000-8000-000000000007",
  "version": 1,
  "status": "PUBLISHED",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceYear": 2026,
  "sourceTitle": "Contoh PDF PastiSiap",
  "sourcePage": 1,
  "content": {
    "stemText": "Berapa hasil 2 + 3?",
    "options": [
      {
        "code": "A",
        "text": "4",
        "correct": false,
        "weight": 0
      },
      {
        "code": "B",
        "text": "5",
        "correct": true,
        "weight": 1
      }
    ],
    "explanation": "Dua ditambah tiga sama dengan lima.",
    "shuffleAllowed": false
  }
}
```

- Wajib tepat satu opsi correct, kode opsi unik A–E, 2–5 opsi, pembahasan dan source title.
- Jika versi terbaru sudah PUBLISHED, respons versi yang sama. Draft baru tidak otomatis diterbitkan worker.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 409 QUESTION_INCOMPLETE.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e40"></a>
### 40. Arsip keluarga soal

**POST `/api/v1/staff/questions/{id}/archive`**

Menetapkan archived_at agar soal tidak masuk pemilihan sesi baru.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/staff/questions/00000000-0000-4000-8000-000000000007/archive HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
```

Body request: **tidak ada**.

#### Response

HTTP 200, **body kosong**.

- 200 body kosong. Pengulangan tidak mengubah timestamp arsip yang sudah ada.
- Snapshot sesi lama tetap dapat dibaca; tidak menghapus versi/hasil. Belum ada endpoint unarchive.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## OAuth dan infrastruktur

<a id="e41"></a>
### 41. Mulai login Google

**GET `/oauth2/authorization/google`**

Mengarahkan browser ke Google; hanya aktif bila profil oauth dikonfigurasi.

Akses: **Publik**. Sukses utama: **HTTP 302**.

#### Contoh request

```http
GET /oauth2/authorization/google HTTP/1.1
Host: localhost:8080
```

Body request: **tidak ada**.

#### Response

```http
HTTP/1.1 302 Found
Location: https://accounts.google.com/o/oauth2/v2/auth?...
```

- Tidak ada body JSON request. Selesaikan login dan consent dalam browser; bukan request credential password ke backend.
- Cookie/state OAuth harus tetap pada browser/session yang sama.

#### Error/kondisi yang perlu ditangani

- Handler/format non-API dapat berbeda dari envelope JSON API; lihat catatan respons dan konfigurasi Security.

<a id="e42"></a>
### 42. Callback Google

**GET `/login/oauth2/code/google`**

Endpoint filter OIDC menerima authorization code dan state dari Google.

Akses: **Publik**. Sukses utama: **HTTP 302**.

#### Contoh request

```http
GET /login/oauth2/code/google HTTP/1.1
Host: localhost:8080
```

Body request: **tidak ada**.

#### Response

```http
HTTP/1.1 302 Found
Location: nilai OAUTH_SUCCESS_REDIRECT
```

- Parameter code/state dibuat provider/Spring, bukan nilai yang dibuat frontend secara manual.
- Sukses membuat session lalu redirect. Gagal OAuth mengikuti handler/redirect Security, tidak selalu ApiError JSON.

#### Error/kondisi yang perlu ditangani

- Handler/format non-API dapat berbeda dari envelope JSON API; lihat catatan respons dan konfigurasi Security.

<a id="e43"></a>
### 43. Health komponen

**GET `/actuator/health`**

Health deployment dengan detail komponen disembunyikan.

Akses: **Publik**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /actuator/health HTTP/1.1
Host: localhost:8080
```

Body request: **tidak ada**.

#### Response

```json
{
  "status": "UP"
}
```

- Status tidak sehat dapat memberi HTTP 503. Endpoint /api/v1/health hanya status aplikasi sederhana.

#### Error/kondisi yang perlu ditangani

- Handler/format non-API dapat berbeda dari envelope JSON API; lihat catatan respons dan konfigurasi Security.

<a id="e44"></a>
### 44. Info Actuator

**GET `/actuator/info`**

Endpoint info yang diekspos konfigurasi Actuator.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /actuator/info HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

```json
{}
```

- Dapat kosong karena contributor info tidak seluruhnya aktif. Tidak dijamin mengembalikan info.app sebagai JSON.
- Di luar prefix /api/, request anonim dapat mengikuti redirect OAuth atau respons Security berbeda, tergantung profil.

#### Error/kondisi yang perlu ditangani

- Handler/format non-API dapat berbeda dari envelope JSON API; lihat catatan respons dan konfigurasi Security.

<a id="e45"></a>
### 45. Discovery Actuator

**GET `/actuator`**

Discovery links Actuator untuk endpoint yang diekspos.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /actuator HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

```json
{
  "_links": {
    "self": {
      "href": "http://localhost:8080/actuator",
      "templated": false
    },
    "health": {
      "href": "http://localhost:8080/actuator/health",
      "templated": false
    },
    "health-path": {
      "href": "http://localhost:8080/actuator/health/{*path}",
      "templated": true
    },
    "info": {
      "href": "http://localhost:8080/actuator/info",
      "templated": false
    }
  }
}
```

- Bukan API data siswa. Host/links mengikuti deployment. Penjagaan anonim sama seperti /actuator/info.

#### Error/kondisi yang perlu ditangani

- Handler/format non-API dapat berbeda dari envelope JSON API; lihat catatan respons dan konfigurasi Security.

<a id="e46"></a>
### 46. Buka Swagger UI

**GET `/swagger-ui.html`**

Redirect ke halaman Swagger UI untuk mencoba endpoint.

Akses: **Publik**. Sukses utama: **HTTP 302**.

#### Contoh request

```http
GET /swagger-ui.html HTTP/1.1
Host: localhost:8080
```

Body request: **tidak ada**.

#### Response

```http
HTTP/1.1 302 Found
Location: /swagger-ui/index.html
```

- Resource HTML/JS/CSS internal Swagger tidak diinventarisasi satu per satu. UI memakai cookie/CSRF yang sama; bukan bypass security.

#### Error/kondisi yang perlu ditangani

- Handler/format non-API dapat berbeda dari envelope JSON API; lihat catatan respons dan konfigurasi Security.

<a id="e47"></a>
### 47. OpenAPI runtime

**GET `/api-docs`**

Membaca spesifikasi controller yang dibangkitkan Springdoc.

Akses: **Publik**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api-docs HTTP/1.1
Host: localhost:8080
```

Body request: **tidak ada**.

#### Response

```json
{
  "openapi": "3.1.0",
  "info": {
    "title": "OpenAPI definition",
    "version": "v0"
  },
  "paths": {},
  "components": {}
}
```

- Contoh struktur dipersingkat; file runtime lengkap ada pada openapi-runtime.json.
- Endpoint filter logout/OAuth dan Actuator dilengkapi pada openapi.json dokumentasi ini.

#### Error/kondisi yang perlu ditangani



## Petugas soal lanjutan

<a id="e48"></a>
### 48. Baca atau simpan draft

**GET `/api/v1/staff/questions/{id}/draft`**

GET latest question + editor revision. PUT hanya DRAFT nonarsip; expectedVersionId dan expectedRevision wajib cocok. Opsi diganti atomik, revision naik sekali; konflik 409 REVISION_CONFLICT.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/staff/questions/00000000-0000-4000-8000-000000000007/draft HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [DraftView](data-contracts.md#draftview).

```json
{
  "question": {
    "id": "00000000-0000-4000-8000-000000000007",
    "versionId": "00000000-0000-4000-8000-000000000008",
    "code": "Q-00000000-0000-4000-8000-000000000007",
    "version": 1,
    "status": "DRAFT",
    "subtestId": "00000000-0000-4000-8000-000000000004",
    "topicId": "00000000-0000-4000-8000-000000000005",
    "sourceYear": 2026,
    "sourceTitle": "Contoh PDF PastiSiap",
    "sourcePage": 1,
    "content": {
      "stemText": "Berapa hasil 2 + 3?",
      "options": [
        {
          "code": "A",
          "text": "4",
          "correct": false,
          "weight": 0
        },
        {
          "code": "B",
          "text": "5",
          "correct": true,
          "weight": 1
        }
      ],
      "explanation": "Dua ditambah tiga sama dengan lima.",
      "shuffleAllowed": false
    }
  },
  "revision": 0
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e49"></a>
### 49. Baca atau simpan draft

**PUT `/api/v1/staff/questions/{id}/draft`**

GET latest question + editor revision. PUT hanya DRAFT nonarsip; expectedVersionId dan expectedRevision wajib cocok. Opsi diganti atomik, revision naik sekali; konflik 409 REVISION_CONFLICT.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PUT /api/v1/staff/questions/00000000-0000-4000-8000-000000000007/draft HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [DraftRequest](data-contracts.md#draftrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "expectedVersionId": "00000000-0000-4000-8000-000000000008",
  "expectedRevision": 0,
  "content": {
    "stemText": "Berapa hasil 2 + 3?",
    "options": [
      {
        "code": "A",
        "text": "4",
        "correct": false,
        "weight": 0
      },
      {
        "code": "B",
        "text": "5",
        "correct": true,
        "weight": 1
      }
    ],
    "explanation": "Dua ditambah tiga sama dengan lima.",
    "shuffleAllowed": false
  },
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [DraftView](data-contracts.md#draftview).

```json
{
  "question": {
    "id": "00000000-0000-4000-8000-000000000007",
    "versionId": "00000000-0000-4000-8000-000000000008",
    "code": "Q-00000000-0000-4000-8000-000000000007",
    "version": 1,
    "status": "DRAFT",
    "subtestId": "00000000-0000-4000-8000-000000000004",
    "topicId": "00000000-0000-4000-8000-000000000005",
    "sourceYear": 2026,
    "sourceTitle": "Contoh PDF PastiSiap",
    "sourcePage": 1,
    "content": {
      "stemText": "Berapa hasil 2 + 3?",
      "options": [
        {
          "code": "A",
          "text": "4",
          "correct": false,
          "weight": 0
        },
        {
          "code": "B",
          "text": "5",
          "correct": true,
          "weight": 1
        }
      ],
      "explanation": "Dua ditambah tiga sama dengan lima.",
      "shuffleAllowed": false
    }
  },
  "revision": 0
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## Admin platform

<a id="e50"></a>
### 50. Ganti role akun

**PUT `/api/v1/admin/users/{id}/roles`**

Mengganti seluruh himpunan role. Registrasi publik tidak bisa memakai endpoint ini. Perubahan menaikkan auth_revision dan mencabut sesi lama; satu admin aktif wajib dipertahankan, termasuk permintaan bersamaan.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PUT /api/v1/admin/users/00000000-0000-4000-8000-000000000006/roles HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [RolesRequest](data-contracts.md#rolesrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "roles": [
    "QUESTION_STAFF"
  ],
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [AdminUserResponse](data-contracts.md#adminuserresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000001",
  "email": "petugas@example.com",
  "displayName": "Petugas Contoh",
  "userType": "INTERNAL",
  "status": "ACTIVE",
  "roles": [
    "QUESTION_STAFF"
  ],
  "createdAt": "2026-11-20T07:00:00Z",
  "updatedAt": "2026-11-20T07:00:00Z"
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## Petugas soal lanjutan

<a id="e51"></a>
### 51. Riwayat versi master

**GET `/api/v1/staff/questions/{id}/versions`**

Metadata versi/revisi/editor/timestamp, terbaru dahulu.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/staff/questions/00000000-0000-4000-8000-000000000007/versions HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [VersionSummary](data-contracts.md#versionsummary).

```json
[
  {
    "id": "00000000-0000-4000-8000-000000000008",
    "version": 1,
    "status": "DRAFT",
    "revision": 0,
    "createdBy": "00000000-0000-4000-8000-000000000001",
    "createdAt": "2026-11-20T07:00:00Z"
  }
]
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e52"></a>
### 52. Simpan kandidat terpilih sebagai draft

**POST `/api/v1/staff/imports/{id}/save`**

candidateIds maksimal 100, satu batch, transaksi semua atau rollback. Replay kandidat tersimpan mengembalikan master sama. Tidak otomatis publish.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/staff/imports/00000000-0000-4000-8000-000000000020/save HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [SaveCandidatesRequest](data-contracts.md#savecandidatesrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "candidateIds": [
    "00000000-0000-4000-8000-000000000022"
  ],
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: array dari [StaffQuestionResponse](data-contracts.md#staffquestionresponse).

```json
[
  {
    "id": "00000000-0000-4000-8000-000000000007",
    "versionId": "00000000-0000-4000-8000-000000000008",
    "code": "Q-00000000-0000-4000-8000-000000000007",
    "version": 1,
    "status": "DRAFT",
    "subtestId": "00000000-0000-4000-8000-000000000004",
    "topicId": "00000000-0000-4000-8000-000000000005",
    "sourceYear": 2026,
    "sourceTitle": "Contoh PDF PastiSiap",
    "sourcePage": 1,
    "content": {
      "stemText": "Berapa hasil 2 + 3?",
      "options": [
        {
          "code": "A",
          "text": "4",
          "correct": false,
          "weight": 0
        },
        {
          "code": "B",
          "text": "5",
          "correct": true,
          "weight": 1
        }
      ],
      "explanation": "Dua ditambah tiga sama dengan lima.",
      "shuffleAllowed": false
    }
  }
]
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e53"></a>
### 53. Retry impor gagal

**POST `/api/v1/staff/imports/{id}/retry`**

Hanya job FAILED setelah retry otomatis habis. Memakai job/batch/file yang sama tanpa duplikasi. Belum mendukung retry per halaman/lampiran kunci terpisah.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/staff/imports/00000000-0000-4000-8000-000000000020/retry HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [MutationReason](data-contracts.md#mutationreason). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [ImportResponse](data-contracts.md#importresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000020",
  "status": "READY",
  "fileId": "00000000-0000-4000-8000-000000000021",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceTitle": "Contoh PDF PastiSiap",
  "sourceYear": 2026,
  "pageCount": 1,
  "candidateCount": 2,
  "validCount": 2,
  "invalidCount": 0,
  "errorCode": null,
  "jobStatus": "SUCCEEDED",
  "jobErrorCode": null,
  "createdAt": "2026-11-19T07:00:00Z"
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## Admin platform

<a id="e54"></a>
### 54. Daftar atau buat akun internal

**GET `/api/v1/admin/users`**

GET mencari nama/email/UUID, status dan periode dengan pagination. POST membuat akun INTERNAL tanpa password, dengan email yang akan dipakai Google SSO; tidak mengirim undangan email. Email duplikat ditolak.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| search | query | Tidak | string; minLength=0; maxLength=180 |
| status | query | Tidak | string; minLength=0; maxLength=32 |
| userId | query | Tidak | string (uuid); — |
| from | query | Tidak | string (date-time); — |
| to | query | Tidak | string (date-time); — |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |

#### Contoh request

```http
GET /api/v1/admin/users HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponseAdminUserResponse](data-contracts.md#pageresponseadminuserresponse).

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000001",
      "email": "petugas@example.com",
      "displayName": "Petugas Contoh",
      "userType": "INTERNAL",
      "status": "ACTIVE",
      "roles": [
        "QUESTION_STAFF"
      ],
      "createdAt": "2026-11-20T07:00:00Z",
      "updatedAt": "2026-11-20T07:00:00Z"
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e55"></a>
### 55. Daftar atau buat akun internal

**POST `/api/v1/admin/users`**

GET mencari nama/email/UUID, status dan periode dengan pagination. POST membuat akun INTERNAL tanpa password, dengan email yang akan dipakai Google SSO; tidak mengirim undangan email. Email duplikat ditolak.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/users HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [InternalUserRequest](data-contracts.md#internaluserrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "email": "petugas-baru@example.com",
  "displayName": "Petugas Contoh",
  "roles": [
    "QUESTION_STAFF"
  ],
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [AdminUserResponse](data-contracts.md#adminuserresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000001",
  "email": "petugas@example.com",
  "displayName": "Petugas Contoh",
  "userType": "INTERNAL",
  "status": "ACTIVE",
  "roles": [
    "QUESTION_STAFF"
  ],
  "createdAt": "2026-11-20T07:00:00Z",
  "updatedAt": "2026-11-20T07:00:00Z"
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e56"></a>
### 56. Buat topik

**POST `/api/v1/admin/topics`**

Parent subtes harus ada.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/topics HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [TopicRequest](data-contracts.md#topicrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "name": "Aritmetika",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [TaxonomyItem](data-contracts.md#taxonomyitem).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "parentId": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "name": "Nama contoh",
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e57"></a>
### 57. Buat subtes

**POST `/api/v1/admin/subtests`**

Parent jenis ujian harus ada.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/subtests HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [SubtestRequest](data-contracts.md#subtestrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "code": "NUMERIK",
  "name": "Penalaran Numerik",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [TaxonomyItem](data-contracts.md#taxonomyitem).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "parentId": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "name": "Nama contoh",
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e58"></a>
### 58. Cabut hak langganan

**POST `/api/v1/admin/subscriptions/{id}/revoke`**

Mencabut seluruh grant dari subscription dan memberi status REVOKED, dengan alasan/audit. Ini tindakan akses, bukan refund keuangan.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/subscriptions/00000000-0000-4000-8000-000000000006/revoke HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [MutationReason](data-contracts.md#mutationreason). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [AdminSubscriptionResponse](data-contracts.md#adminsubscriptionresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "userId": "00000000-0000-4000-8000-000000000090",
  "startsAt": "2026-11-20T07:00:00Z",
  "endsAt": "2026-11-20T07:00:00Z",
  "status": "ACTIVE",
  "grants": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "orderId": "00000000-0000-4000-8000-000000000090",
      "startsAt": "2026-11-20T07:00:00Z",
      "endsAt": "2026-11-20T07:00:00Z",
      "revokedAt": "2026-11-20T07:00:00Z"
    }
  ]
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e59"></a>
### 59. Daftar atau buat policy skor

**GET `/api/v1/admin/scoring-policies`**

POST code yang sama membentuk version berikutnya. Hanya BINARY/OPTION_WEIGHT, SUM/HALF_UP dan aturan yang didukung kalkulator; policy yang dirujuk tidak ditimpa.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| search | query | Tidak | string; minLength=0; maxLength=180 |
| status | query | Tidak | string; minLength=0; maxLength=32 |
| userId | query | Tidak | string (uuid); — |
| from | query | Tidak | string (date-time); — |
| to | query | Tidak | string (date-time); — |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |

#### Contoh request

```http
GET /api/v1/admin/scoring-policies HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponsePolicyResponse](data-contracts.md#pageresponsepolicyresponse).

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000070",
      "code": "BINARY_100",
      "version": 1,
      "method": "BINARY",
      "scale": 100,
      "rules": {
        "correctPoints": 1,
        "wrongPoints": 0,
        "emptyPoints": 0
      },
      "status": "ACTIVE"
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e60"></a>
### 60. Daftar atau buat policy skor

**POST `/api/v1/admin/scoring-policies`**

POST code yang sama membentuk version berikutnya. Hanya BINARY/OPTION_WEIGHT, SUM/HALF_UP dan aturan yang didukung kalkulator; policy yang dirujuk tidak ditimpa.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/scoring-policies HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [PolicyRequest](data-contracts.md#policyrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "code": "BINARY_100",
  "method": "BINARY",
  "scale": 100,
  "rules": {
    "correctPoints": 1,
    "wrongPoints": 0,
    "emptyPoints": 0
  },
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [PolicyResponse](data-contracts.md#policyresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000070",
  "code": "BINARY_100",
  "version": 1,
  "method": "BINARY",
  "scale": 100,
  "rules": {
    "correctPoints": 1,
    "wrongPoints": 0,
    "emptyPoints": 0
  },
  "status": "ACTIVE"
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e61"></a>
### 61. Daftar atau buat keluarga paket

**GET `/api/v1/admin/plans`**

GET pagination/pencarian nama/family code. POST membuat revisi pertama; harga minor unit integer dan manfaat eksplisit.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| search | query | Tidak | string; minLength=0; maxLength=180 |
| status | query | Tidak | string; minLength=0; maxLength=32 |
| userId | query | Tidak | string (uuid); — |
| from | query | Tidak | string (date-time); — |
| to | query | Tidak | string (date-time); — |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |

#### Contoh request

```http
GET /api/v1/admin/plans HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponsePlanResponse](data-contracts.md#pageresponseplanresponse).

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "code": "EXAMPLE",
      "familyCode": "PREMIUM_1_MONTH",
      "revision": 1,
      "name": "Nama contoh",
      "months": 1,
      "amountMinor": 1,
      "currency": "IDR",
      "benefits": {
        "fullLeaderboard": true
      },
      "active": true
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e62"></a>
### 62. Daftar atau buat keluarga paket

**POST `/api/v1/admin/plans`**

GET pagination/pencarian nama/family code. POST membuat revisi pertama; harga minor unit integer dan manfaat eksplisit.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/plans HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [PlanRequest](data-contracts.md#planrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "code": "PREMIUM_1_MONTH",
  "name": "Premium 1 bulan",
  "months": 1,
  "amountMinor": 39000,
  "currency": "IDR",
  "benefits": {
    "fullLeaderboard": true
  },
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [PlanResponse](data-contracts.md#planresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "familyCode": "PREMIUM_1_MONTH",
  "revision": 1,
  "name": "Nama contoh",
  "months": 1,
  "amountMinor": 1,
  "currency": "IDR",
  "benefits": {
    "fullLeaderboard": true
  },
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e63"></a>
### 63. Buat revisi harga baru

**POST `/api/v1/admin/plans/{id}/versions`**

Harus berasal dari revisi terakhir dan family code sama. Versi lama nonaktif untuk penjualan; field harga/manfaat lama tidak ditimpa.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/plans/00000000-0000-4000-8000-000000000006/versions HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [PlanRequest](data-contracts.md#planrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "code": "PREMIUM_1_MONTH",
  "name": "Premium 1 bulan",
  "months": 1,
  "amountMinor": 39000,
  "currency": "IDR",
  "benefits": {
    "fullLeaderboard": true
  },
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [PlanResponse](data-contracts.md#planresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "familyCode": "PREMIUM_1_MONTH",
  "revision": 1,
  "name": "Nama contoh",
  "months": 1,
  "amountMinor": 1,
  "currency": "IDR",
  "benefits": {
    "fullLeaderboard": true
  },
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e64"></a>
### 64. Catat permintaan refund

**POST `/api/v1/admin/orders/{id}/refunds`**

202 PENDING_PROVIDER hanya mencatat kasus; belum memindahkan uang, merevoke grant atau menandai REFUNDED. Hanya order PAID; jumlah kumulatif reserved/succeeded tidak melebihi nilai order. Key scoped ke order, replay nominal sama mengembalikan request lama.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 202**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| Idempotency-Key | header | Tidak | string; minLength=8; maxLength=128; pattern=^[A-Za-z0-9._:-]{8,128}$ |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/orders/00000000-0000-4000-8000-000000000006/refunds HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Idempotency-Key: example-start-001
Content-Type: application/json
```

Body: [RefundRequest](data-contracts.md#refundrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "amountMinor": 10000,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [RefundResponse](data-contracts.md#refundresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000080",
  "orderId": "00000000-0000-4000-8000-000000000081",
  "amountMinor": 10000,
  "currency": "IDR",
  "status": "PENDING_PROVIDER",
  "createdAt": "2026-11-20T07:00:00Z",
  "completedAt": null
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e65"></a>
### 65. Rekonsiliasi belum dikonfigurasi

**POST `/api/v1/admin/orders/{id}/reconcile`**

Resource order diperiksa. Selalu 503 PAYMENT_PROVIDER_NOT_CONFIGURED sampai adapter gateway dipilih/diimplementasikan. Tidak mengubah order menjadi PAID.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 503**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/orders/00000000-0000-4000-8000-000000000006/reconcile HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [MutationReason](data-contracts.md#mutationreason). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [ApiError](data-contracts.md#apierror).

```json
{
  "code": "PAYMENT_PROVIDER_NOT_CONFIGURED",
  "message": "Gateway reconciliation is unavailable until a payment provider is configured; order status was not changed",
  "fieldErrors": {},
  "requestId": "example-request-id"
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e66"></a>
### 66. Retry job gagal

**POST `/api/v1/admin/jobs/{id}/retry`**

Hanya FAILED dan jenis IMPORT_PDF/SCORE_ATTEMPT. Reset attempts/lease dan antrekan ID yang sama; PROCESSING/QUEUED/SUCCEEDED ditolak. Bukan regrade hasil selesai.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/jobs/00000000-0000-4000-8000-000000000006/retry HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [MutationReason](data-contracts.md#mutationreason). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [JobResponse](data-contracts.md#jobresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000082",
  "type": "IMPORT_PDF",
  "status": "FAILED",
  "attempts": 5,
  "maxAttempts": 5,
  "runAfter": "2026-11-20T07:00:00Z",
  "leaseUntil": null,
  "errorCode": "EXTRACTION_FAILED",
  "createdAt": "2026-11-20T07:00:00Z"
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e67"></a>
### 67. Buat jenis ujian

**POST `/api/v1/admin/exam-types`**

Parent kategori harus ada.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/exam-types HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [ExamTypeRequest](data-contracts.md#examtyperequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "categoryId": "00000000-0000-4000-8000-000000000024",
  "code": "TPS",
  "name": "Tes Potensi Skolastik",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [TaxonomyItem](data-contracts.md#taxonomyitem).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "parentId": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "name": "Nama contoh",
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e68"></a>
### 68. Buat kategori

**POST `/api/v1/admin/categories`**

Kode stabil, nama dan status aktif.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/categories HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [CategoryRequest](data-contracts.md#categoryrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "code": "UTBK",
  "name": "Ujian Tulis Berbasis Komputer",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [TaxonomyItem](data-contracts.md#taxonomyitem).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "parentId": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "name": "Nama contoh",
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e69"></a>
### 69. Daftar atau buat paket tryout

**GET `/api/v1/admin/blueprints`**

Hanya mode TRYOUT; blueprint internal latihan tidak dikelola melalui endpoint admin ini.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| search | query | Tidak | string; minLength=0; maxLength=180 |
| status | query | Tidak | string; minLength=0; maxLength=32 |
| userId | query | Tidak | string (uuid); — |
| from | query | Tidak | string (date-time); — |
| to | query | Tidak | string (date-time); — |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |

#### Contoh request

```http
GET /api/v1/admin/blueprints HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponseBlueprintResponse](data-contracts.md#pageresponseblueprintresponse).

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000002",
      "examTypeId": "00000000-0000-4000-8000-000000000025",
      "code": "TRYOUT_UTBK_01",
      "title": "Tryout UTBK 01",
      "status": "ACTIVE",
      "activeVersionId": "00000000-0000-4000-8000-000000000003"
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e70"></a>
### 70. Daftar atau buat paket tryout

**POST `/api/v1/admin/blueprints`**

Hanya mode TRYOUT; blueprint internal latihan tidak dikelola melalui endpoint admin ini.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/blueprints HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [BlueprintRequest](data-contracts.md#blueprintrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "code": "TRYOUT_UTBK_01",
  "title": "Tryout UTBK 01",
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [BlueprintResponse](data-contracts.md#blueprintresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000002",
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "code": "TRYOUT_UTBK_01",
  "title": "Tryout UTBK 01",
  "status": "ACTIVE",
  "activeVersionId": "00000000-0000-4000-8000-000000000003"
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e71"></a>
### 71. Buat komposisi draft baru

**POST `/api/v1/admin/blueprints/{id}/versions`**

Durasi 60–86400 detik, policy aktif dan section dalam jenis ujian yang sama. Satu section per subtes mencegah overlap. Filter topik/tahun eksplisit. Tidak mengubah versi terbit.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 201**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/blueprints/00000000-0000-4000-8000-000000000006/versions HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [BlueprintVersionRequest](data-contracts.md#blueprintversionrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "durationSeconds": 5400,
  "scoringPolicyId": "00000000-0000-4000-8000-000000000070",
  "sections": [
    {
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "questionCount": 3,
      "topicId": "00000000-0000-4000-8000-000000000005",
      "sourceYear": 2026
    }
  ],
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [BlueprintVersion](data-contracts.md#blueprintversion).

```json
{
  "id": "00000000-0000-4000-8000-000000000003",
  "version": 1,
  "status": "ACTIVE",
  "durationSeconds": 5400,
  "scoringPolicyId": "00000000-0000-4000-8000-000000000070",
  "sections": [
    {
      "id": "00000000-0000-4000-8000-000000000023",
      "position": 1,
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "subtestName": "Penalaran Numerik",
      "questionCount": 3,
      "topicId": "00000000-0000-4000-8000-000000000005",
      "sourceYear": 2026,
      "available": 12
    }
  ],
  "ready": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e72"></a>
### 72. Terbitkan versi blueprint

**POST `/api/v1/admin/blueprints/{id}/versions/{versionId}/publish`**

Pool kurang ditolak. expectedActiveVersionId=null untuk publikasi pertama; sesudahnya kirim active version terakhir agar konflik penulisan terdeteksi. Versi/sesi lama tetap immutable.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| versionId | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
POST /api/v1/admin/blueprints/00000000-0000-4000-8000-000000000006/versions/00000000-0000-4000-8000-000000000006/publish HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [PublishRequest](data-contracts.md#publishrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "expectedActiveVersionId": null,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [BlueprintDetail](data-contracts.md#blueprintdetail).

```json
{
  "blueprint": {
    "id": "00000000-0000-4000-8000-000000000002",
    "examTypeId": "00000000-0000-4000-8000-000000000025",
    "code": "TRYOUT_UTBK_01",
    "title": "Tryout UTBK 01",
    "status": "ACTIVE",
    "activeVersionId": "00000000-0000-4000-8000-000000000003"
  },
  "versions": [
    {
      "id": "00000000-0000-4000-8000-000000000003",
      "version": 1,
      "status": "ACTIVE",
      "durationSeconds": 5400,
      "scoringPolicyId": "00000000-0000-4000-8000-000000000070",
      "sections": [
        {
          "id": "00000000-0000-4000-8000-000000000023",
          "position": 1,
          "subtestId": "00000000-0000-4000-8000-000000000004",
          "subtestName": "Penalaran Numerik",
          "questionCount": 3,
          "topicId": "00000000-0000-4000-8000-000000000005",
          "sourceYear": 2026,
          "available": 12
        }
      ],
      "ready": true
    }
  ]
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e73"></a>
### 73. Suspend atau pulihkan akun

**PATCH `/api/v1/admin/users/{id}/status`**

ACTIVE/SUSPENDED dengan alasan. Sesi lama tidak hidup kembali setelah restore; pengguna harus login lagi. Jawaban/snapshot ujian tidak diubah. Admin aktif terakhir dilindungi.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PATCH /api/v1/admin/users/00000000-0000-4000-8000-000000000006/status HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [StatusRequest](data-contracts.md#statusrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "status": "SUSPENDED",
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [AdminUserResponse](data-contracts.md#adminuserresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000001",
  "email": "petugas@example.com",
  "displayName": "Petugas Contoh",
  "userType": "INTERNAL",
  "status": "ACTIVE",
  "roles": [
    "QUESTION_STAFF"
  ],
  "createdAt": "2026-11-20T07:00:00Z",
  "updatedAt": "2026-11-20T07:00:00Z"
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e74"></a>
### 74. Perbarui topik

**PATCH `/api/v1/admin/topics/{id}`**

Parent immutable; nonaktifkan tanpa menghapus soal dan sesi lama.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PATCH /api/v1/admin/topics/00000000-0000-4000-8000-000000000006 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [TopicRequest](data-contracts.md#topicrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "name": "Aritmetika",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [TaxonomyItem](data-contracts.md#taxonomyitem).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "parentId": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "name": "Nama contoh",
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e75"></a>
### 75. Perbarui subtes

**PATCH `/api/v1/admin/subtests/{id}`**

Parent dan kode immutable untuk mencegah pemindahan bank soal lintas ujian.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PATCH /api/v1/admin/subtests/00000000-0000-4000-8000-000000000006 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [SubtestRequest](data-contracts.md#subtestrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "code": "NUMERIK",
  "name": "Penalaran Numerik",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [TaxonomyItem](data-contracts.md#taxonomyitem).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "parentId": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "name": "Nama contoh",
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e76"></a>
### 76. Buka atau hentikan penjualan

**PATCH `/api/v1/admin/plans/{id}/availability`**

Hanya revisi terakhir dapat diaktifkan; tidak mengubah snapshot order/akses yang sudah dibeli.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PATCH /api/v1/admin/plans/00000000-0000-4000-8000-000000000006/availability HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [ActiveRequest](data-contracts.md#activerequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "active": false,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [PlanResponse](data-contracts.md#planresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "familyCode": "PREMIUM_1_MONTH",
  "revision": 1,
  "name": "Nama contoh",
  "months": 1,
  "amountMinor": 1,
  "currency": "IDR",
  "benefits": {
    "fullLeaderboard": true
  },
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e77"></a>
### 77. Perbarui jenis ujian

**PATCH `/api/v1/admin/exam-types/{id}`**

Parent dan kode immutable; buat master baru untuk pindah lingkup.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PATCH /api/v1/admin/exam-types/00000000-0000-4000-8000-000000000006 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [ExamTypeRequest](data-contracts.md#examtyperequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "categoryId": "00000000-0000-4000-8000-000000000024",
  "code": "TPS",
  "name": "Tes Potensi Skolastik",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [TaxonomyItem](data-contracts.md#taxonomyitem).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "parentId": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "name": "Nama contoh",
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e78"></a>
### 78. Perbarui kategori

**PATCH `/api/v1/admin/categories/{id}`**

Nama/status dapat berubah; kode stabil tidak diganti.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PATCH /api/v1/admin/categories/00000000-0000-4000-8000-000000000006 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [CategoryRequest](data-contracts.md#categoryrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "code": "UTBK",
  "name": "Ujian Tulis Berbasis Komputer",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [TaxonomyItem](data-contracts.md#taxonomyitem).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "parentId": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "name": "Nama contoh",
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e79"></a>
### 79. Aktifkan atau arsip katalog

**PATCH `/api/v1/admin/blueprints/{id}/status`**

Reaktivasi memerlukan active version dan stok cukup. Archive menghentikan start baru; sesi/history lama tetap dapat dibaca.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| X-CSRF-TOKEN | header | Ya | string; — |

#### Contoh request

```http
PATCH /api/v1/admin/blueprints/00000000-0000-4000-8000-000000000006/status HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
X-CSRF-TOKEN: example-csrf-token
Content-Type: application/json
```

Body: [BlueprintStatusRequest](data-contracts.md#blueprintstatusrequest). Wajib/validasi setiap key ada pada kamus DTO.

```json
{
  "status": "ARCHIVED",
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

#### Response

Schema: [BlueprintDetail](data-contracts.md#blueprintdetail).

```json
{
  "blueprint": {
    "id": "00000000-0000-4000-8000-000000000002",
    "examTypeId": "00000000-0000-4000-8000-000000000025",
    "code": "TRYOUT_UTBK_01",
    "title": "Tryout UTBK 01",
    "status": "ACTIVE",
    "activeVersionId": "00000000-0000-4000-8000-000000000003"
  },
  "versions": [
    {
      "id": "00000000-0000-4000-8000-000000000003",
      "version": 1,
      "status": "ACTIVE",
      "durationSeconds": 5400,
      "scoringPolicyId": "00000000-0000-4000-8000-000000000070",
      "sections": [
        {
          "id": "00000000-0000-4000-8000-000000000023",
          "position": 1,
          "subtestId": "00000000-0000-4000-8000-000000000004",
          "subtestName": "Penalaran Numerik",
          "questionCount": 3,
          "topicId": "00000000-0000-4000-8000-000000000005",
          "sourceYear": 2026,
          "available": 12
        }
      ],
      "ready": true
    }
  ]
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## Petugas soal lanjutan

<a id="e80"></a>
### 80. Baca versi soal tertentu

**GET `/api/v1/staff/questions/{id}/versions/{versionId}`**

Versi wajib milik question yang dituju; konten/kunci/pembahasan historis untuk petugas saja.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| versionId | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/staff/questions/00000000-0000-4000-8000-000000000007/versions/00000000-0000-4000-8000-000000000007 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [StaffQuestionResponse](data-contracts.md#staffquestionresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000007",
  "versionId": "00000000-0000-4000-8000-000000000008",
  "code": "Q-00000000-0000-4000-8000-000000000007",
  "version": 1,
  "status": "DRAFT",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceYear": 2026,
  "sourceTitle": "Contoh PDF PastiSiap",
  "sourcePage": 1,
  "content": {
    "stemText": "Berapa hasil 2 + 3?",
    "options": [
      {
        "code": "A",
        "text": "4",
        "correct": false,
        "weight": 0
      },
      {
        "code": "B",
        "text": "5",
        "correct": true,
        "weight": 1
      }
    ],
    "explanation": "Dua ditambah tiga sama dengan lima.",
    "shuffleAllowed": false
  }
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e81"></a>
### 81. Cari master dengan pagination

**GET `/api/v1/staff/question-bank`**

Filter search, categoryId, examTypeId, subtestId, topicId, sourceYear, status, batchId. Default 10 per halaman; total dan ringkasan versi terbaru. Versi terbit lama masih bisa dipakai walau versi terbaru draft.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| search | query | Tidak | string; minLength=0; maxLength=180 |
| categoryId | query | Tidak | string (uuid); — |
| examTypeId | query | Tidak | string (uuid); — |
| subtestId | query | Tidak | string (uuid); — |
| topicId | query | Tidak | string (uuid); — |
| sourceYear | query | Tidak | integer (int32); minimum=1900; maximum=2200 |
| status | query | Tidak | string; pattern=DRAFT\|PUBLISHED\|ARCHIVED |
| batchId | query | Tidak | string (uuid); — |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |

#### Contoh request

```http
GET /api/v1/staff/question-bank HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponseQuestionSummary](data-contracts.md#pageresponsequestionsummary).

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000007",
      "code": "Q-EXAMPLE",
      "versionId": "00000000-0000-4000-8000-000000000008",
      "version": 1,
      "status": "DRAFT",
      "revision": 0,
      "category": "UTBK",
      "examType": "TPS",
      "subtest": "Penalaran Numerik",
      "topic": "Aritmetika",
      "sourceYear": 2026,
      "sourceTitle": "Sumber contoh",
      "stemPreview": "Berapa hasil satu tambah satu?",
      "batchId": null
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e82"></a>
### 82. Kebutuhan stok blueprint

**GET `/api/v1/staff/pool-requirements`**

Ringkasan section versi terakhir tiap blueprint nonarsip, filter topik/tahun dan jumlah terbit. Ini indikator pasokan; readiness admin melakukan pemeriksaan opsi/scope lebih lengkap.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| offset | query | Tidak | integer (int32); minimum=0; default=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100; default=10 |

#### Contoh request

```http
GET /api/v1/staff/pool-requirements HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponsePoolRequirement](data-contracts.md#pageresponsepoolrequirement).

```json
{
  "items": [
    {
      "blueprintId": "00000000-0000-4000-8000-000000000090",
      "blueprintTitle": "Tryout contoh",
      "versionId": "00000000-0000-4000-8000-000000000090",
      "version": 1,
      "position": 1,
      "subtestId": "00000000-0000-4000-8000-000000000090",
      "subtestName": "Penalaran Numerik",
      "required": 1,
      "publishedAvailable": 1
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e83"></a>
### 83. Cari riwayat unggahan

**GET `/api/v1/staff/import-history`**

Pagination total dengan search nama berkas/sumber/UUID, subtestId, uploaderId dan status. Seluruh ruang konten sesuai izin petugas.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| search | query | Tidak | string; minLength=0; maxLength=180 |
| subtestId | query | Tidak | string (uuid); — |
| uploaderId | query | Tidak | string (uuid); — |
| status | query | Tidak | string; pattern=QUEUED\|READY\|NEEDS_INPUT\|FAILED |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |

#### Contoh request

```http
GET /api/v1/staff/import-history HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponseImportSummary](data-contracts.md#pageresponseimportsummary).

```json
{
  "items": [
    {
      "batch": {
        "id": "00000000-0000-4000-8000-000000000020",
        "status": "READY",
        "fileId": "00000000-0000-4000-8000-000000000021",
        "subtestId": "00000000-0000-4000-8000-000000000004",
        "topicId": "00000000-0000-4000-8000-000000000005",
        "sourceTitle": "Contoh PDF PastiSiap",
        "sourceYear": 2026,
        "pageCount": 1,
        "candidateCount": 2,
        "validCount": 2,
        "invalidCount": 0,
        "errorCode": null,
        "jobStatus": "SUCCEEDED",
        "jobErrorCode": null,
        "createdAt": "2026-11-19T07:00:00Z"
      },
      "originalName": "soal-contoh.pdf",
      "uploaderId": "00000000-0000-4000-8000-000000000090"
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e84"></a>
### 84. Ringkasan bank soal

**GET `/api/v1/staff/dashboard`**

Jumlah soal published/draft/arsip dan impor. Tidak menyertakan akun customer/transaksi.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/staff/dashboard HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [StaffDashboard](data-contracts.md#staffdashboard).

```json
{
  "publishedQuestions": 1,
  "draftQuestions": 1,
  "archivedQuestions": 1,
  "imports": 1,
  "importsNeedingInput": 1,
  "failedImports": 1
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e85"></a>
### 85. Audit ruang konten

**GET `/api/v1/staff/audit`**

Hanya entity_type CONTENT; tidak mengirim audit pengguna atau keuangan.

Akses: **Session + role QUESTION_STAFF**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| offset | query | Tidak | integer (int32); minimum=0; default=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100; default=10 |

#### Contoh request

```http
GET /api/v1/staff/audit HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponseContentAudit](data-contracts.md#pageresponsecontentaudit).

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "actorId": "00000000-0000-4000-8000-000000000090",
      "action": "USER_STATUS_CHANGED",
      "entityId": "00000000-0000-4000-8000-000000000090",
      "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh",
      "requestId": "example-request-id",
      "occurredAt": "2026-11-20T07:00:00Z"
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## Admin platform

<a id="e86"></a>
### 86. Detail akun untuk dukungan

**GET `/api/v1/admin/users/{id}`**

Wajib query reason; akses detail dicatat. Profil/target dan hitungan sesi/order tidak menyertakan hash password, token atau identitas rahasia provider.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| reason | query | Ya | string; minLength=0; maxLength=500 |

#### Contoh request

```http
GET /api/v1/admin/users/00000000-0000-4000-8000-000000000006 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [UserDetail](data-contracts.md#userdetail).

```json
{
  "account": {
    "id": "00000000-0000-4000-8000-000000000001",
    "email": "petugas@example.com",
    "displayName": "Petugas Contoh",
    "userType": "INTERNAL",
    "status": "ACTIVE",
    "roles": [
      "QUESTION_STAFF"
    ],
    "createdAt": "2026-11-20T07:00:00Z",
    "updatedAt": "2026-11-20T07:00:00Z"
  },
  "profile": {
    "schoolName": "Contoh",
    "schoolGrade": "Contoh",
    "targetKind": "Contoh",
    "institution": "Contoh",
    "program": "Contoh",
    "targetScore": 1
  },
  "attemptCount": 1,
  "orderCount": 1
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e87"></a>
### 87. Riwayat sesi untuk dukungan

**GET `/api/v1/admin/users/{id}/attempts`**

Wajib query reason; daftar ringkasan sesi pengguna, tanpa isi soal/jawaban. Akses dicatat.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| search | query | Tidak | string; minLength=0; maxLength=180 |
| status | query | Tidak | string; minLength=0; maxLength=32 |
| userId | query | Tidak | string (uuid); — |
| from | query | Tidak | string (date-time); — |
| to | query | Tidak | string (date-time); — |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |
| reason | query | Ya | string; minLength=0; maxLength=500 |

#### Contoh request

```http
GET /api/v1/admin/users/00000000-0000-4000-8000-000000000006/attempts HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponseUserAttempt](data-contracts.md#pageresponseuserattempt).

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "title": "Tryout contoh",
      "mode": "TRYOUT",
      "status": "ACTIVE",
      "terminalReason": "SUBMITTED",
      "startedAt": "2026-11-20T07:00:00Z",
      "finalizedAt": "2026-11-20T07:00:00Z",
      "score": 1
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e88"></a>
### 88. Hierarki kategori lengkap

**GET `/api/v1/admin/taxonomy`**

Kategori, jenis ujian, subtes dan topik, termasuk nonaktif. Admin melihat metadata, bukan isi/kunci soal.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/admin/taxonomy HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [TaxonomyTree](data-contracts.md#taxonomytree).

```json
{
  "categories": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "parentId": "00000000-0000-4000-8000-000000000090",
      "code": "EXAMPLE",
      "name": "Nama contoh",
      "active": true
    }
  ],
  "examTypes": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "parentId": "00000000-0000-4000-8000-000000000090",
      "code": "EXAMPLE",
      "name": "Nama contoh",
      "active": true
    }
  ],
  "subtests": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "parentId": "00000000-0000-4000-8000-000000000090",
      "code": "EXAMPLE",
      "name": "Nama contoh",
      "active": true
    }
  ],
  "topics": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "parentId": "00000000-0000-4000-8000-000000000090",
      "code": "EXAMPLE",
      "name": "Nama contoh",
      "active": true
    }
  ]
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e89"></a>
### 89. Daftar langganan dan grant

**GET `/api/v1/admin/subscriptions`**

Filter userId/status/periode; grant menyertakan sumber order dan waktu revoke. Tidak menyediakan pemberian Premium tanpa pembayaran terverifikasi.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| search | query | Tidak | string; minLength=0; maxLength=180 |
| status | query | Tidak | string; minLength=0; maxLength=32 |
| userId | query | Tidak | string (uuid); — |
| from | query | Tidak | string (date-time); — |
| to | query | Tidak | string (date-time); — |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |

#### Contoh request

```http
GET /api/v1/admin/subscriptions HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponseAdminSubscriptionResponse](data-contracts.md#pageresponseadminsubscriptionresponse).

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "userId": "00000000-0000-4000-8000-000000000090",
      "startsAt": "2026-11-20T07:00:00Z",
      "endsAt": "2026-11-20T07:00:00Z",
      "status": "ACTIVE",
      "grants": [
        {
          "id": "00000000-0000-4000-8000-000000000090",
          "orderId": "00000000-0000-4000-8000-000000000090",
          "startsAt": "2026-11-20T07:00:00Z",
          "endsAt": "2026-11-20T07:00:00Z",
          "revokedAt": "2026-11-20T07:00:00Z"
        }
      ]
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e90"></a>
### 90. Matriks peran

**GET `/api/v1/admin/roles`**

CUSTOMER, QUESTION_STAFF dan PLATFORM_ADMIN beserta izin operasional. Admin tidak otomatis memiliki akses kunci/PDF.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/admin/roles HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: array dari [RoleDefinition](data-contracts.md#roledefinition).

```json
[
  {
    "code": "EXAMPLE",
    "name": "Nama contoh",
    "permissions": [
      "OWN_PROFILE"
    ]
  }
]
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e91"></a>
### 91. Detail revisi harga

**GET `/api/v1/admin/plans/{id}`**

Harga/manfaat/periode dari ID revisi tertentu.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/admin/plans/00000000-0000-4000-8000-000000000006 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PlanResponse](data-contracts.md#planresponse).

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "familyCode": "PREMIUM_1_MONTH",
  "revision": 1,
  "name": "Nama contoh",
  "months": 1,
  "amountMinor": 1,
  "currency": "IDR",
  "benefits": {
    "fullLeaderboard": true
  },
  "active": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e92"></a>
### 92. Daftar transaksi

**GET `/api/v1/admin/orders`**

Filter status, userId, pencarian UUID/email/referensi provider dan periode created_at. Menampilkan snapshot order, bukan harga paket terbaru.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| search | query | Tidak | string; minLength=0; maxLength=180 |
| status | query | Tidak | string; minLength=0; maxLength=32 |
| userId | query | Tidak | string (uuid); — |
| from | query | Tidak | string (date-time); — |
| to | query | Tidak | string (date-time); — |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |

#### Contoh request

```http
GET /api/v1/admin/orders HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponseOrderResponse](data-contracts.md#pageresponseorderresponse).

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "userId": "00000000-0000-4000-8000-000000000090",
      "userEmail": "pengguna@example.com",
      "planId": "00000000-0000-4000-8000-000000000090",
      "planCode": "Contoh",
      "months": 1,
      "amountMinor": 1,
      "currency": "IDR",
      "status": "ACTIVE",
      "provider": "EXAMPLE_PROVIDER",
      "providerOrderId": "example-order-001",
      "createdAt": "2026-11-20T07:00:00Z",
      "expiresAt": "2026-11-20T07:00:00Z"
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e93"></a>
### 93. Detail transaksi

**GET `/api/v1/admin/orders/{id}`**

Order, metadata event pembayaran tersensor, dan refund requests. Tidak mengirim payload provider, rekening atau kredensial.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/admin/orders/00000000-0000-4000-8000-000000000006 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [OrderDetail](data-contracts.md#orderdetail).

```json
{
  "order": {
    "id": "00000000-0000-4000-8000-000000000090",
    "userId": "00000000-0000-4000-8000-000000000090",
    "userEmail": "pengguna@example.com",
    "planId": "00000000-0000-4000-8000-000000000090",
    "planCode": "Contoh",
    "months": 1,
    "amountMinor": 1,
    "currency": "IDR",
    "status": "ACTIVE",
    "provider": "EXAMPLE_PROVIDER",
    "providerOrderId": "example-order-001",
    "createdAt": "2026-11-20T07:00:00Z",
    "expiresAt": "2026-11-20T07:00:00Z"
  },
  "events": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "provider": "EXAMPLE_PROVIDER",
      "providerEventId": "example-event-001",
      "amountMinor": 1,
      "verified": true,
      "signatureStatus": "VERIFIED",
      "receivedAt": "2026-11-20T07:00:00Z"
    }
  ],
  "refunds": [
    {
      "id": "00000000-0000-4000-8000-000000000080",
      "orderId": "00000000-0000-4000-8000-000000000081",
      "amountMinor": 10000,
      "currency": "IDR",
      "status": "PENDING_PROVIDER",
      "createdAt": "2026-11-20T07:00:00Z",
      "completedAt": null
    }
  ]
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e94"></a>
### 94. Daftar job tersensor

**GET `/api/v1/admin/jobs`**

Metadata antrean/attempt/error code dan lease; tanpa payload, seed atau path storage. Status/jenis/UUID dan periode bisa difilter.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| search | query | Tidak | string; minLength=0; maxLength=180 |
| status | query | Tidak | string; minLength=0; maxLength=32 |
| userId | query | Tidak | string (uuid); — |
| from | query | Tidak | string (date-time); — |
| to | query | Tidak | string (date-time); — |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |

#### Contoh request

```http
GET /api/v1/admin/jobs HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponseJobResponse](data-contracts.md#pageresponsejobresponse).

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000082",
      "type": "IMPORT_PDF",
      "status": "FAILED",
      "attempts": 5,
      "maxAttempts": 5,
      "runAfter": "2026-11-20T07:00:00Z",
      "leaseUntil": null,
      "errorCode": "EXTRACTION_FAILED",
      "createdAt": "2026-11-20T07:00:00Z"
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e95"></a>
### 95. Ringkasan operasional

**GET `/api/v1/admin/dashboard`**

Jumlah akun ACTIVE saat dibaca, akun baru/sesi selesai dalam [from,to), pool, impor, job, metadata storage dan gross/refund/net per mata uang. Gross memakai created_at order PAID/REFUNDED; refund memakai completed_at SUCCEEDED. Bukan laporan settlement gateway; tidak mencampur mata uang.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| from | query | Tidak | string (date-time); — |
| to | query | Tidak | string (date-time); — |

#### Contoh request

```http
GET /api/v1/admin/dashboard HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [Dashboard](data-contracts.md#dashboard).

```json
{
  "generatedAt": "2026-11-20T07:00:00Z",
  "from": "2026-11-20T07:00:00Z",
  "to": "2026-11-20T07:00:00Z",
  "activeAccounts": 1,
  "newAccounts": 1,
  "completedSessions": 1,
  "publishedQuestions": 1,
  "failedJobs": 1,
  "pendingImports": 1,
  "storedFiles": 1,
  "storedBytes": 1,
  "revenue": [
    {
      "currency": "IDR",
      "paidGrossMinor": 50000,
      "refundedMinor": 0,
      "netMinor": 50000,
      "pendingOrders": 1
    }
  ],
  "paymentProviderConfigured": false
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e96"></a>
### 96. Detail blueprint dan versinya

**GET `/api/v1/admin/blueprints/{id}`**

Semua versi dan section; ready bersifat stok global, bukan jaminan pool personal setelah pengecualian hari yang sama.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/admin/blueprints/00000000-0000-4000-8000-000000000006 HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [BlueprintDetail](data-contracts.md#blueprintdetail).

```json
{
  "blueprint": {
    "id": "00000000-0000-4000-8000-000000000002",
    "examTypeId": "00000000-0000-4000-8000-000000000025",
    "code": "TRYOUT_UTBK_01",
    "title": "Tryout UTBK 01",
    "status": "ACTIVE",
    "activeVersionId": "00000000-0000-4000-8000-000000000003"
  },
  "versions": [
    {
      "id": "00000000-0000-4000-8000-000000000003",
      "version": 1,
      "status": "ACTIVE",
      "durationSeconds": 5400,
      "scoringPolicyId": "00000000-0000-4000-8000-000000000070",
      "sections": [
        {
          "id": "00000000-0000-4000-8000-000000000023",
          "position": 1,
          "subtestId": "00000000-0000-4000-8000-000000000004",
          "subtestName": "Penalaran Numerik",
          "questionCount": 3,
          "topicId": "00000000-0000-4000-8000-000000000005",
          "sourceYear": 2026,
          "available": 12
        }
      ],
      "ready": true
    }
  ]
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e97"></a>
### 97. Cek kebutuhan pool

**GET `/api/v1/admin/blueprints/{id}/versions/{versionId}/readiness`**

Stok soal terbit/nonarsip, scope aktif dan opsi/kunci/bobot diperiksa. Tidak mengirim isi soal atau kunci.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| id | path | Ya | string (uuid); — |
| versionId | path | Ya | string (uuid); — |

#### Contoh request

```http
GET /api/v1/admin/blueprints/00000000-0000-4000-8000-000000000006/versions/00000000-0000-4000-8000-000000000006/readiness HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [BlueprintVersion](data-contracts.md#blueprintversion).

```json
{
  "id": "00000000-0000-4000-8000-000000000003",
  "version": 1,
  "status": "ACTIVE",
  "durationSeconds": 5400,
  "scoringPolicyId": "00000000-0000-4000-8000-000000000070",
  "sections": [
    {
      "id": "00000000-0000-4000-8000-000000000023",
      "position": 1,
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "subtestName": "Penalaran Numerik",
      "questionCount": 3,
      "topicId": "00000000-0000-4000-8000-000000000005",
      "sourceYear": 2026,
      "available": 12
    }
  ],
  "ready": true
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e98"></a>
### 98. Audit operasional

**GET `/api/v1/admin/audit`**

Pagination append-only pada aplikasi; status memfilter entity_type, userId memfilter actor, search memfilter action/UUID objek/requestId, from/to membatasi occurred_at. Tidak ada endpoint edit/hapus audit.

Akses: **Session + role PLATFORM_ADMIN**. Sukses utama: **HTTP 200**.

#### Parameter dan header

| Nama | Letak | Wajib | Tipe / batas |
|---|---|---|---|
| search | query | Tidak | string; minLength=0; maxLength=180 |
| status | query | Tidak | string; minLength=0; maxLength=32 |
| userId | query | Tidak | string (uuid); — |
| from | query | Tidak | string (date-time); — |
| to | query | Tidak | string (date-time); — |
| offset | query | Tidak | integer (int32); minimum=0 |
| limit | query | Tidak | integer (int32); minimum=1; maximum=100 |

#### Contoh request

```http
GET /api/v1/admin/audit HTTP/1.1
Host: localhost:8080
Cookie: JSESSIONID=example-session
```

Body request: **tidak ada**.

#### Response

Schema: [PageResponseAuditResponse](data-contracts.md#pageresponseauditresponse).

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000090",
      "actorId": "00000000-0000-4000-8000-000000000090",
      "action": "USER_STATUS_CHANGED",
      "entityType": "USER",
      "entityId": "00000000-0000-4000-8000-000000000090",
      "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh",
      "requestId": "example-request-id",
      "occurredAt": "2026-11-20T07:00:00Z",
      "before": {},
      "after": {}
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

- Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

## Sumber dan pemeliharaan

Dokumen dibangkitkan oleh [generate-reference.mjs](generate-reference.mjs) dari runtime snapshot serta catatan yang direview terhadap source. Contoh response sintetis tidak membuktikan endpoint sudah mempunyai data produksi. Lihat roadmap untuk fitur parsial.
