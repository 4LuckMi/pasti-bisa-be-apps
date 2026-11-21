# Kamus DTO dan key JSON

[Indeks](README.md) · [API](03-api-reference.md)

Tipe dan batas berasal dari schema runtime yang disesuaikan dengan DTO/behavior. Kolom **Wajib input** berlaku saat model digunakan sebagai request; bukan janji semua response field non-null. Tanda null dijelaskan pada tipe/catatan.

Bean Validation @NotBlank juga melarang string kosong/whitespace meskipun hasil OpenAPI dapat menulis minLength=0. Constraint model request tidak otomatis menjadi validasi hasil ekstraksi PDF; kandidat bisa belum lengkap.

Primitive boolean/numeric request dikirim eksplisit. FinalizeRequest.abandon dan AnswerRequest.flagged adalah Boolean opsional; nilainya mempunyai default sebagaimana API reference.

<a id="activerequest"></a>
## ActiveRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| active | boolean | Ya | — | Hak/grant masih berlaku saat server membaca. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "active": false,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="adminsubscriptionresponse"></a>
## AdminSubscriptionResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| userId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| startsAt | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| endsAt | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| grants | array<GrantResponse> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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

<a id="adminuserresponse"></a>
## AdminUserResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| email | string | Tidak ditandai | — | Alamat email akun; bukan provider subject. |
| displayName | string | Tidak ditandai | — | Nama tampil pengguna. |
| userType | string | Tidak ditandai | — | Tipe produk; registrasi publik menghasilkan SISWA. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| roles | array<string> | Tidak ditandai | — | Daftar role aplikasi, misalnya CUSTOMER. |
| createdAt | string (date-time) | Tidak ditandai | — | Timestamp pembuatan record. |
| updatedAt | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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
| topic_id | string \| null (uuid) | Tidak ditandai | — | ID topik latihan; berbeda dengan tryout_id blueprint internal. Null untuk tryout. |
| topic_name | string \| null | Tidak ditandai | — | Nama topik latihan atau null. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| terminal_reason | string \| null | Tidak ditandai | — | SUBMITTED / ABANDONED / TIMED_OUT; tidak sama dengan lifecycle status. |
| question_count | integer (int32) | Tidak ditandai | — | Jumlah soal per paket/topik. |
| duration_seconds | integer (int32) | Tidak ditandai | — | Durasi versi blueprint dalam detik. |

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
  "details": "{\"label\":\"Indeks latihan PastiSiap; bukan skor resmi\",\"correct\":1,\"wrong\":0,\"empty\":2}",
  "topic_id": null,
  "topic_name": null,
  "category": "UTBK",
  "terminal_reason": "SUBMITTED",
  "question_count": 3,
  "duration_seconds": 5400
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
| scope | QuestionScope | Tidak ditandai | — | Metadata subtes/topik soal tanpa kunci atau bobot. |

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

<a id="auditresponse"></a>
## AuditResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| actorId | string \| null (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| action | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| entityType | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| entityId | string \| null (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| reason | string \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| requestId | string \| null | Tidak ditandai | — | Korelasi error/header/log server. |
| occurredAt | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| before | JsonNode \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| after | JsonNode \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
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

<a id="benefits"></a>
## Benefits

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| fullLeaderboard | boolean | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "fullLeaderboard": true
}
```

<a id="blueprintdetail"></a>
## BlueprintDetail

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| blueprint | BlueprintResponse | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| versions | array<BlueprintVersion> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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
          "sourceYear": null,
          "available": 12,
          "questionSetIds": [
            "00000000-0000-4000-8000-000000000100"
          ],
          "difficultyQuotas": [
            {
              "difficulty": "EASY",
              "count": 1
            },
            {
              "difficulty": "MEDIUM",
              "count": 2
            }
          ],
          "repeatPolicy": "UNSEEN_TODAY"
        }
      ],
      "ready": true
    }
  ]
}
```

<a id="blueprintrequest"></a>
## BlueprintRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| examTypeId | string (uuid) | Ya | — | UUID jenis ujian. |
| code | string | Ya | minLength=1; pattern=[A-Z][A-Z0-9_-]{1,59} | Kode resource atau kode opsi sesuai konteks. |
| title | string | Ya | minLength=0; maxLength=180 | Judul paket/blueprint. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "code": "TRYOUT_UTBK_01",
  "title": "Tryout UTBK 01",
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="blueprintresponse"></a>
## BlueprintResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| examTypeId | string (uuid) | Tidak ditandai | — | UUID jenis ujian. |
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| title | string | Tidak ditandai | — | Judul paket/blueprint. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| activeVersionId | string \| null (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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

<a id="blueprintsection"></a>
## BlueprintSection

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| position | integer (int32) | Tidak ditandai | — | Nomor urutan mulai dari 1. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| subtestName | string | Tidak ditandai | — | Nama subtes. |
| questionCount | integer (int32) | Tidak ditandai | — | Jumlah soal diminta pada practice atau kebutuhan section. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik; harus sesuai subtes. |
| sourceYear | integer \| null (int32) | Tidak ditandai | — | Tahun sumber 1900–2200 atau null. |
| available | integer (int64) | Tidak ditandai | — | Jumlah stok global terbit, belum mengurangi exposure personal. |
| questionSetIds | array<string (uuid)> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| difficultyQuotas | array<DifficultyAllocation> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| repeatPolicy | string \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "id": "00000000-0000-4000-8000-000000000023",
  "position": 1,
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "subtestName": "Penalaran Numerik",
  "questionCount": 3,
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceYear": null,
  "available": 12,
  "questionSetIds": [
    "00000000-0000-4000-8000-000000000100"
  ],
  "difficultyQuotas": [
    {
      "difficulty": "EASY",
      "count": 1
    },
    {
      "difficulty": "MEDIUM",
      "count": 2
    }
  ],
  "repeatPolicy": "UNSEEN_TODAY"
}
```

<a id="blueprintstatusrequest"></a>
## BlueprintStatusRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| status | string | Ya | minLength=1; pattern=ACTIVE\|ARCHIVED | Status resource; baca state machine sesuai attempt/batch/job. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "status": "ARCHIVED",
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="blueprintversion"></a>
## BlueprintVersion

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| version | integer (int32) | Tidak ditandai | — | Nomor versi positif. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| durationSeconds | integer (int32) | Tidak ditandai | — | Durasi konfigurasi dalam detik. |
| scoringPolicyId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| sections | array<BlueprintSection> | Tidak ditandai | — | Rincian subtes atau section pada konteks DTO. |
| ready | boolean | Tidak ditandai | — | Seluruh section mempunyai stok global minimal kebutuhan. |

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
      "sourceYear": null,
      "available": 12,
      "questionSetIds": [
        "00000000-0000-4000-8000-000000000100"
      ],
      "difficultyQuotas": [
        {
          "difficulty": "EASY",
          "count": 1
        },
        {
          "difficulty": "MEDIUM",
          "count": 2
        }
      ],
      "repeatPolicy": "UNSEEN_TODAY"
    }
  ],
  "ready": true
}
```

