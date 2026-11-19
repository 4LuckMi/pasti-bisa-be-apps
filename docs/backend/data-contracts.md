# Kamus DTO dan key JSON

[Indeks](README.md) · [API](03-api-reference.md)

Tipe dan batas berasal dari schema runtime yang disesuaikan dengan DTO/behavior. Kolom **Wajib input** berlaku saat model digunakan sebagai request; bukan janji semua response field non-null. Tanda null dijelaskan pada tipe/catatan.

Bean Validation @NotBlank juga melarang string kosong/whitespace meskipun hasil OpenAPI dapat menulis minLength=0. Constraint model request tidak otomatis menjadi validasi hasil ekstraksi PDF; kandidat bisa belum lengkap.

Primitive boolean/numeric request dikirim eksplisit. FinalizeRequest.abandon dan AnswerRequest.flagged adalah Boolean opsional; nilainya mempunyai default sebagaimana API reference.

<a id="answerrequest"></a>
## AnswerRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| selectedOptionId | string \| null (uuid) | Tidak ditandai | — | UUID opsi versi soal; null berarti kosong. |
| flagged | boolean \| null | Tidak ditandai | — | Jika tidak dikirim/null, service memakai false; bukan mempertahankan flag lama. |
| expectedRevision | integer (int32) | Ya | minimum=0 | Revision yang terakhir diketahui client; >=0. |

```json
{
  "selectedOptionId": "00000000-0000-4000-8000-000000000012",
  "flagged": false,
  "expectedRevision": 0
}
```

<a id="answerresponse"></a>
## AnswerResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| position | integer (int32) | Tidak ditandai | — | Nomor urutan mulai dari 1. |
| selectedOptionId | string \| null (uuid) | Tidak ditandai | — | UUID opsi versi soal; null berarti kosong. |
| flagged | boolean | Tidak ditandai | — | Penanda ragu; default false pada save jika dihilangkan/null. |
| revision | integer (int32) | Tidak ditandai | — | Revision jawaban terakhir; 0 jika belum ada row. |
| receivedAt | string (date-time) | Tidak ditandai | — | Waktu server menerima save. |

```json
{
  "position": 1,
  "selectedOptionId": "00000000-0000-4000-8000-000000000012",
  "flagged": false,
  "revision": 1,
  "receivedAt": "2026-11-19T07:01:00Z"
}
```

<a id="apierror"></a>
## ApiError

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| code | string | Ya | — | Kode error stabil untuk branching frontend, bukan status HTTP. |
| message | string | Ya | — | Pesan error aman untuk client. |
| fieldErrors | object | Ya | — | Map field request ke pesan validasi; {} bila tidak relevan. |
| requestId | string | Ya | — | Korelasi error/header/log server. |

```json
{
  "code": "REVISION_CONFLICT",
  "message": "Fetch the latest question before saving again",
  "fieldErrors": {},
  "requestId": "00000000-0000-4000-8000-000000000099"
}
```

<a id="attempthistoryresponse"></a>
## AttemptHistoryResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| tryout_id | string (uuid) | Tidak ditandai | — | ID keluarga blueprint, termasuk blueprint internal practice pada history. |
| tryout_title | string | Tidak ditandai | — | Judul blueprint saat dibaca. |
| mode | string | Tidak ditandai | — | TRYOUT atau PRACTICE. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| started_at | string (date-time) | Tidak ditandai | — | Timestamp mulai sesi. |
| finalized_at | string \| null (date-time) | Tidak ditandai | — | Timestamp finalize atau null. |
| total_score | number \| null | Tidak ditandai | — | Skor tersimpan atau null bila belum dinilai. |
| details | string \| null | Tidak ditandai | — | String JSON tersimpan atau null; lakukan parsing kedua bila ingin object. |

```json
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
```

