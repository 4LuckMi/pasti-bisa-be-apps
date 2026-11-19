# Referensi endpoint backend

[Indeks](README.md) · [Kamus semua DTO](data-contracts.md) · [OpenAPI lengkap](openapi.json)

Snapshot 19 November 2026. **38 operasi controller** dicocokkan dengan runtime; **46 operasi total** mencakup logout/OAuth/Actuator/Swagger. Resource statis internal library tidak didaftar satu per satu.

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
| 12 | GET | [`/api/v1/users/current/tryout-quota`](#e12) | Kuota harian WIB |
| 13 | GET | [`/api/v1/users/current/attempts`](#e13) | Riwayat sesi sendiri |
| 14 | GET | [`/api/v1/users/current/subscription`](#e14) | Daftar subscription sendiri |
| 15 | GET | [`/api/v1/leaderboard`](#e15) | Baca leaderboard |
| 16 | POST | [`/api/v1/attempts`](#e16) | Mulai tryout |
| 17 | POST | [`/api/v1/practice/attempts`](#e17) | Mulai practice |
| 18 | GET | [`/api/v1/users/current/attempts/active`](#e18) | Resume sesi aktif |
| 19 | GET | [`/api/v1/users/current/attempts/{id}`](#e19) | Navigator dan status sesi |
| 20 | GET | [`/api/v1/users/current/attempts/{attemptId}/questions/{position}`](#e20) | Baca satu soal dalam sesi |
| 21 | GET | [`/api/v1/users/current/attempts/{attemptId}/questions/{position}/assets/{assetId}`](#e21) | Baca asset privat soal |
| 22 | PUT | [`/api/v1/users/current/attempts/{id}/answers/{position}`](#e22) | Simpan jawaban/revision |
| 23 | POST | [`/api/v1/users/current/attempts/{id}/finalize`](#e23) | Submit atau abandon |
| 24 | GET | [`/api/v1/users/current/attempts/{id}/result`](#e24) | Status dan hasil scoring |
| 25 | GET | [`/api/v1/users/current/attempts/{id}/review`](#e25) | Review seluruh soal |
| 26 | GET | [`/api/v1/staff/taxonomy`](#e26) | Taksonomi aktif untuk petugas |
| 27 | POST | [`/api/v1/staff/imports`](#e27) | Upload sumber PDF |
| 28 | GET | [`/api/v1/staff/imports`](#e28) | Daftar riwayat impor |
| 29 | GET | [`/api/v1/staff/imports/{id}`](#e29) | Status batch dan job |
| 30 | GET | [`/api/v1/staff/imports/{id}/candidates`](#e30) | Daftar kandidat ekstraksi |
| 31 | PATCH | [`/api/v1/staff/imports/{id}/candidates/{candidateId}`](#e31) | Koreksi satu kandidat |
| 32 | POST | [`/api/v1/staff/imports/{id}/candidates/{candidateId}/save`](#e32) | Kandidat menjadi draft master |
| 33 | GET | [`/api/v1/staff/imports/{id}/source`](#e33) | Download PDF sumber |
| 34 | POST | [`/api/v1/staff/questions`](#e34) | Buat soal manual |
| 35 | GET | [`/api/v1/staff/questions`](#e35) | Cari master soal |
| 36 | GET | [`/api/v1/staff/questions/{id}`](#e36) | Detail master terbaru |
| 37 | POST | [`/api/v1/staff/questions/{id}/versions`](#e37) | Buat versi draft baru |
| 38 | POST | [`/api/v1/staff/questions/{id}/publish`](#e38) | Publish versi terbaru |
| 39 | POST | [`/api/v1/staff/questions/{id}/archive`](#e39) | Arsip keluarga soal |
| 40 | GET | [`/oauth2/authorization/google`](#e40) | Mulai login Google |
| 41 | GET | [`/login/oauth2/code/google`](#e41) | Callback Google |
| 42 | GET | [`/actuator/health`](#e42) | Health komponen |
| 43 | GET | [`/actuator/info`](#e43) | Info Actuator |
| 44 | GET | [`/actuator`](#e44) | Discovery Actuator |
| 45 | GET | [`/swagger-ui.html`](#e45) | Buka Swagger UI |
| 46 | GET | [`/api-docs`](#e46) | OpenAPI runtime |

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 INVALID_REQUEST untuk kategori target tidak didukung.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e9"></a>
### 9. Katalog tryout

**GET `/api/v1/tryouts`**

Membaca paket TRYOUT aktif, jumlah soal dan riwayat ringkas untuk versi blueprint aktif.

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

- Array kosong valid. Riwayat ringkas tidak menghitung seluruh versi blueprint lama dari paket yang sama.
- Beberapa key sengaja snake_case, sesuai DTO legacy.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e12"></a>
### 12. Kuota harian WIB

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e13"></a>
### 13. Riwayat sesi sendiri

**GET `/api/v1/users/current/attempts`**

Maksimal 100 attempt terbaru milik akun, termasuk TRYOUT dan PRACTICE.

Akses: **Session login**. Sukses utama: **HTTP 200**.

#### Contoh request

```http
GET /api/v1/users/current/attempts HTTP/1.1
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
    "details": "{\"label\":\"Indeks latihan PastiSiap; bukan skor resmi\",\"correct\":1,\"wrong\":0,\"empty\":2}"
  }
]
```

- Belum ada parameter pagination/filter per paket pada endpoint ini.
- details adalah string berisi JSON atau null; berbeda dengan object details pada endpoint result.
- total_score dan finalized_at dapat null untuk sesi yang belum selesai.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e14"></a>
### 14. Daftar subscription sendiri

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e15"></a>
### 15. Baca leaderboard

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## Lifecycle tryout dan practice

<a id="e16"></a>
### 16. Mulai tryout

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
  ]
}
```

- Key 8–128 karakter dari huruf, angka, titik, underscore, colon atau hyphen. Key sama + body berbeda ditolak.
- Jika sesi aktif ada, response bisa berupa sesi berbeda dari paket yang diminta. Selalu baca attemptId/mode/status response.
- Pool kurang rollback seluruh start dan tidak memakai kuota; start berhasil memakai satu kuota.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 IDEMPOTENCY_KEY_REQUIRED.
- 409 IDEMPOTENCY_CONFLICT.
- 409 DAILY_QUOTA_EXHAUSTED.
- 409 POOL_NOT_READY.
- 409 OVERLAPPING_SECTIONS.
- 409 UNSUPPORTED_SCORING_POLICY.
- 409 UNSUPPORTED_BLUEPRINT_FILTER.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e17"></a>
### 17. Mulai practice

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
  ]
}
```

- questionCount wajib dikirim 1–50. Durasi saat ini questionCount × 120 detik; tidak mengambil quota tryout.
- Satu sesi aktif dibagi dengan tryout. tryoutId response adalah ID blueprint internal practice.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 IDEMPOTENCY_KEY_REQUIRED.
- 409 IDEMPOTENCY_CONFLICT.
- 409 POOL_NOT_READY.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e18"></a>
### 18. Resume sesi aktif

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
  ]
}
```

- Jika tidak ada sesi, HTTP 200 dengan body kosong. Client perlu memeriksa body sebelum parsing JSON.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Tidak ada sesi aktif adalah sukses 200 kosong, bukan error.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e19"></a>
### 19. Navigator dan status sesi

**GET `/api/v1/users/current/attempts/{id}`**

Membaca deadline server, status dan daftar nomor/flag/revision milik pengguna.

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
  ]
}
```

- GET ini dapat mengubah sesi kedaluwarsa menjadi FINALIZING dan membuat job scoring.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e20"></a>
### 20. Baca satu soal dalam sesi

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
  "explanation": null
}
```

- position dimulai dari 1. selectedOptionId adalah UUID opsi, bukan huruf display.
- Sebelum score tersedia, options[].correct, correct dan explanation bernilai null. Tidak mengirim score_weight.
- Setelah scoring, correct null juga dapat berarti soal belum dijawab. Response siswa tidak berisi sumber PDF/kunci file.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e21"></a>
### 21. Baca asset privat soal

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e22"></a>
### 22. Simpan jawaban/revision

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 INVALID_OPTION.
- 409 REVISION_CONFLICT.
- 409 ATTEMPT_CLOSED.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e23"></a>
### 23. Submit atau abandon

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
  ]
}
```

- Body boleh kosong atau {}; abandon false/null berarti submit. abandon true berarti keluar/abandon.
- Jika deadline lewat, terminalReason TIMED_OUT mengalahkan parameter abandon.
- Status normal berikutnya FINALIZING, bukan nilai langsung. Request berulang tidak membuat hasil/job ganda.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e24"></a>
### 24. Status dan hasil scoring

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
  ]
}
```

- Sebelum hasil: totalScore/method/policyVersion/details null dan sections kosong. Jangan menganggap null sebagai nilai nol.
- status bisa IN_PROGRESS/FINALIZING/COMPLETED. Belum ada status API FAILED khusus scoring; telusuri job/log jika lama.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e25"></a>
### 25. Review seluruh soal

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
    "explanation": "Dua ditambah tiga sama dengan lima."
  }
]
```

- Array ini bukan master soal terbaru. Jumlah contoh dipersingkat menjadi satu item untuk keterbacaan.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 409 RESULT_PROCESSING.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## Bank soal dan impor

<a id="e26"></a>
### 26. Taksonomi aktif untuk petugas

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
- Belum tersedia taxonomy CRUD.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e27"></a>
### 27. Upload sumber PDF

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 PDF_REQUIRED.
- 400 INVALID_PDF_SIGNATURE.
- 400 INVALID_CONTENT_SCOPE.
- 413 FILE_TOO_LARGE / HTTP_413.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e28"></a>
### 28. Daftar riwayat impor

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e29"></a>
### 29. Status batch dan job

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e30"></a>
### 30. Daftar kandidat ekstraksi

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e31"></a>
### 31. Koreksi satu kandidat

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 409 CANDIDATE_ALREADY_SAVED.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e32"></a>
### 32. Kandidat menjadi draft master

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e33"></a>
### 33. Download PDF sumber

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e34"></a>
### 34. Buat soal manual

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 400 INVALID_CONTENT_SCOPE.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e35"></a>
### 35. Cari master soal

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e36"></a>
### 36. Detail master terbaru

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e37"></a>
### 37. Buat versi draft baru

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
- Tidak ada endpoint update draft di tempat pada implementasi sekarang; publish selalu menargetkan versi terbaru.

#### Error/kondisi yang perlu ditangani

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e38"></a>
### 38. Publish versi terbaru

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 409 QUESTION_INCOMPLETE.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.

<a id="e39"></a>
### 39. Arsip keluarga soal

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

- 401 tanpa session login; 403 bila method security/CSRF menolak.
- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.
- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.


## OAuth dan infrastruktur

<a id="e40"></a>
### 40. Mulai login Google

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

<a id="e41"></a>
### 41. Callback Google

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

<a id="e42"></a>
### 42. Health komponen

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

<a id="e43"></a>
### 43. Info Actuator

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

<a id="e44"></a>
### 44. Discovery Actuator

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

<a id="e45"></a>
### 45. Buka Swagger UI

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

<a id="e46"></a>
### 46. OpenAPI runtime

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


## Sumber dan pemeliharaan

Dokumen dibangkitkan oleh [generate-reference.mjs](generate-reference.mjs) dari runtime snapshot serta catatan yang direview terhadap source. Contoh response sintetis tidak membuktikan endpoint sudah mempunyai data produksi. Lihat roadmap untuk fitur parsial.