<a id="blueprintversionrequest"></a>
## BlueprintVersionRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| durationSeconds | integer (int32) | Tidak ditandai | minimum=60; maximum=86400 | Durasi konfigurasi dalam detik. |
| scoringPolicyId | string (uuid) | Ya | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| sections | array<SectionRequest> | Ya | minItems=0; maxItems=30 | Rincian subtes atau section pada konteks DTO. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "durationSeconds": 5400,
  "scoringPolicyId": "00000000-0000-4000-8000-000000000070",
  "sections": [
    {
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "questionCount": 3,
      "topicId": "00000000-0000-4000-8000-000000000005",
      "sourceYear": null,
      "questionSetIds": [
        "00000000-0000-4000-8000-000000000100"
      ],
      "difficultyQuotas": [
        {
          "difficulty": "EASY",
          "count": 1
        },
        {
          "difficulty": "MEDIUM",
          "count": 2
        }
      ],
      "repeatPolicy": "UNSEEN_TODAY"
    }
  ],
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="candidatecorrection"></a>
## CandidateCorrection

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| sourceItemCode | string | Ya | minLength=1; pattern=[A-Za-z0-9][A-Za-z0-9_.-]{0,79} | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik; harus sesuai subtes. |
| difficulty | string | Ya | minLength=1; pattern=EASY\|MEDIUM\|HARD | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| content | QuestionContent | Ya | — | JSON konten opsi siswa atau QuestionContent authoring sesuai DTO. |

```json
{
  "sourceItemCode": "PU_Q001",
  "topicId": null,
  "difficulty": "MEDIUM",
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

<a id="categoryrequest"></a>
## CategoryRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| code | string | Ya | minLength=1; pattern=[A-Z][A-Z0-9_]{1,39} | Kode resource atau kode opsi sesuai konteks. |
| name | string | Ya | minLength=0; maxLength=120 | Nama section/subtes. |
| active | boolean | Ya | — | Hak/grant masih berlaku saat server membaca. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "code": "UTBK",
  "name": "Ujian Tulis Berbasis Komputer",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="cohort"></a>
## Cohort

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| title | string | Tidak ditandai | — | Judul paket/blueprint. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| examTypeId | string (uuid) | Tidak ditandai | — | UUID jenis ujian. |
| formId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| scoringPolicyId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| periodStart | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| periodEnd | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| attemptPolicy | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| ended | boolean | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "id": "00000000-0000-4000-8000-000000000061",
  "title": "Tryout UTBK Contoh",
  "category": "UTBK",
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "formId": "00000000-0000-4000-8000-000000000062",
  "scoringPolicyId": "00000000-0000-4000-8000-000000000070",
  "periodStart": "2026-11-01T00:00:00Z",
  "periodEnd": "2026-12-01T00:00:00Z",
  "attemptPolicy": "BEST",
  "ended": false
}
```

<a id="contentaudit"></a>
## ContentAudit

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| actorId | string \| null (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| action | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| entityId | string \| null (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| reason | string \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| requestId | string \| null | Tidak ditandai | — | Korelasi error/header/log server. |
| occurredAt | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "actorId": "00000000-0000-4000-8000-000000000090",
  "action": "USER_STATUS_CHANGED",
  "entityId": "00000000-0000-4000-8000-000000000090",
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh",
  "requestId": "example-request-id",
  "occurredAt": "2026-11-20T07:00:00Z"
}
```

<a id="coursecontext"></a>
## CourseContext

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| title | string | Tidak ditandai | — | Judul sesi yang dibaca dari keluarga blueprint terkait. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| examType | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik asal pada practice; gunakan untuk link detail latihan, bukan tryoutId internal. |
| topicName | string \| null | Tidak ditandai | — | Nama topik; null jika tidak ada. |
| sections | array<SessionSection> | Tidak ditandai | — | Nama dan jumlah soal subtes dari versi blueprint sesi, bukan versi katalog aktif terbaru. |

```json
{
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

<a id="dashboard"></a>
## Dashboard

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| generatedAt | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| from | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| to | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| activeAccounts | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| newAccounts | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| completedSessions | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| publishedQuestions | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| failedJobs | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| pendingImports | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| storedFiles | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| storedBytes | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| revenue | array<Revenue> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| paymentProviderConfigured | boolean | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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

<a id="difficultyallocation"></a>
## DifficultyAllocation

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| difficulty | string | Ya | minLength=1; pattern=EASY\|MEDIUM\|HARD | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| count | integer (int32) | Tidak ditandai | minimum=1; maximum=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "difficulty": "EASY",
  "count": 1
}
```

<a id="draftrequest"></a>
## DraftRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| expectedVersionId | string (uuid) | Ya | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| expectedRevision | integer (int64) | Ya | minimum=0 | Revision yang terakhir diketahui client; >=0. |
| content | QuestionContent | Ya | — | JSON konten opsi siswa atau QuestionContent authoring sesuai DTO. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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

<a id="draftview"></a>
## DraftView

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| question | StaffQuestionResponse | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| revision | integer (int64) | Tidak ditandai | — | Revision jawaban terakhir; 0 jika belum ada row. |

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

<a id="examtyperequest"></a>
## ExamTypeRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| categoryId | string (uuid) | Ya | — | UUID kategori. |
| code | string | Ya | minLength=1; pattern=[A-Z][A-Z0-9_]{1,39} | Kode resource atau kode opsi sesuai konteks. |
| name | string | Ya | minLength=0; maxLength=120 | Nama section/subtes. |
| active | boolean | Ya | — | Hak/grant masih berlaku saat server membaca. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "categoryId": "00000000-0000-4000-8000-000000000024",
  "code": "TPS",
  "name": "Tes Potensi Skolastik",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="excelcandidate"></a>
## ExcelCandidate

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| sheetId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| sheetName | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| sourceRow | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| sourceItemCode | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik; harus sesuai subtes. |
| difficulty | string \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| extractedContent | QuestionContent | Tidak ditandai | — | Konten ekstraksi asli; bisa tidak lengkap. |
| draftContent | QuestionContent \| null | Tidak ditandai | — | Koreksi petugas atau null jika belum dikoreksi. |
| validationErrors | array<string> | Tidak ditandai | — | Kode kekurangan/ambiguity kandidat; [] berarti lengkap menurut validator. |
| expectedVersionId | string \| null (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| resolvedQuestionId | string \| null (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| resolvedVersionId | string \| null (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "id": "00000000-0000-4000-8000-000000000103",
  "sheetId": "00000000-0000-4000-8000-000000000102",
  "sheetName": "NUMERIK",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "sourceRow": 2,
  "sourceItemCode": "PU_Q001",
  "topicId": null,
  "difficulty": "MEDIUM",
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
  "expectedVersionId": null,
  "resolvedQuestionId": null,
  "resolvedVersionId": null
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

<a id="grantresponse"></a>
## GrantResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| orderId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| startsAt | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| endsAt | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| revokedAt | string \| null (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "orderId": "00000000-0000-4000-8000-000000000090",
  "startsAt": "2026-11-20T07:00:00Z",
  "endsAt": "2026-11-20T07:00:00Z",
  "revokedAt": "2026-11-20T07:00:00Z"
}
```

<a id="historyentry"></a>
## HistoryEntry

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| attemptId | string (uuid) | Tidak ditandai | — | UUID sesi milik akun aktif. |
| tryoutId | string (uuid) | Tidak ditandai | — | ID blueprint/package; pada practice menunjuk blueprint internal. |
| title | string | Tidak ditandai | — | Judul paket/blueprint. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| mode | string | Tidak ditandai | — | TRYOUT atau PRACTICE. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| terminalReason | string \| null | Tidak ditandai | — | null, SUBMITTED, ABANDONED atau TIMED_OUT. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik; harus sesuai subtes. |
| topicName | string \| null | Tidak ditandai | — | Nama topik; null jika tidak ada. |
| startedAt | string (date-time) | Tidak ditandai | — | Timestamp mulai server. |
| finalizedAt | string \| null (date-time) | Tidak ditandai | — | Timestamp finalize server; null sebelum sesi ditutup. |
| totalScore | number \| null | Tidak ditandai | — | Indeks latihan tersimpan; null bila belum ada hasil. |
| questionCount | integer (int32) | Tidak ditandai | — | Jumlah soal diminta pada practice atau kebutuhan section. |
| durationSeconds | integer (int32) | Tidak ditandai | — | Durasi konfigurasi dalam detik. |
| processing | Processing | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "attemptId": "00000000-0000-4000-8000-000000000006",
  "tryoutId": "00000000-0000-4000-8000-000000000002",
  "title": "Tryout UTBK Contoh",
  "category": "UTBK",
  "mode": "TRYOUT",
  "status": "COMPLETED",
  "terminalReason": "SUBMITTED",
  "topicId": null,
  "topicName": null,
  "startedAt": "2026-11-21T06:50:00Z",
  "finalizedAt": "2026-11-21T07:00:00Z",
  "totalScore": 80,
  "questionCount": 3,
  "durationSeconds": 5400,
  "processing": {
    "state": "READY",
    "poll": false,
    "retryAfterSeconds": 0,
    "errorCode": null
  }
}
```

<a id="historypage"></a>
## HistoryPage

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<HistoryEntry> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |
| summary | HistorySummary | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "items": [
    {
      "attemptId": "00000000-0000-4000-8000-000000000006",
      "tryoutId": "00000000-0000-4000-8000-000000000002",
      "title": "Tryout UTBK Contoh",
      "category": "UTBK",
      "mode": "TRYOUT",
      "status": "COMPLETED",
      "terminalReason": "SUBMITTED",
      "topicId": null,
      "topicName": null,
      "startedAt": "2026-11-21T06:50:00Z",
      "finalizedAt": "2026-11-21T07:00:00Z",
      "totalScore": 80,
      "questionCount": 3,
      "durationSeconds": 5400,
      "processing": {
        "state": "READY",
        "poll": false,
        "retryAfterSeconds": 0,
        "errorCode": null
      }
    }
  ],
  "total": 10,
  "offset": 0,
  "limit": 6,
  "summary": {
    "total": 10,
    "completed": 8,
    "inProgress": 1,
    "scoring": 1,
    "scoringFailed": 0
  }
}
```

