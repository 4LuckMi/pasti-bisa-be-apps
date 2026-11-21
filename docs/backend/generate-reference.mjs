// Rebuild local documentation from reviewed runtime/schema snapshots. No network or DB writes.
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {extendInternalReference} from './internal-reference.mjs';
import {extendStudentReference} from './student-reference.mjs';
import {extendWorkbookReference} from './workbook-reference.mjs';
const dir=path.dirname(fileURLToPath(import.meta.url));
const read=name=>JSON.parse(fs.readFileSync(path.join(dir,name),'utf8').replace(/^\uFEFF/,''));
const write=(name,value)=>fs.writeFileSync(path.join(dir,name),value+'\n');
const clone=value=>structuredClone(value);
const ref=name=>({'$ref':'#/components/schemas/'+name});
const uuid=n=>'00000000-0000-4000-8000-'+String(n).padStart(12,'0');
const id={user:uuid(1),tryout:uuid(2),blueprintVersion:uuid(3),subtest:uuid(4),topic:uuid(5),attempt:uuid(6),
 question:uuid(7),version:uuid(8),optionA:uuid(11),optionB:uuid(12),batch:uuid(20),file:uuid(21),candidate:uuid(22),section:uuid(23),category:uuid(24),type:uuid(25)};
const date='2026-11-19T07:00:00Z';
const e={};
e.UserProfileResponse={id:id.user,email:'siswa@example.com',displayName:'Nama Siswa',userType:'SISWA',roles:['CUSTOMER']};
e.AuthResponse={user:e.UserProfileResponse};
e.RegisterRequest={email:'siswa@example.com',displayName:'Nama Siswa',password:'PasswordAman123!'};
e.LoginRequest={email:'siswa@example.com',password:'PasswordAman123!'};
e.UpdateStudentProfileRequest={displayName:'Nama Siswa',schoolName:'SMA Negeri 1 Bandung',schoolGrade:'XII',targetCategory:'UTBK',targetInstitution:'Universitas Contoh',targetProgram:'Informatika',targetScore:800};
e.StudentProfileResponse={id:id.user,email:'siswa@example.com',displayName:'Nama Siswa',userType:'SISWA',schoolName:'SMA Negeri 1 Bandung',schoolGrade:'XII',leaderboardOptIn:false,targetScore:800,targetKind:'CAMPUS',targetInstitution:'Universitas Contoh',targetProgram:'Informatika'};
e.CsrfResponse={headerName:'X-CSRF-TOKEN',token:'example-csrf-token'};
e.ServiceStatus={status:'UP',service:'pastisiap-backend',timestamp:date};
e.CategoryQuery={category:'UTBK'};
e.TryoutResponse={id:id.tryout,code:'DEMO_TRYOUT',title:'Tryout UTBK - Contoh',category:'UTBK',exam_type:'Tes Numerik Contoh',blueprint_version_id:id.blueprintVersion,duration_seconds:5400,question_count:3,attempt_count:0,latest_score:null,latest_completed_at:null};
e.SectionResponse={id:id.section,subtestId:id.subtest,name:'Penalaran Numerik Contoh',position:1,questionCount:3,available:12};
e.TryoutDetail={id:id.tryout,title:'Tryout UTBK - Contoh',category:'UTBK',durationSeconds:5400,scoringMethod:'BINARY',scale:100,demo:true,ready:true,sections:[e.SectionResponse]};
e.PracticeTopicResponse={id:id.topic,topic:'Aritmetika Dasar Contoh',subtest_code:'NUMERIK',subtest:'Penalaran Numerik Contoh',category:'UTBK',question_count:12};
e.QuotaResponse={dateWib:'2026-11-19',used:1,limit:2,remaining:1};
e.SubscriptionResponse={id:uuid(40),starts_at:'2026-11-01T00:00:00Z',ends_at:'2026-12-01T00:00:00Z',status:'ACTIVE',active:true};
e.LeaderboardResponse={rank:1,display_name:'Siswa Contoh',total_score:80,category:'UTBK',period_start:'2026-11-01T00:00:00Z',period_end:'2026-12-01T00:00:00Z'};
e.AttemptHistoryResponse={id:id.attempt,tryout_id:id.tryout,tryout_title:'Tryout UTBK - Contoh',mode:'TRYOUT',status:'COMPLETED',started_at:date,finalized_at:'2026-11-19T07:05:00Z',total_score:33.3333,details:'{"label":"Indeks latihan PastiSiap; bukan skor resmi","correct":1,"wrong":0,"empty":2}'};
e.StartTryoutRequest={tryoutId:id.tryout};
e.StartPracticeRequest={topicId:id.topic,questionCount:3};
e.Navigation={position:1,answered:false,flagged:false,revision:0};
e.SessionResponse={attemptId:id.attempt,tryoutId:id.tryout,mode:'TRYOUT',status:'IN_PROGRESS',terminalReason:null,serverNow:date,startedAt:date,deadlineAt:'2026-11-19T08:30:00Z',questions:[1,2,3].map(position=>({...e.Navigation,position}))};
e.OptionResponse={id:id.optionA,code:'A',content:{text:'4'},correct:null};
e.AttemptQuestionResponse={position:1,stem:{text:'Berapa hasil 2 + 3?'},options:[e.OptionResponse,{id:id.optionB,code:'B',content:{text:'5'},correct:null}],selectedOptionId:null,flagged:false,revision:0,correct:null,explanation:null};
e.AnswerRequest={selectedOptionId:id.optionB,flagged:false,expectedRevision:0};
e.AnswerResponse={position:1,selectedOptionId:id.optionB,flagged:false,revision:1,receivedAt:'2026-11-19T07:01:00Z'};
e.FinalizeRequest={abandon:false};
e.SectionResult={subtestId:id.subtest,correct:1,wrong:0,empty:2,points:1,maxPoints:3,accuracy:33.3333};
e.ResultResponse={attemptId:id.attempt,status:'COMPLETED',terminalReason:'SUBMITTED',method:'BINARY',policyVersion:'DEMO_BINARY:1',totalScore:33.3333,details:{label:'Indeks latihan PastiSiap; bukan skor resmi',correct:1,wrong:0,empty:2},sections:[e.SectionResult]};
e.TaxonomyResponse={categoryId:id.category,categoryCode:'UTBK',categoryName:'UTBK',examTypeId:id.type,examTypeCode:'DEMO',examTypeName:'Tes Numerik Contoh',subtestId:id.subtest,subtestName:'Penalaran Numerik Contoh',topicId:id.topic,topicName:'Aritmetika Dasar Contoh'};
e.OptionDraft={code:'A',text:'4',correct:false,weight:0};
e.QuestionContent={stemText:'Berapa hasil 2 + 3?',options:[e.OptionDraft,{code:'B',text:'5',correct:true,weight:1}],explanation:'Dua ditambah tiga sama dengan lima.',shuffleAllowed:false};
e.ImportMetadata={subtestId:id.subtest,topicId:id.topic,sourceTitle:'Contoh PDF PastiSiap',sourceYear:2026};
e.ImportResponse={id:id.batch,status:'READY',fileId:id.file,subtestId:id.subtest,topicId:id.topic,sourceTitle:'Contoh PDF PastiSiap',sourceYear:2026,pageCount:1,candidateCount:2,validCount:2,invalidCount:0,errorCode:null,jobStatus:'SUCCEEDED',jobErrorCode:null,createdAt:date};
e.CandidateResponse={id:id.candidate,sourcePage:1,sourceItemNo:1,extractedContent:e.QuestionContent,draftContent:null,validationErrors:[],questionId:null};
e.CreateQuestionRequest={subtestId:id.subtest,topicId:id.topic,sourceTitle:'Soal manual contoh',sourceYear:2026,content:e.QuestionContent};
e.StaffQuestionResponse={id:id.question,versionId:id.version,code:'Q-'+id.question,version:1,status:'DRAFT',subtestId:id.subtest,topicId:id.topic,sourceYear:2026,sourceTitle:'Contoh PDF PastiSiap',sourcePage:1,content:e.QuestionContent};
e.ApiError={code:'REVISION_CONFLICT',message:'Fetch the latest question before saving again',fieldErrors:{},requestId:uuid(99)};
e.SessionSection={position:1,subtestId:id.subtest,name:'Penalaran Numerik Contoh',questionCount:3};
e.CourseContext={title:'Tryout UTBK - Contoh',category:'UTBK',examType:'Tes Numerik Contoh',topicId:null,topicName:null,sections:[e.SessionSection]};
e.QuestionScope={subtestId:id.subtest,subtestName:'Penalaran Numerik Contoh',topicId:id.topic,topicName:'Aritmetika Dasar Contoh'};
e.PracticeTopicDetail={id:id.topic,topic:'Aritmetika Dasar Contoh',subtestId:id.subtest,subtestCode:'NUMERIK',subtest:'Penalaran Numerik Contoh',examTypeId:id.type,examType:'Tes Numerik Contoh',category:'UTBK',questionCount:12,defaultQuestionCount:10,maxQuestionCount:12,secondsPerQuestion:120,ready:true};
e.SessionResponse.finalizedAt=null;e.SessionResponse.context=e.CourseContext;
e.ResultResponse.finalizedAt='2026-11-19T07:05:00Z';
e.AttemptQuestionResponse.scope=e.QuestionScope;
Object.assign(e.AttemptHistoryResponse,{topic_id:null,topic_name:null,category:'UTBK',terminal_reason:'SUBMITTED',question_count:3,duration_seconds:5400});

