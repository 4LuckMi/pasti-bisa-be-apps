export function extendWorkbookReference({schemas,examples:e,add,existing}){
 const id=n=>'00000000-0000-4000-8000-'+String(n).padStart(12,'0'),reason='Review sumber dan unggahan soal contoh';
 Object.assign(e,{
  SourceRequest:{examTypeId:id(25),code:'UTBK_2025_A',sourceYear:2025,title:'Sumber UTBK 2025 paket A',reason},
  SourceStatus:{status:'ARCHIVED',reason},LinkSourceQuestion:{questionId:id(7),sourceItemCode:'PU_Q001',reason},
  QuestionSource:{id:id(100),examTypeId:id(25),code:'UTBK_2025_A',sourceYear:2025,title:'Sumber UTBK 2025 paket A',status:'ACTIVE',questionCount:100,publishedCount:80},
  SourceItem:{questionId:id(7),subtestId:id(4),sourceItemCode:'PU_Q001',latestVersionId:id(8),status:'PUBLISHED',difficulty:'MEDIUM',archived:false},
  SheetMapping:{sheetName:'NUMERIK',subtestId:id(4)},
  WorkbookSheet:{id:id(102),sheetName:'NUMERIK',subtestId:id(4),sheetIndex:0,status:'READY',candidateCount:2,invalidCount:0},
  DifficultyAllocation:{difficulty:'EASY',count:1},
  SaveWorkbookCandidates:{candidateIds:[id(103)],reason}
 });
 e.WorkbookMetadata={questionSetId:id(100),sheets:[e.SheetMapping]};
 e.WorkbookImport={id:id(101),questionSetId:id(100),fileId:id(21),inputFormat:'XLSX',status:'READY',candidateCount:2,validCount:2,invalidCount:0,errorCode:null,jobStatus:'SUCCEEDED',createdAt:'2026-11-21T07:00:00Z',sheets:[e.WorkbookSheet]};
 e.ExcelCandidate={id:id(103),sheetId:id(102),sheetName:'NUMERIK',subtestId:id(4),sourceRow:2,sourceItemCode:'PU_Q001',topicId:null,difficulty:'MEDIUM',extractedContent:e.QuestionContent,draftContent:null,validationErrors:[],expectedVersionId:null,resolvedQuestionId:null,resolvedVersionId:null};
 e.CandidateCorrection={sourceItemCode:'PU_Q001',topicId:null,difficulty:'MEDIUM',content:e.QuestionContent};
 for(const [schema,item]of Object.entries({PageResponseQuestionSource:'QuestionSource',PageResponseSourceItem:'SourceItem',PageResponseWorkbookImport:'WorkbookImport',PageResponseExcelCandidate:'ExcelCandidate'}))
  e[schema]={items:[e[item]],total:1,offset:0,limit:20};
 Object.assign(e.SectionRequest,{questionSetIds:[id(100)],sourceYear:null,difficultyQuotas:[{difficulty:'EASY',count:1},{difficulty:'MEDIUM',count:2}],repeatPolicy:'UNSEEN_TODAY'});
 e.BlueprintVersionRequest.sections=[e.SectionRequest];
 Object.assign(e.BlueprintSection,{questionSetIds:[id(100)],sourceYear:null,difficultyQuotas:[{difficulty:'EASY',count:1},{difficulty:'MEDIUM',count:2}],repeatPolicy:'UNSEEN_TODAY'});
 e.BlueprintVersion.sections=[e.BlueprintSection];if(e.BlueprintDetail)e.BlueprintDetail.versions=[e.BlueprintVersion];
 const nullable={SourceItem:['difficulty'],WorkbookSheet:['sheetIndex'],WorkbookImport:['errorCode','jobStatus'],ExcelCandidate:['topicId','difficulty','draftContent','expectedVersionId','resolvedQuestionId','resolvedVersionId'],CandidateCorrection:['topicId'],BlueprintSection:['repeatPolicy'],SectionRequest:['topicId','sourceYear','repeatPolicy']};
 for(const [name,fields]of Object.entries(nullable))for(const field of fields){const s=schemas[name]?.properties?.[field];if(!s)throw Error('Missing workbook field '+name+'.'+field);if(s.$ref)schemas[name].properties[field]={anyOf:[s,{type:'null'}]};else s.type=[...new Set([...(Array.isArray(s.type)?s.type:[s.type??'object']),'null'])];}
 const group='Sumber soal dan Excel';
 const definitions=[
  ['get','/staff/question-sets','Daftar sumber soal','PageResponseQuestionSource','Filter examTypeId, sourceYear, status, search, offset dan limit 1..100 (default 20). publishedCount menghitung master dengan versi terbit yang memenuhi kelengkapan pool, bukan jumlah seluruh revisi.'],
  ['post','/staff/question-sets','Buat sumber soal','QuestionSource','Identitas jenis ujian, kode dan tahun immutable. Upload koreksi menggunakan source ID yang sama. Kode global unik; tidak membuat tabel baru per tahun/sheet.','SourceRequest',201],
  ['get','/staff/question-sets/{id}','Detail sumber soal','QuestionSource','Metadata sumber dan stok. sourceYear adalah tahun sumber, berbeda dari edisi katalog tryout.'],
  ['patch','/staff/question-sets/{id}/status','Arsip atau aktifkan sumber','QuestionSource','Arsip menghentikan pemilihan dari blueprint yang menunjuk sumber tersebut. Tidak mengubah master soal atau snapshot sesi lama.','SourceStatus'],
  ['get','/staff/question-sets/{id}/items','Anggota sumber soal','PageResponseSourceItem','Daftar master, kode asal, subtes, versi terbaru/difficulty/status dan flag arsip. Versi terbaru dapat DRAFT sementara versi PUBLISHED sebelumnya masih tersedia di pool.'],
  ['post','/staff/question-sets/{id}/items','Hubungkan master yang sudah ada','QuestionSource','Relasi many-to-many tanpa menggandakan isi soal. Subtes master wajib sesuai exam type sumber. Kode asal unik per sumber/subtes; pasangan master yang sama dengan kode sama idempoten. Kode yang sudah dipakai tidak dapat diganti.','LinkSourceQuestion'],
  ['get','/staff/question-sets/{id}/template-mapping','Pemetaan sheet template','SheetMapping','Array sheetName dan subtestId aktif untuk metadata upload. Nama berasal dari kode subtes, dibatasi 31 karakter; benturan nama ditolak.'],
  ['get','/staff/question-sets/{id}/template','Unduh template XLSX',null,'Workbook berisi header, pilihan difficulty/kunci dan sheet untuk subtes aktif. Isi teks sesuai panduan. Hapus sheet kosong dan pemetaannya jika hanya mengunggah sebagian subtes.'],
  ['post','/staff/workbook-imports','Upload Excel per sheet','WorkbookImport','Multipart file XLSX dan metadata JSON WorkbookMetadata. File disimpan privat dan job IMPORT_XLSX diproses worker. 202 bukan jaminan workbook valid. Dedup berdasarkan actor, checksum file, source dan pemetaan sheet; replay mengembalikan batch lama. Format/header/sheet invalid gagal dengan kode aman; row error tetap tersedia untuk review.'],
  ['get','/staff/workbook-imports','Daftar impor Excel','PageResponseWorkbookImport','Filter questionSetId/status dan pagination offset/limit. Tidak menyertakan isi/kunci soal.'],
  ['get','/staff/workbook-imports/{id}','Detail hasil impor Excel','WorkbookImport','Status batch/job, hitungan valid-invalid, dan status tiap sheet. READY berarti siap direview/disimpan, bukan otomatis published.'],
  ['get','/staff/workbook-imports/{id}/candidates','Preview kandidat Excel','PageResponseExcelCandidate','Filter sheetId opsional, pagination 1..100. sourceRow mulai 2; sourceItemCode adalah identitas stabil pada sumber/subtes. expectedVersionId dibekukan saat ekstraksi; resolvedVersionId merekam hasil simpan.'],
  ['patch','/staff/workbook-imports/{id}/candidates/{candidateId}','Koreksi kandidat Excel','ExcelCandidate','Memperbaiki kode/topik/difficulty/konten sebelum simpan. Topik harus aktif dan sesuai subtes sheet. Kandidat tersimpan tidak dapat diedit. Mengubah konten saja tidak memperbarui expectedVersionId untuk melewati konflik.','CandidateCorrection'],
  ['post','/staff/workbook-imports/{id}/save','Simpan kandidat Excel terpilih','StaffQuestionResponse','Maksimal 100 ID unik satu batch, semua atau rollback. Kode baru membuat master/DRAFT; perubahan pada soal terbit membuat revisi DRAFT. Konten sama termasuk bobot numerik yang setara menggunakan versi sama. Revisi hanya memakai baseline PUBLISHED yang immutable; jika master masih DRAFT saat ekstraksi, selesaikan draft lalu unggah ulang. Replay kandidat mengembalikan resolvedVersionId yang sama, bukan revisi terbaru. Tidak otomatis publish.','SaveWorkbookCandidates'],
  ['get','/admin/question-sets','Daftar sumber untuk komposisi','PageResponseQuestionSource','Admin membaca metadata/stok sumber untuk menyusun blueprint; tidak mendapat akses file, kandidat atau kunci soal.'],
  ['get','/admin/question-sets/{id}','Detail metadata sumber untuk admin','QuestionSource','Metadata jenis ujian/tahun dan hitungan soal, tanpa konten/kunci.']
 ];
 for(const [method,path,title,response,description,request,status]of definitions){
  const extra={request,status:status??200,errors:['400 VALIDATION_FAILED','409 INVALID_CONTENT_SCOPE / CANDIDATE_STALE / SOURCE_ARCHIVED']};
  if(path.endsWith('/template-mapping'))extra.array=true;
  if(path.endsWith('/save'))extra.array=true;
  if(path.endsWith('/template'))Object.assign(extra,{binary:true,mime:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'});
  if(path==='/staff/workbook-imports'&&method==='post')Object.assign(extra,{status:202,multipart:true,metadataSchema:'WorkbookMetadata',uploadName:'questions.xlsx',uploadMime:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',errors:['400 XLSX_REQUIRED / INVALID_XLSX_SIGNATURE','409 INVALID_CONTENT_SCOPE','413 FILE_TOO_LARGE']});
  add(method,'/api/v1'+path,title,group,path.startsWith('/admin')?'admin':'staff',response,description,extra);
 }
 const version=existing.find(x=>x.method==='post'&&x.path==='/api/v1/admin/blueprints/{id}/versions');
 version.description+=' questionSetIds membatasi pool ke kumpulan sumber aktif dari examType yang sama; kosong mempertahankan pool legacy. Jangan gabungkan dengan sourceYear. difficultyQuotas berisi EASY/MEDIUM/HARD unik dengan jumlah tepat questionCount. repeatPolicy=UNSEEN_TODAY (default tryout), NEVER_SEEN, atau RECYCLE_OLDEST. Tidak menurunkan jumlah/komposisi saat stok personal habis: 409 POOL_NOT_READY tanpa kuota terpakai.';
 const sourceFile=existing.find(x=>x.path==='/api/v1/staff/imports/{id}/source');sourceFile.mime='application/octet-stream';sourceFile.description='Unduh sumber privat PDF atau XLSX. Content-Type dan nama ekstensi mengikuti format; tetap hanya QUESTION_STAFF.';
 for(const ep of existing.filter(x=>x.path.endsWith('/retry')))ep.description=ep.description.replace('IMPORT_PDF/SCORE_ATTEMPT','IMPORT_PDF/IMPORT_XLSX/SCORE_ATTEMPT');
}