<a id="attemptquestionresponse"></a>
## AttemptQuestionResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| position | integer (int32) | Tidak ditandai | — | Nomor urutan mulai dari 1. |
| stem | object | Tidak ditandai | — | JSON isi soal; saat ini bentuk umum {text:...}. |
| options | array<OptionResponse> | Tidak ditandai | — | Opsi berurutan; bentuk siswa berbeda dari konten authoring. |
| selectedOptionId | string \| null | Tidak ditandai | — | UUID opsi versi soal; null berarti kosong. |
| flagged | boolean | Tidak ditandai | — | Penanda ragu; default false pada save jika dihilangkan/null. |
| revision | integer (int32) | Tidak ditandai | — | Revision jawaban terakhir; 0 jika belum ada row. |
| correct | boolean \| null | Tidak ditandai | — | Flag benar; pada respons siswa null sebelum scoring/ketika tidak dijawab. |
| explanation | string \| null | Tidak ditandai | — | Pembahasan; disembunyikan pada sesi aktif, dibutuhkan saat publish. |

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

<a id="authresponse"></a>
## AuthResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| user | UserProfileResponse | Tidak ditandai | — | Ringkasan akun tanpa hash password/token. |

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

<a id="candidateresponse"></a>
## CandidateResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | UUID kandidat, bukan questionId atau batchId. |
| sourcePage | integer (int32) | Tidak ditandai | — | Halaman sumber mulai 1 atau null pada input manual. |
| sourceItemNo | integer (int32) | Tidak ditandai | — | Nomor asal yang dinormalisasi per halaman untuk dedup kandidat. |
| extractedContent | QuestionContent | Tidak ditandai | — | Konten ekstraksi asli; bisa tidak lengkap. |
| draftContent | QuestionContent \| null | Tidak ditandai | — | Koreksi petugas atau null jika belum dikoreksi. |
| validationErrors | array<string> | Tidak ditandai | — | Kode kekurangan/ambiguity kandidat; [] berarti lengkap menurut validator. |
| questionId | string \| null (uuid) | Tidak ditandai | — | UUID keluarga master setelah kandidat disimpan, selain itu null. |

```json
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
```

<a id="categoryquery"></a>
## CategoryQuery

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |

```json
{
  "category": "UTBK"
}
```

<a id="createquestionrequest"></a>
## CreateQuestionRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| subtestId | string (uuid) | Ya | — | UUID subtes dalam lingkup exam type. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik; harus sesuai subtes. |
| sourceTitle | string | Ya | minLength=0; maxLength=180 | Nama sumber/provenance, maksimal 180 karakter input. |
| sourceYear | integer \| null (int32) | Tidak ditandai | minimum=1900; maximum=2200 | Tahun sumber 1900–2200 atau null. |
| content | QuestionContent | Ya | — | JSON konten opsi siswa atau QuestionContent authoring sesuai DTO. |

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

<a id="csrfresponse"></a>
## CsrfResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| headerName | string | Tidak ditandai | — | Nama header yang harus dipakai untuk CSRF. |
| token | string | Tidak ditandai | — | Token CSRF yang terikat session; jangan dicatat ke log. |

```json
{
  "headerName": "X-CSRF-TOKEN",
  "token": "example-csrf-token"
}
```

<a id="finalizerequest"></a>
## FinalizeRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| abandon | boolean \| null | Tidak ditandai | — | true untuk abandon; false/null/body kosong untuk submit. |

```json
{
  "abandon": false
}
```

<a id="importmetadata"></a>
## ImportMetadata

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| subtestId | string (uuid) | Ya | — | UUID subtes dalam lingkup exam type. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik; harus sesuai subtes. |
| sourceTitle | string | Ya | minLength=0; maxLength=180 | Nama sumber/provenance, maksimal 180 karakter input. |
| sourceYear | integer \| null (int32) | Tidak ditandai | minimum=1900; maximum=2200 | Tahun sumber 1900–2200 atau null. |

```json
{
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceTitle": "Contoh PDF PastiSiap",
  "sourceYear": 2026
}
```