// auth: public, session, customer, staff, or csrf (logout may invalidate an already-empty session).
const endpoints=[];
function add(method,url,title,group,auth,response,description,extra={}) {
 endpoints.push({method:method.toLowerCase(),path:url,title,group,auth,response,description,status:200,...extra});
}
add('GET','/api/v1/health','Health aplikasi','Publik dan identity','public','ServiceStatus','Memeriksa proses API dan waktu UTC. Ini bukan pemeriksaan lengkap semua dependency.');
add('GET','/api/v1/csrf','Ambil token CSRF','Publik dan identity','public','CsrfResponse','Membuat/menggunakan session dan memberikan headerName serta token untuk mutasi.',{notes:['Simpan cookie dari respons; kirim token dengan cookie session yang sama. Token contoh dalam dokumen tidak dapat dipakai.']});
add('POST','/api/v1/auth/register','Registrasi password','Publik dan identity','public','AuthResponse','Membuat SISWA dengan role CUSTOMER dan login otomatis.',{status:201,request:'RegisterRequest',errors:['409 EMAIL_ALREADY_REGISTERED'],notes:['Password 12–72 karakter, displayName 2–120, email valid maksimal 320. Tidak menerima pilihan role/userType/premium.','Respons membuat/merotasi cookie session. Tidak ada accessToken atau refreshToken.']});
add('POST','/api/v1/auth/login','Login password','Publik dan identity','public','AuthResponse','Memverifikasi email/password lalu membentuk session.',{request:'LoginRequest',errors:['401 INVALID_CREDENTIALS'],notes:['Login password untuk akun SSO tanpa password tidak tersedia. Email dinormalisasi oleh service.']});
add('POST','/api/v1/auth/logout','Logout','Publik dan identity','csrf',null,'Ditangani filter Spring Security; menghapus session saat ini.',{status:204,manual:true,notes:['Body respons kosong; jangan memanggil response.json(). Logout juga bisa mengembalikan 204 jika tidak ada autentikasi aktif, selama CSRF valid.']});
add('GET','/api/v1/users/current','Ringkasan akun aktif','Profil dan read API siswa','session','UserProfileResponse','Membaca akun dari principal session, tanpa user ID dari client.');
add('GET','/api/v1/users/current/profile','Profil dan target siswa','Profil dan read API siswa','session','StudentProfileResponse','Membaca sekolah, kelas, target dan skor sasaran.',{notes:['Field sekolah/target boleh null sebelum pengguna melengkapinya. Role tersedia pada ringkasan akun, bukan response profil ini.']});
add('PATCH','/api/v1/users/current/profile','Update profil dan target','Profil dan read API siswa','session','StudentProfileResponse','Memperbarui data profil sesuai form lengkap.',{request:'UpdateStudentProfileRequest',errors:['400 INVALID_REQUEST untuk kategori target tidak didukung'],notes:['Walaupun method PATCH, displayName, schoolName, schoolGrade, targetCategory dan targetInstitution tetap wajib.','targetCategory dipetakan ke targetKind: UTBK→CAMPUS, CPNS→CIVIL_SERVICE, KEDINASAN→ACADEMY, SEKOLAH→SCHOOL.','targetProgram dan targetScore boleh null; targetScore 0–1000. leaderboardOptIn, email, roles dan userType tidak dapat diubah lewat body ini.']});
add('GET','/api/v1/tryouts','Katalog tryout','Profil dan read API siswa','session','TryoutResponse','Membaca paket TRYOUT aktif, jumlah soal dan riwayat ringkas lintas versi dalam keluarga blueprint yang sama.',{array:true,category:true,notes:['Array kosong valid. attempt_count dan latest_score tetap mencakup sesi versi blueprint sebelumnya; blueprint_version_id menunjuk versi aktif.','Beberapa key sengaja snake_case, sesuai DTO legacy.']});
add('GET','/api/v1/tryouts/{id}','Detail dan kesiapan katalog','Profil dan read API siswa','customer','TryoutDetail','Menampilkan durasi, metode/skala dan kapasitas pool section.',{notes:['ready mengukur stok global, bukan ketersediaan personal setelah exposure harian. Start dapat tetap menolak POOL_NOT_READY.','id adalah blueprint/package ID, bukan blueprint_version_id.']});
add('GET','/api/v1/practice/topics','Topik practice','Profil dan read API siswa','session','PracticeTopicResponse','Daftar topik aktif dan hitungan soal terbit yang tidak diarsipkan.',{array:true,category:true,notes:['Topik dengan question_count=0 dapat muncul. Ketersediaan final diverifikasi pada start.']});
add('GET','/api/v1/practice/topics/{id}','Detail topik dan batas latihan','Profil dan read API siswa','session','PracticeTopicDetail','Membaca satu topik dengan scope, stok terbit dan pilihan jumlah/durasi latihan.',{notes:['questionCount adalah stok global. maxQuestionCount=min(stok,50); defaultQuestionCount=min(stok,10). Jika stok nol keduanya 0 dan ready=false.','secondsPerQuestion saat ini 120. UI memilih bilangan bulat 1..maxQuestionCount lalu mengirimnya ke StartPracticeRequest.','id adalah topic ID, bukan ID blueprint practice internal.']});
add('GET','/api/v1/users/current/tryout-quota','Kuota harian WIB','Profil dan read API siswa','session','QuotaResponse','Menghitung pemakaian dan sisa dua start tryout per tanggal Asia/Jakarta.',{notes:['Practice tidak memakai kuota ini. Gratis dan Premium memakai batas teknis yang sama pada implementasi saat ini.']});
add('GET','/api/v1/users/current/attempts','Riwayat sesi sendiri','Profil dan read API siswa','session','AttemptHistoryResponse','Riwayat milik akun, dengan filter mode/paket/topik dan pagination offset/limit.',{array:true,page:true,errors:['400 INVALID_HISTORY_FILTER'],notes:['Query opsional: mode=TRYOUT/PRACTICE, tryoutId atau topicId, offset>=0, limit=1..100 (default 100). Array response lama tetap dipertahankan.','Untuk riwayat latihan kirim mode=PRACTICE&topicId=<uuid>; jangan membandingkan tryout_id internal dengan ID topik. Paket tryout difilter memakai tryoutId dan mencakup seluruh versi.','tryoutId dan topicId tidak boleh dikirim bersamaan; mode harus cocok dengan filter. Filter selalu diterapkan sesudah pembatasan akun sehingga data pengguna lain tidak terbuka.','terminal_reason membedakan SUBMITTED, ABANDONED dan TIMED_OUT walaupun status telah COMPLETED. details tetap string JSON/null.','total_score dan finalized_at dapat null untuk sesi yang belum selesai.']});
add('GET','/api/v1/users/current/subscription','Daftar subscription sendiri','Profil dan read API siswa','session','SubscriptionResponse','Mengembalikan periode langganan dan active berdasarkan entitlement grant yang masih berlaku.',{array:true,notes:['Tanpa langganan mengembalikan []. Contoh item aktif bersifat ilustrasi; belum ada API pembelian/grant.','Bukan satu objek boolean premium; active dibaca server dari interval grant yang tidak dicabut.']});
add('GET','/api/v1/leaderboard','Baca leaderboard','Profil dan read API siswa','session','LeaderboardResponse','Maksimal 100 entry dari cohort aktif yang belum berakhir dan user masih opt-in.',{array:true,category:true,notes:['Array kosong adalah respons normal tanpa cohort/entry yang diprovisikan. Contoh item hanya ilustrasi kontrak.','Sesi acak tidak otomatis ditambahkan ke leaderboard.']});
add('POST','/api/v1/attempts','Mulai tryout','Lifecycle tryout dan practice','customer','SessionResponse','Membuat attempt TRYOUT dari blueprint aktif, atau mengembalikan replay/sesi aktif.',{request:'StartTryoutRequest',idempotency:true,errors:['400 IDEMPOTENCY_KEY_REQUIRED','409 IDEMPOTENCY_CONFLICT','409 DAILY_QUOTA_EXHAUSTED','409 POOL_NOT_READY','409 OVERLAPPING_SECTIONS','409 UNSUPPORTED_SCORING_POLICY','409 UNSUPPORTED_BLUEPRINT_FILTER'],notes:['Key 8–128 karakter dari huruf, angka, titik, underscore, colon atau hyphen. Key sama + body berbeda ditolak.','Jika sesi aktif ada, response bisa berupa sesi berbeda dari paket yang diminta. Selalu baca attemptId/mode/status response.','Pool kurang rollback seluruh start dan tidak memakai kuota; start berhasil memakai satu kuota.']});
add('POST','/api/v1/practice/attempts','Mulai practice','Lifecycle tryout dan practice','customer','SessionResponse','Memulai latihan dalam satu topik, memakai blueprint internal dan pool yang sama.',{request:'StartPracticeRequest',idempotency:true,example:{...e.SessionResponse,tryoutId:uuid(70),mode:'PRACTICE',deadlineAt:'2026-11-19T07:06:00Z',context:{...e.CourseContext,title:'Latihan Aritmetika Dasar Contoh',topicId:id.topic,topicName:'Aritmetika Dasar Contoh'}},errors:['400 IDEMPOTENCY_KEY_REQUIRED','409 IDEMPOTENCY_CONFLICT','409 POOL_NOT_READY'],notes:['questionCount wajib dikirim 1–50. Durasi saat ini questionCount × 120 detik; tidak mengambil quota tryout.','Satu sesi aktif dibagi dengan tryout. tryoutId response tetap ID blueprint internal demi kompatibilitas; link kembali ke detail latihan menggunakan context.topicId.']});
add('GET','/api/v1/users/current/attempts/active','Resume sesi aktif','Lifecycle tryout dan practice','customer','SessionResponse','Mengembalikan sesi IN_PROGRESS/FINALIZING, atau memfinalisasi sesi yang sudah kedaluwarsa.',{notes:['Jika tidak ada sesi, HTTP 200 dengan body kosong. Client perlu memeriksa body sebelum parsing JSON.']});
add('GET','/api/v1/users/current/attempts/{id}','Navigator dan status sesi','Lifecycle tryout dan practice','customer','SessionResponse','Membaca deadline server, status, navigator dan konteks sesi milik pengguna.',{notes:['context menyediakan title/category/examType, topicId/topicName untuk practice, serta nama/jumlah soal subtes versi blueprint sesi. Frontend tidak perlu menebak kategori atau memanggil detail katalog terbaru.','finalizedAt berasal dari attempt dan null sebelum finalize; hasil tidak perlu mencari timestamp di 100 history terbaru.','GET ini dapat mengubah sesi kedaluwarsa menjadi FINALIZING dan membuat job scoring.']});
add('GET','/api/v1/users/current/attempts/{attemptId}/questions/{position}','Baca satu soal dalam sesi','Lifecycle tryout dan practice','session','AttemptQuestionResponse','Membaca versi soal dan urutan opsi snapshot milik pengguna.',{notes:['position dimulai dari 1. selectedOptionId adalah UUID opsi, bukan huruf display.','Sebelum score tersedia, options[].correct, correct dan explanation bernilai null. Tidak mengirim score_weight.','Setelah scoring, correct null juga dapat berarti soal belum dijawab. Response siswa tidak berisi sumber PDF/kunci file.']});
add('GET','/api/v1/users/current/attempts/{attemptId}/questions/{position}/assets/{assetId}','Baca asset privat soal','Lifecycle tryout dan practice','session',null,'Memeriksa kepemilikan attempt dan relasi asset dengan versi soal sebelum mengirim binary.',{binary:true,mime:'application/pdf atau MIME file',notes:['200 binary dengan Content-Disposition inline dan Cache-Control no-store.','404 asset tidak ditemukan dapat memiliki body kosong, bukan ApiError. File IMPORT_SOURCE selalu dikecualikan.','Tidak ada endpoint upload/attach asset baru pada flow siswa.']});
add('PUT','/api/v1/users/current/attempts/{id}/answers/{position}','Simpan jawaban/revision','Lifecycle tryout dan practice','customer','AnswerResponse','Autosave jawaban terakhir dengan optimistic revision.',{request:'AnswerRequest',errors:['400 INVALID_OPTION','409 REVISION_CONFLICT','409 ATTEMPT_CLOSED'],notes:['expectedRevision wajib >=0. Awal 0, sukses menjadi 1; stale revision ditolak.','selectedOptionId null berarti mengosongkan pilihan. flagged tidak dikirim/null dianggap false.','Update parsial flagged saja tetap dapat mengosongkan pilihan; kirim selectedOptionId yang ingin dipertahankan.']});
add('POST','/api/v1/users/current/attempts/{id}/finalize','Submit atau abandon','Lifecycle tryout dan practice','customer','SessionResponse','Menutup attempt dan membuat job scoring idempoten.',{request:'FinalizeRequest',example:{...e.SessionResponse,status:'FINALIZING',terminalReason:'SUBMITTED',serverNow:'2026-11-19T07:05:00Z'},notes:['Body boleh kosong atau {}; abandon false/null berarti submit. abandon true berarti keluar/abandon.','Jika deadline lewat, terminalReason TIMED_OUT mengalahkan parameter abandon.','Status normal berikutnya FINALIZING, bukan nilai langsung. Request berulang tidak membuat hasil/job ganda.']});
add('GET','/api/v1/users/current/attempts/{id}/result','Status dan hasil scoring','Lifecycle tryout dan practice','customer','ResultResponse','Membaca status attempt dan score terbaru.',{notes:['Sebelum hasil: totalScore/method/policyVersion/details null dan sections kosong. Jangan menganggap null sebagai nilai nol.','status bisa IN_PROGRESS/FINALIZING/COMPLETED. Belum ada status API FAILED khusus scoring; telusuri job/log jika lama.']});
const reviewed=clone(e.AttemptQuestionResponse);reviewed.options[0].correct=false;reviewed.options[1].correct=true;reviewed.selectedOptionId=id.optionB;reviewed.correct=true;reviewed.revision=1;reviewed.explanation=e.QuestionContent.explanation;
add('GET','/api/v1/users/current/attempts/{id}/review','Review seluruh soal','Lifecycle tryout dan practice','customer','AttemptQuestionResponse','Mengembalikan semua nomor snapshot beserta jawaban/kunci/pembahasan setelah COMPLETED.',{array:true,example:[reviewed],errors:['409 RESULT_PROCESSING'],notes:['Array ini bukan master soal terbaru. Jumlah contoh dipersingkat menjadi satu item untuk keterbacaan.']});
add('GET','/api/v1/staff/taxonomy','Taksonomi aktif untuk petugas','Bank soal dan impor','staff','TaxonomyResponse','Mengambil kategori, exam type, subtes dan topik dalam bentuk baris datar.',{array:true,notes:['Satu subtes dengan beberapa topik muncul beberapa baris. topicId/topicName boleh null jika subtes belum mempunyai topik.','Pengelolaan taxonomy tersedia melalui endpoint admin, terpisah dari izin petugas.']});
add('POST','/api/v1/staff/imports','Upload sumber PDF','Bank soal dan impor','staff','ImportResponse','Menyimpan sumber privat dan enqueue IMPORT_PDF di luar thread ekstraksi HTTP.',{status:202,multipart:true,example:{...e.ImportResponse,status:'QUEUED',pageCount:null,candidateCount:0,validCount:0,invalidCount:0,jobStatus:'QUEUED'},errors:['400 PDF_REQUIRED','400 INVALID_PDF_SIGNATURE','400 INVALID_CONTENT_SCOPE','413 FILE_TOO_LARGE / HTTP_413'],notes:['Multipart file + metadata; part metadata wajib application/json. Body metadata mengikuti ImportMetadata.','Maksimal 25MB, request 26MB. PDF invalid/encrypted/page limit dapat baru gagal pada worker; 202 bukan jaminan konten valid.','Checksum sama dalam uploader/subtest/topic yang sama mengembalikan batch lama, termasuk bila sourceTitle/sourceYear input berbeda. Tidak otomatis membuat ulang batch yang FAILED.']});
add('GET','/api/v1/staff/imports','Daftar riwayat impor','Bank soal dan impor','staff','ImportResponse','Daftar batch seluruh petugas, terbaru dahulu.',{array:true,page:true,notes:['Bukan hanya unggahan akun sendiri. Daftar berupa array tanpa total count/pagination envelope.']});
add('GET','/api/v1/staff/imports/{id}','Status batch dan job','Bank soal dan impor','staff','ImportResponse','Memantau status impor, jumlah kandidat dan error/status job.',{notes:['READY berarti semua kandidat dinilai lengkap oleh validator; belum otomatis terbit.','Batch status dan jobStatus berbeda. Batch FAILED dapat bersamaan dengan job RETRY_WAIT.']});
add('GET','/api/v1/staff/imports/{id}/candidates','Daftar kandidat ekstraksi','Bank soal dan impor','staff','CandidateResponse','Membaca kandidat, koreksi, validationErrors dan questionId jika sudah disimpan.',{array:true,page:true,notes:['extractedContent bisa belum lengkap dan tidak memenuhi batas request QuestionContent; tampilkan error untuk perbaikan.','sourceItemNo adalah nomor asal yang dinormalisasi per halaman untuk dedup, tidak selalu nomor cetak asli PDF.']});
add('PATCH','/api/v1/staff/imports/{id}/candidates/{candidateId}','Koreksi satu kandidat','Bank soal dan impor','staff',null,'Menulis draft_content dan menghitung ulang validationErrors/batch counts.',{request:'QuestionContent',errors:['409 CANDIDATE_ALREADY_SAVED'],notes:['200 body kosong. Kirim seluruh konten, bukan JSON Patch.','Hasil ekstraksi asli tetap tersimpan. Kandidat yang sudah menjadi master tidak dapat dikoreksi lewat endpoint ini.','Bean validation body berlaku; kunci/pembahasan yang belum valid tetap ditandai oleh validator publikasi.']});
add('POST','/api/v1/staff/imports/{id}/candidates/{candidateId}/save','Kandidat menjadi draft master','Bank soal dan impor','staff','StaffQuestionResponse','Membuat keluarga question dan versi DRAFT dari koreksi terakhir atau ekstraksi asli.',{notes:['Request berulang pada kandidat yang sudah disimpan mengembalikan master yang sama.','Save bukan publish; draft belum lengkap masih dapat dibuat, kemudian publish menolak konten tidak lengkap.']});
add('GET','/api/v1/staff/imports/{id}/source','Download PDF sumber','Bank soal dan impor','staff',null,'Membaca file asli yang dapat berisi kunci untuk review petugas.',{binary:true,mime:'application/pdf',notes:['200 binary; Content-Disposition attachment; filename=source.pdf; Cache-Control no-store.','Semua QUESTION_STAFF dapat membaca batch yang valid, bukan hanya uploader. File tidak tersedia dapat menghasilkan error internal yang harus dilacak log.']});
add('POST','/api/v1/staff/questions','Buat soal manual','Bank soal dan impor','staff','StaffQuestionResponse','Membuat master DRAFT tanpa perlu upload PDF.',{status:201,request:'CreateQuestionRequest',example:{...e.StaffQuestionResponse,sourceTitle:'Soal manual contoh',sourcePage:null},errors:['400 INVALID_CONTENT_SCOPE'],notes:['subtestId/topicId harus satu lingkup aktif. Konten body harus memenuhi validasi request; aturan lengkap publikasi tetap diperiksa saat publish.']});
add('GET','/api/v1/staff/questions','Cari master soal','Bank soal dan impor','staff','StaffQuestionResponse','Daftar question yang belum diarsipkan beserta versi paling baru.',{array:true,page:true,notes:['Filter opsional subtestId dan search. Search mencocokkan kode atau stem pada versi yang tersimpan.','Versi terbaru yang dikembalikan bisa DRAFT walaupun versi PUBLISHED lama masih dipakai sesi baru.']});
add('GET','/api/v1/staff/questions/{id}','Detail master terbaru','Bank soal dan impor','staff','StaffQuestionResponse','Detail versi paling baru dengan konten/kunci/bobot dan provenance sumber.',{notes:['Bukan endpoint untuk memilih nomor versi historis. Detail by ID masih dapat dibaca setelah archive.']});
add('POST','/api/v1/staff/questions/{id}/versions','Buat versi draft baru','Bank soal dan impor','staff','StaffQuestionResponse','Menambahkan versi berikutnya tanpa menimpa versi PUBLISHED sebelumnya.',{request:'QuestionContent',example:{...e.StaffQuestionResponse,versionId:uuid(80),version:2,status:'DRAFT'},notes:['Endpoint ini tidak idempoten: pengiriman ulang membuat versi berikutnya lagi.','Edit draft memakai PUT /staff/questions/{id}/draft dengan version ID dan revision; publish menargetkan versi terbaru.']});
add('POST','/api/v1/staff/questions/{id}/publish','Publish versi terbaru','Bank soal dan impor','staff','StaffQuestionResponse','Memvalidasi sumber/stem/opsi/kunci/bobot/pembahasan lalu menerbitkan DRAFT terbaru.',{example:{...e.StaffQuestionResponse,status:'PUBLISHED'},errors:['409 QUESTION_INCOMPLETE'],notes:['Wajib tepat satu opsi correct, kode opsi unik A–E, 2–5 opsi, pembahasan dan source title.','Jika versi terbaru sudah PUBLISHED, respons versi yang sama. Draft baru tidak otomatis diterbitkan worker.']});
add('POST','/api/v1/staff/questions/{id}/archive','Arsip keluarga soal','Bank soal dan impor','staff',null,'Menetapkan archived_at agar soal tidak masuk pemilihan sesi baru.',{notes:['200 body kosong. Pengulangan tidak mengubah timestamp arsip yang sudah ada.','Snapshot sesi lama tetap dapat dibaca; tidak menghapus versi/hasil. Belum ada endpoint unarchive.']});
add('GET','/oauth2/authorization/google','Mulai login Google','OAuth dan infrastruktur','public',null,'Mengarahkan browser ke Google; hanya aktif bila profil oauth dikonfigurasi.',{status:302,manual:true,redirect:true,location:'https://accounts.google.com/o/oauth2/v2/auth?...',notes:['Tidak ada body JSON request. Selesaikan login dan consent dalam browser; bukan request credential password ke backend.','Cookie/state OAuth harus tetap pada browser/session yang sama.']});
add('GET','/login/oauth2/code/google','Callback Google','OAuth dan infrastruktur','public',null,'Endpoint filter OIDC menerima authorization code dan state dari Google.',{status:302,manual:true,redirect:true,location:'nilai OAUTH_SUCCESS_REDIRECT',notes:['Parameter code/state dibuat provider/Spring, bukan nilai yang dibuat frontend secara manual.','Sukses membuat session lalu redirect. Gagal OAuth mengikuti handler/redirect Security, tidak selalu ApiError JSON.']});
add('GET','/actuator/health','Health komponen','OAuth dan infrastruktur','public',null,'Health deployment dengan detail komponen disembunyikan.',{manual:true,example:{status:'UP'},notes:['Status tidak sehat dapat memberi HTTP 503. Endpoint /api/v1/health hanya status aplikasi sederhana.']});
add('GET','/actuator/info','Info Actuator','OAuth dan infrastruktur','session',null,'Endpoint info yang diekspos konfigurasi Actuator.',{manual:true,example:{},notes:['Dapat kosong karena contributor info tidak seluruhnya aktif. Tidak dijamin mengembalikan info.app sebagai JSON.','Di luar prefix /api/, request anonim dapat mengikuti redirect OAuth atau respons Security berbeda, tergantung profil.']});
add('GET','/actuator','Discovery Actuator','OAuth dan infrastruktur','session',null,'Discovery links Actuator untuk endpoint yang diekspos.',{manual:true,example:{_links:{self:{href:'http://localhost:8080/actuator',templated:false},health:{href:'http://localhost:8080/actuator/health',templated:false},'health-path':{href:'http://localhost:8080/actuator/health/{*path}',templated:true},info:{href:'http://localhost:8080/actuator/info',templated:false}}},notes:['Bukan API data siswa. Host/links mengikuti deployment. Penjagaan anonim sama seperti /actuator/info.']});
add('GET','/swagger-ui.html','Buka Swagger UI','OAuth dan infrastruktur','public',null,'Redirect ke halaman Swagger UI untuk mencoba endpoint.',{status:302,manual:true,redirect:true,location:'/swagger-ui/index.html',notes:['Resource HTML/JS/CSS internal Swagger tidak diinventarisasi satu per satu. UI memakai cookie/CSRF yang sama; bukan bypass security.']});
add('GET','/api-docs','OpenAPI runtime','OAuth dan infrastruktur','public',null,'Membaca spesifikasi controller yang dibangkitkan Springdoc.',{manual:true,example:{openapi:'3.1.0',info:{title:'OpenAPI definition',version:'v0'},paths:{},components:{}},notes:['Contoh struktur dipersingkat; file runtime lengkap ada pada openapi-runtime.json.','Endpoint filter logout/OAuth dan Actuator dilengkapi pada openapi.json dokumentasi ini.']});

