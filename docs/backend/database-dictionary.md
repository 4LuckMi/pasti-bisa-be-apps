# Kamus tabel PostgreSQL

[Indeks](README.md) · [ERD dan model data](02-database.md) · [Metadata JSON](database-schema.json)

Snapshot schema public V6, 19 November 2026: **40 tabel**, **306 kolom**. Metadata diambil tanpa baris pengguna/kredensial. flyway_schema_history tidak dimasukkan.

Constraint dan index ditulis sebagai definisi PostgreSQL aktual. PK/unique kadang juga mempunyai index; keduanya ditampilkan untuk audit. Foreign key composite jangan disederhanakan menjadi FK tunggal saat mengubah schema.

## Inventaris

| Tabel | Status implementasi | Tanggung jawab |
|---|---|---|
| [answer](#answer) | Aktif | Jawaban terakhir/revision; bukan semua event autosave. |
| [app_user](#app-user) | Aktif | Akun, email canonical, BCrypt atau password null SSO, status dan tipe produk. |
| [attempt](#attempt) | Aktif | Sesi pengguna; entitlement_snapshot belum diisi aturan hak produk. |
| [attempt_item](#attempt-item) | Aktif | Snapshot referensi versi dan urutan opsi. |
| [audit_event](#audit-event) | Sebagian | Audit sejumlah mutasi konten; belum seluruh tindakan sistem. |
| [blueprint](#blueprint) | Aktif/read + fixture | Identitas katalog atau blueprint practice internal. |
| [blueprint_section](#blueprint-section) | Aktif | Kebutuhan/filter pool per section. |
| [blueprint_version](#blueprint-version) | Aktif | Durasi dan policy konfigurasi terversi. |
| [email_verification_token](#email-verification-token) | Schema saja | Hash/expiry token; endpoint/mail belum ada. |
| [entitlement_grant](#entitlement-grant) | Read + schema | Grant order; creation/refund workflow belum ada. |
| [exam_category](#exam-category) | Aktif/read + fixture | Kategori taksonomi; belum CRUD admin. |
| [exam_form](#exam-form) | Schema saja | Form tetap untuk cohort sebanding mendatang. |
| [exam_form_item](#exam-form-item) | Schema saja | Daftar soal/posisi form tetap. |
| [exam_type](#exam-type) | Aktif/read + fixture | Jenis ujian dalam kategori. |
| [import_batch](#import-batch) | Aktif | Lingkup, status dan statistik batch PDF. |
| [import_candidate](#import-candidate) | Aktif | Ekstraksi/koreksi/flag dan hubungan master. |
| [job](#job) | Aktif | Antrean persisten dengan dedup/retry/lease token. |
| [leaderboard_cohort](#leaderboard-cohort) | Read + schema | Definisi cohort; belum admin/provisioning flow. |
| [leaderboard_entry](#leaderboard-entry) | Read + schema | Peringkat yang bisa dibaca; ranking worker belum dibuat. |
| [oauth_account](#oauth-account) | Aktif | Relasi provider-subject ke akun. |
| [password_reset_token](#password-reset-token) | Schema saja | Hash/expiry token; endpoint/mail belum ada. |
| [payment_event](#payment-event) | Schema saja | Event provider dan verifikasi signature mendatang. |
| [plan](#plan) | Schema saja | Paket/harga/benefit untuk commerce mendatang. |
| [purchase_order](#purchase-order) | Schema saja | Snapshot order dan idempotency checkout mendatang. |
| [question](#question) | Aktif | Keluarga soal dan archive/provenance kandidat. |
| [question_asset](#question-asset) | Read + schema | Asset privat versi; belum upload/attach authoring API. |
| [question_option](#question-option) | Aktif | Opsi/kunci/bobot per versi. |
| [question_version](#question-version) | Aktif | Konten, sumber dan status versi soal. |
| [quota_day](#quota-day) | Aktif | Counter start tryout tanggal WIB. |
| [quota_usage](#quota-usage) | Aktif | Satu pemakaian kuota per attempt. |
| [score_run](#score-run) | Aktif | Hasil, policyVersion, correctionVersion dan input hash. |
| [score_section](#score-section) | Aktif | Rincian hasil per subtes. |
| [scoring_policy](#scoring-policy) | Aktif | Konfigurasi scoring terversi. |
| [stored_file](#stored-file) | Aktif | Metadata file; binary tetap disk privat. |
| [subscription](#subscription) | Read + schema | Interval langganan; belum purchase/grant workflow. |
| [subtest](#subtest) | Aktif/read + fixture | Subtes dalam jenis ujian. |
| [target](#target) | Aktif | Tujuan institusi/program; belum master institusi resmi. |
| [topic](#topic) | Aktif/read + fixture | Topik dalam subtes. |
| [user_profile](#user-profile) | Aktif | Profil siswa, sekolah/kelas, target dan consent/opt-in. |
| [user_role](#user-role) | Aktif | Role many-to-many berbasis kode; tidak menciptakan superuser otomatis. |

<a id="answer"></a>
## answer

**Aktif** — Jawaban terakhir/revision; bukan semua event autosave.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| attempt_item_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. Lihat primary key. |
| selected_option_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| revision | integer | Tidak | 0 | Revision jawaban terakhir. |
| flagged | boolean | Tidak | false | Field flagged pada answer; tipe/batas dijaga definisi di bawah. |
| received_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `answer_attempt_item_id_fkey`: `FOREIGN KEY (attempt_item_id) REFERENCES attempt_item(id)`
- `answer_pkey`: `PRIMARY KEY (attempt_item_id)`
- `answer_revision_check`: `CHECK ((revision >= 0))`
- `answer_selected_option_id_fkey`: `FOREIGN KEY (selected_option_id) REFERENCES question_option(id)`

### Index

- `answer_pkey`: `CREATE UNIQUE INDEX answer_pkey ON public.answer USING btree (attempt_item_id)`

### Trigger

- `answer_attempt_state_guard`: `CREATE TRIGGER answer_attempt_state_guard BEFORE INSERT OR DELETE OR UPDATE ON public.answer FOR EACH ROW EXECUTE FUNCTION guard_answer_attempt_state()`
- `answer_option_version_guard`: `CREATE TRIGGER answer_option_version_guard BEFORE INSERT OR UPDATE OF attempt_item_id, selected_option_id ON public.answer FOR EACH ROW EXECUTE FUNCTION validate_answer_option_version()`

<a id="app-user"></a>
## app_user

**Aktif** — Akun, email canonical, BCrypt atau password null SSO, status dan tipe produk.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| email | character varying(320) | Tidak | — | Email tampil/input akun. |
| email_canonical | character varying(320) | Tidak | — | Email normalized untuk lookup unik. |
| password_hash | character varying(255) | Ya | — | Hash BCrypt; rahasia, boleh NULL untuk SSO. |
| display_name | character varying(120) | Tidak | — | Field display_name pada app_user; tipe/batas dijaga definisi di bawah. |
| status | character varying(24) | Tidak | 'ACTIVE'::character varying | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| email_verified_at | timestamp with time zone | Ya | — | Timestamp lifecycle sesuai konteks tabel. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| updated_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| user_type | character varying(32) | Tidak | 'SISWA'::character varying | Tipe produk, default SISWA. |

### Constraint

- `app_user_email_canonical_key`: `UNIQUE (email_canonical)`
- `app_user_pkey`: `PRIMARY KEY (id)`
- `app_user_status_check`: `CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'PENDING_VERIFICATION'::character varying])::text[])))`

### Index

- `app_user_email_canonical_key`: `CREATE UNIQUE INDEX app_user_email_canonical_key ON public.app_user USING btree (email_canonical)`
- `app_user_pkey`: `CREATE UNIQUE INDEX app_user_pkey ON public.app_user USING btree (id)`

### Trigger

- `app_user_create_profile`: `CREATE TRIGGER app_user_create_profile AFTER INSERT ON public.app_user FOR EACH ROW EXECUTE FUNCTION create_user_profile()`

<a id="attempt"></a>
## attempt

**Aktif** — Sesi pengguna; entitlement_snapshot belum diisi aturan hak produk.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| user_id | uuid | Tidak | — | FK pemilik/pengguna. |
| blueprint_version_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| mode | character varying(24) | Tidak | — | Field mode pada attempt; tipe/batas dijaga definisi di bawah. |
| status | character varying(24) | Tidak | — | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| idempotency_key | character varying(128) | Tidak | — | Key client scoped per pengguna/order. |
| started_at | timestamp with time zone | Tidak | — | Timestamp lifecycle sesuai konteks tabel. |
| deadline_at | timestamp with time zone | Tidak | — | Timestamp lifecycle sesuai konteks tabel. |
| finalized_at | timestamp with time zone | Ya | — | Timestamp lifecycle sesuai konteks tabel. |
| entitlement_snapshot | jsonb | Tidak | '{}'::jsonb | Disiapkan untuk snapshot hak; start saat ini menggunakan default DB. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| form_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| request_hash | character(64) | Ya | — | SHA-256 mode/target/count untuk deteksi replay berbeda. |
| terminal_reason | character varying(24) | Ya | — | Reason SUBMITTED/ABANDONED/TIMED_OUT pada flow attempt. |

### Constraint

- `attempt_blueprint_version_id_fkey`: `FOREIGN KEY (blueprint_version_id) REFERENCES blueprint_version(id)`
- `attempt_check`: `CHECK ((deadline_at > started_at))`
- `attempt_form_blueprint_fk`: `FOREIGN KEY (form_id, blueprint_version_id) REFERENCES exam_form(id, blueprint_version_id) ON DELETE RESTRICT`
- `attempt_id_user_unique`: `UNIQUE (id, user_id)`
- `attempt_pkey`: `PRIMARY KEY (id)`
- `attempt_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES app_user(id)`
- `attempt_user_id_idempotency_key_key`: `UNIQUE (user_id, idempotency_key)`

### Index

- `attempt_deadline_idx`: `CREATE INDEX attempt_deadline_idx ON public.attempt USING btree (deadline_at) WHERE ((status)::text = 'IN_PROGRESS'::text)`
- `attempt_id_user_unique`: `CREATE UNIQUE INDEX attempt_id_user_unique ON public.attempt USING btree (id, user_id)`
- `attempt_one_active_per_user_idx`: `CREATE UNIQUE INDEX attempt_one_active_per_user_idx ON public.attempt USING btree (user_id) WHERE ((status)::text = ANY ((ARRAY['IN_PROGRESS'::character varying, 'FINALIZING'::character varying])::text[]))`
- `attempt_pkey`: `CREATE UNIQUE INDEX attempt_pkey ON public.attempt USING btree (id)`
- `attempt_user_id_idempotency_key_key`: `CREATE UNIQUE INDEX attempt_user_id_idempotency_key_key ON public.attempt USING btree (user_id, idempotency_key)`
- `attempt_user_started_idx`: `CREATE INDEX attempt_user_started_idx ON public.attempt USING btree (user_id, started_at DESC)`

### Trigger

- `attempt_blueprint_form_guard`: `CREATE TRIGGER attempt_blueprint_form_guard BEFORE INSERT OR UPDATE OF blueprint_version_id, form_id ON public.attempt FOR EACH ROW EXECUTE FUNCTION validate_attempt_blueprint_and_form()`

<a id="attempt-item"></a>
## attempt_item

**Aktif** — Snapshot referensi versi dan urutan opsi.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| attempt_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| question_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| question_version_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| section_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| position | integer | Tidak | — | Field position pada attempt_item; tipe/batas dijaga definisi di bawah. |
| option_order | jsonb | Tidak | — | JSON array kode opsi urutan snapshot. |

### Constraint

- `attempt_item_attempt_id_fkey`: `FOREIGN KEY (attempt_id) REFERENCES attempt(id)`
- `attempt_item_attempt_id_position_key`: `UNIQUE (attempt_id, "position")`
- `attempt_item_attempt_id_question_id_key`: `UNIQUE (attempt_id, question_id)`
- `attempt_item_pkey`: `PRIMARY KEY (id)`
- `attempt_item_question_id_fkey`: `FOREIGN KEY (question_id) REFERENCES question(id)`
- `attempt_item_question_version_id_fkey`: `FOREIGN KEY (question_version_id) REFERENCES question_version(id)`
- `attempt_item_section_id_fkey`: `FOREIGN KEY (section_id) REFERENCES blueprint_section(id)`
- `attempt_item_version_belongs_to_question_fk`: `FOREIGN KEY (question_id, question_version_id) REFERENCES question_version(question_id, id) ON DELETE RESTRICT`

### Index

- `attempt_item_attempt_id_position_key`: `CREATE UNIQUE INDEX attempt_item_attempt_id_position_key ON public.attempt_item USING btree (attempt_id, "position")`
- `attempt_item_attempt_id_question_id_key`: `CREATE UNIQUE INDEX attempt_item_attempt_id_question_id_key ON public.attempt_item USING btree (attempt_id, question_id)`
- `attempt_item_pkey`: `CREATE UNIQUE INDEX attempt_item_pkey ON public.attempt_item USING btree (id)`
- `attempt_item_question_idx`: `CREATE INDEX attempt_item_question_idx ON public.attempt_item USING btree (question_id, attempt_id)`
- `attempt_item_seen_idx`: `CREATE INDEX attempt_item_seen_idx ON public.attempt_item USING btree (question_id, attempt_id)`

### Trigger

- `attempt_item_immutable_guard`: `CREATE TRIGGER attempt_item_immutable_guard BEFORE DELETE OR UPDATE ON public.attempt_item FOR EACH ROW EXECUTE FUNCTION guard_frozen_attempt_item()`
- `attempt_item_snapshot_guard`: `CREATE TRIGGER attempt_item_snapshot_guard BEFORE INSERT OR UPDATE OF attempt_id, question_id, question_version_id, section_id ON public.attempt_item FOR EACH ROW EXECUTE FUNCTION validate_attempt_item_snapshot()`

<a id="audit-event"></a>
## audit_event

**Sebagian** — Audit sejumlah mutasi konten; belum seluruh tindakan sistem.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| actor_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| action | character varying(100) | Tidak | — | Field action pada audit_event; tipe/batas dijaga definisi di bawah. |
| entity_type | character varying(80) | Tidak | — | Field entity_type pada audit_event; tipe/batas dijaga definisi di bawah. |
| entity_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| reason | text | Ya | — | Field reason pada audit_event; tipe/batas dijaga definisi di bawah. |
| request_id | character varying(100) | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| occurred_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| before_redacted | jsonb | Ya | — | Data audit sebelum perubahan yang tersensor; belum seluruh flow mengisi. |
| after_redacted | jsonb | Ya | — | Data audit sesudah perubahan yang tersensor; belum seluruh flow mengisi. |

### Constraint

- `audit_event_actor_id_fkey`: `FOREIGN KEY (actor_id) REFERENCES app_user(id)`
- `audit_event_pkey`: `PRIMARY KEY (id)`

### Index

- `audit_actor_time_idx`: `CREATE INDEX audit_actor_time_idx ON public.audit_event USING btree (actor_id, occurred_at DESC)`
- `audit_entity_time_idx`: `CREATE INDEX audit_entity_time_idx ON public.audit_event USING btree (entity_type, entity_id, occurred_at DESC)`
- `audit_event_pkey`: `CREATE UNIQUE INDEX audit_event_pkey ON public.audit_event USING btree (id)`

<a id="blueprint"></a>
## blueprint

**Aktif/read + fixture** — Identitas katalog atau blueprint practice internal.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| exam_type_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| code | character varying(80) | Tidak | — | Field code pada blueprint; tipe/batas dijaga definisi di bawah. |
| title | character varying(180) | Tidak | — | Field title pada blueprint; tipe/batas dijaga definisi di bawah. |
| status | character varying(24) | Tidak | 'DRAFT'::character varying | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| active_version_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| mode | character varying(16) | Tidak | 'TRYOUT'::character varying | Field mode pada blueprint; tipe/batas dijaga definisi di bawah. |
| demo | boolean | Tidak | false | Field demo pada blueprint; tipe/batas dijaga definisi di bawah. |

### Constraint

- `blueprint_active_version_fk`: `FOREIGN KEY (active_version_id, id) REFERENCES blueprint_version(id, blueprint_id) ON DELETE RESTRICT`
- `blueprint_exam_type_id_code_key`: `UNIQUE (exam_type_id, code)`
- `blueprint_exam_type_id_fkey`: `FOREIGN KEY (exam_type_id) REFERENCES exam_type(id)`
- `blueprint_mode_check`: `CHECK (((mode)::text = ANY ((ARRAY['TRYOUT'::character varying, 'PRACTICE'::character varying])::text[])))`
- `blueprint_pkey`: `PRIMARY KEY (id)`

### Index

- `blueprint_exam_type_id_code_key`: `CREATE UNIQUE INDEX blueprint_exam_type_id_code_key ON public.blueprint USING btree (exam_type_id, code)`
- `blueprint_pkey`: `CREATE UNIQUE INDEX blueprint_pkey ON public.blueprint USING btree (id)`

<a id="blueprint-section"></a>
## blueprint_section

**Aktif** — Kebutuhan/filter pool per section.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| blueprint_version_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| subtest_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| position | integer | Tidak | — | Field position pada blueprint_section; tipe/batas dijaga definisi di bawah. |
| item_count | integer | Tidak | — | Field item_count pada blueprint_section; tipe/batas dijaga definisi di bawah. |
| filters | jsonb | Tidak | '{}'::jsonb | Field filters pada blueprint_section; tipe/batas dijaga definisi di bawah. |

### Constraint

- `blueprint_section_blueprint_version_id_fkey`: `FOREIGN KEY (blueprint_version_id) REFERENCES blueprint_version(id)`
- `blueprint_section_blueprint_version_id_position_key`: `UNIQUE (blueprint_version_id, "position")`
- `blueprint_section_item_count_check`: `CHECK ((item_count > 0))`
- `blueprint_section_pkey`: `PRIMARY KEY (id)`
- `blueprint_section_subtest_id_fkey`: `FOREIGN KEY (subtest_id) REFERENCES subtest(id)`

### Index

- `blueprint_section_blueprint_version_id_position_key`: `CREATE UNIQUE INDEX blueprint_section_blueprint_version_id_position_key ON public.blueprint_section USING btree (blueprint_version_id, "position")`
- `blueprint_section_pkey`: `CREATE UNIQUE INDEX blueprint_section_pkey ON public.blueprint_section USING btree (id)`

### Trigger

- `blueprint_section_immutable_guard`: `CREATE TRIGGER blueprint_section_immutable_guard BEFORE INSERT OR DELETE OR UPDATE ON public.blueprint_section FOR EACH ROW EXECUTE FUNCTION prevent_frozen_blueprint_section_changes()`
- `blueprint_section_subtest_guard`: `CREATE TRIGGER blueprint_section_subtest_guard BEFORE INSERT OR UPDATE OF blueprint_version_id, subtest_id ON public.blueprint_section FOR EACH ROW EXECUTE FUNCTION validate_blueprint_section_subtest()`

<a id="blueprint-version"></a>
## blueprint_version

**Aktif** — Durasi dan policy konfigurasi terversi.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| blueprint_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| version | integer | Tidak | — | Field version pada blueprint_version; tipe/batas dijaga definisi di bawah. |
| duration_seconds | integer | Tidak | — | Field duration_seconds pada blueprint_version; tipe/batas dijaga definisi di bawah. |
| scoring_policy | jsonb | Tidak | — | Field scoring_policy pada blueprint_version; tipe/batas dijaga definisi di bawah. |
| status | character varying(24) | Tidak | 'DRAFT'::character varying | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| effective_at | timestamp with time zone | Ya | — | Timestamp lifecycle sesuai konteks tabel. |
| scoring_policy_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |

### Constraint

- `blueprint_version_blueprint_id_fkey`: `FOREIGN KEY (blueprint_id) REFERENCES blueprint(id)`
- `blueprint_version_blueprint_id_version_key`: `UNIQUE (blueprint_id, version)`
- `blueprint_version_duration_seconds_check`: `CHECK ((duration_seconds > 0))`
- `blueprint_version_id_blueprint_unique`: `UNIQUE (id, blueprint_id)`
- `blueprint_version_pkey`: `PRIMARY KEY (id)`
- `blueprint_version_scoring_policy_fk`: `FOREIGN KEY (scoring_policy_id) REFERENCES scoring_policy(id) ON DELETE RESTRICT`
- `blueprint_version_version_check`: `CHECK ((version > 0))`

### Index

- `blueprint_version_blueprint_id_version_key`: `CREATE UNIQUE INDEX blueprint_version_blueprint_id_version_key ON public.blueprint_version USING btree (blueprint_id, version)`
- `blueprint_version_id_blueprint_unique`: `CREATE UNIQUE INDEX blueprint_version_id_blueprint_unique ON public.blueprint_version USING btree (id, blueprint_id)`
- `blueprint_version_pkey`: `CREATE UNIQUE INDEX blueprint_version_pkey ON public.blueprint_version USING btree (id)`

### Trigger

- `blueprint_version_immutable_guard`: `CREATE TRIGGER blueprint_version_immutable_guard BEFORE DELETE OR UPDATE ON public.blueprint_version FOR EACH ROW EXECUTE FUNCTION prevent_published_blueprint_version_changes()`

<a id="email-verification-token"></a>
## email_verification_token

**Schema saja** — Hash/expiry token; endpoint/mail belum ada.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | gen_random_uuid() | UUID identitas record. Lihat primary key. |
| user_id | uuid | Tidak | — | FK pemilik/pengguna. |
| token_hash | character(64) | Tidak | — | Hash token sensitif, bukan token mentah. |
| expires_at | timestamp with time zone | Tidak | — | Timestamp lifecycle sesuai konteks tabel. |
| consumed_at | timestamp with time zone | Ya | — | Timestamp lifecycle sesuai konteks tabel. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `email_verification_token_pkey`: `PRIMARY KEY (id)`
- `email_verification_token_token_hash_key`: `UNIQUE (token_hash)`
- `email_verification_token_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE`

### Index

- `email_verification_token_pkey`: `CREATE UNIQUE INDEX email_verification_token_pkey ON public.email_verification_token USING btree (id)`
- `email_verification_token_token_hash_key`: `CREATE UNIQUE INDEX email_verification_token_token_hash_key ON public.email_verification_token USING btree (token_hash)`
- `email_verification_user_created_idx`: `CREATE INDEX email_verification_user_created_idx ON public.email_verification_token USING btree (user_id, created_at DESC)`

<a id="entitlement-grant"></a>
## entitlement_grant

**Read + schema** — Grant order; creation/refund workflow belum ada.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| user_id | uuid | Tidak | — | FK pemilik/pengguna. |
| order_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| starts_at | timestamp with time zone | Tidak | — | Timestamp lifecycle sesuai konteks tabel. |
| ends_at | timestamp with time zone | Tidak | — | Timestamp lifecycle sesuai konteks tabel. |
| revoked_at | timestamp with time zone | Ya | — | Timestamp lifecycle sesuai konteks tabel. |
| revoke_reason | text | Ya | — | Field revoke_reason pada entitlement_grant; tipe/batas dijaga definisi di bawah. |
| subscription_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |

### Constraint

- `entitlement_grant_check`: `CHECK ((ends_at > starts_at))`
- `entitlement_grant_order_id_fkey`: `FOREIGN KEY (order_id) REFERENCES purchase_order(id)`
- `entitlement_grant_order_id_key`: `UNIQUE (order_id)`
- `entitlement_grant_pkey`: `PRIMARY KEY (id)`
- `entitlement_grant_subscription_id_fkey`: `FOREIGN KEY (subscription_id) REFERENCES subscription(id) ON DELETE RESTRICT`
- `entitlement_grant_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES app_user(id)`

### Index

- `entitlement_grant_order_id_key`: `CREATE UNIQUE INDEX entitlement_grant_order_id_key ON public.entitlement_grant USING btree (order_id)`
- `entitlement_grant_pkey`: `CREATE UNIQUE INDEX entitlement_grant_pkey ON public.entitlement_grant USING btree (id)`
- `entitlement_user_interval_idx`: `CREATE INDEX entitlement_user_interval_idx ON public.entitlement_grant USING btree (user_id, starts_at, ends_at)`

### Trigger

- `entitlement_subscription_guard`: `CREATE TRIGGER entitlement_subscription_guard BEFORE INSERT OR UPDATE OF user_id, subscription_id, starts_at, ends_at ON public.entitlement_grant FOR EACH ROW EXECUTE FUNCTION validate_entitlement_subscription()`

<a id="exam-category"></a>
## exam_category

**Aktif/read + fixture** — Kategori taksonomi; belum CRUD admin.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| code | character varying(40) | Tidak | — | Field code pada exam_category; tipe/batas dijaga definisi di bawah. |
| name | character varying(120) | Tidak | — | Field name pada exam_category; tipe/batas dijaga definisi di bawah. |
| active | boolean | Tidak | true | Field active pada exam_category; tipe/batas dijaga definisi di bawah. |

### Constraint

- `exam_category_code_key`: `UNIQUE (code)`
- `exam_category_pkey`: `PRIMARY KEY (id)`

### Index

- `exam_category_code_key`: `CREATE UNIQUE INDEX exam_category_code_key ON public.exam_category USING btree (code)`
- `exam_category_pkey`: `CREATE UNIQUE INDEX exam_category_pkey ON public.exam_category USING btree (id)`

<a id="exam-form"></a>
## exam_form

**Schema saja** — Form tetap untuk cohort sebanding mendatang.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | gen_random_uuid() | UUID identitas record. Lihat primary key. |
| blueprint_version_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| label | character varying(120) | Tidak | — | Field label pada exam_form; tipe/batas dijaga definisi di bawah. |
| status | character varying(24) | Tidak | 'DRAFT'::character varying | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `exam_form_blueprint_version_id_fkey`: `FOREIGN KEY (blueprint_version_id) REFERENCES blueprint_version(id) ON DELETE RESTRICT`
- `exam_form_blueprint_version_id_label_key`: `UNIQUE (blueprint_version_id, label)`
- `exam_form_id_blueprint_version_id_key`: `UNIQUE (id, blueprint_version_id)`
- `exam_form_pkey`: `PRIMARY KEY (id)`

### Index

- `exam_form_blueprint_version_id_label_key`: `CREATE UNIQUE INDEX exam_form_blueprint_version_id_label_key ON public.exam_form USING btree (blueprint_version_id, label)`
- `exam_form_id_blueprint_version_id_key`: `CREATE UNIQUE INDEX exam_form_id_blueprint_version_id_key ON public.exam_form USING btree (id, blueprint_version_id)`
- `exam_form_pkey`: `CREATE UNIQUE INDEX exam_form_pkey ON public.exam_form USING btree (id)`

### Trigger

- `exam_form_immutable_guard`: `CREATE TRIGGER exam_form_immutable_guard BEFORE DELETE OR UPDATE ON public.exam_form FOR EACH ROW EXECUTE FUNCTION prevent_frozen_exam_form_changes()`

<a id="exam-form-item"></a>
## exam_form_item

**Schema saja** — Daftar soal/posisi form tetap.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| form_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. Lihat primary key. |
| question_version_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. Lihat primary key. |
| section_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| position | integer | Tidak | — | Field position pada exam_form_item; tipe/batas dijaga definisi di bawah. |

### Constraint

- `exam_form_item_form_id_fkey`: `FOREIGN KEY (form_id) REFERENCES exam_form(id) ON DELETE RESTRICT`
- `exam_form_item_form_id_position_key`: `UNIQUE (form_id, "position")`
- `exam_form_item_pkey`: `PRIMARY KEY (form_id, question_version_id)`
- `exam_form_item_position_check`: `CHECK (("position" > 0))`
- `exam_form_item_question_version_id_fkey`: `FOREIGN KEY (question_version_id) REFERENCES question_version(id) ON DELETE RESTRICT`
- `exam_form_item_section_id_fkey`: `FOREIGN KEY (section_id) REFERENCES blueprint_section(id) ON DELETE RESTRICT`

### Index

- `exam_form_item_form_id_position_key`: `CREATE UNIQUE INDEX exam_form_item_form_id_position_key ON public.exam_form_item USING btree (form_id, "position")`
- `exam_form_item_pkey`: `CREATE UNIQUE INDEX exam_form_item_pkey ON public.exam_form_item USING btree (form_id, question_version_id)`

### Trigger

- `exam_form_item_guard`: `CREATE TRIGGER exam_form_item_guard BEFORE INSERT OR UPDATE OF form_id, question_version_id, section_id ON public.exam_form_item FOR EACH ROW EXECUTE FUNCTION validate_exam_form_item()`
- `exam_form_item_immutable_guard`: `CREATE TRIGGER exam_form_item_immutable_guard BEFORE INSERT OR DELETE OR UPDATE ON public.exam_form_item FOR EACH ROW EXECUTE FUNCTION prevent_frozen_exam_form_item_changes()`

<a id="exam-type"></a>
## exam_type

**Aktif/read + fixture** — Jenis ujian dalam kategori.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| category_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| code | character varying(40) | Tidak | — | Field code pada exam_type; tipe/batas dijaga definisi di bawah. |
| name | character varying(120) | Tidak | — | Field name pada exam_type; tipe/batas dijaga definisi di bawah. |
| active | boolean | Tidak | true | Field active pada exam_type; tipe/batas dijaga definisi di bawah. |

### Constraint

- `exam_type_category_id_code_key`: `UNIQUE (category_id, code)`
- `exam_type_category_id_fkey`: `FOREIGN KEY (category_id) REFERENCES exam_category(id)`
- `exam_type_pkey`: `PRIMARY KEY (id)`

### Index

- `exam_type_category_id_code_key`: `CREATE UNIQUE INDEX exam_type_category_id_code_key ON public.exam_type USING btree (category_id, code)`
- `exam_type_pkey`: `CREATE UNIQUE INDEX exam_type_pkey ON public.exam_type USING btree (id)`

<a id="import-batch"></a>
## import_batch

**Aktif** — Lingkup, status dan statistik batch PDF.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| file_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| uploader_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| status | character varying(24) | Tidak | 'QUEUED'::character varying | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| metadata | jsonb | Tidak | '{}'::jsonb | JSON metadata batch dasar; data scope baru tersedia pada kolom eksplisit. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| completed_at | timestamp with time zone | Ya | — | Timestamp lifecycle sesuai konteks tabel. |
| candidate_count | integer | Tidak | 0 | Field candidate_count pada import_batch; tipe/batas dijaga definisi di bawah. |
| valid_count | integer | Tidak | 0 | Field valid_count pada import_batch; tipe/batas dijaga definisi di bawah. |
| invalid_count | integer | Tidak | 0 | Field invalid_count pada import_batch; tipe/batas dijaga definisi di bawah. |
| updated_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| subtest_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| topic_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| source_title | character varying(180) | Ya | — | Field source_title pada import_batch; tipe/batas dijaga definisi di bawah. |
| source_year | integer | Ya | — | Field source_year pada import_batch; tipe/batas dijaga definisi di bawah. |
| page_count | integer | Ya | — | Field page_count pada import_batch; tipe/batas dijaga definisi di bawah. |
| error_code | character varying(64) | Ya | — | Field error_code pada import_batch; tipe/batas dijaga definisi di bawah. |

### Constraint

- `import_batch_candidate_count_check`: `CHECK ((candidate_count >= 0))`
- `import_batch_file_id_fkey`: `FOREIGN KEY (file_id) REFERENCES stored_file(id)`
- `import_batch_invalid_count_check`: `CHECK ((invalid_count >= 0))`
- `import_batch_page_count_check`: `CHECK ((page_count > 0))`
- `import_batch_pkey`: `PRIMARY KEY (id)`
- `import_batch_source_year_check`: `CHECK (((source_year >= 1900) AND (source_year <= 2200)))`
- `import_batch_subtest_id_fkey`: `FOREIGN KEY (subtest_id) REFERENCES subtest(id)`
- `import_batch_topic_scope_fk`: `FOREIGN KEY (topic_id, subtest_id) REFERENCES topic(id, subtest_id)`
- `import_batch_uploader_id_fkey`: `FOREIGN KEY (uploader_id) REFERENCES app_user(id)`
- `import_batch_valid_count_check`: `CHECK ((valid_count >= 0))`

### Index

- `import_batch_pkey`: `CREATE UNIQUE INDEX import_batch_pkey ON public.import_batch USING btree (id)`
- `import_batch_status_created_idx`: `CREATE INDEX import_batch_status_created_idx ON public.import_batch USING btree (status, created_at)`

<a id="import-candidate"></a>
## import_candidate

**Aktif** — Ekstraksi/koreksi/flag dan hubungan master.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | gen_random_uuid() | UUID identitas record. Lihat primary key. |
| batch_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| source_page | integer | Tidak | — | Field source_page pada import_candidate; tipe/batas dijaga definisi di bawah. |
| source_item_no | integer | Tidak | — | Field source_item_no pada import_candidate; tipe/batas dijaga definisi di bawah. |
| extracted_content | jsonb | Tidak | — | Konten asli hasil parser/OCR. |
| validation_errors | jsonb | Tidak | '[]'::jsonb | JSON array kode kekurangan kandidat. |
| resolved_question_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| draft_content | jsonb | Ya | — | Koreksi kandidat, terpisah dari ekstraksi asli. |

### Constraint

- `import_candidate_batch_id_fkey`: `FOREIGN KEY (batch_id) REFERENCES import_batch(id) ON DELETE CASCADE`
- `import_candidate_batch_id_source_page_source_item_no_key`: `UNIQUE (batch_id, source_page, source_item_no)`
- `import_candidate_pkey`: `PRIMARY KEY (id)`
- `import_candidate_resolved_question_id_fkey`: `FOREIGN KEY (resolved_question_id) REFERENCES question(id) ON DELETE SET NULL`
- `import_candidate_source_item_no_check`: `CHECK ((source_item_no > 0))`
- `import_candidate_source_page_check`: `CHECK ((source_page > 0))`

### Index

- `import_candidate_batch_id_source_page_source_item_no_key`: `CREATE UNIQUE INDEX import_candidate_batch_id_source_page_source_item_no_key ON public.import_candidate USING btree (batch_id, source_page, source_item_no)`
- `import_candidate_batch_idx`: `CREATE INDEX import_candidate_batch_idx ON public.import_candidate USING btree (batch_id, source_page, source_item_no)`
- `import_candidate_pkey`: `CREATE UNIQUE INDEX import_candidate_pkey ON public.import_candidate USING btree (id)`

<a id="job"></a>
## job

**Aktif** — Antrean persisten dengan dedup/retry/lease token.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| type | character varying(80) | Tidak | — | Field type pada job; tipe/batas dijaga definisi di bawah. |
| dedup_key | character varying(180) | Tidak | — | Field dedup_key pada job; tipe/batas dijaga definisi di bawah. |
| payload | jsonb | Tidak | '{}'::jsonb | JSON dispatcher job; hanya ID resource terkait. |
| status | character varying(24) | Tidak | 'QUEUED'::character varying | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| run_after | timestamp with time zone | Tidak | now() | Field run_after pada job; tipe/batas dijaga definisi di bawah. |
| lease_owner | character varying(120) | Ya | — | Field lease_owner pada job; tipe/batas dijaga definisi di bawah. |
| lease_until | timestamp with time zone | Ya | — | Batas waktu kepemilikan job. |
| retry_count | integer | Tidak | 0 | Jumlah claim/percobaan yang sudah dilakukan. |
| max_attempts | integer | Tidak | 5 | Batas default lima claim. |
| last_error | text | Ya | — | Kode/kelas error tersensor; stack trace pada log. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| lease_token | uuid | Ya | — | UUID identitas claim saat ini untuk menolak worker lama. |

### Constraint

- `job_dedup_key_key`: `UNIQUE (dedup_key)`
- `job_pkey`: `PRIMARY KEY (id)`

### Index

- `job_claim_idx`: `CREATE INDEX job_claim_idx ON public.job USING btree (status, run_after, lease_until)`
- `job_dedup_key_key`: `CREATE UNIQUE INDEX job_dedup_key_key ON public.job USING btree (dedup_key)`
- `job_pkey`: `CREATE UNIQUE INDEX job_pkey ON public.job USING btree (id)`

<a id="leaderboard-cohort"></a>
## leaderboard_cohort

**Read + schema** — Definisi cohort; belum admin/provisioning flow.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | gen_random_uuid() | UUID identitas record. Lihat primary key. |
| form_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| scoring_policy_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| period_start | timestamp with time zone | Tidak | — | Field period_start pada leaderboard_cohort; tipe/batas dijaga definisi di bawah. |
| period_end | timestamp with time zone | Tidak | — | Field period_end pada leaderboard_cohort; tipe/batas dijaga definisi di bawah. |
| attempt_policy | character varying(32) | Tidak | — | Field attempt_policy pada leaderboard_cohort; tipe/batas dijaga definisi di bawah. |
| status | character varying(24) | Tidak | 'DRAFT'::character varying | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `leaderboard_cohort_check`: `CHECK ((period_end > period_start))`
- `leaderboard_cohort_form_id_fkey`: `FOREIGN KEY (form_id) REFERENCES exam_form(id) ON DELETE RESTRICT`
- `leaderboard_cohort_form_id_scoring_policy_id_period_start_p_key`: `UNIQUE (form_id, scoring_policy_id, period_start, period_end, attempt_policy)`
- `leaderboard_cohort_pkey`: `PRIMARY KEY (id)`
- `leaderboard_cohort_scoring_policy_id_fkey`: `FOREIGN KEY (scoring_policy_id) REFERENCES scoring_policy(id) ON DELETE RESTRICT`

### Index

- `leaderboard_cohort_form_id_scoring_policy_id_period_start_p_key`: `CREATE UNIQUE INDEX leaderboard_cohort_form_id_scoring_policy_id_period_start_p_key ON public.leaderboard_cohort USING btree (form_id, scoring_policy_id, period_start, period_end, attempt_policy)`
- `leaderboard_cohort_pkey`: `CREATE UNIQUE INDEX leaderboard_cohort_pkey ON public.leaderboard_cohort USING btree (id)`

<a id="leaderboard-entry"></a>
## leaderboard_entry

**Read + schema** — Peringkat yang bisa dibaca; ranking worker belum dibuat.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| cohort_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. Lihat primary key. |
| user_id | uuid | Tidak | — | FK pemilik/pengguna. Lihat primary key. |
| score_run_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| rank | integer | Tidak | — | Field rank pada leaderboard_entry; tipe/batas dijaga definisi di bawah. |
| display_name_snapshot | character varying(120) | Tidak | — | Field display_name_snapshot pada leaderboard_entry; tipe/batas dijaga definisi di bawah. |
| tie_break_data | jsonb | Tidak | '{}'::jsonb | Field tie_break_data pada leaderboard_entry; tipe/batas dijaga definisi di bawah. |
| updated_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `leaderboard_entry_cohort_id_fkey`: `FOREIGN KEY (cohort_id) REFERENCES leaderboard_cohort(id) ON DELETE RESTRICT`
- `leaderboard_entry_pkey`: `PRIMARY KEY (cohort_id, user_id)`
- `leaderboard_entry_rank_check`: `CHECK ((rank > 0))`
- `leaderboard_entry_score_run_id_fkey`: `FOREIGN KEY (score_run_id) REFERENCES score_run(id) ON DELETE RESTRICT`
- `leaderboard_entry_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE RESTRICT`

### Index

- `leaderboard_entry_pkey`: `CREATE UNIQUE INDEX leaderboard_entry_pkey ON public.leaderboard_entry USING btree (cohort_id, user_id)`
- `leaderboard_entry_rank_idx`: `CREATE INDEX leaderboard_entry_rank_idx ON public.leaderboard_entry USING btree (cohort_id, rank)`

### Trigger

- `leaderboard_entry_integrity_guard`: `CREATE TRIGGER leaderboard_entry_integrity_guard BEFORE INSERT OR UPDATE OF cohort_id, user_id, score_run_id ON public.leaderboard_entry FOR EACH ROW EXECUTE FUNCTION validate_leaderboard_entry()`

<a id="oauth-account"></a>
## oauth_account

**Aktif** — Relasi provider-subject ke akun.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| user_id | uuid | Tidak | — | FK pemilik/pengguna. |
| provider | character varying(40) | Tidak | — | Field provider pada oauth_account; tipe/batas dijaga definisi di bawah. Lihat primary key. |
| provider_subject | character varying(255) | Tidak | — | Subject stabil provider OAuth; bagian primary key bersama provider. Lihat primary key. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `oauth_account_pkey`: `PRIMARY KEY (provider, provider_subject)`
- `oauth_account_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES app_user(id)`
- `oauth_account_user_id_provider_key`: `UNIQUE (user_id, provider)`

### Index

- `oauth_account_pkey`: `CREATE UNIQUE INDEX oauth_account_pkey ON public.oauth_account USING btree (provider, provider_subject)`
- `oauth_account_user_id_provider_key`: `CREATE UNIQUE INDEX oauth_account_user_id_provider_key ON public.oauth_account USING btree (user_id, provider)`

<a id="password-reset-token"></a>
## password_reset_token

**Schema saja** — Hash/expiry token; endpoint/mail belum ada.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | gen_random_uuid() | UUID identitas record. Lihat primary key. |
| user_id | uuid | Tidak | — | FK pemilik/pengguna. |
| token_hash | character(64) | Tidak | — | Hash token sensitif, bukan token mentah. |
| expires_at | timestamp with time zone | Tidak | — | Timestamp lifecycle sesuai konteks tabel. |
| consumed_at | timestamp with time zone | Ya | — | Timestamp lifecycle sesuai konteks tabel. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `password_reset_token_pkey`: `PRIMARY KEY (id)`
- `password_reset_token_token_hash_key`: `UNIQUE (token_hash)`
- `password_reset_token_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE`

### Index

- `password_reset_token_pkey`: `CREATE UNIQUE INDEX password_reset_token_pkey ON public.password_reset_token USING btree (id)`
- `password_reset_token_token_hash_key`: `CREATE UNIQUE INDEX password_reset_token_token_hash_key ON public.password_reset_token USING btree (token_hash)`
- `password_reset_user_created_idx`: `CREATE INDEX password_reset_user_created_idx ON public.password_reset_token USING btree (user_id, created_at DESC)`

<a id="payment-event"></a>
## payment_event

**Schema saja** — Event provider dan verifikasi signature mendatang.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| provider | character varying(48) | Tidak | — | Field provider pada payment_event; tipe/batas dijaga definisi di bawah. |
| provider_event_id | character varying(180) | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| order_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| amount_minor | bigint | Ya | — | Nilai uang minor unit, bukan float. |
| verified | boolean | Tidak | false | Field verified pada payment_event; tipe/batas dijaga definisi di bawah. |
| received_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| signature_status | character varying(24) | Tidak | 'UNVERIFIED'::character varying | Field signature_status pada payment_event; tipe/batas dijaga definisi di bawah. |
| payload_redacted | jsonb | Tidak | '{}'::jsonb | Field payload_redacted pada payment_event; tipe/batas dijaga definisi di bawah. |

### Constraint

- `payment_event_order_id_fkey`: `FOREIGN KEY (order_id) REFERENCES purchase_order(id)`
- `payment_event_pkey`: `PRIMARY KEY (id)`
- `payment_event_provider_provider_event_id_key`: `UNIQUE (provider, provider_event_id)`

### Index

- `payment_event_order_received_idx`: `CREATE INDEX payment_event_order_received_idx ON public.payment_event USING btree (order_id, received_at DESC)`
- `payment_event_pkey`: `CREATE UNIQUE INDEX payment_event_pkey ON public.payment_event USING btree (id)`
- `payment_event_provider_provider_event_id_key`: `CREATE UNIQUE INDEX payment_event_provider_provider_event_id_key ON public.payment_event USING btree (provider, provider_event_id)`

<a id="plan"></a>
## plan

**Schema saja** — Paket/harga/benefit untuk commerce mendatang.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| code | character varying(80) | Tidak | — | Field code pada plan; tipe/batas dijaga definisi di bawah. |
| name | character varying(120) | Tidak | — | Field name pada plan; tipe/batas dijaga definisi di bawah. |
| months | integer | Tidak | — | Field months pada plan; tipe/batas dijaga definisi di bawah. |
| amount_minor | bigint | Tidak | — | Nilai uang minor unit, bukan float. |
| currency | character(3) | Tidak | — | Field currency pada plan; tipe/batas dijaga definisi di bawah. |
| benefits | jsonb | Tidak | — | Field benefits pada plan; tipe/batas dijaga definisi di bawah. |
| active | boolean | Tidak | true | Field active pada plan; tipe/batas dijaga definisi di bawah. |

### Constraint

- `plan_amount_minor_check`: `CHECK ((amount_minor >= 0))`
- `plan_code_key`: `UNIQUE (code)`
- `plan_months_check`: `CHECK ((months > 0))`
- `plan_pkey`: `PRIMARY KEY (id)`

### Index

- `plan_code_key`: `CREATE UNIQUE INDEX plan_code_key ON public.plan USING btree (code)`
- `plan_pkey`: `CREATE UNIQUE INDEX plan_pkey ON public.plan USING btree (id)`

<a id="purchase-order"></a>
## purchase_order

**Schema saja** — Snapshot order dan idempotency checkout mendatang.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| user_id | uuid | Tidak | — | FK pemilik/pengguna. |
| plan_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| provider_order_id | character varying(160) | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| amount_minor | bigint | Tidak | — | Nilai uang minor unit, bukan float. |
| currency | character(3) | Tidak | — | Field currency pada purchase_order; tipe/batas dijaga definisi di bawah. |
| status | character varying(24) | Tidak | 'PENDING'::character varying | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| idempotency_key | character varying(128) | Tidak | — | Key client scoped per pengguna/order. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| plan_code_snapshot | character varying(80) | Tidak | — | Field plan_code_snapshot pada purchase_order; tipe/batas dijaga definisi di bawah. |
| months_snapshot | integer | Tidak | — | Field months_snapshot pada purchase_order; tipe/batas dijaga definisi di bawah. |
| benefits_snapshot | jsonb | Tidak | — | Field benefits_snapshot pada purchase_order; tipe/batas dijaga definisi di bawah. |
| provider | character varying(48) | Ya | — | Field provider pada purchase_order; tipe/batas dijaga definisi di bawah. |
| expires_at | timestamp with time zone | Ya | — | Timestamp lifecycle sesuai konteks tabel. |
| updated_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `purchase_order_amount_minor_check`: `CHECK ((amount_minor >= 0))`
- `purchase_order_months_snapshot_check`: `CHECK ((months_snapshot > 0))`
- `purchase_order_pkey`: `PRIMARY KEY (id)`
- `purchase_order_plan_id_fkey`: `FOREIGN KEY (plan_id) REFERENCES plan(id)`
- `purchase_order_provider_order_id_key`: `UNIQUE (provider_order_id)`
- `purchase_order_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES app_user(id)`
- `purchase_order_user_id_idempotency_key_key`: `UNIQUE (user_id, idempotency_key)`

### Index

- `purchase_order_pkey`: `CREATE UNIQUE INDEX purchase_order_pkey ON public.purchase_order USING btree (id)`
- `purchase_order_provider_order_id_key`: `CREATE UNIQUE INDEX purchase_order_provider_order_id_key ON public.purchase_order USING btree (provider_order_id)`
- `purchase_order_status_created_idx`: `CREATE INDEX purchase_order_status_created_idx ON public.purchase_order USING btree (status, created_at DESC)`
- `purchase_order_user_created_idx`: `CREATE INDEX purchase_order_user_created_idx ON public.purchase_order USING btree (user_id, created_at DESC)`
- `purchase_order_user_id_idempotency_key_key`: `CREATE UNIQUE INDEX purchase_order_user_id_idempotency_key_key ON public.purchase_order USING btree (user_id, idempotency_key)`

<a id="question"></a>
## question

**Aktif** — Keluarga soal dan archive/provenance kandidat.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| subtest_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| code | character varying(80) | Tidak | — | Field code pada question; tipe/batas dijaga definisi di bawah. |
| archived_at | timestamp with time zone | Ya | — | Timestamp lifecycle sesuai konteks tabel. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| topic_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| candidate_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |

### Constraint

- `question_candidate_id_fkey`: `FOREIGN KEY (candidate_id) REFERENCES import_candidate(id) ON DELETE SET NULL`
- `question_code_key`: `UNIQUE (code)`
- `question_pkey`: `PRIMARY KEY (id)`
- `question_subtest_id_fkey`: `FOREIGN KEY (subtest_id) REFERENCES subtest(id)`
- `question_topic_subtest_fk`: `FOREIGN KEY (topic_id, subtest_id) REFERENCES topic(id, subtest_id)`

### Index

- `question_candidate_unique_idx`: `CREATE UNIQUE INDEX question_candidate_unique_idx ON public.question USING btree (candidate_id) WHERE (candidate_id IS NOT NULL)`
- `question_code_key`: `CREATE UNIQUE INDEX question_code_key ON public.question USING btree (code)`
- `question_pkey`: `CREATE UNIQUE INDEX question_pkey ON public.question USING btree (id)`
- `question_subtest_active_idx`: `CREATE INDEX question_subtest_active_idx ON public.question USING btree (subtest_id) WHERE (archived_at IS NULL)`
- `question_topic_active_idx`: `CREATE INDEX question_topic_active_idx ON public.question USING btree (topic_id) WHERE (archived_at IS NULL)`

<a id="question-asset"></a>
## question_asset

**Read + schema** — Asset privat versi; belum upload/attach authoring API.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | gen_random_uuid() | UUID identitas record. Lihat primary key. |
| question_version_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| file_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| purpose | character varying(32) | Tidak | — | Jenis file/asset; IMPORT_SOURCE tidak boleh dibaca sebagai asset siswa. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `question_asset_file_id_fkey`: `FOREIGN KEY (file_id) REFERENCES stored_file(id) ON DELETE RESTRICT`
- `question_asset_pkey`: `PRIMARY KEY (id)`
- `question_asset_question_version_id_file_id_purpose_key`: `UNIQUE (question_version_id, file_id, purpose)`
- `question_asset_question_version_id_fkey`: `FOREIGN KEY (question_version_id) REFERENCES question_version(id) ON DELETE RESTRICT`

### Index

- `question_asset_pkey`: `CREATE UNIQUE INDEX question_asset_pkey ON public.question_asset USING btree (id)`
- `question_asset_question_version_id_file_id_purpose_key`: `CREATE UNIQUE INDEX question_asset_question_version_id_file_id_purpose_key ON public.question_asset USING btree (question_version_id, file_id, purpose)`

### Trigger

- `question_asset_source_guard`: `CREATE TRIGGER question_asset_source_guard BEFORE INSERT OR DELETE OR UPDATE ON public.question_asset FOR EACH ROW EXECUTE FUNCTION guard_question_asset_source()`

<a id="question-option"></a>
## question_option

**Aktif** — Opsi/kunci/bobot per versi.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| question_version_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| option_code | character varying(32) | Tidak | — | Field option_code pada question_option; tipe/batas dijaga definisi di bawah. |
| content | jsonb | Tidak | — | JSONB isi opsi, lazimnya {text:...}. |
| original_position | integer | Tidak | — | Field original_position pada question_option; tipe/batas dijaga definisi di bawah. |
| correct_key | boolean | Tidak | false | Kunci opsi; tidak dikirim pada sesi aktif. |
| score_weight | numeric(10,4) | Tidak | 0 | Bobot opsi untuk metode penilaian. |

### Constraint

- `question_option_pkey`: `PRIMARY KEY (id)`
- `question_option_question_version_id_fkey`: `FOREIGN KEY (question_version_id) REFERENCES question_version(id)`
- `question_option_question_version_id_option_code_key`: `UNIQUE (question_version_id, option_code)`
- `question_option_question_version_id_original_position_key`: `UNIQUE (question_version_id, original_position)`

### Index

- `question_option_pkey`: `CREATE UNIQUE INDEX question_option_pkey ON public.question_option USING btree (id)`
- `question_option_question_version_id_option_code_key`: `CREATE UNIQUE INDEX question_option_question_version_id_option_code_key ON public.question_option USING btree (question_version_id, option_code)`
- `question_option_question_version_id_original_position_key`: `CREATE UNIQUE INDEX question_option_question_version_id_original_position_key ON public.question_option USING btree (question_version_id, original_position)`
- `question_option_version_idx`: `CREATE INDEX question_option_version_idx ON public.question_option USING btree (question_version_id)`

### Trigger

- `question_option_immutable_guard`: `CREATE TRIGGER question_option_immutable_guard BEFORE INSERT OR DELETE OR UPDATE ON public.question_option FOR EACH ROW EXECUTE FUNCTION prevent_published_question_option_changes()`

<a id="question-version"></a>
## question_version

**Aktif** — Konten, sumber dan status versi soal.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| question_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| version | integer | Tidak | — | Field version pada question_version; tipe/batas dijaga definisi di bawah. |
| stem | jsonb | Tidak | — | JSONB konten versi, lazimnya {text:...}. |
| explanation | text | Ya | — | Field explanation pada question_version; tipe/batas dijaga definisi di bawah. |
| source_year | integer | Ya | — | Field source_year pada question_version; tipe/batas dijaga definisi di bawah. |
| difficulty | character varying(24) | Ya | — | Field difficulty pada question_version; tipe/batas dijaga definisi di bawah. |
| status | character varying(24) | Tidak | — | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| shuffle_allowed | boolean | Tidak | true | Field shuffle_allowed pada question_version; tipe/batas dijaga definisi di bawah. |
| created_by | uuid | Ya | — | Field created_by pada question_version; tipe/batas dijaga definisi di bawah. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| source_title | character varying(180) | Ya | — | Field source_title pada question_version; tipe/batas dijaga definisi di bawah. |
| source_page | integer | Ya | — | Field source_page pada question_version; tipe/batas dijaga definisi di bawah. |

### Constraint

- `question_version_created_by_fkey`: `FOREIGN KEY (created_by) REFERENCES app_user(id)`
- `question_version_pkey`: `PRIMARY KEY (id)`
- `question_version_question_id_fkey`: `FOREIGN KEY (question_id) REFERENCES question(id)`
- `question_version_question_id_id_unique`: `UNIQUE (question_id, id)`
- `question_version_question_id_version_key`: `UNIQUE (question_id, version)`
- `question_version_source_page_check`: `CHECK ((source_page > 0))`
- `question_version_version_check`: `CHECK ((version > 0))`

### Index

- `question_version_pkey`: `CREATE UNIQUE INDEX question_version_pkey ON public.question_version USING btree (id)`
- `question_version_question_id_id_unique`: `CREATE UNIQUE INDEX question_version_question_id_id_unique ON public.question_version USING btree (question_id, id)`
- `question_version_question_id_version_key`: `CREATE UNIQUE INDEX question_version_question_id_version_key ON public.question_version USING btree (question_id, version)`
- `question_version_status_idx`: `CREATE INDEX question_version_status_idx ON public.question_version USING btree (status)`

### Trigger

- `question_version_immutable_guard`: `CREATE TRIGGER question_version_immutable_guard BEFORE DELETE OR UPDATE ON public.question_version FOR EACH ROW EXECUTE FUNCTION prevent_published_question_version_changes()`

<a id="quota-day"></a>
## quota_day

**Aktif** — Counter start tryout tanggal WIB.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| user_id | uuid | Tidak | — | FK pemilik/pengguna. Lihat primary key. |
| day_wib | date | Tidak | — | Tanggal Asia/Jakarta. Lihat primary key. |
| used_count | smallint | Tidak | 0 | Counter harian, CHECK 0..2. |

### Constraint

- `quota_day_pkey`: `PRIMARY KEY (user_id, day_wib)`
- `quota_day_used_count_check`: `CHECK (((used_count >= 0) AND (used_count <= 2)))`
- `quota_day_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES app_user(id)`

### Index

- `quota_day_pkey`: `CREATE UNIQUE INDEX quota_day_pkey ON public.quota_day USING btree (user_id, day_wib)`

<a id="quota-usage"></a>
## quota_usage

**Aktif** — Satu pemakaian kuota per attempt.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| attempt_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. Lihat primary key. |
| user_id | uuid | Tidak | — | FK pemilik/pengguna. |
| day_wib | date | Tidak | — | Tanggal Asia/Jakarta. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `quota_usage_attempt_id_fkey`: `FOREIGN KEY (attempt_id) REFERENCES attempt(id)`
- `quota_usage_attempt_owner_fk`: `FOREIGN KEY (attempt_id, user_id) REFERENCES attempt(id, user_id) ON DELETE RESTRICT`
- `quota_usage_pkey`: `PRIMARY KEY (attempt_id)`
- `quota_usage_user_id_day_wib_fkey`: `FOREIGN KEY (user_id, day_wib) REFERENCES quota_day(user_id, day_wib)`

### Index

- `quota_usage_pkey`: `CREATE UNIQUE INDEX quota_usage_pkey ON public.quota_usage USING btree (attempt_id)`
- `quota_usage_user_day_idx`: `CREATE INDEX quota_usage_user_day_idx ON public.quota_usage USING btree (user_id, day_wib)`

<a id="score-run"></a>
## score_run

**Aktif** — Hasil, policyVersion, correctionVersion dan input hash.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| attempt_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| policy_version | character varying(80) | Tidak | — | Field policy_version pada score_run; tipe/batas dijaga definisi di bawah. |
| correction_version | integer | Tidak | 1 | Field correction_version pada score_run; tipe/batas dijaga definisi di bawah. |
| input_hash | character varying(128) | Tidak | — | Hash input scoring yang tersimpan. |
| total_score | numeric(12,4) | Tidak | — | Field total_score pada score_run; tipe/batas dijaga definisi di bawah. |
| details | jsonb | Tidak | — | JSON hasil scoring. |
| computed_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| scoring_policy_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |

### Constraint

- `score_run_attempt_id_fkey`: `FOREIGN KEY (attempt_id) REFERENCES attempt(id)`
- `score_run_attempt_id_policy_version_correction_version_inpu_key`: `UNIQUE (attempt_id, policy_version, correction_version, input_hash)`
- `score_run_pkey`: `PRIMARY KEY (id)`
- `score_run_scoring_policy_fk`: `FOREIGN KEY (scoring_policy_id) REFERENCES scoring_policy(id) ON DELETE RESTRICT`

### Index

- `score_run_attempt_id_policy_version_correction_version_inpu_key`: `CREATE UNIQUE INDEX score_run_attempt_id_policy_version_correction_version_inpu_key ON public.score_run USING btree (attempt_id, policy_version, correction_version, input_hash)`
- `score_run_attempt_time_idx`: `CREATE INDEX score_run_attempt_time_idx ON public.score_run USING btree (attempt_id, computed_at DESC)`
- `score_run_pkey`: `CREATE UNIQUE INDEX score_run_pkey ON public.score_run USING btree (id)`

<a id="score-section"></a>
## score_section

**Aktif** — Rincian hasil per subtes.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| score_run_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. Lihat primary key. |
| subtest_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. Lihat primary key. |
| correct_count | integer | Tidak | — | Field correct_count pada score_section; tipe/batas dijaga definisi di bawah. |
| wrong_count | integer | Tidak | — | Field wrong_count pada score_section; tipe/batas dijaga definisi di bawah. |
| empty_count | integer | Tidak | — | Field empty_count pada score_section; tipe/batas dijaga definisi di bawah. |
| points | numeric(12,4) | Tidak | — | Field points pada score_section; tipe/batas dijaga definisi di bawah. |
| max_points | numeric(12,4) | Tidak | — | Field max_points pada score_section; tipe/batas dijaga definisi di bawah. |
| accuracy | numeric(7,4) | Tidak | — | Field accuracy pada score_section; tipe/batas dijaga definisi di bawah. |

### Constraint

- `score_section_accuracy_check`: `CHECK (((accuracy >= (0)::numeric) AND (accuracy <= (100)::numeric)))`
- `score_section_correct_count_check`: `CHECK ((correct_count >= 0))`
- `score_section_empty_count_check`: `CHECK ((empty_count >= 0))`
- `score_section_max_points_check`: `CHECK ((max_points >= (0)::numeric))`
- `score_section_pkey`: `PRIMARY KEY (score_run_id, subtest_id)`
- `score_section_score_run_id_fkey`: `FOREIGN KEY (score_run_id) REFERENCES score_run(id) ON DELETE RESTRICT`
- `score_section_subtest_id_fkey`: `FOREIGN KEY (subtest_id) REFERENCES subtest(id) ON DELETE RESTRICT`
- `score_section_wrong_count_check`: `CHECK ((wrong_count >= 0))`

### Index

- `score_section_pkey`: `CREATE UNIQUE INDEX score_section_pkey ON public.score_section USING btree (score_run_id, subtest_id)`

<a id="scoring-policy"></a>
## scoring_policy

**Aktif** — Konfigurasi scoring terversi.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | gen_random_uuid() | UUID identitas record. Lihat primary key. |
| code | character varying(100) | Tidak | — | Field code pada scoring_policy; tipe/batas dijaga definisi di bawah. |
| version | integer | Tidak | — | Field version pada scoring_policy; tipe/batas dijaga definisi di bawah. |
| method | character varying(48) | Tidak | — | Field method pada scoring_policy; tipe/batas dijaga definisi di bawah. |
| rules | jsonb | Tidak | — | JSON konfigurasi policy terversi. |
| aggregation | character varying(48) | Tidak | — | Field aggregation pada scoring_policy; tipe/batas dijaga definisi di bawah. |
| scale | numeric(12,4) | Tidak | — | Field scale pada scoring_policy; tipe/batas dijaga definisi di bawah. |
| rounding | character varying(32) | Tidak | — | Field rounding pada scoring_policy; tipe/batas dijaga definisi di bawah. |
| code_hash | character varying(128) | Tidak | — | Field code_hash pada scoring_policy; tipe/batas dijaga definisi di bawah. |
| status | character varying(24) | Tidak | 'DRAFT'::character varying | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `scoring_policy_code_version_key`: `UNIQUE (code, version)`
- `scoring_policy_pkey`: `PRIMARY KEY (id)`
- `scoring_policy_version_check`: `CHECK ((version > 0))`

### Index

- `scoring_policy_code_version_key`: `CREATE UNIQUE INDEX scoring_policy_code_version_key ON public.scoring_policy USING btree (code, version)`
- `scoring_policy_pkey`: `CREATE UNIQUE INDEX scoring_policy_pkey ON public.scoring_policy USING btree (id)`

### Trigger

- `scoring_policy_immutable_guard`: `CREATE TRIGGER scoring_policy_immutable_guard BEFORE DELETE OR UPDATE ON public.scoring_policy FOR EACH ROW EXECUTE FUNCTION prevent_referenced_scoring_policy_changes()`

<a id="stored-file"></a>
## stored_file

**Aktif** — Metadata file; binary tetap disk privat.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| relative_path | character varying(500) | Tidak | — | Lokasi relatif dalam storage privat, bukan URL publik. |
| sha256 | character(64) | Tidak | — | Checksum isi file, 64 karakter hex. |
| size_bytes | bigint | Tidak | — | Field size_bytes pada stored_file; tipe/batas dijaga definisi di bawah. |
| mime_type | character varying(160) | Tidak | — | Field mime_type pada stored_file; tipe/batas dijaga definisi di bawah. |
| uploaded_by | uuid | Tidak | — | Field uploaded_by pada stored_file; tipe/batas dijaga definisi di bawah. |
| visibility | character varying(24) | Tidak | 'PRIVATE'::character varying | Field visibility pada stored_file; tipe/batas dijaga definisi di bawah. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| original_name | character varying(180) | Ya | — | Field original_name pada stored_file; tipe/batas dijaga definisi di bawah. |
| purpose | character varying(24) | Tidak | 'QUESTION_ASSET'::character varying | Jenis file/asset; IMPORT_SOURCE tidak boleh dibaca sebagai asset siswa. |

### Constraint

- `stored_file_pkey`: `PRIMARY KEY (id)`
- `stored_file_purpose_check`: `CHECK (((purpose)::text = ANY ((ARRAY['QUESTION_ASSET'::character varying, 'IMPORT_SOURCE'::character varying])::text[])))`
- `stored_file_relative_path_key`: `UNIQUE (relative_path)`
- `stored_file_size_bytes_check`: `CHECK ((size_bytes >= 0))`
- `stored_file_uploaded_by_fkey`: `FOREIGN KEY (uploaded_by) REFERENCES app_user(id)`

### Index

- `import_batch_checksum_scope_idx`: `CREATE INDEX import_batch_checksum_scope_idx ON public.stored_file USING btree (uploaded_by, sha256) WHERE ((purpose)::text = 'IMPORT_SOURCE'::text)`
- `stored_file_pkey`: `CREATE UNIQUE INDEX stored_file_pkey ON public.stored_file USING btree (id)`
- `stored_file_relative_path_key`: `CREATE UNIQUE INDEX stored_file_relative_path_key ON public.stored_file USING btree (relative_path)`
- `stored_file_sha256_idx`: `CREATE INDEX stored_file_sha256_idx ON public.stored_file USING btree (sha256)`

<a id="subscription"></a>
## subscription

**Read + schema** — Interval langganan; belum purchase/grant workflow.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | gen_random_uuid() | UUID identitas record. Lihat primary key. |
| user_id | uuid | Tidak | — | FK pemilik/pengguna. |
| starts_at | timestamp with time zone | Tidak | — | Timestamp lifecycle sesuai konteks tabel. |
| ends_at | timestamp with time zone | Tidak | — | Timestamp lifecycle sesuai konteks tabel. |
| status | character varying(24) | Tidak | — | Status resource; service hanya memakai subset yang dijelaskan pada arsitektur. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `subscription_check`: `CHECK ((ends_at > starts_at))`
- `subscription_pkey`: `PRIMARY KEY (id)`
- `subscription_user_id_ends_at_key`: `UNIQUE (user_id, ends_at)`
- `subscription_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE RESTRICT`

### Index

- `subscription_pkey`: `CREATE UNIQUE INDEX subscription_pkey ON public.subscription USING btree (id)`
- `subscription_user_ends_idx`: `CREATE INDEX subscription_user_ends_idx ON public.subscription USING btree (user_id, ends_at DESC)`
- `subscription_user_id_ends_at_key`: `CREATE UNIQUE INDEX subscription_user_id_ends_at_key ON public.subscription USING btree (user_id, ends_at)`

<a id="subtest"></a>
## subtest

**Aktif/read + fixture** — Subtes dalam jenis ujian.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | — | UUID identitas record. Lihat primary key. |
| exam_type_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| code | character varying(40) | Tidak | — | Field code pada subtest; tipe/batas dijaga definisi di bawah. |
| name | character varying(120) | Tidak | — | Field name pada subtest; tipe/batas dijaga definisi di bawah. |
| active | boolean | Tidak | true | Field active pada subtest; tipe/batas dijaga definisi di bawah. |

### Constraint

- `subtest_exam_type_id_code_key`: `UNIQUE (exam_type_id, code)`
- `subtest_exam_type_id_fkey`: `FOREIGN KEY (exam_type_id) REFERENCES exam_type(id)`
- `subtest_pkey`: `PRIMARY KEY (id)`

### Index

- `subtest_exam_type_id_code_key`: `CREATE UNIQUE INDEX subtest_exam_type_id_code_key ON public.subtest USING btree (exam_type_id, code)`
- `subtest_pkey`: `CREATE UNIQUE INDEX subtest_pkey ON public.subtest USING btree (id)`

<a id="target"></a>
## target

**Aktif** — Tujuan institusi/program; belum master institusi resmi.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | gen_random_uuid() | UUID identitas record. Lihat primary key. |
| kind | character varying(32) | Tidak | — | Field kind pada target; tipe/batas dijaga definisi di bawah. |
| institution | character varying(180) | Tidak | — | Field institution pada target; tipe/batas dijaga definisi di bawah. |
| program | character varying(180) | Ya | — | Field program pada target; tipe/batas dijaga definisi di bawah. |
| source_status | character varying(32) | Tidak | 'USER_SELECTED'::character varying | Field source_status pada target; tipe/batas dijaga definisi di bawah. |
| created_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |

### Constraint

- `target_kind_check`: `CHECK (((kind)::text = ANY ((ARRAY['CAMPUS'::character varying, 'CIVIL_SERVICE'::character varying, 'ACADEMY'::character varying, 'SCHOOL'::character varying, 'OTHER'::character varying])::text[])))`
- `target_pkey`: `PRIMARY KEY (id)`

### Index

- `target_kind_institution_idx`: `CREATE INDEX target_kind_institution_idx ON public.target USING btree (kind, institution)`
- `target_pkey`: `CREATE UNIQUE INDEX target_pkey ON public.target USING btree (id)`

<a id="topic"></a>
## topic

**Aktif/read + fixture** — Topik dalam subtes.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| id | uuid | Tidak | gen_random_uuid() | UUID identitas record. Lihat primary key. |
| subtest_id | uuid | Tidak | — | Referensi identitas; lihat foreign key di bawah. |
| name | character varying(160) | Tidak | — | Field name pada topic; tipe/batas dijaga definisi di bawah. |
| active | boolean | Tidak | true | Field active pada topic; tipe/batas dijaga definisi di bawah. |

### Constraint

- `topic_id_subtest_id_key`: `UNIQUE (id, subtest_id)`
- `topic_pkey`: `PRIMARY KEY (id)`
- `topic_subtest_id_fkey`: `FOREIGN KEY (subtest_id) REFERENCES subtest(id)`
- `topic_subtest_id_name_key`: `UNIQUE (subtest_id, name)`

### Index

- `topic_id_subtest_id_key`: `CREATE UNIQUE INDEX topic_id_subtest_id_key ON public.topic USING btree (id, subtest_id)`
- `topic_pkey`: `CREATE UNIQUE INDEX topic_pkey ON public.topic USING btree (id)`
- `topic_subtest_id_name_key`: `CREATE UNIQUE INDEX topic_subtest_id_name_key ON public.topic USING btree (subtest_id, name)`

<a id="user-profile"></a>
## user_profile

**Aktif** — Profil siswa, sekolah/kelas, target dan consent/opt-in.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| user_id | uuid | Tidak | — | FK pemilik/pengguna. Lihat primary key. |
| display_name | character varying(120) | Tidak | — | Field display_name pada user_profile; tipe/batas dijaga definisi di bawah. |
| target_id | uuid | Ya | — | Referensi identitas; lihat foreign key di bawah. |
| leaderboard_opt_in | boolean | Tidak | false | Field leaderboard_opt_in pada user_profile; tipe/batas dijaga definisi di bawah. |
| consent | jsonb | Tidak | '{}'::jsonb | Field consent pada user_profile; tipe/batas dijaga definisi di bawah. |
| updated_at | timestamp with time zone | Tidak | now() | Timestamp lifecycle sesuai konteks tabel. |
| school_name | character varying(180) | Ya | — | Field school_name pada user_profile; tipe/batas dijaga definisi di bawah. |
| school_grade | character varying(32) | Ya | — | Field school_grade pada user_profile; tipe/batas dijaga definisi di bawah. |
| target_score | integer | Ya | — | Field target_score pada user_profile; tipe/batas dijaga definisi di bawah. |

### Constraint

- `user_profile_pkey`: `PRIMARY KEY (user_id)`
- `user_profile_target_id_fkey`: `FOREIGN KEY (target_id) REFERENCES target(id) ON DELETE SET NULL`
- `user_profile_target_score_check`: `CHECK (((target_score IS NULL) OR ((target_score >= 0) AND (target_score <= 1000))))`
- `user_profile_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE`

### Index

- `user_profile_pkey`: `CREATE UNIQUE INDEX user_profile_pkey ON public.user_profile USING btree (user_id)`

<a id="user-role"></a>
## user_role

**Aktif** — Role many-to-many berbasis kode; tidak menciptakan superuser otomatis.

| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |
|---|---|---|---|---|
| user_id | uuid | Tidak | — | FK pemilik/pengguna. Lihat primary key. |
| role_code | character varying(32) | Tidak | — | CUSTOMER / QUESTION_STAFF / PLATFORM_ADMIN. Lihat primary key. |

### Constraint

- `user_role_code_check`: `CHECK (((role_code)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'QUESTION_STAFF'::character varying, 'PLATFORM_ADMIN'::character varying])::text[])))`
- `user_role_pkey`: `PRIMARY KEY (user_id, role_code)`
- `user_role_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES app_user(id)`

### Index

- `user_role_pkey`: `CREATE UNIQUE INDEX user_role_pkey ON public.user_role USING btree (user_id, role_code)`