<a id="importresponse"></a>
## ImportResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | UUID batch impor. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| fileId | string (uuid) | Tidak ditandai | — | UUID metadata stored_file sumber. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik; harus sesuai subtes. |
| sourceTitle | string | Tidak ditandai | — | Nama sumber/provenance, maksimal 180 karakter input. |
| sourceYear | integer \| null (int32) | Tidak ditandai | — | Tahun sumber 1900–2200 atau null. |
| pageCount | integer \| null (int32) | Tidak ditandai | — | Jumlah halaman setelah worker memproses; awal null. |
| candidateCount | integer (int32) | Tidak ditandai | — | Total kandidat. |
| validCount | integer (int32) | Tidak ditandai | — | Kandidat tanpa validationErrors. |
| invalidCount | integer (int32) | Tidak ditandai | — | Kandidat yang masih mempunyai error. |
| errorCode | string \| null | Tidak ditandai | — | Kode error batch atau null. |
| jobStatus | string \| null | Tidak ditandai | — | Status pekerjaan terpisah dari status batch. |
| jobErrorCode | string \| null | Tidak ditandai | — | Kelas/error terakhir job yang tersensor atau null. |
| createdAt | string (date-time) | Tidak ditandai | — | Timestamp pembuatan record. |

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

<a id="leaderboardresponse"></a>
## LeaderboardResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| rank | integer (int32) | Tidak ditandai | — | Peringkat tersimpan. |
| display_name | string | Tidak ditandai | — | Nama tampil snapshot pada entry. |
| total_score | number | Tidak ditandai | — | Skor tersimpan atau null bila belum dinilai. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| period_start | string (date-time) | Tidak ditandai | — | Awal periode cohort. |
| period_end | string (date-time) | Tidak ditandai | — | Akhir periode cohort. |

```json
{
  "rank": 1,
  "display_name": "Siswa Contoh",
  "total_score": 80,
  "category": "UTBK",
  "period_start": "2026-11-01T00:00:00Z",
  "period_end": "2026-12-01T00:00:00Z"
}
```

<a id="loginrequest"></a>
## LoginRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| email | string (email) | Ya | minLength=0; maxLength=320 | Alamat email akun; bukan provider subject. |
| password | string | Ya | minLength=0; maxLength=72 | Password input saja; tidak pernah dikembalikan. Contoh bukan kredensial akun nyata. |

```json
{
  "email": "siswa@example.com",
  "password": "PasswordAman123!"
}
```

<a id="navigation"></a>
## Navigation

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| position | integer (int32) | Tidak ditandai | — | Nomor urutan mulai dari 1. |
| answered | boolean | Tidak ditandai | — | Apakah selectedOptionId terisi. |
| flagged | boolean | Tidak ditandai | — | Penanda ragu; default false pada save jika dihilangkan/null. |
| revision | integer (int32) | Tidak ditandai | — | Revision jawaban terakhir; 0 jika belum ada row. |

```json
{
  "position": 1,
  "answered": false,
  "flagged": false,
  "revision": 0
}
```

<a id="optiondraft"></a>
## OptionDraft

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| code | string | Ya | minLength=1; pattern=[A-E] | Kode resource atau kode opsi sesuai konteks. |
| text | string | Ya | minLength=0; maxLength=10000 | Teks opsi authoring. |
| correct | boolean | Ya | — | Kunci opsi; tepat satu true saat publish. Wajib dikirim eksplisit. |
| weight | number | Ya | minimum=0; maximum=1000 | Bobot numerik opsi, request 0–1000. |

```json
{
  "code": "A",
  "text": "4",
  "correct": false,
  "weight": 0
}
```

<a id="optionresponse"></a>
## OptionResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| content | object | Tidak ditandai | — | JSON konten opsi siswa atau QuestionContent authoring sesuai DTO. |
| correct | boolean \| null | Tidak ditandai | — | null sebelum hasil; true/false setelah penilaian. |

```json
{
  "id": "00000000-0000-4000-8000-000000000011",
  "code": "A",
  "content": {
    "text": "4"
  },
  "correct": null
}
```

<a id="practicetopicresponse"></a>
## PracticeTopicResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | UUID topic untuk StartPracticeRequest.topicId. |
| topic | string | Tidak ditandai | — | Nama topik. |
| subtest_code | string | Tidak ditandai | — | Kode subtes. |
| subtest | string | Tidak ditandai | — | Nama subtes. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| question_count | integer (int64) | Tidak ditandai | — | Jumlah soal per paket/topik. |

```json
{
  "id": "00000000-0000-4000-8000-000000000005",
  "topic": "Aritmetika Dasar Contoh",
  "subtest_code": "NUMERIK",
  "subtest": "Penalaran Numerik Contoh",
  "category": "UTBK",
  "question_count": 12
}
```