const raw=read('openapi-runtime.json');
const spec=clone(raw);
spec.info={title:'PastiSiap Backend - documented implementation',version:'2026-11-21',description:'Kontrak berdasarkan source dan runtime V9; semua contoh sintetis. Perilaku/catatan tiap endpoint ada di 03-api-reference.md. Tidak menyatakan semua roadmap PRD telah diimplementasikan.'};
spec.servers=[{url:'http://localhost:8080',description:'Docker development'}];
spec.components??={};spec.components.schemas??={};
const schemas=spec.components.schemas;
schemas.CategoryQuery??={type:'object',properties:{category:{type:'string'}},description:'Model binding query opsional; dikirim sebagai ?category=UTBK, bukan body atau parameter query object.'};
schemas.ApiError={type:'object',required:['code','message','fieldErrors','requestId'],properties:{code:{type:'string'},message:{type:'string'},fieldErrors:{type:'object',additionalProperties:{type:'string'}},requestId:{type:'string'}}};
spec.components.securitySchemes={SessionCookie:{type:'apiKey',in:'cookie',name:'JSESSIONID',description:'Session login aplikasi. Browser/Postman cookie jar mengirimnya; bukan access token Google/JWT.'}};
// Explicit nullability/runtime deserialization notes supplement annotation-only schemas.
const nullable={AnswerRequest:['selectedOptionId','flagged'],AnswerResponse:['selectedOptionId'],FinalizeRequest:['abandon'],
 StudentProfileResponse:['schoolName','schoolGrade','targetScore','targetKind','targetInstitution','targetProgram'],UpdateStudentProfileRequest:['targetProgram','targetScore'],
 TryoutResponse:['latest_score','latest_completed_at'],
 SessionResponse:['terminalReason','finalizedAt'],CourseContext:['topicId','topicName'],QuestionScope:['topicId','topicName'],AttemptHistoryResponse:['finalized_at','total_score','details','topic_id','topic_name','terminal_reason'],AttemptQuestionResponse:['selectedOptionId','correct','explanation'],OptionResponse:['correct'],
 ResultResponse:['method','policyVersion','totalScore','details','finalizedAt'],QuestionContent:['explanation'],ImportMetadata:['topicId','sourceYear'],
 ImportResponse:['topicId','sourceYear','pageCount','errorCode','jobStatus','jobErrorCode'],CandidateResponse:['draftContent','questionId'],
 CreateQuestionRequest:['topicId','sourceYear'],StaffQuestionResponse:['topicId','sourceYear','sourceTitle','sourcePage'],TaxonomyResponse:['topicId','topicName']};