<a id="historysummary"></a>
## HistorySummary

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| completed | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| inProgress | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| scoring | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| scoringFailed | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "total": 10,
  "completed": 8,
  "inProgress": 1,
  "scoring": 1,
  "scoringFailed": 0
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

<a id="importsummary"></a>
## ImportSummary

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| batch | ImportResponse | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| originalName | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| uploaderId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
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
```

<a id="internaluserrequest"></a>
## InternalUserRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| email | string (email) | Ya | minLength=0; maxLength=320 | Alamat email akun; bukan provider subject. |
| displayName | string | Ya | minLength=0; maxLength=120 | Nama tampil pengguna. |
| roles | array<string> | Ya | minItems=0; maxItems=2 | Daftar role aplikasi, misalnya CUSTOMER. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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

<a id="jobresponse"></a>
## JobResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| type | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| attempts | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| maxAttempts | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| runAfter | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| leaseUntil | string \| null (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| errorCode | string \| null | Tidak ditandai | — | Kode error batch atau null. |
| createdAt | string (date-time) | Tidak ditandai | — | Timestamp pembuatan record. |

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

<a id="jsonnode"></a>
## JsonNode

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|

```json
{}
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

<a id="linksourcequestion"></a>
## LinkSourceQuestion

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| questionId | string (uuid) | Ya | — | UUID keluarga master setelah kandidat disimpan, selain itu null. |
| sourceItemCode | string | Ya | minLength=1; pattern=[A-Za-z0-9][A-Za-z0-9_.-]{0,79} | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "questionId": "00000000-0000-4000-8000-000000000007",
  "sourceItemCode": "PU_Q001",
  "reason": "Review sumber dan unggahan soal contoh"
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

