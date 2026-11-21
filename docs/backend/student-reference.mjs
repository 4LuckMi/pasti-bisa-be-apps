// Student screen contracts, kept in the same reviewed inventory as existing APIs.
export function extendStudentReference({schemas,examples:e,add,existing}) {
 const id=n=>'00000000-0000-4000-8000-'+String(n).padStart(12,'0'),at='2026-11-21T07:00:00Z';
 Object.assign(e,{
  Processing:{state:'READY',poll:false,retryAfterSeconds:0,errorCode:null},
  PrivacyRequest:{leaderboardOptIn:true},PrivacyResponse:{leaderboardOptIn:true,updatedAt:at},
  Benefits:{fullLeaderboard:true},
  Plan:{id:id(60),code:'PREMIUM_MONTH_R1',familyCode:'PREMIUM_MONTH',revision:1,name:'Premium 1 bulan',months:1,amountMinor:39000,currency:'IDR',benefits:{fullLeaderboard:true}},
  MembershipPlan:{id:id(60),code:'PREMIUM_MONTH_R1',name:'Premium 1 bulan',months:1,amountMinor:39000,currency:'IDR'},
  ScorePoint:{attemptId:id(6),completedAt:at,score:80},
  HistorySummary:{total:10,completed:8,inProgress:1,scoring:1,scoringFailed:0},
  HistoryEntry:{attemptId:id(6),tryoutId:id(2),title:'Tryout UTBK Contoh',category:'UTBK',mode:'TRYOUT',status:'COMPLETED',terminalReason:'SUBMITTED',topicId:null,topicName:null,startedAt:'2026-11-21T06:50:00Z',finalizedAt:at,totalScore:80,questionCount:3,durationSeconds:5400},
  TryoutCard:{id:id(2),code:'DEMO_TRYOUT',title:'Tryout UTBK Contoh',category:'UTBK',examTypeId:id(25),examType:'TPS',blueprintVersionId:id(3),durationSeconds:5400,questionCount:3,ready:true,attemptCount:2,latestScore:80,latestCompletedAt:at},
  TopicCard:{id:id(5),topic:'Aritmetika',subtestId:id(4),subtestCode:'NUMERIK',subtest:'Penalaran Numerik',examTypeId:id(25),examType:'TPS',category:'UTBK',questionCount:12,ready:true},
  Cohort:{id:id(61),title:'Tryout UTBK Contoh',category:'UTBK',examTypeId:id(25),formId:id(62),scoringPolicyId:id(70),periodStart:'2026-11-01T00:00:00Z',periodEnd:'2026-12-01T00:00:00Z',attemptPolicy:'BEST',ended:false},
  RankingEntry:{rank:1,displayName:'Siswa Contoh',totalScore:80,targetKind:'CAMPUS',targetInstitution:'Universitas Contoh',targetProgram:'Informatika',currentUser:true}
 });
 e.HistoryEntry.processing=e.Processing;
 e.Membership={tier:'PREMIUM',startsAt:'2026-11-01T00:00:00Z',endsAt:'2026-12-01T00:00:00Z',plan:e.MembershipPlan,benefits:e.Benefits,leaderboardPreviewLimit:3,dailyTryoutLimit:2};
 e.HistoryPage={items:[e.HistoryEntry],total:10,offset:0,limit:6,summary:e.HistorySummary};
 e.ScoreGroup={examTypeId:id(25),examType:'TPS',policyVersion:'DEMO_BINARY:1',count:8,average:80,trend:[e.ScorePoint]};
 e.StudentDashboard={category:'UTBK',mode:'TRYOUT',totalTryouts:10,totalPractices:0,completed:8,elapsedSeconds:3600,currentStreakDays:1,scoreGroups:[e.ScoreGroup],recentActivity:[e.HistoryEntry],quota:e.QuotaResponse,membership:e.Membership};
 e.Rankings={cohort:e.Cohort,accessTier:'FULL',leaderboardOptIn:true,totalParticipants:1,accessibleTotal:1,offset:0,limit:20,items:[e.RankingEntry],ownRank:e.RankingEntry};
 e.ResultResponse.processing=e.Processing;
 const nullable={Processing:['errorCode'],PrivacyResponse:['updatedAt'],Membership:['startsAt','endsAt','plan'],
  StudentDashboard:['category','mode'],HistoryEntry:['terminalReason','topicId','topicName','finalizedAt','totalScore'],
  TryoutCard:['latestScore','latestCompletedAt'],RankingEntry:['targetKind','targetInstitution','targetProgram'],Rankings:['ownRank'],
  UpdateStudentProfileRequest:['schoolName','schoolGrade','targetCategory','targetInstitution','targetProgram','targetScore']};
 for(const [name,fields]of Object.entries(nullable))for(const field of fields){const s=schemas[name]?.properties?.[field];if(!s)throw Error('Missing student schema '+name+'.'+field);
  if(s.$ref)schemas[name].properties[field]={anyOf:[s,{type:'null'}]};else s.type=[...new Set([...(Array.isArray(s.type)?s.type:[s.type??'object']),'null'])];}
 schemas.UpdateStudentProfileRequest.required=[];
 const group='Pengalaman siswa terbaru';
 const definitions=[
  ['/users/current/dashboard','Dashboard sesuai kategori','StudentDashboard','Filter category dan mode. Menghitung seluruh sesi milik akun, bukan 100 baris pertama. scoreGroups memisahkan examType dan policyVersion; jangan menggabungkan rerata kelompok berbeda. Trend maksimal 12 per kelompok, urut waktu naik. elapsedSeconds adalah wall time sesi yang dibatasi deadline dan waktu server, bukan waktu interaksi aktif. Streak memakai tanggal penyelesaian WIB; skor/penyelesaian mengecualikan abandon.'],
  ['/users/current/learning-history','Riwayat berhalaman','HistoryPage','Filter category, mode, status, search, tryoutId atau topicId; from/to tanggal WIB inklusif menurut startedAt. period=WEEK berarti hari ini dan enam hari sebelumnya; MONTH berarti awal bulan sampai hari ini. period tidak dapat digabung dengan rentang tanggal. total dan summary dihitung pada seluruh hasil filter, offset default 0, limit default 6 maksimal 100.'],
  ['/users/current/membership','Ringkasan hak premium','Membership','FREE/PREMIUM berasal dari grant aktif yang belum dicabut dan order PAID. fullLeaderboard memakai benefits_snapshot, bukan konfigurasi paket saat ini. plan name merupakan label versi paket; harga, kode dan months dari snapshot order. Menonaktifkan penjualan paket tidak mencabut hak yang sudah dibeli.'],
  ['/plans','Daftar paket aktif','PageResponsePlan','Harga integer amountMinor dan currency dari database. Hanya versi paket active. Default limit 12 maksimal 100. Tidak membuat checkout atau pembayaran.'],
  ['/plans/{id}','Detail versi paket aktif','Plan','ID versi paket aktif; paket nonaktif/tidak ditemukan mendapat 404. Gunakan harga dan mata uang dari server.'],
  ['/catalog/tryouts','Katalog tryout berhalaman','PageResponseTryoutCard','Filter category, examTypeId, subtestId, search dan ready. sort NEWEST (default) atau TITLE; default limit 12 maksimal 100. Stok readiness mengikuti versi soal PUBLISHED terbaru dan taksonomi aktif; start tetap memeriksa stock/exposure personal. Tidak mengarang editionYear dari sourceYear soal.'],
  ['/catalog/practice-topics','Katalog topik berhalaman','PageResponseTopicCard','Filter category, examTypeId, subtestId, search dan ready. Urutan TITLE; NEWEST ditolak. Default limit 12 maksimal 100. ready berarti stok global >0; batas/jumlah personal tetap diperiksa saat start.'],
  ['/leaderboards/cohorts','Pilih kelompok leaderboard','PageResponseCohort','Filter category, examTypeId dan includeEnded (default false). Hanya cohort ACTIVE yang sudah dimulai; calon cohort masa depan tidak tampil. Satu cohort menunjuk tepat satu form/policy/periode/attemptPolicy. Tidak membuat cohort atau entry otomatis.'],
  ['/leaderboards/cohorts/{id}/entries','Peringkat satu cohort','Rankings','Preview gratis maksimal tiga baris pertama; offset tidak dapat melewati batas itu. ownRank tersedia terpisah bila pengguna sudah masuk entry dan menyetujui publikasi. Hak fullLeaderboard membuka pagination dan filter targetKind/targetInstitution/targetProgram (persamaan persis). Filter target tanpa hak mendapat 403 PREMIUM_REQUIRED. Rank tetap peringkat cohort ketika difilter, bukan dihitung ulang. totalParticipants mengikuti filter; accessibleTotal dibatasi 3 pada preview. Akun suspended/opt-out dan hasil abandon tidak tampil. Tidak membocorkan email/ID akun.'],
  ['/users/current/privacy','Baca persetujuan leaderboard','PrivacyResponse','Default opt-out. updatedAt null jika belum pernah mengubah persetujuan.']
 ];
 for(const [url,title,response,description]of definitions)add('GET','/api/v1'+url,title,group,'customer',response,description,{errors:['400 INVALID_FILTER']});
 add('PATCH','/api/v1/users/current/privacy','Ubah persetujuan leaderboard',group,'customer','PrivacyResponse','Body eksplisit leaderboardOptIn wajib. Persetujuan/waktu dan audit request dicatat atomik. Opt-out langsung menyembunyikan entry lama; opt-in tidak otomatis membuat ranking.',{request:'PrivacyRequest'});
 const profile=existing.find(x=>x.method==='patch'&&x.path==='/api/v1/users/current/profile');
 profile.description='Partial PATCH: field yang tidak dikirim dipertahankan. Explicit null menghapus field opsional. Perubahan diserialisasi dengan lock akun.';
 profile.errors=['400 VALIDATION_FAILED dengan fieldErrors'];profile.notes=[
  'displayName jika dikirim harus non-null dan minimal dua karakter setelah trim. targetCategory: UTBK→CAMPUS, CPNS→CIVIL_SERVICE, KEDINASAN→ACADEMY, SEKOLAH→SCHOOL.',
  'Target terpilih memerlukan institusi. Sekolah/kelas wajib untuk target UTBK/SEKOLAH; opsional untuk CPNS/KEDINASAN. Ganti kategori harus menyertakan institusi baru. targetCategory:null menghapus target; targetScore dapat dihapus terpisah dengan null.',
  'leaderboardOptIn hanya lewat endpoint privacy; email, roles dan userType tidak bisa diedit lewat profil.'
 ];
 const result=existing.find(x=>x.path.endsWith('/result'));
 result.notes=['totalScore/method/policyVersion/details null sebelum siap; jangan dianggap nol. processing.state=NOT_STARTED/QUEUED/PROCESSING/RETRY_WAIT/FAILED/READY.',
  'Poll hanya ketika processing.poll=true, dengan jeda minimal retryAfterSeconds. FAILED menghentikan polling; tampilkan pesan dukungan dan requestId. Admin dapat retry job FAILED. Attempt tetap FINALIZING agar snapshot dan satu sesi aktif aman; belum ada cancel/regrade siswa.'];
 const legacy=existing.find(x=>x.path==='/api/v1/leaderboard');legacy.auth='customer';legacy.description='Array legacy dari satu cohort aktif terbaru sesuai kategori. Gratis dibatasi tiga; fullLeaderboard maksimal 100. Gunakan API cohort untuk pilihan periode, pagination, filter dan ownRank.';legacy.notes=[];
}