for(const [name,fields] of Object.entries(nullable)) for(const field of fields) {
 const value=schemas[name]?.properties?.[field];if(!value)throw Error(`Missing nullable field ${name}.${field}`);
 if(value.$ref) schemas[name].properties[field]={anyOf:[value,{type:'null'}]};
 else if(value.type)value.type=[...new Set([...(Array.isArray(value.type)?value.type:[value.type]),'null'])];
}
for(const [name,fields] of Object.entries({OptionDraft:['correct'],QuestionContent:['shuffleAllowed'],StartPracticeRequest:['questionCount']}))
 schemas[name].required=[...new Set([...(schemas[name].required??[]),...fields])];
schemas.UpdateStudentProfileRequest.properties.targetCategory.enum=['UTBK','CPNS','KEDINASAN','SEKOLAH'];

extendStudentReference({schemas,examples:e,add,existing:endpoints});
extendInternalReference({raw,schemas,examples:e,add,existing:endpoints});
extendWorkbookReference({schemas,examples:e,add,existing:endpoints});
const authNames={public:'Publik',session:'Session login',customer:'Session + role CUSTOMER',staff:'Session + role QUESTION_STAFF',admin:'Session + role PLATFORM_ADMIN',csrf:'Session CSRF; tidak memerlukan autentikasi aktif untuk menghapus session'};
function opKey(ep){return ep.method.toUpperCase()+' '+ep.path;}
const expected=new Set();
for(const [url,methods] of Object.entries(raw.paths)) for(const method of ['get','post','put','patch','delete']) if(methods[method]) expected.add(method.toUpperCase()+' '+url);
for(const key of expected)if(!endpoints.some(x=>opKey(x)===key))throw Error('Undocumented runtime operation: '+key);
for(const ep of endpoints) {
 if(!ep.manual&&!expected.has(opKey(ep)))throw Error('Documented route absent from runtime: '+opKey(ep));
 spec.paths[ep.path]??={};
 const op=spec.paths[ep.path][ep.method]??={operationId:'documented_'+ep.method+'_'+ep.path.replace(/\W+/g,'_')};
 op.summary=ep.title;op.description=[ep.description,...(ep.notes??[])].join('\n\n');op.tags=[ep.group];
 op.security=['session','customer','staff','admin'].includes(ep.auth)?[{SessionCookie:[]}]:[];
 op['x-access-policy']=authNames[ep.auth];
 op.parameters=(op.parameters??[]).filter(p=>p.name!=='query');
 if(ep.category)op.parameters.push({name:'category',in:'query',required:false,schema:{type:'string'},description:'Kode kategori; contoh UTBK. Opsional, bukan object query.'});
 if(ep.page)for(const p of op.parameters)if(p.in==='query'&&p.name==='limit'){p.schema.minimum=1;p.schema.maximum=100;}
 if(['post','put','patch','delete'].includes(ep.method))op.parameters.push({name:'X-CSRF-TOKEN',in:'header',required:true,schema:{type:'string'},description:'Token dari GET /api/v1/csrf dengan cookie session yang sama, termasuk login/register.'});
 if(ep.idempotency){const p=op.parameters.find(p=>p.name==='Idempotency-Key');p.schema={type:'string',pattern:'^[A-Za-z0-9._:-]{8,128}$',minLength:8,maxLength:128};}
 if(ep.request){op.requestBody??={content:{'application/json':{schema:ref(ep.request)}},required:true};op.requestBody.content['application/json'].example=e[ep.request];}
 if(ep.multipart)op.requestBody.content['multipart/form-data'].encoding={metadata:{contentType:'application/json'}};
 const response={description:ep.binary?'Binary file':ep.redirect?'Redirect':ep.response?'Success DTO':ep.status===204?'No Content':'Success'};
 if(ep.response)response.content={'application/json':{schema:ep.array?{type:'array',items:ref(ep.response)}:ref(ep.response),example:ep.example??(ep.array?[e[ep.response]]:e[ep.response])}};
 else if(ep.binary)response.content={[ep.mime??'application/octet-stream']:{schema:{type:'string',format:'binary'}}};
 else if(ep.redirect)response.headers={Location:{schema:{type:'string'},example:ep.location}};
 else if(ep.example!==undefined)response.content={'application/json':{schema:{type:'object',additionalProperties:true},example:ep.example}};
 response.headers??={};response.headers['X-Request-ID']={schema:{type:'string'},description:'Korelasi request dan log server'};
 op.responses={[ep.status]:response};
 const statuses=new Set(ep.path.startsWith('/api/')?[500]:[]);
 if(ep.request||ep.multipart)statuses.add(400);
 if(['session','customer','staff','admin'].includes(ep.auth)&&ep.path.startsWith('/api/'))statuses.add(401);
 if(['customer','staff','admin'].includes(ep.auth)||['post','put','patch','delete'].includes(ep.method))statuses.add(403);
 if(op.parameters.some(p=>p.in==='path')&&!ep.redirect)statuses.add(404);
 for(const error of ep.errors??[])statuses.add(Number(error.slice(0,3)));
 for(const status of statuses)if(status!==ep.status)op.responses[status]={description:'Lihat kode/kondisi pada API reference; bentuk error binary/security non-API dapat berbeda',content:{'application/json':{schema:ref('ApiError')}}};
 if(ep.path==='/actuator/health')op.responses[503]={description:'Health DOWN/OUT_OF_SERVICE',content:{'application/json':{schema:{type:'object'},example:{status:'DOWN'}}}};
 // The binary asset handler explicitly returns an empty 404, not ApiError.
 if(ep.binary&&ep.auth==='session')op.responses[404]={description:'Asset tidak ditemukan; body dapat kosong'};
}