<a id="membership"></a>
## Membership

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| tier | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| startsAt | string \| null (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| endsAt | string \| null (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| plan | MembershipPlan \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| benefits | Benefits | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| leaderboardPreviewLimit | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| dailyTryoutLimit | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "tier": "PREMIUM",
  "startsAt": "2026-11-01T00:00:00Z",
  "endsAt": "2026-12-01T00:00:00Z",
  "plan": {
    "id": "00000000-0000-4000-8000-000000000060",
    "code": "PREMIUM_MONTH_R1",
    "name": "Premium 1 bulan",
    "months": 1,
    "amountMinor": 39000,
    "currency": "IDR"
  },
  "benefits": {
    "fullLeaderboard": true
  },
  "leaderboardPreviewLimit": 3,
  "dailyTryoutLimit": 2
}
```

<a id="membershipplan"></a>
## MembershipPlan

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| name | string | Tidak ditandai | — | Nama section/subtes. |
| months | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| amountMinor | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| currency | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "id": "00000000-0000-4000-8000-000000000060",
  "code": "PREMIUM_MONTH_R1",
  "name": "Premium 1 bulan",
  "months": 1,
  "amountMinor": 39000,
  "currency": "IDR"
}
```

<a id="mutationreason"></a>
## MutationReason

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
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

<a id="orderdetail"></a>
## OrderDetail

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| order | OrderResponse | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| events | array<PaymentEventResponse> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| refunds | array<RefundResponse> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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

<a id="orderresponse"></a>
## OrderResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| userId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| userEmail | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| planId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| planCode | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| months | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| amountMinor | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| currency | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| provider | string \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| providerOrderId | string \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| createdAt | string (date-time) | Tidak ditandai | — | Timestamp pembuatan record. |
| expiresAt | string \| null (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
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
```

<a id="pageresponseadminsubscriptionresponse"></a>
## PageResponseAdminSubscriptionResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<AdminSubscriptionResponse> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponseadminuserresponse"></a>
## PageResponseAdminUserResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<AdminUserResponse> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponseauditresponse"></a>
## PageResponseAuditResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<AuditResponse> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponseblueprintresponse"></a>
## PageResponseBlueprintResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<BlueprintResponse> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponsecohort"></a>
## PageResponseCohort

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<Cohort> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000061",
      "title": "Tryout UTBK Contoh",
      "category": "UTBK",
      "examTypeId": "00000000-0000-4000-8000-000000000025",
      "formId": "00000000-0000-4000-8000-000000000062",
      "scoringPolicyId": "00000000-0000-4000-8000-000000000070",
      "periodStart": "2026-11-01T00:00:00Z",
      "periodEnd": "2026-12-01T00:00:00Z",
      "attemptPolicy": "BEST",
      "ended": false
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

<a id="pageresponsecontentaudit"></a>
## PageResponseContentAudit

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<ContentAudit> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponseexcelcandidate"></a>
## PageResponseExcelCandidate

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<ExcelCandidate> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000103",
      "sheetId": "00000000-0000-4000-8000-000000000102",
      "sheetName": "NUMERIK",
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "sourceRow": 2,
      "sourceItemCode": "PU_Q001",
      "topicId": null,
      "difficulty": "MEDIUM",
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
      "expectedVersionId": null,
      "resolvedQuestionId": null,
      "resolvedVersionId": null
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

<a id="pageresponseimportsummary"></a>
## PageResponseImportSummary

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<ImportSummary> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponsejobresponse"></a>
## PageResponseJobResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<JobResponse> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponseorderresponse"></a>
## PageResponseOrderResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<OrderResponse> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponseplan"></a>
## PageResponsePlan

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<Plan> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000060",
      "code": "PREMIUM_MONTH_R1",
      "familyCode": "PREMIUM_MONTH",
      "revision": 1,
      "name": "Premium 1 bulan",
      "months": 1,
      "amountMinor": 39000,
      "currency": "IDR",
      "benefits": {
        "fullLeaderboard": true
      }
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

<a id="pageresponseplanresponse"></a>
## PageResponsePlanResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<PlanResponse> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponsepolicyresponse"></a>
## PageResponsePolicyResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<PolicyResponse> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponsepoolrequirement"></a>
## PageResponsePoolRequirement

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<PoolRequirement> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponsequestionsource"></a>
## PageResponseQuestionSource

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<QuestionSource> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000100",
      "examTypeId": "00000000-0000-4000-8000-000000000025",
      "code": "UTBK_2025_A",
      "sourceYear": 2025,
      "title": "Sumber UTBK 2025 paket A",
      "status": "ACTIVE",
      "questionCount": 100,
      "publishedCount": 80
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

<a id="pageresponsequestionsummary"></a>
## PageResponseQuestionSummary

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<QuestionSummary> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponsesourceitem"></a>
## PageResponseSourceItem

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<SourceItem> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

```json
{
  "items": [
    {
      "questionId": "00000000-0000-4000-8000-000000000007",
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "sourceItemCode": "PU_Q001",
      "latestVersionId": "00000000-0000-4000-8000-000000000008",
      "status": "PUBLISHED",
      "difficulty": "MEDIUM",
      "archived": false
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

<a id="pageresponsetopiccard"></a>
## PageResponseTopicCard

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<TopicCard> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000005",
      "topic": "Aritmetika",
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "subtestCode": "NUMERIK",
      "subtest": "Penalaran Numerik",
      "examTypeId": "00000000-0000-4000-8000-000000000025",
      "examType": "TPS",
      "category": "UTBK",
      "questionCount": 12,
      "ready": true
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

<a id="pageresponsetryoutcard"></a>
## PageResponseTryoutCard

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<TryoutCard> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000002",
      "code": "DEMO_TRYOUT",
      "title": "Tryout UTBK Contoh",
      "category": "UTBK",
      "examTypeId": "00000000-0000-4000-8000-000000000025",
      "examType": "TPS",
      "blueprintVersionId": "00000000-0000-4000-8000-000000000003",
      "durationSeconds": 5400,
      "questionCount": 3,
      "ready": true,
      "attemptCount": 2,
      "latestScore": 80,
      "latestCompletedAt": "2026-11-21T07:00:00Z"
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

<a id="pageresponseuserattempt"></a>
## PageResponseUserAttempt

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<UserAttempt> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

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

<a id="pageresponseworkbookimport"></a>
## PageResponseWorkbookImport

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| items | array<WorkbookImport> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| total | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |

```json
{
  "items": [
    {
      "id": "00000000-0000-4000-8000-000000000101",
      "questionSetId": "00000000-0000-4000-8000-000000000100",
      "fileId": "00000000-0000-4000-8000-000000000021",
      "inputFormat": "XLSX",
      "status": "READY",
      "candidateCount": 2,
      "validCount": 2,
      "invalidCount": 0,
      "errorCode": null,
      "jobStatus": "SUCCEEDED",
      "createdAt": "2026-11-21T07:00:00Z",
      "sheets": [
        {
          "id": "00000000-0000-4000-8000-000000000102",
          "sheetName": "NUMERIK",
          "subtestId": "00000000-0000-4000-8000-000000000004",
          "sheetIndex": 0,
          "status": "READY",
          "candidateCount": 2,
          "invalidCount": 0
        }
      ]
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 20
}
```

<a id="paymenteventresponse"></a>
## PaymentEventResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| provider | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| providerEventId | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| amountMinor | integer \| null (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| verified | boolean | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| signatureStatus | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| receivedAt | string (date-time) | Tidak ditandai | — | Waktu server menerima save. |

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "provider": "EXAMPLE_PROVIDER",
  "providerEventId": "example-event-001",
  "amountMinor": 1,
  "verified": true,
  "signatureStatus": "VERIFIED",
  "receivedAt": "2026-11-20T07:00:00Z"
}
```

<a id="plan"></a>
## Plan

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| familyCode | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| revision | integer (int32) | Tidak ditandai | — | Revision jawaban terakhir; 0 jika belum ada row. |
| name | string | Tidak ditandai | — | Nama section/subtes. |
| months | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| amountMinor | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| currency | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| benefits | Benefits | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "id": "00000000-0000-4000-8000-000000000060",
  "code": "PREMIUM_MONTH_R1",
  "familyCode": "PREMIUM_MONTH",
  "revision": 1,
  "name": "Premium 1 bulan",
  "months": 1,
  "amountMinor": 39000,
  "currency": "IDR",
  "benefits": {
    "fullLeaderboard": true
  }
}
```

<a id="planbenefits"></a>
## PlanBenefits

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| fullLeaderboard | boolean | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "fullLeaderboard": true
}
```

<a id="planrequest"></a>
## PlanRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| code | string | Ya | minLength=1; pattern=[A-Z][A-Z0-9_]{1,59} | Kode resource atau kode opsi sesuai konteks. |
| name | string | Ya | minLength=0; maxLength=120 | Nama section/subtes. |
| months | integer (int32) | Tidak ditandai | minimum=1; maximum=36 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| amountMinor | integer (int64) | Tidak ditandai | minimum=0; maximum=1000000000000 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| currency | string | Ya | minLength=1; pattern=[A-Z]{3} | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| benefits | PlanBenefits | Ya | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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

<a id="planresponse"></a>
## PlanResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| familyCode | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| revision | integer (int32) | Tidak ditandai | — | Revision jawaban terakhir; 0 jika belum ada row. |
| name | string | Tidak ditandai | — | Nama section/subtes. |
| months | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| amountMinor | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| currency | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| benefits | PlanBenefits | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| active | boolean | Tidak ditandai | — | Hak/grant masih berlaku saat server membaca. |

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

<a id="policyrequest"></a>
## PolicyRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| code | string | Ya | minLength=1; pattern=[A-Z][A-Z0-9_]{1,79} | Kode resource atau kode opsi sesuai konteks. |
| method | string | Ya | minLength=1; pattern=BINARY\|OPTION_WEIGHT | Metode scoring BINARY atau OPTION_WEIGHT setelah hasil tersedia. |
| scale | number | Ya | minimum=0.0001; maximum=1000000 | Skala normalisasi indeks latihan. |
| rules | ScoreRules | Ya | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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

<a id="policyresponse"></a>
## PolicyResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| version | integer (int32) | Tidak ditandai | — | Nomor versi positif. |
| method | string | Tidak ditandai | — | Metode scoring BINARY atau OPTION_WEIGHT setelah hasil tersedia. |
| scale | number | Tidak ditandai | — | Skala normalisasi indeks latihan. |
| rules | ScoreRules | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |

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

<a id="poolrequirement"></a>
## PoolRequirement

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| blueprintId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| blueprintTitle | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| versionId | string (uuid) | Tidak ditandai | — | UUID versi soal terbaru yang dikembalikan. |
| version | integer (int32) | Tidak ditandai | — | Nomor versi positif. |
| position | integer (int32) | Tidak ditandai | — | Nomor urutan mulai dari 1. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| subtestName | string | Tidak ditandai | — | Nama subtes. |
| required | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| publishedAvailable | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
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
```

<a id="practicetopicdetail"></a>
## PracticeTopicDetail

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| topic | string | Tidak ditandai | — | Nama topik. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| subtestCode | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| subtest | string | Tidak ditandai | — | Nama subtes. |
| examTypeId | string (uuid) | Tidak ditandai | — | UUID jenis ujian. |
| examType | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| questionCount | integer (int64) | Tidak ditandai | — | Stok soal PUBLISHED yang tidak diarsipkan pada topik aktif. |
| defaultQuestionCount | integer (int32) | Tidak ditandai | — | Jumlah latihan default min(stok,10). |
| maxQuestionCount | integer (int32) | Tidak ditandai | — | Batas jumlah latihan min(stok,50); 0 bila belum ada stok. |
| secondsPerQuestion | integer (int32) | Tidak ditandai | — | Durasi teknis per soal dalam detik; saat ini 120. |
| ready | boolean | Tidak ditandai | — | Seluruh section mempunyai stok global minimal kebutuhan. |

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

<a id="privacyrequest"></a>
## PrivacyRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| leaderboardOptIn | boolean | Ya | — | Persetujuan tampil pada leaderboard; diubah lewat PATCH privacy, bukan PATCH profile. |

```json
{
  "leaderboardOptIn": true
}
```

<a id="privacyresponse"></a>
## PrivacyResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| leaderboardOptIn | boolean | Tidak ditandai | — | Persetujuan tampil pada leaderboard; diubah lewat PATCH privacy, bukan PATCH profile. |
| updatedAt | string \| null (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "leaderboardOptIn": true,
  "updatedAt": "2026-11-21T07:00:00Z"
}
```

<a id="processing"></a>
## Processing

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| state | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| poll | boolean | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| retryAfterSeconds | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| errorCode | string \| null | Tidak ditandai | — | Kode error batch atau null. |

```json
{
  "state": "READY",
  "poll": false,
  "retryAfterSeconds": 0,
  "errorCode": null
}
```

<a id="publishrequest"></a>
## PublishRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| expectedActiveVersionId | string \| null (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "expectedActiveVersionId": null,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
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

<a id="questionscope"></a>
## QuestionScope

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| subtestName | string | Tidak ditandai | — | Nama subtes. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik; harus sesuai subtes. |
| topicName | string \| null | Tidak ditandai | — | Nama topik; null jika tidak ada. |

```json
{
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "subtestName": "Penalaran Numerik Contoh",
  "topicId": "00000000-0000-4000-8000-000000000005",
  "topicName": "Aritmetika Dasar Contoh"
}
```

<a id="questionsource"></a>
## QuestionSource

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| examTypeId | string (uuid) | Tidak ditandai | — | UUID jenis ujian. |
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| sourceYear | integer (int32) | Tidak ditandai | — | Tahun sumber 1900–2200 atau null. |
| title | string | Tidak ditandai | — | Judul paket/blueprint. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| questionCount | integer (int64) | Tidak ditandai | — | Jumlah soal diminta pada practice atau kebutuhan section. |
| publishedCount | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "id": "00000000-0000-4000-8000-000000000100",
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "code": "UTBK_2025_A",
  "sourceYear": 2025,
  "title": "Sumber UTBK 2025 paket A",
  "status": "ACTIVE",
  "questionCount": 100,
  "publishedCount": 80
}
```

<a id="questionsummary"></a>
## QuestionSummary

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| versionId | string (uuid) | Tidak ditandai | — | UUID versi soal terbaru yang dikembalikan. |
| version | integer (int32) | Tidak ditandai | — | Nomor versi positif. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| revision | integer (int64) | Tidak ditandai | — | Revision jawaban terakhir; 0 jika belum ada row. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| examType | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| subtest | string | Tidak ditandai | — | Nama subtes. |
| topic | string \| null | Tidak ditandai | — | Nama topik. |
| sourceYear | integer \| null (int32) | Tidak ditandai | — | Tahun sumber 1900–2200 atau null. |
| sourceTitle | string \| null | Tidak ditandai | — | Nama sumber/provenance, maksimal 180 karakter input. |
| stemPreview | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| batchId | string \| null (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
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
```

<a id="quotaresponse"></a>
## QuotaResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| dateWib | string (date) | Tidak ditandai | — | Tanggal kuota Asia/Jakarta. |
| used | integer (int32) | Tidak ditandai | — | Start tryout yang sudah memakai kuota tanggal ini. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |
| remaining | integer (int32) | Tidak ditandai | — | Sisa kuota, minimal 0. |

```json
{
  "dateWib": "2026-11-19",
  "used": 1,
  "limit": 2,
  "remaining": 1
}
```

<a id="rankingentry"></a>
## RankingEntry

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| rank | integer (int32) | Tidak ditandai | — | Peringkat tersimpan. |
| displayName | string | Tidak ditandai | — | Nama tampil pengguna. |
| totalScore | number | Tidak ditandai | — | Indeks latihan tersimpan; null bila belum ada hasil. |
| targetKind | string \| null | Tidak ditandai | — | Jenis target tersimpan hasil mapping kategori. |
| targetInstitution | string \| null | Tidak ditandai | — | Nama institusi target. |
| targetProgram | string \| null | Tidak ditandai | — | Program/jurusan target, boleh null. |
| currentUser | boolean | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "rank": 1,
  "displayName": "Siswa Contoh",
  "totalScore": 80,
  "targetKind": "CAMPUS",
  "targetInstitution": "Universitas Contoh",
  "targetProgram": "Informatika",
  "currentUser": true
}
```

<a id="rankings"></a>
## Rankings

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| cohort | Cohort | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| accessTier | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| leaderboardOptIn | boolean | Tidak ditandai | — | Persetujuan tampil pada leaderboard; diubah lewat PATCH privacy, bukan PATCH profile. |
| totalParticipants | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| accessibleTotal | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| offset | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| limit | integer (int32) | Tidak ditandai | — | Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint. |
| items | array<RankingEntry> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| ownRank | RankingEntry \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "cohort": {
    "id": "00000000-0000-4000-8000-000000000061",
    "title": "Tryout UTBK Contoh",
    "category": "UTBK",
    "examTypeId": "00000000-0000-4000-8000-000000000025",
    "formId": "00000000-0000-4000-8000-000000000062",
    "scoringPolicyId": "00000000-0000-4000-8000-000000000070",
    "periodStart": "2026-11-01T00:00:00Z",
    "periodEnd": "2026-12-01T00:00:00Z",
    "attemptPolicy": "BEST",
    "ended": false
  },
  "accessTier": "FULL",
  "leaderboardOptIn": true,
  "totalParticipants": 1,
  "accessibleTotal": 1,
  "offset": 0,
  "limit": 20,
  "items": [
    {
      "rank": 1,
      "displayName": "Siswa Contoh",
      "totalScore": 80,
      "targetKind": "CAMPUS",
      "targetInstitution": "Universitas Contoh",
      "targetProgram": "Informatika",
      "currentUser": true
    }
  ],
  "ownRank": {
    "rank": 1,
    "displayName": "Siswa Contoh",
    "totalScore": 80,
    "targetKind": "CAMPUS",
    "targetInstitution": "Universitas Contoh",
    "targetProgram": "Informatika",
    "currentUser": true
  }
}
```

<a id="refundrequest"></a>
## RefundRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| amountMinor | integer (int64) | Tidak ditandai | minimum=1 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "amountMinor": 10000,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="refundresponse"></a>
## RefundResponse

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| orderId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| amountMinor | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| currency | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| createdAt | string (date-time) | Tidak ditandai | — | Timestamp pembuatan record. |
| completedAt | string \| null (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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
| finalizedAt | string \| null (date-time) | Tidak ditandai | — | Timestamp finalize server; null sebelum sesi ditutup. |
| processing | Processing | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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
  "finalizedAt": "2026-11-19T07:05:00Z",
  "processing": {
    "state": "READY",
    "poll": false,
    "retryAfterSeconds": 0,
    "errorCode": null
  }
}
```

<a id="revenue"></a>
## Revenue

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| currency | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| paidGrossMinor | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| refundedMinor | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| netMinor | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| pendingOrders | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "currency": "IDR",
  "paidGrossMinor": 50000,
  "refundedMinor": 0,
  "netMinor": 50000,
  "pendingOrders": 1
}
```

<a id="roledefinition"></a>
## RoleDefinition

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| name | string | Tidak ditandai | — | Nama section/subtes. |
| permissions | array<string> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "code": "EXAMPLE",
  "name": "Nama contoh",
  "permissions": [
    "OWN_PROFILE"
  ]
}
```

<a id="rolesrequest"></a>
## RolesRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| roles | array<string> | Ya | minItems=0; maxItems=3 | Daftar role aplikasi, misalnya CUSTOMER. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "roles": [
    "QUESTION_STAFF"
  ],
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="savecandidatesrequest"></a>
## SaveCandidatesRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| candidateIds | array<string (uuid)> | Ya | minItems=0; maxItems=100 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "candidateIds": [
    "00000000-0000-4000-8000-000000000022"
  ],
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="saveworkbookcandidates"></a>
## SaveWorkbookCandidates

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| candidateIds | array<string (uuid)> | Ya | minItems=0; maxItems=100 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "candidateIds": [
    "00000000-0000-4000-8000-000000000103"
  ],
  "reason": "Review sumber dan unggahan soal contoh"
}
```

<a id="scoregroup"></a>
## ScoreGroup

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| examTypeId | string (uuid) | Tidak ditandai | — | UUID jenis ujian. |
| examType | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| policyVersion | string | Tidak ditandai | — | Identitas policy code:version hasil. |
| count | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| average | number | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| trend | array<ScorePoint> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "examType": "TPS",
  "policyVersion": "DEMO_BINARY:1",
  "count": 8,
  "average": 80,
  "trend": [
    {
      "attemptId": "00000000-0000-4000-8000-000000000006",
      "completedAt": "2026-11-21T07:00:00Z",
      "score": 80
    }
  ]
}
```

