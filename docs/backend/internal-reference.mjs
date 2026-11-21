// Reviewed internal contracts; shared by documentation and Postman generation.
export function extendInternalReference({raw,schemas,examples:e,add,existing=[]}) {
 const id=n=>'00000000-0000-4000-8000-'+String(n).padStart(12,'0');
 const reason='Pemeriksaan operasional berdasarkan tiket dukungan contoh';
 Object.assign(e,{
  MutationReason:{reason},StatusRequest:{status:'SUSPENDED',reason},RolesRequest:{roles:['QUESTION_STAFF'],reason},
  InternalUserRequest:{email:'petugas-baru@example.com',displayName:'Petugas Contoh',roles:['QUESTION_STAFF'],reason},
  RefundRequest:{amountMinor:10000,reason},
  CategoryRequest:{code:'UTBK',name:'Ujian Tulis Berbasis Komputer',active:true,reason},
  ExamTypeRequest:{categoryId:id(24),code:'TPS',name:'Tes Potensi Skolastik',active:true,reason},
  SubtestRequest:{examTypeId:id(25),code:'NUMERIK',name:'Penalaran Numerik',active:true,reason},
  TopicRequest:{subtestId:id(4),name:'Aritmetika',active:true,reason},
  PlanBenefits:{fullLeaderboard:true},PlanRequest:{code:'PREMIUM_1_MONTH',name:'Premium 1 bulan',months:1,amountMinor:39000,currency:'IDR',benefits:{fullLeaderboard:true},reason},
  ActiveRequest:{active:false,reason},ScoreRules:{correctPoints:1,wrongPoints:0,emptyPoints:0},
  PolicyRequest:{code:'BINARY_100',method:'BINARY',scale:100,rules:{correctPoints:1,wrongPoints:0,emptyPoints:0},reason},
  BlueprintRequest:{examTypeId:id(25),code:'TRYOUT_UTBK_01',title:'Tryout UTBK 01',reason},
  SectionRequest:{subtestId:id(4),questionCount:3,topicId:id(5),sourceYear:2026},
  BlueprintVersionRequest:{durationSeconds:5400,scoringPolicyId:id(70),sections:[{subtestId:id(4),questionCount:3,topicId:id(5),sourceYear:2026}],reason},
  PublishRequest:{expectedActiveVersionId:null,reason},BlueprintStatusRequest:{status:'ARCHIVED',reason},
  DraftRequest:{expectedVersionId:id(8),expectedRevision:0,content:e.QuestionContent,reason},
  SaveCandidatesRequest:{candidateIds:[id(22)],reason}
 });
 Object.assign(e,{
  AdminUserResponse:{id:id(1),email:'petugas@example.com',displayName:'Petugas Contoh',userType:'INTERNAL',status:'ACTIVE',roles:['QUESTION_STAFF'],createdAt:'2026-11-20T07:00:00Z',updatedAt:'2026-11-20T07:00:00Z'},
  RefundResponse:{id:id(80),orderId:id(81),amountMinor:10000,currency:'IDR',status:'PENDING_PROVIDER',createdAt:'2026-11-20T07:00:00Z',completedAt:null},
  BlueprintResponse:{id:id(2),examTypeId:id(25),code:'TRYOUT_UTBK_01',title:'Tryout UTBK 01',status:'ACTIVE',activeVersionId:id(3)},
  BlueprintSection:{id:id(23),position:1,subtestId:id(4),subtestName:'Penalaran Numerik',questionCount:3,topicId:id(5),sourceYear:2026,available:12},
  PolicyResponse:{id:id(70),code:'BINARY_100',version:1,method:'BINARY',scale:100,rules:{correctPoints:1,wrongPoints:0,emptyPoints:0},status:'ACTIVE'},
  DraftView:{question:{...e.StaffQuestionResponse,status:'DRAFT'},revision:0},
  VersionSummary:{id:id(8),version:1,status:'DRAFT',revision:0,createdBy:id(1),createdAt:'2026-11-20T07:00:00Z'},
  JobResponse:{id:id(82),type:'IMPORT_PDF',status:'FAILED',attempts:5,maxAttempts:5,runAfter:'2026-11-20T07:00:00Z',leaseUntil:null,errorCode:'EXTRACTION_FAILED',createdAt:'2026-11-20T07:00:00Z'},
  Revenue:{currency:'IDR',paidGrossMinor:50000,refundedMinor:0,netMinor:50000,pendingOrders:1}
 });
 e.BlueprintVersion={id:id(3),version:1,status:'ACTIVE',durationSeconds:5400,scoringPolicyId:id(70),sections:[e.BlueprintSection],ready:true};
 e.QuestionSummary={id:id(7),code:'Q-EXAMPLE',versionId:id(8),version:1,status:'DRAFT',revision:0,category:'UTBK',examType:'TPS',subtest:'Penalaran Numerik',topic:'Aritmetika',sourceYear:2026,sourceTitle:'Sumber contoh',stemPreview:'Berapa hasil satu tambah satu?',batchId:null};
 schemas.JsonNode={anyOf:[{type:'object',additionalProperties:true},{type:'array',items:{}},{type:'string'},{type:'number'},{type:'boolean'},{type:'null'}],description:'Nilai JSON audit tersensor; dapat berupa objek atau nilai sederhana.'};
 const nullable={PublishRequest:['expectedActiveVersionId'],TaxonomyItem:['parentId','code'],SupportProfile:['schoolName','schoolGrade','targetKind','institution','program','targetScore'],
  OrderResponse:['provider','providerOrderId','expiresAt'],PaymentEventResponse:['amountMinor'],UserAttempt:['terminalReason','finalizedAt','score'],RefundResponse:['completedAt'],GrantResponse:['revokedAt'],
  JobResponse:['leaseUntil','errorCode'],AuditResponse:['actorId','entityId','reason','requestId','before','after'],ContentAudit:['actorId','entityId','reason','requestId'],
  BlueprintResponse:['activeVersionId'],BlueprintSection:['topicId','sourceYear'],QuestionSummary:['topic','sourceYear','sourceTitle','batchId'],ScoreRules:['correctPoints','wrongPoints','emptyPoints']};
 for(const [name,fields]of Object.entries(nullable))for(const field of fields){const s=schemas[name]?.properties?.[field];if(!s)continue;if(s.$ref)schemas[name].properties[field]={anyOf:[s,{type:'null'}]};else s.type=[...new Set([...(Array.isArray(s.type)?s.type:[s.type??'object']),'null'])];}
 const fieldValues={email:'pengguna@example.com',userEmail:'pengguna@example.com',name:'Nama contoh',displayName:'Pengguna Contoh',userType:'SISWA',status:'ACTIVE',code:'EXAMPLE',familyCode:'PREMIUM_1_MONTH',currency:'IDR',method:'BINARY',reason,requestId:'example-request-id',title:'Tryout contoh',blueprintTitle:'Tryout contoh',category:'UTBK',examType:'TPS',subtest:'Penalaran Numerik',subtestName:'Penalaran Numerik',topic:'Aritmetika',sourceTitle:'Sumber contoh',stemPreview:'Berapa hasil satu tambah satu?',originalName:'soal-contoh.pdf',action:'USER_STATUS_CHANGED',entityType:'USER',terminalReason:'SUBMITTED',mode:'TRYOUT',provider:'EXAMPLE_PROVIDER',providerEventId:'example-event-001',providerOrderId:'example-order-001',signatureStatus:'VERIFIED',permissions:'OWN_PROFILE',roles:'CUSTOMER',errorCode:'EXTRACTION_FAILED'};
 function example(s,key='',seen=[]) {
  if(s.$ref){const name=s.$ref.split('/').at(-1);if(e[name])return structuredClone(e[name]);if(seen.includes(name))return {};return example(schemas[name],name,[...seen,name]);}
  if(s.anyOf)return example(s.anyOf.find(x=>x.type!=='null')??s.anyOf[0],key,seen);
  const type=Array.isArray(s.type)?s.type.find(x=>x!=='null'):s.type;
  if(type==='array')return [example(s.items??{},key,seen)];
  if(type==='object'||s.properties)return Object.fromEntries(Object.entries(s.properties??{}).map(([k,v])=>[k,example(v,k,seen)]));
  if(s.enum)return s.enum[0];
  if(type==='boolean')return key==='paymentProviderConfigured'?false:true;
  if(type==='number'||type==='integer'){if(key==='sourceYear')return 2026;if(key==='durationSeconds')return 5400;if(key==='limit')return 20;if(key==='offset')return 0;return Math.max(s.minimum??1,1);}
  if(s.format==='uuid')return id(90);
  if(s.format==='date-time')return '2026-11-20T07:00:00Z';
  return fieldValues[key]??'Contoh';
 }
 for(const [name,schema]of Object.entries(schemas))if(!e[name])e[name]=example(schema,name,[name]);
 const definitions={
  '/admin/dashboard':['Ringkasan operasional','Jumlah akun ACTIVE saat dibaca, akun baru/sesi selesai dalam [from,to), pool, impor, job, metadata storage dan gross/refund/net per mata uang. Gross memakai created_at order PAID/REFUNDED; refund memakai completed_at SUCCEEDED. Bukan laporan settlement gateway; tidak mencampur mata uang.'],
  '/admin/roles':['Matriks peran','CUSTOMER, QUESTION_STAFF dan PLATFORM_ADMIN beserta izin operasional. Admin tidak otomatis memiliki akses kunci/PDF.'],
  '/admin/users':['Daftar atau buat akun internal','GET mencari nama/email/UUID, status dan periode dengan pagination. POST membuat akun INTERNAL tanpa password, dengan email yang akan dipakai Google SSO; tidak mengirim undangan email. Email duplikat ditolak.'],
  '/admin/users/{id}':['Detail akun untuk dukungan','Wajib query reason; akses detail dicatat. Profil/target dan hitungan sesi/order tidak menyertakan hash password, token atau identitas rahasia provider.'],
  '/admin/users/{id}/attempts':['Riwayat sesi untuk dukungan','Wajib query reason; daftar ringkasan sesi pengguna, tanpa isi soal/jawaban. Akses dicatat.'],
  '/admin/users/{id}/roles':['Ganti role akun','Mengganti seluruh himpunan role. Registrasi publik tidak bisa memakai endpoint ini. Perubahan menaikkan auth_revision dan mencabut sesi lama; satu admin aktif wajib dipertahankan, termasuk permintaan bersamaan.'],
  '/admin/users/{id}/status':['Suspend atau pulihkan akun','ACTIVE/SUSPENDED dengan alasan. Sesi lama tidak hidup kembali setelah restore; pengguna harus login lagi. Jawaban/snapshot ujian tidak diubah. Admin aktif terakhir dilindungi.'],
  '/admin/orders':['Daftar transaksi','Filter status, userId, pencarian UUID/email/referensi provider dan periode created_at. Menampilkan snapshot order, bukan harga paket terbaru.'],
  '/admin/orders/{id}':['Detail transaksi','Order, metadata event pembayaran tersensor, dan refund requests. Tidak mengirim payload provider, rekening atau kredensial.'],
  '/admin/orders/{id}/reconcile':['Rekonsiliasi belum dikonfigurasi','Resource order diperiksa. Selalu 503 PAYMENT_PROVIDER_NOT_CONFIGURED sampai adapter gateway dipilih/diimplementasikan. Tidak mengubah order menjadi PAID.'],
  '/admin/orders/{id}/refunds':['Catat permintaan refund','202 PENDING_PROVIDER hanya mencatat kasus; belum memindahkan uang, merevoke grant atau menandai REFUNDED. Hanya order PAID; jumlah kumulatif reserved/succeeded tidak melebihi nilai order. Key scoped ke order, replay nominal sama mengembalikan request lama.'],
  '/admin/subscriptions':['Daftar langganan dan grant','Filter userId/status/periode; grant menyertakan sumber order dan waktu revoke. Tidak menyediakan pemberian Premium tanpa pembayaran terverifikasi.'],
  '/admin/subscriptions/{id}/revoke':['Cabut hak langganan','Mencabut seluruh grant dari subscription dan memberi status REVOKED, dengan alasan/audit. Ini tindakan akses, bukan refund keuangan.'],
  '/admin/jobs':['Daftar job tersensor','Metadata antrean/attempt/error code dan lease; tanpa payload, seed atau path storage. Status/jenis/UUID dan periode bisa difilter.'],
  '/admin/jobs/{id}/retry':['Retry job gagal','Hanya FAILED dan jenis IMPORT_PDF/SCORE_ATTEMPT. Reset attempts/lease dan antrekan ID yang sama; PROCESSING/QUEUED/SUCCEEDED ditolak. Bukan regrade hasil selesai.'],
  '/admin/audit':['Audit operasional','Pagination append-only pada aplikasi; status memfilter entity_type, userId memfilter actor, search memfilter action/UUID objek/requestId, from/to membatasi occurred_at. Tidak ada endpoint edit/hapus audit.'],
  '/admin/taxonomy':['Hierarki kategori lengkap','Kategori, jenis ujian, subtes dan topik, termasuk nonaktif. Admin melihat metadata, bukan isi/kunci soal.'],
  '/admin/categories':['Buat kategori','Kode stabil, nama dan status aktif.'], '/admin/categories/{id}':['Perbarui kategori','Nama/status dapat berubah; kode stabil tidak diganti.'],
  '/admin/exam-types':['Buat jenis ujian','Parent kategori harus ada.'], '/admin/exam-types/{id}':['Perbarui jenis ujian','Parent dan kode immutable; buat master baru untuk pindah lingkup.'],
  '/admin/subtests':['Buat subtes','Parent jenis ujian harus ada.'], '/admin/subtests/{id}':['Perbarui subtes','Parent dan kode immutable untuk mencegah pemindahan bank soal lintas ujian.'],
  '/admin/topics':['Buat topik','Parent subtes harus ada.'], '/admin/topics/{id}':['Perbarui topik','Parent immutable; nonaktifkan tanpa menghapus soal dan sesi lama.'],
  '/admin/plans':['Daftar atau buat keluarga paket','GET pagination/pencarian nama/family code. POST membuat revisi pertama; harga minor unit integer dan manfaat eksplisit.'],
  '/admin/plans/{id}':['Detail revisi harga','Harga/manfaat/periode dari ID revisi tertentu.'], '/admin/plans/{id}/versions':['Buat revisi harga baru','Harus berasal dari revisi terakhir dan family code sama. Versi lama nonaktif untuk penjualan; field harga/manfaat lama tidak ditimpa.'],
  '/admin/plans/{id}/availability':['Buka atau hentikan penjualan','Hanya revisi terakhir dapat diaktifkan; tidak mengubah snapshot order/akses yang sudah dibeli.'],
  '/admin/scoring-policies':['Daftar atau buat policy skor','POST code yang sama membentuk version berikutnya. Hanya BINARY/OPTION_WEIGHT, SUM/HALF_UP dan aturan yang didukung kalkulator; policy yang dirujuk tidak ditimpa.'],
  '/admin/blueprints':['Daftar atau buat paket tryout','Hanya mode TRYOUT; blueprint internal latihan tidak dikelola melalui endpoint admin ini.'],
  '/admin/blueprints/{id}':['Detail blueprint dan versinya','Semua versi dan section; ready bersifat stok global, bukan jaminan pool personal setelah pengecualian hari yang sama.'],
  '/admin/blueprints/{id}/versions':['Buat komposisi draft baru','Durasi 60–86400 detik, policy aktif dan section dalam jenis ujian yang sama. Satu section per subtes mencegah overlap. Filter topik/tahun eksplisit. Tidak mengubah versi terbit.'],
  '/admin/blueprints/{id}/versions/{versionId}/readiness':['Cek kebutuhan pool','Stok soal terbit/nonarsip, scope aktif dan opsi/kunci/bobot diperiksa. Tidak mengirim isi soal atau kunci.'],
  '/admin/blueprints/{id}/versions/{versionId}/publish':['Terbitkan versi blueprint','Pool kurang ditolak. expectedActiveVersionId=null untuk publikasi pertama; sesudahnya kirim active version terakhir agar konflik penulisan terdeteksi. Versi/sesi lama tetap immutable.'],
  '/admin/blueprints/{id}/status':['Aktifkan atau arsip katalog','Reaktivasi memerlukan active version dan stok cukup. Archive menghentikan start baru; sesi/history lama tetap dapat dibaca.'],
  '/staff/dashboard':['Ringkasan bank soal','Jumlah soal published/draft/arsip dan impor. Tidak menyertakan akun customer/transaksi.'],
  '/staff/question-bank':['Cari master dengan pagination','Filter search, categoryId, examTypeId, subtestId, topicId, sourceYear, status, batchId. Default 10 per halaman; total dan ringkasan versi terbaru. Versi terbit lama masih bisa dipakai walau versi terbaru draft.'],
  '/staff/questions/{id}/versions':['Riwayat versi master','Metadata versi/revisi/editor/timestamp, terbaru dahulu.'],
  '/staff/questions/{id}/versions/{versionId}':['Baca versi soal tertentu','Versi wajib milik question yang dituju; konten/kunci/pembahasan historis untuk petugas saja.'],
  '/staff/questions/{id}/draft':['Baca atau simpan draft','GET latest question + editor revision. PUT hanya DRAFT nonarsip; expectedVersionId dan expectedRevision wajib cocok. Opsi diganti atomik, revision naik sekali; konflik 409 REVISION_CONFLICT.'],
  '/staff/import-history':['Cari riwayat unggahan','Pagination total dengan search nama berkas/sumber/UUID, subtestId, uploaderId dan status. Seluruh ruang konten sesuai izin petugas.'],
  '/staff/imports/{id}/retry':['Retry impor gagal','Hanya job FAILED setelah retry otomatis habis. Memakai job/batch/file yang sama tanpa duplikasi. Belum mendukung retry per halaman/lampiran kunci terpisah.'],
  '/staff/imports/{id}/save':['Simpan kandidat terpilih sebagai draft','candidateIds maksimal 100, satu batch, transaksi semua atau rollback. Replay kandidat tersimpan mengembalikan master sama. Tidak otomatis publish.'],
  '/staff/audit':['Audit ruang konten','Hanya entity_type CONTENT; tidak mengirim audit pengguna atau keuangan.'],
  '/staff/pool-requirements':['Kebutuhan stok blueprint','Ringkasan section versi terakhir tiap blueprint nonarsip, filter topik/tahun dan jumlah terbit. Ini indikator pasokan; readiness admin melakukan pemeriksaan opsi/scope lebih lengkap.']
 };
 for(const [path,methods]of Object.entries(raw.paths))for(const method of ['get','post','put','patch','delete'])if(methods[method]){
  const route=path.replace('/api/v1',''),definition=definitions[route];if(!definition)continue;
  if(existing.some(endpoint=>endpoint.path===path&&endpoint.method===method))continue;
  const op=methods[method],request=op.requestBody?.content?.['application/json']?.schema?.$ref?.split('/').at(-1);
  const status=Number(Object.keys(op.responses).find(s=>s.startsWith('2'))??200);
  const responseSchema=Object.values(op.responses[status]?.content??{})[0]?.schema;
  const array=responseSchema?.type==='array';const response=(array?responseSchema.items:responseSchema)?.$ref?.split('/').at(-1);
  const extra={status,request,response,array,notes:['Mutasi memerlukan CSRF dari cookie session yang sama. Respons list baru memakai {items,total,offset,limit}; list legacy tetap kompatibel.']};
  if(route.endsWith('/refunds'))extra.idempotency=true;
  if(route.endsWith('/reconcile')){extra.status=503;extra.response='ApiError';extra.example={code:'PAYMENT_PROVIDER_NOT_CONFIGURED',message:'Gateway reconciliation is unavailable until a payment provider is configured; order status was not changed',fieldErrors:{},requestId:'example-request-id'};}
  add(method.toUpperCase(),path,definition[0],route.startsWith('/admin/')?'Admin platform':'Petugas soal lanjutan',route.startsWith('/admin/')?'admin':'staff',extra.response,definition[1],extra);
 }
}