<a id="questioncontent"></a>
## QuestionContent

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| stemText | string | Ya | minLength=0; maxLength=20000 | Teks stem authoring, bukan JSON stem siswa. |
| options | array<OptionDraft> | Ya | minItems=2; maxItems=5 | 2–5 OptionDraft untuk request; pada hasil ekstraksi kandidat bisa belum lengkap. |
| explanation | string \| null | Tidak ditandai | minLength=0; maxLength=20000 | Opsional pada draft request; wajib tidak kosong saat publish. |
| shuffleAllowed | boolean | Ya | — | Izin mengacak opsi; kirim eksplisit agar urutan bergantung konteks tidak rusak. |

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

<a id="quotaresponse"></a>
## QuotaResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| dateWib | string (date) | Tidak ditandai | — | Tanggal kuota Asia/Jakarta. |
| used | integer (int32) | Tidak ditandai | — | Start tryout yang sudah memakai kuota tanggal ini. |
| limit | integer (int32) | Tidak ditandai | — | Batas kuota harian saat ini 2. |
| remaining | integer (int32) | Tidak ditandai | — | Sisa kuota, minimal 0. |

```json
{
  "dateWib": "2026-11-19",
  "used": 1,
  "limit": 2,
  "remaining": 1
}
```

<a id="registerrequest"></a>
## RegisterRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| email | string (email) | Ya | minLength=0; maxLength=320 | Alamat email akun; bukan provider subject. |
| displayName | string | Ya | minLength=2; maxLength=120 | Nama tampil pengguna. |
| password | string | Ya | minLength=12; maxLength=72 | Password input saja; tidak pernah dikembalikan. Contoh bukan kredensial akun nyata. |

```json
{
  "email": "siswa@example.com",
  "displayName": "Nama Siswa",
  "password": "PasswordAman123!"
}
```

<a id="resultresponse"></a>
## ResultResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| attemptId | string (uuid) | Tidak ditandai | — | UUID sesi milik akun aktif. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| terminalReason | string | Tidak ditandai | — | null, SUBMITTED, ABANDONED atau TIMED_OUT. |
| method | string \| null | Tidak ditandai | — | Metode scoring BINARY atau OPTION_WEIGHT setelah hasil tersedia. |
| policyVersion | string \| null | Tidak ditandai | — | Identitas policy code:version hasil. |
| totalScore | number \| null | Tidak ditandai | — | Indeks latihan tersimpan; null bila belum ada hasil. |
| details | object | Tidak ditandai | — | Rincian hasil; bentuk object pada result, string JSON pada history. |
| sections | array<SectionResult> | Tidak ditandai | — | Rincian subtes atau section pada konteks DTO. |

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

<a id="sectionresponse"></a>
## SectionResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| name | string | Tidak ditandai | — | Nama section/subtes. |
| position | integer (int32) | Tidak ditandai | — | Nomor urutan mulai dari 1. |
| questionCount | integer (int32) | Tidak ditandai | — | Jumlah soal diminta pada practice atau kebutuhan section. |
| available | integer (int64) | Tidak ditandai | — | Jumlah stok global terbit, belum mengurangi exposure personal. |

```json
{
  "id": "00000000-0000-4000-8000-000000000023",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "name": "Penalaran Numerik Contoh",
  "position": 1,
  "questionCount": 3,
  "available": 12
}
```

<a id="sectionresult"></a>
## SectionResult

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| correct | integer (int32) | Tidak ditandai | — | Jumlah jawaban benar dalam subtes, bukan boolean. |
| wrong | integer (int32) | Tidak ditandai | — | Jumlah salah. |
| empty | integer (int32) | Tidak ditandai | — | Jumlah kosong. |
| points | number | Tidak ditandai | — | Poin mentah section. |
| maxPoints | number | Tidak ditandai | — | Jumlah maksimum poin section. |
| accuracy | number | Tidak ditandai | — | Persentase benar, 4 desimal. |

```json
{
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "correct": 1,
  "wrong": 0,
  "empty": 2,
  "points": 1,
  "maxPoints": 3,
  "accuracy": 33.3333
}
```