<a id="scorepoint"></a>
## ScorePoint

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| attemptId | string (uuid) | Tidak ditandai | — | UUID sesi milik akun aktif. |
| completedAt | string (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| score | number | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "attemptId": "00000000-0000-4000-8000-000000000006",
  "completedAt": "2026-11-21T07:00:00Z",
  "score": 80
}
```

<a id="scorerules"></a>
## ScoreRules

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| correctPoints | number \| null | Tidak ditandai | minimum=0.0001; maximum=1000 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| wrongPoints | number \| null | Tidak ditandai | minimum=-1000; maximum=1000 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| emptyPoints | number \| null | Tidak ditandai | minimum=-1000; maximum=1000 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "correctPoints": 1,
  "wrongPoints": 0,
  "emptyPoints": 0
}
```

<a id="sectionrequest"></a>
## SectionRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| subtestId | string (uuid) | Ya | — | UUID subtes dalam lingkup exam type. |
| questionCount | integer (int32) | Tidak ditandai | minimum=1; maximum=500 | Jumlah soal diminta pada practice atau kebutuhan section. |
| topicId | string \| null (uuid) | Tidak ditandai | — | ID topik; harus sesuai subtes. |
| sourceYear | integer \| null (int32) | Tidak ditandai | minimum=1900; maximum=2200 | Tahun sumber 1900–2200 atau null. |
| questionSetIds | array<string (uuid)> | Tidak ditandai | minItems=0; maxItems=20 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| difficultyQuotas | array<DifficultyAllocation> | Tidak ditandai | minItems=0; maxItems=3 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| repeatPolicy | string \| null | Tidak ditandai | pattern=UNSEEN_TODAY\|NEVER_SEEN\|RECYCLE_OLDEST | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "questionCount": 3,
  "topicId": "00000000-0000-4000-8000-000000000005",
  "sourceYear": null,
  "questionSetIds": [
    "00000000-0000-4000-8000-000000000100"
  ],
  "difficultyQuotas": [
    {
      "difficulty": "EASY",
      "count": 1
    },
    {
      "difficulty": "MEDIUM",
      "count": 2
    }
  ],
  "repeatPolicy": "UNSEEN_TODAY"
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
| finalizedAt | string \| null (date-time) | Tidak ditandai | — | Timestamp finalize server; null sebelum sesi ditutup. |
| context | CourseContext | Tidak ditandai | — | Konteks tampilan sesi dari blueprint terkait; bukan kunci jawaban. |

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