const descriptions={id:'Identitas resource/record pada konteks DTO.',email:'Alamat email akun; bukan provider subject.',displayName:'Nama tampil pengguna.',userType:'Tipe produk; registrasi publik menghasilkan SISWA.',roles:'Daftar role aplikasi, misalnya CUSTOMER.',
 password:'Password input saja; tidak pernah dikembalikan. Contoh bukan kredensial akun nyata.',schoolName:'Nama sekolah.',schoolGrade:'Kelas/tingkat, contoh XII.',leaderboardOptIn:'Persetujuan tampil pada leaderboard; diubah lewat PATCH privacy, bukan PATCH profile.',targetScore:'Skor target pilihan pengguna, 0–1000 atau null.',targetCategory:'Kategori form: UTBK/CPNS/KEDINASAN/SEKOLAH.',targetKind:'Jenis target tersimpan hasil mapping kategori.',targetInstitution:'Nama institusi target.',targetProgram:'Program/jurusan target, boleh null.',
 user:'Ringkasan akun tanpa hash password/token.',category:'Kode kategori; filter opsional pada read API.',code:'Kode resource atau kode opsi sesuai konteks.',title:'Judul paket/blueprint.',exam_type:'Nama jenis ujian.',blueprint_version_id:'UUID versi blueprint aktif.',duration_seconds:'Durasi versi blueprint dalam detik.',question_count:'Jumlah soal per paket/topik.',attempt_count:'Jumlah sesi pengguna pada versi blueprint aktif.',latest_score:'Skor sesi terakhir pada versi ini atau null.',latest_completed_at:'Timestamp hasil terakhir pada versi ini atau null.',
 subtest_code:'Kode subtes.',subtest:'Nama subtes.',topic:'Nama topik.',dateWib:'Tanggal kuota Asia/Jakarta.',used:'Start tryout yang sudah memakai kuota tanggal ini.',limit:'Batas halaman atau kuota sesuai konteks DTO; baca catatan endpoint.',remaining:'Sisa kuota, minimal 0.',
 tryout_id:'ID keluarga blueprint, termasuk blueprint internal practice pada history.',tryout_title:'Judul blueprint saat dibaca.',mode:'TRYOUT atau PRACTICE.',status:'Status resource; baca state machine sesuai attempt/batch/job.',started_at:'Timestamp mulai sesi.',finalized_at:'Timestamp finalize atau null.',total_score:'Skor tersimpan atau null bila belum dinilai.',details:'Rincian hasil; bentuk object pada result, string JSON pada history.',starts_at:'Awal interval.',ends_at:'Akhir interval.',active:'Hak/grant masih berlaku saat server membaca.',rank:'Peringkat tersimpan.',display_name:'Nama tampil snapshot pada entry.',period_start:'Awal periode cohort.',period_end:'Akhir periode cohort.',
 tryoutId:'ID blueprint/package; pada practice menunjuk blueprint internal.',topicId:'ID topik; harus sesuai subtes.',questionCount:'Jumlah soal diminta pada practice atau kebutuhan section.',attemptId:'UUID sesi milik akun aktif.',terminalReason:'null, SUBMITTED, ABANDONED atau TIMED_OUT.',serverNow:'Waktu server agar frontend menghitung countdown.',startedAt:'Timestamp mulai server.',deadlineAt:'Deadline server yang otoritatif.',questions:'Navigator nomor/flag/revision, bukan isi/kunci soal.',position:'Nomor urutan mulai dari 1.',answered:'Apakah selectedOptionId terisi.',flagged:'Penanda ragu; default false pada save jika dihilangkan/null.',revision:'Revision jawaban terakhir; 0 jika belum ada row.',
 stem:'JSON isi soal; saat ini bentuk umum {text:...}.',options:'Opsi berurutan; bentuk siswa berbeda dari konten authoring.',content:'JSON konten opsi siswa atau QuestionContent authoring sesuai DTO.',selectedOptionId:'UUID opsi versi soal; null berarti kosong.',correct:'Flag benar; pada respons siswa null sebelum scoring/ketika tidak dijawab.',explanation:'Pembahasan; disembunyikan pada sesi aktif, dibutuhkan saat publish.',expectedRevision:'Revision yang terakhir diketahui client; >=0.',receivedAt:'Waktu server menerima save.',abandon:'true untuk abandon; false/null/body kosong untuk submit.',method:'Metode scoring BINARY atau OPTION_WEIGHT setelah hasil tersedia.',policyVersion:'Identitas policy code:version hasil.',totalScore:'Indeks latihan tersimpan; null bila belum ada hasil.',sections:'Rincian subtes atau section pada konteks DTO.',correctCount:'Hitungan benar.',wrong:'Jumlah salah.',empty:'Jumlah kosong.',points:'Poin mentah section.',maxPoints:'Jumlah maksimum poin section.',accuracy:'Persentase benar, 4 desimal.',
 subtestId:'UUID subtes dalam lingkup exam type.',name:'Nama section/subtes.',available:'Jumlah stok global terbit, belum mengurangi exposure personal.',durationSeconds:'Durasi konfigurasi dalam detik.',scoringMethod:'Metode policy blueprint.',scale:'Skala normalisasi indeks latihan.',demo:'Menandai blueprint contoh.',ready:'Seluruh section mempunyai stok global minimal kebutuhan.',
 categoryId:'UUID kategori.',categoryCode:'Kode kategori.',categoryName:'Nama kategori.',examTypeId:'UUID jenis ujian.',examTypeCode:'Kode jenis ujian.',examTypeName:'Nama jenis ujian.',subtestName:'Nama subtes.',topicName:'Nama topik; null jika tidak ada.',
 sourceTitle:'Nama sumber/provenance, maksimal 180 karakter input.',sourceYear:'Tahun sumber 1900–2200 atau null.',stemText:'Teks stem authoring, bukan JSON stem siswa.',text:'Teks opsi authoring.',weight:'Bobot numerik opsi, request 0–1000.',shuffleAllowed:'Izin mengacak opsi; kirim eksplisit agar urutan bergantung konteks tidak rusak.',versionId:'UUID versi soal terbaru yang dikembalikan.',version:'Nomor versi positif.',sourcePage:'Halaman sumber mulai 1 atau null pada input manual.',
 sourceItemNo:'Nomor asal yang dinormalisasi per halaman untuk dedup kandidat.',extractedContent:'Konten ekstraksi asli; bisa tidak lengkap.',draftContent:'Koreksi petugas atau null jika belum dikoreksi.',validationErrors:'Kode kekurangan/ambiguity kandidat; [] berarti lengkap menurut validator.',questionId:'UUID keluarga master setelah kandidat disimpan, selain itu null.',fileId:'UUID metadata stored_file sumber.',pageCount:'Jumlah halaman setelah worker memproses; awal null.',candidateCount:'Total kandidat.',validCount:'Kandidat tanpa validationErrors.',invalidCount:'Kandidat yang masih mempunyai error.',errorCode:'Kode error batch atau null.',jobStatus:'Status pekerjaan terpisah dari status batch.',jobErrorCode:'Kelas/error terakhir job yang tersensor atau null.',createdAt:'Timestamp pembuatan record.',
 headerName:'Nama header yang harus dipakai untuk CSRF.',token:'Token CSRF yang terikat session; jangan dicatat ke log.',service:'Nama aplikasi.',timestamp:'Timestamp server UTC.',fieldErrors:'Map field request ke pesan validasi; {} bila tidak relevan.',requestId:'Korelasi error/header/log server.',message:'Pesan error aman untuk client.'};