<a id="servicestatus"></a>
## ServiceStatus

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| service | string | Tidak ditandai | — | Nama aplikasi. |
| timestamp | string (date-time) | Tidak ditandai | — | Timestamp server UTC. |

```json
{
  "status": "UP",
  "service": "pastisiap-backend",
  "timestamp": "2026-11-19T07:00:00Z"
}
```

<a id="sessionresponse"></a>
## SessionResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| attemptId | string (uuid) | Tidak ditandai | — | UUID sesi milik akun aktif. |
| tryoutId | string (uuid) | Tidak ditandai | — | ID blueprint/package; pada practice menunjuk blueprint internal. |
| mode | string | Tidak ditandai | — | TRYOUT atau PRACTICE. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| terminalReason | string \| null | Tidak ditandai | — | null, SUBMITTED, ABANDONED atau TIMED_OUT. |
| serverNow | string (date-time) | Tidak ditandai | — | Waktu server agar frontend menghitung countdown. |
| startedAt | string (date-time) | Tidak ditandai | — | Timestamp mulai server. |
| deadlineAt | string (date-time) | Tidak ditandai | — | Deadline server yang otoritatif. |
| questions | array<Navigation> | Tidak ditandai | — | Navigator {position,answered,flagged,revision}; isi soal dibaca lewat endpoint question. |

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

<a id="staffquestionresponse"></a>
## StaffQuestionResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | UUID keluarga master question, bukan UUID versi. |
| versionId | string (uuid) | Tidak ditandai | — | UUID versi soal terbaru yang dikembalikan. |
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| version | integer (int32) | Tidak ditandai | — | Nomor versi positif. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik; harus sesuai subtes. |
| sourceYear | integer \| null (int32) | Tidak ditandai | — | Tahun sumber 1900–2200 atau null. |
| sourceTitle | string \| null | Tidak ditandai | — | Nama sumber/provenance, maksimal 180 karakter input. |
| sourcePage | integer \| null (int32) | Tidak ditandai | — | Halaman sumber mulai 1 atau null pada input manual. |
| content | QuestionContent | Tidak ditandai | — | JSON konten opsi siswa atau QuestionContent authoring sesuai DTO. |

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

<a id="startpracticerequest"></a>
## StartPracticeRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| topicId | string (uuid) | Ya | — | ID topik; harus sesuai subtes. |
| questionCount | integer (int32) | Ya | minimum=1; maximum=50 | Jumlah soal diminta pada practice atau kebutuhan section. |

```json
{
  "topicId": "00000000-0000-4000-8000-000000000005",
  "questionCount": 3
}
```

<a id="starttryoutrequest"></a>
## StartTryoutRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| tryoutId | string (uuid) | Ya | — | ID blueprint/package; pada practice menunjuk blueprint internal. |

```json
{
  "tryoutId": "00000000-0000-4000-8000-000000000002"
}
```

<a id="studentprofileresponse"></a>
## StudentProfileResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| email | string | Tidak ditandai | — | Alamat email akun; bukan provider subject. |
| displayName | string | Tidak ditandai | — | Nama tampil pengguna. |
| userType | string | Tidak ditandai | — | Tipe produk; registrasi publik menghasilkan SISWA. |
| schoolName | string \| null | Tidak ditandai | — | Nama sekolah. |
| schoolGrade | string \| null | Tidak ditandai | — | Kelas/tingkat, contoh XII. |
| leaderboardOptIn | boolean | Tidak ditandai | — | Persetujuan tampil pada leaderboard; belum dapat diubah lewat PATCH profile. |
| targetScore | integer \| null (int32) | Tidak ditandai | — | Skor target pilihan pengguna, 0–1000 atau null. |
| targetKind | string \| null | Tidak ditandai | — | Jenis target tersimpan hasil mapping kategori. |
| targetInstitution | string \| null | Tidak ditandai | — | Nama institusi target. |
| targetProgram | string \| null | Tidak ditandai | — | Program/jurusan target, boleh null. |

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

<a id="subscriptionresponse"></a>
## SubscriptionResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| starts_at | string (date-time) | Tidak ditandai | — | Awal interval. |
| ends_at | string (date-time) | Tidak ditandai | — | Akhir interval. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| active | boolean | Tidak ditandai | — | Hak/grant masih berlaku saat server membaca. |