<a id="sessionsection"></a>
## SessionSection

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| position | integer (int32) | Tidak ditandai | — | Nomor urutan mulai dari 1. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| name | string | Tidak ditandai | — | Nama section/subtes. |
| questionCount | integer (int32) | Tidak ditandai | — | Jumlah soal diminta pada practice atau kebutuhan section. |

```json
{
  "position": 1,
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "name": "Penalaran Numerik Contoh",
  "questionCount": 3
}
```

<a id="sheetmapping"></a>
## SheetMapping

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| sheetName | string | Ya | minLength=0; maxLength=31 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| subtestId | string (uuid) | Ya | — | UUID subtes dalam lingkup exam type. |

```json
{
  "sheetName": "NUMERIK",
  "subtestId": "00000000-0000-4000-8000-000000000004"
}
```

<a id="sourceitem"></a>
## SourceItem

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| questionId | string (uuid) | Tidak ditandai | — | UUID keluarga master setelah kandidat disimpan, selain itu null. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| sourceItemCode | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| latestVersionId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| difficulty | string \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| archived | boolean | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "questionId": "00000000-0000-4000-8000-000000000007",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "sourceItemCode": "PU_Q001",
  "latestVersionId": "00000000-0000-4000-8000-000000000008",
  "status": "PUBLISHED",
  "difficulty": "MEDIUM",
  "archived": false
}
```

<a id="sourcerequest"></a>
## SourceRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| examTypeId | string (uuid) | Ya | — | UUID jenis ujian. |
| code | string | Ya | minLength=1; pattern=[A-Z0-9][A-Z0-9_-]{1,79} | Kode resource atau kode opsi sesuai konteks. |
| sourceYear | integer (int32) | Tidak ditandai | minimum=1900; maximum=2200 | Tahun sumber 1900–2200 atau null. |
| title | string | Ya | minLength=0; maxLength=180 | Judul paket/blueprint. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "code": "UTBK_2025_A",
  "sourceYear": 2025,
  "title": "Sumber UTBK 2025 paket A",
  "reason": "Review sumber dan unggahan soal contoh"
}
```