Object.assign(descriptions,{context:'Konteks tampilan sesi dari blueprint terkait; bukan kunci jawaban.',scope:'Metadata subtes/topik soal tanpa kunci atau bobot.',finalizedAt:'Timestamp finalize server; null sebelum sesi ditutup.',topic_id:'ID topik latihan; berbeda dengan tryout_id blueprint internal. Null untuk tryout.',topic_name:'Nama topik latihan atau null.',terminal_reason:'SUBMITTED / ABANDONED / TIMED_OUT; tidak sama dengan lifecycle status.',defaultQuestionCount:'Jumlah latihan default min(stok,10).',maxQuestionCount:'Batas jumlah latihan min(stok,50); 0 bila belum ada stok.',secondsPerQuestion:'Durasi teknis per soal dalam detik; saat ini 120.'});
const specific={
 'SectionResult.correct':'Jumlah jawaban benar dalam subtes, bukan boolean.',
 'QuestionContent.options':'2–5 OptionDraft untuk request; pada hasil ekstraksi kandidat bisa belum lengkap.',
 'OptionDraft.correct':'Kunci opsi; tepat satu true saat publish. Wajib dikirim eksplisit.',
 'OptionResponse.correct':'null sebelum hasil; true/false setelah penilaian.',
 'ApiError.code':'Kode error stabil untuk branching frontend, bukan status HTTP.',
 'CandidateResponse.id':'UUID kandidat, bukan questionId atau batchId.',
 'ImportResponse.id':'UUID batch impor.',
 'StaffQuestionResponse.id':'UUID keluarga master question, bukan UUID versi.',
 'TryoutResponse.id':'UUID blueprint untuk StartTryoutRequest.tryoutId.',
 'TryoutResponse.attempt_count':'Jumlah sesi pada keluarga blueprint ini, termasuk versi sebelumnya.',
 'TryoutResponse.latest_score':'Skor terbaru pada keluarga blueprint ini lintas versi, atau null.',
 'CourseContext.title':'Judul sesi yang dibaca dari keluarga blueprint terkait.',
 'CourseContext.topicId':'ID topik asal pada practice; gunakan untuk link detail latihan, bukan tryoutId internal.',
 'CourseContext.sections':'Nama dan jumlah soal subtes dari versi blueprint sesi, bukan versi katalog aktif terbaru.',
 'PracticeTopicDetail.questionCount':'Stok soal PUBLISHED yang tidak diarsipkan pada topik aktif.',
 'PracticeTopicResponse.id':'UUID topic untuk StartPracticeRequest.topicId.',
 'SessionResponse.questions':'Navigator {position,answered,flagged,revision}; isi soal dibaca lewat endpoint question.',
 'AnswerRequest.flagged':'Jika tidak dikirim/null, service memakai false; bukan mempertahankan flag lama.',
 'QuestionContent.explanation':'Opsional pada draft request; wajib tidak kosong saat publish.',
 'AttemptHistoryResponse.details':'String JSON tersimpan atau null; lakukan parsing kedua bila ingin object.',
};
function typeOf(schema={}) {
 if(schema.$ref)return schema.$ref.split('/').at(-1);
 if(schema.anyOf)return schema.anyOf.map(typeOf).join(' | ');
 const t=Array.isArray(schema.type)?schema.type.join(' | '):schema.type??'object';
 return t==='array'?'array<'+typeOf(schema.items)+'>':t+(schema.format?' ('+schema.format+')':'');
}
function constraints(s={}) {
 const a=[];for(const k of ['minimum','maximum','minLength','maxLength','minItems','maxItems','pattern'])if(s[k]!==undefined)a.push(k+'='+s[k]);
 if(s.enum)a.push('enum='+s.enum.join('/'));if(s.default!==undefined)a.push('default='+s.default);
 return a.join('; ')||'—';
}
const pipe=value=>String(value??'—').replaceAll('|','\\|').replaceAll('\n',' ');
const fence=(lang,value)=>'```'+lang+'\n'+(typeof value==='string'?value:JSON.stringify(value,null,2))+'\n```';
function sampleUrl(ep){let url=ep.path.replace(/\{([^}]+)\}/g,(_,key)=>key==='position'?'1':key==='candidateId'?id.candidate:key==='assetId'?uuid(30):key==='attemptId'?id.attempt:ep.path.includes('/staff/imports')?id.batch:ep.path.includes('/staff/questions')?id.question:ep.path.includes('/tryouts/')?id.tryout:id.attempt);if(ep.category)url+='?category=UTBK';if(ep.page)url+='?offset=0&limit=20';return url;}
const api=['# Referensi endpoint backend','', '[Indeks](README.md) · [Kamus semua DTO](data-contracts.md) · [OpenAPI lengkap](openapi.json)','',
 `Snapshot 21 November 2026. **${expected.size} operasi controller** dicocokkan dengan runtime; **${endpoints.length} operasi total** mencakup logout/OAuth/Actuator/Swagger. Resource statis internal library tidak didaftar satu per satu.`, '',
 '## Konvensi request dan response','',
 '- Base URL lokal: `http://localhost:8080`. Semua contoh memakai ID/data sintetis; ganti dengan ID response nyata.',
 '- Cookie `JSESSIONID` adalah session aplikasi. Tidak ada bearer access/refresh JWT untuk API ini.',
 '- POST/PUT/PATCH/DELETE mengirim CSRF dari `GET /api/v1/csrf` dan cookie yang sama, termasuk login/register.',
 '- Header Content-Type JSON untuk body JSON; upload memakai multipart dengan part metadata application/json.',
 '- GET tidak mempunyai body kecuali dinyatakan lain. Path ID memakai UUID; position nomor mulai 1.',
 '- Sukses dapat berupa object, array, 200 kosong, 204 kosong, redirect atau binary. Jangan selalu memanggil response.json().',
 '- Respons error API biasanya mempunyai X-Request-ID dan envelope di bawah; exception binary/non-API dijelaskan per endpoint.',
 '- Field camelCase/snake_case mengikuti DTO aktual. Semua key dijelaskan pada kamus DTO; tidak ada key password/token pada response user.', '',
 '### Bentuk error umum','',fence('json',e.ApiError),'',
 '| Status | Kode umum | Makna |','|---|---|---|',
 '| 400 | VALIDATION_FAILED / MALFORMED_JSON / INVALID_REQUEST / HTTP_400 | Body, header atau parameter tidak valid |',
 '| 401 | AUTHENTICATION_REQUIRED / INVALID_CREDENTIALS | Session/login tidak valid |',
 '| 403 | CSRF_INVALID / ACCESS_DENIED | Token session tidak cocok atau role tidak cukup |',
 '| 404 | NOT_FOUND / HTTP_404 | Resource tidak ada/tidak dimiliki; asset bisa body kosong |',
 '| 409 | Kode konflik spesifik | Quota, replay, pool, revision, status atau publish |',
 '| 413 | FILE_TOO_LARGE / HTTP_413 | Limit file/request |',
 '| 500 | INTERNAL_ERROR | Telusuri requestId pada log server |','',
 'Contoh fieldErrors untuk validasi: `{"email":"must be a well-formed email address"}`. Pesan validator bisa berbeda; frontend sebaiknya menggunakan status/code dan map fieldErrors.', '',
 '## Indeks endpoint','', '| No | Method | Path | Fungsi |','|---|---|---|---|'];