```json
{
  "id": "00000000-0000-4000-8000-000000000040",
  "starts_at": "2026-11-01T00:00:00Z",
  "ends_at": "2026-12-01T00:00:00Z",
  "status": "ACTIVE",
  "active": true
}
```

<a id="taxonomyresponse"></a>
## TaxonomyResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| categoryId | string (uuid) | Tidak ditandai | — | UUID kategori. |
| categoryCode | string | Tidak ditandai | — | Kode kategori. |
| categoryName | string | Tidak ditandai | — | Nama kategori. |
| examTypeId | string (uuid) | Tidak ditandai | — | UUID jenis ujian. |
| examTypeCode | string | Tidak ditandai | — | Kode jenis ujian. |
| examTypeName | string | Tidak ditandai | — | Nama jenis ujian. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| subtestName | string | Tidak ditandai | — | Nama subtes. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik; harus sesuai subtes. |
| topicName | string \| null | Tidak ditandai | — | Nama topik; null jika tidak ada. |

```json
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
```

<a id="tryoutdetail"></a>
## TryoutDetail

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| title | string | Tidak ditandai | — | Judul paket/blueprint. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| durationSeconds | integer (int32) | Tidak ditandai | — | Durasi konfigurasi dalam detik. |
| scoringMethod | string | Tidak ditandai | — | Metode policy blueprint. |
| scale | number | Tidak ditandai | — | Skala normalisasi indeks latihan. |
| demo | boolean | Tidak ditandai | — | Menandai blueprint contoh. |
| ready | boolean | Tidak ditandai | — | Seluruh section mempunyai stok global minimal kebutuhan. |
| sections | array<SectionResponse> | Tidak ditandai | — | Rincian subtes atau section pada konteks DTO. |

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

<a id="tryoutresponse"></a>
## TryoutResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | UUID blueprint untuk StartTryoutRequest.tryoutId. |
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| title | string | Tidak ditandai | — | Judul paket/blueprint. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| exam_type | string | Tidak ditandai | — | Nama jenis ujian. |
| blueprint_version_id | string (uuid) | Tidak ditandai | — | UUID versi blueprint aktif. |
| duration_seconds | integer (int32) | Tidak ditandai | — | Durasi versi blueprint dalam detik. |
| question_count | integer (int64) | Tidak ditandai | — | Jumlah soal per paket/topik. |
| attempt_count | integer (int64) | Tidak ditandai | — | Jumlah sesi pengguna pada versi blueprint aktif. |
| latest_score | number \| null | Tidak ditandai | — | Skor sesi terakhir pada versi ini atau null. |
| latest_completed_at | string \| null (date-time) | Tidak ditandai | — | Timestamp hasil terakhir pada versi ini atau null. |

```json
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
```

<a id="updatestudentprofilerequest"></a>
## UpdateStudentProfileRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| displayName | string | Ya | minLength=2; maxLength=120 | Nama tampil pengguna. |
| schoolName | string | Ya | minLength=0; maxLength=180 | Nama sekolah. |
| schoolGrade | string | Ya | minLength=0; maxLength=32 | Kelas/tingkat, contoh XII. |
| targetCategory | string | Ya | minLength=0; maxLength=24; enum=UTBK/CPNS/KEDINASAN/SEKOLAH | Kategori form: UTBK/CPNS/KEDINASAN/SEKOLAH. |
| targetInstitution | string | Ya | minLength=0; maxLength=180 | Nama institusi target. |
| targetProgram | string \| null | Tidak ditandai | minLength=0; maxLength=180 | Program/jurusan target, boleh null. |
| targetScore | integer \| null (int32) | Tidak ditandai | minimum=0; maximum=1000 | Skor target pilihan pengguna, 0–1000 atau null. |

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

<a id="userprofileresponse"></a>
## UserProfileResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| email | string | Tidak ditandai | — | Alamat email akun; bukan provider subject. |
| displayName | string | Tidak ditandai | — | Nama tampil pengguna. |
| userType | string | Tidak ditandai | — | Tipe produk; registrasi publik menghasilkan SISWA. |
| roles | array<string> | Tidak ditandai | — | Daftar role aplikasi, misalnya CUSTOMER. |

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