<a id="sourcestatus"></a>
## SourceStatus

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| status | string | Ya | minLength=1; pattern=ACTIVE\|ARCHIVED | Status resource; baca state machine sesuai attempt/batch/job. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "status": "ARCHIVED",
  "reason": "Review sumber dan unggahan soal contoh"
}
```

<a id="staffdashboard"></a>
## StaffDashboard

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| publishedQuestions | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| draftQuestions | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| archivedQuestions | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| imports | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| importsNeedingInput | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| failedImports | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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

<a id="statusrequest"></a>
## StatusRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| status | string | Ya | minLength=1; pattern=ACTIVE\|SUSPENDED | Status resource; baca state machine sesuai attempt/batch/job. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "status": "SUSPENDED",
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="studentdashboard"></a>
## StudentDashboard

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| category | string \| null | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| mode | string \| null | Tidak ditandai | — | TRYOUT atau PRACTICE. |
| totalTryouts | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| totalPractices | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| completed | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| elapsedSeconds | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| currentStreakDays | integer (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| scoreGroups | array<ScoreGroup> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| recentActivity | array<HistoryEntry> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| quota | QuotaResponse | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| membership | Membership | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "category": "UTBK",
  "mode": "TRYOUT",
  "totalTryouts": 10,
  "totalPractices": 0,
  "completed": 8,
  "elapsedSeconds": 3600,
  "currentStreakDays": 1,
  "scoreGroups": [
    {
      "examTypeId": "00000000-0000-4000-8000-000000000025",
      "examType": "TPS",
      "policyVersion": "DEMO_BINARY:1",
      "count": 8,
      "average": 80,
      "trend": [
        {
          "attemptId": "00000000-0000-4000-8000-000000000006",
          "completedAt": "2026-11-21T07:00:00Z",
          "score": 80
        }
      ]
    }
  ],
  "recentActivity": [
    {
      "attemptId": "00000000-0000-4000-8000-000000000006",
      "tryoutId": "00000000-0000-4000-8000-000000000002",
      "title": "Tryout UTBK Contoh",
      "category": "UTBK",
      "mode": "TRYOUT",
      "status": "COMPLETED",
      "terminalReason": "SUBMITTED",
      "topicId": null,
      "topicName": null,
      "startedAt": "2026-11-21T06:50:00Z",
      "finalizedAt": "2026-11-21T07:00:00Z",
      "totalScore": 80,
      "questionCount": 3,
      "durationSeconds": 5400,
      "processing": {
        "state": "READY",
        "poll": false,
        "retryAfterSeconds": 0,
        "errorCode": null
      }
    }
  ],
  "quota": {
    "dateWib": "2026-11-19",
    "used": 1,
    "limit": 2,
    "remaining": 1
  },
  "membership": {
    "tier": "PREMIUM",
    "startsAt": "2026-11-01T00:00:00Z",
    "endsAt": "2026-12-01T00:00:00Z",
    "plan": {
      "id": "00000000-0000-4000-8000-000000000060",
      "code": "PREMIUM_MONTH_R1",
      "name": "Premium 1 bulan",
      "months": 1,
      "amountMinor": 39000,
      "currency": "IDR"
    },
    "benefits": {
      "fullLeaderboard": true
    },
    "leaderboardPreviewLimit": 3,
    "dailyTryoutLimit": 2
  }
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
| leaderboardOptIn | boolean | Tidak ditandai | — | Persetujuan tampil pada leaderboard; diubah lewat PATCH privacy, bukan PATCH profile. |
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

<a id="subtestrequest"></a>
## SubtestRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| examTypeId | string (uuid) | Ya | — | UUID jenis ujian. |
| code | string | Ya | minLength=1; pattern=[A-Z][A-Z0-9_]{1,39} | Kode resource atau kode opsi sesuai konteks. |
| name | string | Ya | minLength=0; maxLength=120 | Nama section/subtes. |
| active | boolean | Ya | — | Hak/grant masih berlaku saat server membaca. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "code": "NUMERIK",
  "name": "Penalaran Numerik",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="supportprofile"></a>
## SupportProfile

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| schoolName | string \| null | Tidak ditandai | — | Nama sekolah. |
| schoolGrade | string \| null | Tidak ditandai | — | Kelas/tingkat, contoh XII. |
| targetKind | string \| null | Tidak ditandai | — | Jenis target tersimpan hasil mapping kategori. |
| institution | string \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| program | string \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| targetScore | integer \| null (int32) | Tidak ditandai | — | Skor target pilihan pengguna, 0–1000 atau null. |

```json
{
  "schoolName": "Contoh",
  "schoolGrade": "Contoh",
  "targetKind": "Contoh",
  "institution": "Contoh",
  "program": "Contoh",
  "targetScore": 1
}
```

<a id="taxonomyitem"></a>
## TaxonomyItem

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| parentId | string \| null (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| code | string \| null | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| name | string | Tidak ditandai | — | Nama section/subtes. |
| active | boolean | Tidak ditandai | — | Hak/grant masih berlaku saat server membaca. |

```json
{
  "id": "00000000-0000-4000-8000-000000000090",
  "parentId": "00000000-0000-4000-8000-000000000090",
  "code": "EXAMPLE",
  "name": "Nama contoh",
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

<a id="taxonomytree"></a>
## TaxonomyTree

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| categories | array<TaxonomyItem> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| examTypes | array<TaxonomyItem> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| subtests | array<TaxonomyItem> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| topics | array<TaxonomyItem> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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

<a id="topiccard"></a>
## TopicCard

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| topic | string | Tidak ditandai | — | Nama topik. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| subtestCode | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| subtest | string | Tidak ditandai | — | Nama subtes. |
| examTypeId | string (uuid) | Tidak ditandai | — | UUID jenis ujian. |
| examType | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| questionCount | integer (int64) | Tidak ditandai | — | Jumlah soal diminta pada practice atau kebutuhan section. |
| ready | boolean | Tidak ditandai | — | Seluruh section mempunyai stok global minimal kebutuhan. |

```json
{
  "id": "00000000-0000-4000-8000-000000000005",
  "topic": "Aritmetika",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "subtestCode": "NUMERIK",
  "subtest": "Penalaran Numerik",
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "examType": "TPS",
  "category": "UTBK",
  "questionCount": 12,
  "ready": true
}
```

<a id="topicrequest"></a>
## TopicRequest

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| subtestId | string (uuid) | Ya | — | UUID subtes dalam lingkup exam type. |
| name | string | Ya | minLength=0; maxLength=160 | Nama section/subtes. |
| active | boolean | Ya | — | Hak/grant masih berlaku saat server membaca. |
| reason | string | Ya | minLength=0; maxLength=500 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "name": "Aritmetika",
  "active": true,
  "reason": "Pemeriksaan operasional berdasarkan tiket dukungan contoh"
}
```

<a id="tryoutcard"></a>
## TryoutCard

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| code | string | Tidak ditandai | — | Kode resource atau kode opsi sesuai konteks. |
| title | string | Tidak ditandai | — | Judul paket/blueprint. |
| category | string | Tidak ditandai | — | Kode kategori; filter opsional pada read API. |
| examTypeId | string (uuid) | Tidak ditandai | — | UUID jenis ujian. |
| examType | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| blueprintVersionId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| durationSeconds | integer (int32) | Tidak ditandai | — | Durasi konfigurasi dalam detik. |
| questionCount | integer (int32) | Tidak ditandai | — | Jumlah soal diminta pada practice atau kebutuhan section. |
| ready | boolean | Tidak ditandai | — | Seluruh section mempunyai stok global minimal kebutuhan. |
| attemptCount | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| latestScore | number \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| latestCompletedAt | string \| null (date-time) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "id": "00000000-0000-4000-8000-000000000002",
  "code": "DEMO_TRYOUT",
  "title": "Tryout UTBK Contoh",
  "category": "UTBK",
  "examTypeId": "00000000-0000-4000-8000-000000000025",
  "examType": "TPS",
  "blueprintVersionId": "00000000-0000-4000-8000-000000000003",
  "durationSeconds": 5400,
  "questionCount": 3,
  "ready": true,
  "attemptCount": 2,
  "latestScore": 80,
  "latestCompletedAt": "2026-11-21T07:00:00Z"
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
| attempt_count | integer (int64) | Tidak ditandai | — | Jumlah sesi pada keluarga blueprint ini, termasuk versi sebelumnya. |
| latest_score | number \| null | Tidak ditandai | — | Skor terbaru pada keluarga blueprint ini lintas versi, atau null. |
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
| displayName | string | Tidak ditandai | minLength=2; maxLength=120 | Nama tampil pengguna. |
| schoolName | string \| null | Tidak ditandai | minLength=0; maxLength=180 | Nama sekolah. |
| schoolGrade | string \| null | Tidak ditandai | minLength=0; maxLength=32 | Kelas/tingkat, contoh XII. |
| targetCategory | string \| null | Tidak ditandai | minLength=0; maxLength=24; enum=UTBK/CPNS/KEDINASAN/SEKOLAH | Kategori form: UTBK/CPNS/KEDINASAN/SEKOLAH. |
| targetInstitution | string \| null | Tidak ditandai | minLength=0; maxLength=180 | Nama institusi target. |
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

<a id="userattempt"></a>
## UserAttempt

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| title | string | Tidak ditandai | — | Judul paket/blueprint. |
| mode | string | Tidak ditandai | — | TRYOUT atau PRACTICE. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| terminalReason | string \| null | Tidak ditandai | — | null, SUBMITTED, ABANDONED atau TIMED_OUT. |
| startedAt | string (date-time) | Tidak ditandai | — | Timestamp mulai server. |
| finalizedAt | string \| null (date-time) | Tidak ditandai | — | Timestamp finalize server; null sebelum sesi ditutup. |
| score | number \| null | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
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
```