endpoints.forEach((ep,index)=>api.push(`| ${index+1} | ${ep.method.toUpperCase()} | [\`${ep.path}\`](#e${index+1}) | ${ep.title} |`));
let group;
for(const [index,ep] of endpoints.entries()) {
 if(ep.group!==group){group=ep.group;api.push('',`## ${group}`,'');}
 const op=spec.paths[ep.path][ep.method];
 api.push(`<a id="e${index+1}"></a>`,`### ${index+1}. ${ep.title}`,'',`**${ep.method.toUpperCase()} \`${ep.path}\`**`,'',ep.description,'',`Akses: **${authNames[ep.auth]}**. Sukses utama: **HTTP ${ep.status}**.`,'');
 const params=op.parameters??[];
 if(params.length){api.push('#### Parameter dan header','', '| Nama | Letak | Wajib | Tipe / batas |','|---|---|---|---|');for(const p of params)api.push(`| ${p.name} | ${p.in} | ${p.required?'Ya':'Tidak'} | ${pipe(typeOf(p.schema))}; ${pipe(constraints(p.schema))} |`);api.push('');}
 api.push('#### Contoh request','');
 let http=ep.method.toUpperCase()+' '+sampleUrl(ep)+' HTTP/1.1\nHost: localhost:8080';
 if(ep.auth!=='public'||['post','put','patch'].includes(ep.method))http+='\nCookie: JSESSIONID=example-session';
 if(['post','put','patch','delete'].includes(ep.method))http+='\nX-CSRF-TOKEN: example-csrf-token';
 if(ep.idempotency)http+='\nIdempotency-Key: example-start-001';
 if(ep.request)http+='\nContent-Type: application/json';
 if(ep.multipart)http+='\nContent-Type: multipart/form-data; boundary=example-boundary\n\n--example-boundary\nContent-Disposition: form-data; name="metadata"\nContent-Type: application/json\n\n'+JSON.stringify(e[ep.metadataSchema??'ImportMetadata'],null,2)+'\n--example-boundary\nContent-Disposition: form-data; name="file"; filename="'+(ep.uploadName??'sample-questions.pdf')+'"\nContent-Type: '+(ep.uploadMime??'application/pdf')+'\n\n[binary file]\n--example-boundary--';
 api.push(fence('http',http),'');
 if(ep.request)api.push(`Body: [${ep.request}](data-contracts.md#${ep.request.toLowerCase()}). Wajib/validasi setiap key ada pada kamus DTO.`,'',fence('json',e[ep.request]),'');
 else if(!ep.multipart)api.push('Body request: **tidak ada**.','');
 if(ep.multipart)api.push('Key `file` berisi binary '+(ep.metadataSchema==='WorkbookMetadata'?'XLSX':'PDF')+' dan `metadata` mengikuti ['+(ep.metadataSchema??'ImportMetadata')+'](data-contracts.md#'+(ep.metadataSchema??'ImportMetadata').toLowerCase()+'). Gunakan file picker Postman agar boundary dibentuk otomatis.','');
 api.push('#### Response','');
 if(ep.response){api.push(`Schema: ${ep.array?'array dari ':''}[${ep.response}](data-contracts.md#${ep.response.toLowerCase()}).`,'',fence('json',ep.example??(ep.array?[e[ep.response]]:e[ep.response])),'');}
 else if(ep.redirect)api.push(fence('http',`HTTP/1.1 ${ep.status} Found\nLocation: ${ep.location}`),'');
 else if(ep.binary)api.push(fence('http',`HTTP/1.1 200 OK\nContent-Type: ${ep.mime}\nCache-Control: no-store\n\n[binary file]`),'');
 else if(ep.example!==undefined)api.push(fence('json',ep.example),'');
 else api.push(`HTTP ${ep.status}, **body kosong**.`,'');
 for(const note of ep.notes??[])api.push('- '+note);
 if(ep.notes?.length)api.push('');
 api.push('#### Error/kondisi yang perlu ditangani','');
 if(['session','customer','staff','admin'].includes(ep.auth)&&ep.path.startsWith('/api/'))api.push('- 401 tanpa session login atau sesi dicabut; 403 bila role/CSRF menolak.');
 if(params.some(p=>p.in==='path')&&!ep.redirect)api.push('- Gunakan UUID/position yang valid. Resource tidak tersedia/tidak dimiliki memberi 404; pemetaan error konversi parameter masih perlu diseragamkan.');
 if(ep.request||ep.multipart)api.push('- 400 pada validasi/deserialisasi, 403 CSRF_INVALID bila token/cookie tidak cocok.');
 for(const error of ep.errors??[])api.push('- '+error+'.');
 if(ep.path==='/api/v1/users/current/attempts/active')api.push('- Tidak ada sesi aktif adalah sukses 200 kosong, bukan error.');
 if(ep.path.startsWith('/api/')&&!ep.redirect)api.push('- 500 INTERNAL_ERROR untuk kegagalan tak terduga; cocokkan requestId dengan log.');
 if(ep.redirect||ep.path.startsWith('/actuator'))api.push('- Handler/format non-API dapat berbeda dari envelope JSON API; lihat catatan respons dan konfigurasi Security.');
 api.push('');
}
api.push('## Sumber dan pemeliharaan','', 'Dokumen dibangkitkan oleh [generate-reference.mjs](generate-reference.mjs) dari runtime snapshot serta catatan yang direview terhadap source. Contoh response sintetis tidak membuktikan endpoint sudah mempunyai data produksi. Lihat roadmap untuk fitur parsial.');
write('03-api-reference.md',api.join('\n'));

const contracts=['# Kamus DTO dan key JSON','', '[Indeks](README.md) · [API](03-api-reference.md)','',
 'Tipe dan batas berasal dari schema runtime yang disesuaikan dengan DTO/behavior. Kolom **Wajib input** berlaku saat model digunakan sebagai request; bukan janji semua response field non-null. Tanda null dijelaskan pada tipe/catatan.', '',
 'Bean Validation @NotBlank juga melarang string kosong/whitespace meskipun hasil OpenAPI dapat menulis minLength=0. Constraint model request tidak otomatis menjadi validasi hasil ekstraksi PDF; kandidat bisa belum lengkap.', '',
 'Primitive boolean/numeric request dikirim eksplisit. FinalizeRequest.abandon dan AnswerRequest.flagged adalah Boolean opsional; nilainya mempunyai default sebagaimana API reference.', ''];
for(const [name,schema] of Object.entries(schemas).sort(([a],[b])=>a.localeCompare(b))) {
 contracts.push(`<a id="${name.toLowerCase()}"></a>`,`## ${name}`,'', '| Key | Tipe JSON | Wajib input | Batas dari schema | Makna |','|---|---|---|---|---|');
 for(const [field,value] of Object.entries(schema.properties??{}))contracts.push(`| ${field} | ${pipe(typeOf(value))} | ${(schema.required??[]).includes(field)?'Ya':'Tidak ditandai'} | ${pipe(constraints(value))} | ${pipe(specific[name+'.'+field]??descriptions[field]??value.description??'Data sesuai konteks resource; lihat contoh dan endpoint pemakai.')} |`);
 if(e[name]!==undefined)contracts.push('',fence('json',e[name]));
 contracts.push('');
}
write('data-contracts.md',contracts.join('\n'));
write('api-examples.json',JSON.stringify(e,null,2));
write('openapi.json',JSON.stringify(spec,null,2));
write('endpoint-inventory.json',JSON.stringify(endpoints.map((ep,index)=>({...ep,index:index+1,runtimeController:!ep.manual})),null,2));