<a id="userdetail"></a>
## UserDetail

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| account | AdminUserResponse | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| profile | SupportProfile | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| attemptCount | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| orderCount | integer (int64) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

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

<a id="versionsummary"></a>
## VersionSummary

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| version | integer (int32) | Tidak ditandai | — | Nomor versi positif. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| revision | integer (int64) | Tidak ditandai | — | Revision jawaban terakhir; 0 jika belum ada row. |
| createdBy | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| createdAt | string (date-time) | Tidak ditandai | — | Timestamp pembuatan record. |

```json
{
  "id": "00000000-0000-4000-8000-000000000008",
  "version": 1,
  "status": "DRAFT",
  "revision": 0,
  "createdBy": "00000000-0000-4000-8000-000000000001",
  "createdAt": "2026-11-20T07:00:00Z"
}
```

<a id="workbookimport"></a>
## WorkbookImport

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| questionSetId | string (uuid) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| fileId | string (uuid) | Tidak ditandai | — | UUID metadata stored_file sumber. |
| inputFormat | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| candidateCount | integer (int32) | Tidak ditandai | — | Total kandidat. |
| validCount | integer (int32) | Tidak ditandai | — | Kandidat tanpa validationErrors. |
| invalidCount | integer (int32) | Tidak ditandai | — | Kandidat yang masih mempunyai error. |
| errorCode | string \| null | Tidak ditandai | — | Kode error batch atau null. |
| jobStatus | string \| null | Tidak ditandai | — | Status pekerjaan terpisah dari status batch. |
| createdAt | string (date-time) | Tidak ditandai | — | Timestamp pembuatan record. |
| sheets | array<WorkbookSheet> | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "id": "00000000-0000-4000-8000-000000000101",
  "questionSetId": "00000000-0000-4000-8000-000000000100",
  "fileId": "00000000-0000-4000-8000-000000000021",
  "inputFormat": "XLSX",
  "status": "READY",
  "candidateCount": 2,
  "validCount": 2,
  "invalidCount": 0,
  "errorCode": null,
  "jobStatus": "SUCCEEDED",
  "createdAt": "2026-11-21T07:00:00Z",
  "sheets": [
    {
      "id": "00000000-0000-4000-8000-000000000102",
      "sheetName": "NUMERIK",
      "subtestId": "00000000-0000-4000-8000-000000000004",
      "sheetIndex": 0,
      "status": "READY",
      "candidateCount": 2,
      "invalidCount": 0
    }
  ]
}
```

<a id="workbookmetadata"></a>
## WorkbookMetadata

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| questionSetId | string (uuid) | Ya | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| sheets | array<SheetMapping> | Ya | minItems=0; maxItems=30 | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |

```json
{
  "questionSetId": "00000000-0000-4000-8000-000000000100",
  "sheets": [
    {
      "sheetName": "NUMERIK",
      "subtestId": "00000000-0000-4000-8000-000000000004"
    }
  ]
}
```

<a id="workbooksheet"></a>
## WorkbookSheet

| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |
|---|---|---|---|---|
| id | string (uuid) | Tidak ditandai | — | Identitas resource/record pada konteks DTO. |
| sheetName | string | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| subtestId | string (uuid) | Tidak ditandai | — | UUID subtes dalam lingkup exam type. |
| sheetIndex | integer \| null (int32) | Tidak ditandai | — | Data sesuai konteks resource; lihat contoh dan endpoint pemakai. |
| status | string | Tidak ditandai | — | Status resource; baca state machine sesuai attempt/batch/job. |
| candidateCount | integer (int32) | Tidak ditandai | — | Total kandidat. |
| invalidCount | integer (int32) | Tidak ditandai | — | Kandidat yang masih mempunyai error. |

```json
{
  "id": "00000000-0000-4000-8000-000000000102",
  "sheetName": "NUMERIK",
  "subtestId": "00000000-0000-4000-8000-000000000004",
  "sheetIndex": 0,
  "status": "READY",
  "candidateCount": 2,
  "invalidCount": 0
}
```