const db=read('database-schema.json');
const tables=[...new Set(db.columns.map(c=>c.table_name))].sort();
const purposes={
 app_user:['Aktif','Akun, email canonical, BCrypt atau password null SSO, status dan tipe produk.'],user_role:['Aktif','Role many-to-many berbasis kode; tidak menciptakan superuser otomatis.'],user_profile:['Aktif','Profil siswa, sekolah/kelas, target dan consent/opt-in.'],target:['Aktif','Tujuan institusi/program; belum master institusi resmi.'],oauth_account:['Aktif','Relasi provider-subject ke akun.'],
 exam_category:['Aktif/admin','Kategori taksonomi dengan kode stabil.'],exam_type:['Aktif/admin','Jenis ujian dalam kategori; parent immutable.'],subtest:['Aktif/admin','Subtes dalam jenis ujian; parent immutable.'],topic:['Aktif/admin','Topik dalam subtes; parent immutable.'],question:['Aktif','Keluarga soal dan archive/provenance kandidat.'],question_version:['Aktif','Konten/sumber/status versi; editor_revision untuk edit draft.'],question_option:['Aktif','Opsi/kunci/bobot per versi.'],
 blueprint:['Aktif/read + fixture','Identitas katalog atau blueprint practice internal.'],blueprint_version:['Aktif','Durasi dan policy konfigurasi terversi.'],blueprint_section:['Aktif','Kebutuhan/filter pool per section.'],attempt:['Aktif','Sesi pengguna; entitlement_snapshot belum diisi aturan hak produk.'],attempt_item:['Aktif','Snapshot referensi versi dan urutan opsi.'],answer:['Aktif','Jawaban terakhir/revision; bukan semua event autosave.'],quota_day:['Aktif','Counter start tryout tanggal WIB.'],quota_usage:['Aktif','Satu pemakaian kuota per attempt.'],scoring_policy:['Aktif','Konfigurasi scoring terversi.'],score_run:['Aktif','Hasil, policyVersion, correctionVersion dan input hash.'],score_section:['Aktif','Rincian hasil per subtes.'],
 stored_file:['Aktif','Metadata file; binary tetap disk privat.'],import_batch:['Aktif','Lingkup, status dan statistik batch PDF.'],import_candidate:['Aktif','Ekstraksi/koreksi/flag dan hubungan master.'],question_asset:['Read + schema','Asset privat versi; belum upload/attach authoring API.'],job:['Aktif','Antrean persisten dengan dedup/retry/lease token.'],audit_event:['Sebagian','Audit sejumlah mutasi konten; belum seluruh tindakan sistem.'],
 subscription:['Read/revoke admin','Interval langganan dan pencabutan grant; belum purchase/grant workflow.'],plan:['Admin terversi','Keluarga harga/manfaat immutable dan revisi penjualan.'],purchase_order:['Read admin + schema','Snapshot order; checkout/gateway belum tersedia.'],payment_event:['Read admin + schema','Metadata provider tersensor; verifikasi signature mendatang.'],refund_request:['Kasus operasional','Permintaan idempoten PENDING_PROVIDER; tidak memindahkan uang.'],entitlement_grant:['Read/revoke admin','Grant order; creation/payment workflow belum ada.'],exam_form:['Schema saja','Form tetap untuk cohort sebanding mendatang.'],exam_form_item:['Schema saja','Daftar soal/posisi form tetap.'],leaderboard_cohort:['Read + schema','Definisi cohort; belum admin/provisioning flow.'],leaderboard_entry:['Read + schema','Peringkat yang bisa dibaca; ranking worker belum dibuat.'],password_reset_token:['Schema saja','Hash/expiry token; endpoint/mail belum ada.'],email_verification_token:['Schema saja','Hash/expiry token; endpoint/mail belum ada.']};
const columnNotes={id:'UUID identitas record.',user_id:'FK pemilik/pengguna.',email:'Email tampil/input akun.',email_canonical:'Email normalized untuk lookup unik.',password_hash:'Hash BCrypt; rahasia, boleh NULL untuk SSO.',user_type:'Tipe produk, default SISWA.',role_code:'CUSTOMER / QUESTION_STAFF / PLATFORM_ADMIN.',provider_subject:'Subject stabil provider OAuth; bagian primary key bersama provider.',relative_path:'Lokasi relatif dalam storage privat, bukan URL publik.',sha256:'Checksum isi file, 64 karakter hex.',purpose:'Jenis file/asset; IMPORT_SOURCE tidak boleh dibaca sebagai asset siswa.',stem:'JSONB konten versi, lazimnya {text:...}.',content:'JSONB isi opsi, lazimnya {text:...}.',correct_key:'Kunci opsi; tidak dikirim pada sesi aktif.',score_weight:'Bobot opsi untuk metode penilaian.',option_order:'JSON array kode opsi urutan snapshot.',revision:'Revision jawaban terakhir.',entitlement_snapshot:'Disiapkan untuk snapshot hak; start saat ini menggunakan default DB.',request_hash:'SHA-256 mode/target/count untuk deteksi replay berbeda.',idempotency_key:'Key client scoped per pengguna/order.',terminal_reason:'Reason SUBMITTED/ABANDONED/TIMED_OUT pada flow attempt.',day_wib:'Tanggal Asia/Jakarta.',used_count:'Counter harian, CHECK 0..2.',input_hash:'Hash input scoring yang tersimpan.',rules:'JSON konfigurasi policy terversi.',details:'JSON hasil scoring.',extracted_content:'Konten asli hasil parser/OCR.',draft_content:'Koreksi kandidat, terpisah dari ekstraksi asli.',validation_errors:'JSON array kode kekurangan kandidat.',payload:'JSON dispatcher job; hanya ID resource terkait.',metadata:'JSON metadata batch dasar; data scope baru tersedia pada kolom eksplisit.',lease_token:'UUID identitas claim saat ini untuk menolak worker lama.',lease_until:'Batas waktu kepemilikan job.',retry_count:'Jumlah claim/percobaan yang sudah dilakukan.',max_attempts:'Batas default lima claim.',last_error:'Kode/kelas error tersensor; stack trace pada log.',amount_minor:'Nilai uang minor unit, bukan float.',token_hash:'Hash token sensitif, bukan token mentah.',before_redacted:'Data audit sebelum perubahan yang tersensor; belum seluruh flow mengisi.',after_redacted:'Data audit sesudah perubahan yang tersensor; belum seluruh flow mengisi.'};
Object.assign(purposes,{
 question_set:['Aktif','Identitas sumber jenis ujian/tahun immutable, status ACTIVE atau ARCHIVED.'],
 question_set_item:['Aktif','Keanggotaan N:M master soal, kode sumber per subtes dan composite FK lingkup.'],
 import_sheet:['Aktif','Pemetaan sheet XLSX ke subtes, urutan dan status staging.'],
 blueprint_section_source:['Aktif','Sumber yang diizinkan per section; immutable setelah versi blueprint diterbitkan.']
});
const dictionary=['# Kamus tabel PostgreSQL','', '[Indeks](README.md) · [ERD dan model data](02-database.md) · [Metadata JSON](database-schema.json)','',
 `Snapshot schema public V9, 21 November 2026: **${tables.length} tabel**, **${db.columns.length} kolom**. Metadata diambil tanpa baris pengguna/kredensial. flyway_schema_history tidak dimasukkan.`, '',
 'Constraint dan index ditulis sebagai definisi PostgreSQL aktual. PK/unique kadang juga mempunyai index; keduanya ditampilkan untuk audit. Foreign key composite jangan disederhanakan menjadi FK tunggal saat mengubah schema.', '',
 '## Inventaris','', '| Tabel | Status implementasi | Tanggung jawab |','|---|---|---|'];
for(const table of tables){if(!purposes[table])throw Error('Missing table description '+table);dictionary.push(`| [${table}](#${table.replaceAll('_','-')}) | ${purposes[table][0]} | ${purposes[table][1]} |`);}
for(const table of tables) {
 dictionary.push('',`<a id="${table.replaceAll('_','-')}"></a>`,`## ${table}`,'',`**${purposes[table][0]}** — ${purposes[table][1]}`,'', '| Kolom | Tipe PostgreSQL | NULL | Default | Makna/catatan |','|---|---|---|---|---|');
 for(const c of db.columns.filter(c=>c.table_name===table)){
  let type=c.data_type;if(c.character_maximum_length)type+='('+c.character_maximum_length+')';if(c.data_type==='numeric')type+='('+c.numeric_precision+','+c.numeric_scale+')';
  const related=db.constraints.filter(k=>k.table_name===table&&k.definition.includes(c.column_name));
  let note=columnNotes[c.column_name]??(c.column_name.endsWith('_id')?'Referensi identitas; lihat foreign key di bawah.':c.column_name.endsWith('_at')?'Timestamp lifecycle sesuai konteks tabel.':c.column_name==='status'?'Status resource; service hanya memakai subset yang dijelaskan pada arsitektur.':'Field '+c.column_name+' pada '+table+'; tipe/batas dijaga definisi di bawah.');
  if(related.some(x=>x.type==='p'))note+=' Lihat primary key.';
  dictionary.push(`| ${c.column_name} | ${pipe(type)} | ${c.is_nullable==='YES'?'Ya':'Tidak'} | ${pipe(c.column_default)} | ${pipe(note)} |`);
 }
 const cons=db.constraints.filter(c=>c.table_name===table);
 dictionary.push('', '### Constraint','');for(const c of cons)dictionary.push('- `'+c.name+'`: `'+c.definition+'`');
 dictionary.push('', '### Index','');for(const item of db.indexes.filter(i=>i.tablename===table))dictionary.push('- `'+item.indexname+'`: `'+item.indexdef+'`');
 const triggers=db.triggers.filter(t=>t.table_name===table);if(triggers.length){dictionary.push('', '### Trigger','');for(const t of triggers)dictionary.push('- `'+t.name+'`: `'+t.definition+'`');}
}
write('database-dictionary.md',dictionary.join('\n'));
console.log(`Generated API (${endpoints.length} operations, ${expected.size} runtime), ${Object.keys(schemas).length} schemas and ${tables.length} tables.`);
